package com.buldreinfo.jersey.jaxb.batch;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class FixMedia {
	public static class Movie {
		private final int idUploaderUserId = 1;
		private final Path path;
		private int idPhotographerUserId;
		private final Map<Integer, Long> idProblemMsMap = new LinkedHashMap<>();
		private List<Integer> inPhoto = new ArrayList<>();
		public Movie(Path path) {
			this.path = path;
		}
		public Movie withIdPhotographerUserId(int id) {
			this.idPhotographerUserId = id;
			return this;
		}
		public Movie withInPhoto(int userId) {
			this.inPhoto.add(userId);
			return this;
		}
		public Movie withProblem(int idProblem, long ms) {
			this.idProblemMsMap.put(idProblem, ms);
			return this;
		}
	}
	private record MediaTask(int id, String embedUrl) {}
	private static Logger logger = LogManager.getLogger();
	private final static String LOCAL_BUCKET_ROOT = "G:/My Drive/web/buldreinfo/s3_bucket_climbing_web";
	private final static String LOCAL_FFMPEG_PATH = "G:/My Drive/web/buldreinfo/sw/ffmpeg-master-latest-win64-gpl-shared/bin/ffmpeg.exe";
	private final static String LOCAL_YT_DLP_PATH = "G:/My Drive/web/buldreinfo/sw/yt-dlp/yt-dlp.exe";
	private final static List<Integer> PRIVATE_EMBEDDED_VIDEOS_TO_IGNORE = List.of(36365,36368,36370,36374,36379,36380,36381,36383,36388,38412,39003);
	public static void main(String[] args) {
		Preconditions.checkArgument(Files.exists(Path.of(LOCAL_BUCKET_ROOT)), LOCAL_BUCKET_ROOT + " does not exist");
		Preconditions.checkArgument(Files.exists(Path.of(LOCAL_FFMPEG_PATH)), LOCAL_FFMPEG_PATH + " does not exist");
		Preconditions.checkArgument(Files.exists(Path.of(LOCAL_YT_DLP_PATH)), LOCAL_YT_DLP_PATH + " does not exist");
		new FixMedia();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(12);
	private final List<String> warnings = new ArrayList<>();

	private FixMedia() {
		List<MediaTask> tasks = new ArrayList<>();
		Server.runSql((dao, c) -> {
			c.setAutoCommit(true);
			List<Movie> movies = new ArrayList<>();
			// movies.add(new Movie(Path.of("")).withProblem(, 0l).withIdPhotographerUserId().withInPhoto()); // TODO
			List<Integer> newIdMedia = Lists.newArrayList();
			for (Movie m : movies) {
				newIdMedia.add(addMovie(c, m.path, m.idPhotographerUserId, m.idUploaderUserId, m.idProblemMsMap, m.inPhoto));
				for (int idProblem : m.idProblemMsMap.keySet()) {
					dao.fillActivity(c, idProblem);
				}
			}
			String sqlStr = "SELECT id, embed_url FROM media WHERE is_movie=1";
			if (!newIdMedia.isEmpty()) {
				sqlStr += " AND id IN (" + newIdMedia.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
			}
			try (PreparedStatement ps = c.prepareStatement(sqlStr);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					tasks.add(new MediaTask(rst.getInt("id"), rst.getString("embed_url")));
				}
			}
		});
		for (MediaTask task : tasks) {
			executor.submit(() -> {
				try {
					processTask(task);
				} catch (Exception e) {
					warnings.add("Error processing id=" + task.id() + ": " + e.getMessage());
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			logger.error("Executor interrupted", e);
			Thread.currentThread().interrupt();
		}
		for (String w : warnings) {
			logger.warn(w);
		}
		logger.debug("Done");
	}

	private int addMovie(Connection c, Path src, int idPhotographerUserId, int idUploaderUserId, Map<Integer, Long> idProblemMsMap, List<Integer> inPhoto) throws SQLException, IOException {
		Preconditions.checkArgument(Files.exists(src), "Source file " + src + " not found.");
		int idMedia = 0;
		String suffix = com.google.common.io.Files.getFileExtension(src.getFileName().toString()).toLowerCase();
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created) VALUES (1, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, suffix);
			ps.setInt(2, idPhotographerUserId);
			ps.setInt(3, idUploaderUserId);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					idMedia = rst.getInt(1);
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_problem (media_id, problem_id, milliseconds) VALUES (?, ?, ?)")) {
			for (Map.Entry<Integer, Long> entry : idProblemMsMap.entrySet()) {
				ps.setInt(1, idMedia);
				ps.setInt(2, entry.getKey());
				ps.setLong(3, entry.getValue());
				ps.addBatch();
			}
			ps.executeBatch();
		}
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
			for (int idUser : inPhoto) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idUser);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		Path dst = getLocalPath(S3KeyGenerator.getOriginalMp4(idMedia));
		Files.createDirectories(dst.getParent());
		Files.copy(src, dst);
		logger.info("New movie added to DB and copied to local drive with idMedia={} and dst={}", idMedia, dst);
		return idMedia;
	}

	private Path getLocalPath(String s3Key) {
		return Paths.get(LOCAL_BUCKET_ROOT, s3Key);
	}

	private void processTask(MediaTask task) throws Exception {
		int id = task.id();
		String embedUrl = task.embedUrl();
		Path originalMp4 = getLocalPath(S3KeyGenerator.getOriginalMp4(id));
		Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(id));
		Path webm = getLocalPath(S3KeyGenerator.getWebWebm(id));
		Path mp4 = getLocalPath(S3KeyGenerator.getWebMp4(id));
		if (embedUrl != null) {
			// OriginalMp4
			if (!PRIVATE_EMBEDDED_VIDEOS_TO_IGNORE.contains(id)) {
				if (!Files.exists(originalMp4)) {
					logger.info("Downloading embed video with id={} to {}", id, originalMp4);
					Files.createDirectories(originalMp4.getParent());
					String[] commands = {
						    LOCAL_YT_DLP_PATH, 
						    "--ffmpeg-location", LOCAL_FFMPEG_PATH, 
						    embedUrl, 
						    "-S", "ext:mp4:m4a", 
						    "--merge-output-format", "mp4", 
						    "-o", originalMp4.toString()
						};
					new ProcessBuilder().inheritIO().command(commands).start().waitFor();
				}
				if (!Files.exists(originalMp4)) {
					warnings.add("Failed to download embedded video with id=" + id + " to originalMp4=" + originalMp4 + " from " + embedUrl);
				}
			}
			// Thumbnail
			if (!Files.exists(originalJpg)) {
				Server.runSql((dao, c) -> ImageHelper.saveImageFromEmbedVideo(dao, c, id, embedUrl));
			}
			if (!Files.exists(originalJpg)) {
				warnings.add("Failed to download embedded video thumbnail with id=" + id + " to originalJpg=" + originalJpg);
			}
			// We don't want to create scaled versions of embedded videos, return
			return;
		}
		if (!Files.exists(webm) || Files.size(webm) == 0) {
			Preconditions.checkArgument(Files.exists(originalMp4), "Original MP4 missing: id=" + id);
			logger.info("Generating WebM for id={} to {}", id, webm);
			Files.createDirectories(webm.getParent());
			String[] cmd = {LOCAL_FFMPEG_PATH, "-y", "-nostdin", "-i", originalMp4.toString(), 
	                "-codec:v", "libvpx", "-quality", "good", "-cpu-used", "0", 
	                "-b:v", "500k", "-qmin", "10", "-qmax", "42", "-maxrate", "500k", 
	                "-bufsize", "1000k", "-threads", "4", "-vf", "scale=-1:1080", 
	                "-codec:a", "libvorbis", "-b:a", "128k", "-ar", "44100", webm.toString()};
			new ProcessBuilder().inheritIO().command(cmd).start().waitFor();
		}

		if (!Files.exists(mp4) || Files.size(mp4) == 0) {
			Preconditions.checkArgument(Files.exists(webm) && Files.size(webm) > 0, "WebM missing or empty: id=" + id);
			logger.info("Generating WebMp4 for id={} to {}", id, mp4);
			Files.createDirectories(mp4.getParent());
			String[] cmd = {LOCAL_FFMPEG_PATH, "-y", "-nostdin", "-i", webm.toString(), "-vf", "crop=((in_w/2)*2):((in_h/2)*2)", mp4.toString()};
			new ProcessBuilder().inheritIO().command(cmd).start().waitFor();
		}

		if (!Files.exists(originalJpg) || Files.size(originalJpg) == 0) {
			Path tmp = Files.createTempFile("fix_" + id + "_", ".jpg");
			String[] cmd = {LOCAL_FFMPEG_PATH, "-y", "-nostdin", "-sseof", "-10", "-i", originalMp4.toString(), "-t", "00:00:1", "-r", "1", "-f", "mjpeg", tmp.toString()};
			new ProcessBuilder().inheritIO().command(cmd).start().waitFor();
			if (Files.exists(tmp) && Files.size(tmp) > 0) {
				BufferedImage b = ImageIO.read(tmp.toFile());
				Server.runSql((dao, c) -> ImageHelper.saveImage(dao, c, id, b));
				Files.deleteIfExists(tmp);
			}
		}
	}
}
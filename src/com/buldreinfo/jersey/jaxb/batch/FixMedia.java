package com.buldreinfo.jersey.jaxb.batch;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
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
		public Movie withIdPhotographerUserId(int idPhotographerUserId) {
			this.idPhotographerUserId = idPhotographerUserId;
			return this;
		}
		public Movie withInPhoto(int inPhoto) {
			this.inPhoto.add(inPhoto);
			return this;
		}
		public Movie withProblem(int idProblem, long ms) {
			this.idProblemMsMap.put(idProblem, ms);
			return this;
		}
	}

	private static Logger logger = LogManager.getLogger();
	private final static String LOCAL_FFMPEG_PATH = "G:/My Drive/web/buldreinfo/sw/ffmpeg-2023-10-04-git-9078dc0c52-full_build/bin/ffmpeg.exe";
	private final static String LOCAL_YT_DLP_PATH = "G:/My Drive/web/buldreinfo/sw/yt-dlp/yt-dlp.exe";

	public static void main(String[] args) {
		new FixMedia();
	}

	private final ExecutorService executor = Executors.newFixedThreadPool(12);
	private final List<String> warnings = new ArrayList<>();

	public FixMedia() {
		Server.runSql((dao, c) -> {
			c.setAutoCommit(true);
			StorageManager storage = StorageManager.getInstance();
			List<Movie> movies = new ArrayList<>();
			// movies.add(new Movie(Path.of("")).withProblem(, 0l).withIdPhotographerUserId().withInPhoto()); // TODO
			List<Integer> newIdMedia = Lists.newArrayList();
			for (Movie m : movies) {
				newIdMedia.add(addMovie(c, m.path, m.idPhotographerUserId, m.idUploaderUserId, m.idProblemMsMap, m.inPhoto));
				for (int idProblem : m.idProblemMsMap.keySet()) {
					dao.fillActivity(c, idProblem);
				}
			}
			String sqlStr = "SELECT id, width, height, suffix, embed_url FROM media WHERE is_movie=1";
			if (!newIdMedia.isEmpty()) {
				sqlStr += " AND id IN (" + newIdMedia.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
			}
			try (PreparedStatement ps = c.prepareStatement(sqlStr);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					final int id = rst.getInt("id");
					final String embedUrl = rst.getString("embed_url");

					executor.submit(() -> {
						Path tempFile = null;
						try {
							// Original Video (yt-dlp or S3 download)
							tempFile = Files.createTempFile("original_" + id, ".mp4");
							if (embedUrl != null) {
								String[] commands = {LOCAL_YT_DLP_PATH, "--ffmpeg-location", LOCAL_FFMPEG_PATH, embedUrl, "-f", "mp4", "-o", tempFile.toString()};
								new ProcessBuilder().inheritIO().command(commands).start().waitFor();
								if (Files.exists(tempFile) && Files.size(tempFile) > 0) {
									storage.uploadBytes(S3KeyGenerator.getOriginalMp4(id), Files.readAllBytes(tempFile), StorageType.MP4);
									ImageHelper.saveImageFromEmbedVideo(dao, c, id, embedUrl);
								}
							}
							else {
								byte[] originalData = storage.downloadBytes(S3KeyGenerator.getOriginalMp4(id));
								Files.write(tempFile, originalData);
							}
							// WEBM
							Path webmTmp = Files.createTempFile("web_" + id, ".webm");
							String[] webmCmd = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", tempFile.toString(), "-codec:v", "libvpx", "-quality", "good", "-cpu-used", "0", "-b:v", "500k", "-qmin", "10", "-qmax", "42", "-maxrate", "500k", "-bufsize", "1000k", "-threads", "4", "-vf", "scale=-1:1080", "-codec:a", "libvorbis", "-b:a", "128k", webmTmp.toString()};
							new ProcessBuilder().inheritIO().command(webmCmd).start().waitFor();
							storage.uploadBytes(S3KeyGenerator.getWebWebm(id), Files.readAllBytes(webmTmp), StorageType.WEBM);
							Files.deleteIfExists(webmTmp);
							// MP4
							Path mp4Tmp = Files.createTempFile("web_" + id, ".mp4");
							String[] mp4Cmd = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", tempFile.toString(), "-vf", "crop=((in_w/2)*2):((in_h/2)*2)", mp4Tmp.toString()};
							new ProcessBuilder().inheritIO().command(mp4Cmd).start().waitFor();
							storage.uploadBytes(S3KeyGenerator.getWebMp4(id), Files.readAllBytes(mp4Tmp), StorageType.MP4);
							Files.deleteIfExists(mp4Tmp);
							// Thumb
							Path thumbTmp = Files.createTempFile("thumb_" + id, ".jpg");
							String[] thumbCmd = {LOCAL_FFMPEG_PATH, "-nostdin", "-sseof", "-10", "-i", tempFile.toString(), "-t", "00:00:1", "-r", "1", "-f", "mjpeg", thumbTmp.toString()};
							new ProcessBuilder().inheritIO().command(thumbCmd).start().waitFor();
							if (Files.exists(thumbTmp)) {
								BufferedImage b = ImageIO.read(thumbTmp.toFile());
								ImageHelper.saveImage(dao, c, id, b);
								Files.deleteIfExists(thumbTmp);
							}

						} catch (Exception e) {
							warnings.add("id=" + id + ": " + e.getMessage());
						} finally {
							try {
								if (tempFile != null) {
									Files.deleteIfExists(tempFile);
								}
							} catch (IOException _) {
							}
						}
					});
				}
			}
			executor.shutdown();
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			for (String warning : warnings) {
				logger.warn(warning);
			}
		});
		logger.debug("Done");
	}

	private int addMovie(Connection c, Path src, int idPhotographerUserId, int idUploaderUserId, Map<Integer, Long> idProblemMsMap, List<Integer> inPhoto) throws SQLException, IOException {
		Preconditions.checkArgument(Files.exists(src), src.toString() + " does not exist");
		int idMedia = 0;
		final String suffix = com.google.common.io.Files.getFileExtension(src.getFileName().toString()).toLowerCase();
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created) VALUES (1, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, suffix);
			ps.setInt(2, idPhotographerUserId);
			ps.setInt(3, idUploaderUserId);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) idMedia = rst.getInt(1);
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
		StorageManager.getInstance().uploadBytes(S3KeyGenerator.getOriginalMp4(idMedia), Files.readAllBytes(src), StorageType.MP4);
		return idMedia;
	}
}
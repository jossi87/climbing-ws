package com.buldreinfo.jersey.jaxb.batch;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
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
import com.buldreinfo.jersey.jaxb.io.IOHelper;
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
			List<Movie> movies = new ArrayList<>();
			// movies.add(new Movie(Paths.get("")).withProblem(, 0l).withIdPhotographerUserId().withInPhoto()); // TODO
			List<Integer> newIdMedia = Lists.newArrayList();
			for (Movie m : movies) {
				newIdMedia.add(addMovie(c, m.path, m.idPhotographerUserId, m.idUploaderUserId, m.idProblemMsMap, m.inPhoto));
				for (int idProblem : m.idProblemMsMap.keySet()) {
					dao.fillActivity(c, idProblem);
				}
			}
			// Create all formats and set checksum
			String sqlStr = "SELECT id, width, height, suffix, embed_url FROM media WHERE is_movie=1";
			if (!newIdMedia.isEmpty()) {
				sqlStr += " AND id IN (" + newIdMedia.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
			}
			try (PreparedStatement ps = c.prepareStatement(sqlStr);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					final int id = rst.getInt("id");
					final int width = rst.getInt("width");
					final int height = rst.getInt("height");
					final String suffix = rst.getString("suffix");
					final String embedUrl = rst.getString("embed_url");
					final Path originalMp4 = IOHelper.getPathMediaOriginalMp4(id);
					final Path originalJpg = IOHelper.getPathMediaOriginalJpg(id);
					final Path jpg = IOHelper.getPathMediaWebJpg(id);
					final Path mp4 = IOHelper.getPathMediaWebMp4(id);
					final Path webm = IOHelper.getPathMediaWebWebm(id);
					final Path webp = IOHelper.getPathMediaWebWebp(id);
					executor.submit(() -> {
						try {
							Preconditions.checkNotNull(suffix, "suffix cannot be null");
							if (embedUrl != null) {
								// Try to download original video from embed url
								if (!Files.exists(originalMp4)) {
									IOHelper.createDirectories(originalMp4.getParent());
									String[] commands = {LOCAL_YT_DLP_PATH, "--ffmpeg-location", LOCAL_FFMPEG_PATH, embedUrl, "-f", "mp4", "-o", originalMp4.toString()};
									Process p = new ProcessBuilder().inheritIO().command(commands).start();
									p.waitFor();
									logger.debug("Embedded video " + embedUrl + " downloaded to " + originalMp4.toString());
									if (!Files.exists(originalMp4) || Files.size(originalMp4) == 0) {
										warnings.add(originalMp4.toString() + " does not exist (could not download " + embedUrl + ")");
									}
								}
								if (!Files.exists(originalJpg)) {
									ImageHelper.saveImageFromEmbedVideo(dao, c, id, embedUrl);
								}
							}
							else {
								if (!Files.exists(webm) || Files.size(webm) == 0) {
									Preconditions.checkArgument(Files.exists(originalMp4) && Files.size(originalMp4) > 0, originalMp4.toString() + " is required");
									logger.debug("Create " + webm);
									IOHelper.deleteIfExistsCreateParent(webm);
									String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", originalMp4.toString(), "-codec:v", "libvpx", "-quality", "good", "-cpu-used", "0", "-b:v", "500k", "-qmin", "10", "-qmax", "42", "-maxrate", "500k", "-bufsize", "1000k", "-threads", "4", "-vf", "scale=-1:1080", "-codec:a", "libvorbis", "-b:a", "128k", webm.toString()};
									Process p = new ProcessBuilder().inheritIO().command(commands).start();
									p.waitFor();
								}
								if (!Files.exists(mp4) || Files.size(mp4) == 0) {
									Preconditions.checkArgument(Files.exists(webm) && Files.size(webm) > 0, webm.toString() + " is required");
									logger.debug("Create " + mp4);
									IOHelper.deleteIfExistsCreateParent(mp4);
									String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", webm.toString(), "-vf", "crop=((in_w/2)*2):((in_h/2)*2)", mp4.toString()};
									Process p = new ProcessBuilder().inheritIO().command(commands).start();
									p.waitFor();
								}
								if (!Files.exists(originalJpg) || Files.size(originalJpg) == 0) {
									Files.createDirectories(originalJpg.getParent());
									Path tmp = Paths.get("C:/temp/" + System.currentTimeMillis() + ".jpg");
									Files.createDirectories(tmp.getParent());
									String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", originalMp4.toString(), "-ss", "00:00:02", "-t", "00:00:1", "-r", "1", "-f", "mjpeg", tmp.toString()};
									Process p = new ProcessBuilder().inheritIO().command(commands).start();
									p.waitFor();
									Preconditions.checkArgument(Files.exists(tmp), tmp + " does not exist");

									BufferedImage b = ImageIO.read(tmp.toFile());
									Graphics g = b.getGraphics();
									g.setFont(new Font("Arial", Font.BOLD, 40));
									final String str = "VIDEO";
									final int x = (b.getWidth()/2)-70;
									final int y = (b.getHeight()/2)-20;
									FontMetrics fm = g.getFontMetrics();
									Rectangle2D rect = fm.getStringBounds(str, g);
									g.setColor(Color.WHITE);
									g.fillRect(x,
											y - fm.getAscent(),
											(int) rect.getWidth(),
											(int) rect.getHeight());
									g.setColor(Color.BLUE);
									g.drawString(str, x, y);
									g.dispose();
									ImageHelper.saveImage(dao, c, id, b);
									Preconditions.checkArgument(Files.exists(originalJpg) && Files.size(originalJpg)>0, originalJpg.toString() + " does not exist (or is 0 byte)");
								}
							}
							if (width == 0 || height == 0 || !Files.exists(jpg) || Files.size(jpg) == 0 || !Files.exists(webp) || Files.size(webp) == 0) {
								throw new RuntimeException("Scaled versions dont exist, this is an error");
							}
						} catch (Exception e) {
							warnings.add("id=" + id + ", originalMp4=" + originalMp4.toString() + ": " + e.getMessage());
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
		Preconditions.checkArgument(idPhotographerUserId > 0, "Invalid idPhotographerUserId=" + idPhotographerUserId);
		Preconditions.checkArgument(idUploaderUserId > 0, "Invalid idUploaderUserId=" + idUploaderUserId);
		Preconditions.checkArgument(!idProblemMsMap.isEmpty(), idProblemMsMap + " is empty");
		Preconditions.checkArgument(!inPhoto.isEmpty(), inPhoto + " is empty");
		// DB - add media
		int idMedia = 0;
		final String suffix = com.google.common.io.Files.getFileExtension(src.getFileName().toString()).toLowerCase();
		Preconditions.checkArgument(suffix.equals("mp4") || suffix.equals("mov"), "Invalid suffix on " + src.toString() + ": " + suffix);
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
		Preconditions.checkArgument(idMedia>0);
		// DB - connect to problem
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_problem (media_id, problem_id, milliseconds) VALUES (?, ?, ?)")) {
			for (int idProblem : idProblemMsMap.keySet()) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idProblem);
				ps.setLong(3, idProblemMsMap.get(idProblem));
				ps.addBatch();
			}
			ps.executeBatch();
		}
		// DB - add inPhoto
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
			for (int idUser : inPhoto) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idUser);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		// IO
		final Path dst = IOHelper.getPathMediaOriginalMp4(idMedia);
		Preconditions.checkArgument(!Files.exists(dst), dst.toString() + " already exists");
		Preconditions.checkArgument(Files.exists(dst.getParent().getParent()), dst.getParent().getParent().toString() + " does not exist");
		Files.createDirectories(dst.getParent());
		Files.copy(src, dst);
		return idMedia;
	}
}

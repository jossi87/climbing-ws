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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class FixMedia {
	private static Logger logger = LogManager.getLogger();
	private final static String LOCAL_FFMPEG_PATH = "G:/My Drive/web/buldreinfo/sw/ffmpeg-2023-10-04-git-9078dc0c52-full_build/bin/ffmpeg.exe";
	private final static String LOCAL_YT_DLP_PATH = "G:/My Drive/web/buldreinfo/sw/yt-dlp/yt-dlp.exe";
	public static void main(String[] args) {
		new FixMedia();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(12);
	private final List<String> warnings = new ArrayList<>();

	public FixMedia() {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			c.getConnection().setAutoCommit(true);
			List<Integer> newIdMedia = Lists.newArrayList();
			// Add movie
			//			final int idUploaderUserId = 1;
			//			Path src = Paths.get(""); // TODO
			//			int idPhotographerUserId = 1; // TODO
			//			Map<Integer, Long> idProblemMsMap = new LinkedHashMap<>();
			//			idProblemMsMap.put(1, 0l); // TODO
			//			List<Integer> inPhoto = Lists.newArrayList(); // TODO
			//			newIdMedia.add(addMovie(c.getConnection(), src, idPhotographerUserId, idUploaderUserId, idProblemMsMap, inPhoto));
			//			for (int idProblem : idProblemMsMap.keySet()) {
			//				c.getBuldreinfoRepo().fillActivity(idProblem);
			//			}
			// Create all formats and set checksum
			String sqlStr = "SELECT id, width, height, suffix, is_movie, embed_url FROM media";
			if (newIdMedia != null && !newIdMedia.isEmpty()) {
				sqlStr += " WHERE id IN (" + newIdMedia.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					final int id = rst.getInt("id");
					final int width = rst.getInt("width");
					final int height = rst.getInt("height");
					final String suffix = rst.getString("suffix");
					final boolean isMovie = rst.getBoolean("is_movie");
					final String embedUrl = rst.getString("embed_url");
					final String originalFolder = isMovie? "original/mp4" : "original/jpg";
					final Path root = GlobalFunctions.getPathRoot();
					final Path original = root.resolve(originalFolder).resolve(String.valueOf(id/100*100)).resolve(id + "." + suffix);
					final Path originalJpg = root.resolve("original/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
					final Path jpg = root.resolve("web/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
					final Path mp4 = root.resolve("web/mp4").resolve(String.valueOf(id/100*100)).resolve(id + ".mp4");
					final Path webm = root.resolve("web/webm").resolve(String.valueOf(id/100*100)).resolve(id + ".webm");
					final Path webp = root.resolve("web/webp").resolve(String.valueOf(id/100*100)).resolve(id + ".webp");
					executor.submit(() -> {
						try {
							if (isMovie) {
								Preconditions.checkNotNull(suffix, "suffix cannot be null");
								if (embedUrl != null) {
									// Try to download original video from embed url
									if (!Files.exists(original)) {
										Files.createDirectories(original.getParent());
										String[] commands = {LOCAL_YT_DLP_PATH, "--ffmpeg-location", LOCAL_FFMPEG_PATH, embedUrl, "-o", original.toString()};
										Process p = new ProcessBuilder().inheritIO().command(commands).start();
										p.waitFor();
										logger.debug("Embedded video " + embedUrl + " downloaded to " + original.toString());
										if (!Files.exists(original) || Files.size(original) == 0) {
											warnings.add(original.toString() + " does not exist (could not download " + embedUrl + ")");
										}
									}
									if (!Files.exists(originalJpg)) {
										GlobalFunctions.downloadJpgFromEmbedVideo(id, embedUrl);
									}
								}
								else {
									if (!Files.exists(webm) || Files.size(webm) == 0) {
										logger.debug("Create " + webm);
										Files.deleteIfExists(webm);
										Files.createDirectories(webm.getParent());
										String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", original.toString(), "-codec:v", "libvpx", "-quality", "good", "-cpu-used", "0", "-b:v", "500k", "-qmin", "10", "-qmax", "42", "-maxrate", "500k", "-bufsize", "1000k", "-threads", "4", "-vf", "scale=-1:1080", "-codec:a", "libvorbis", "-b:a", "128k", webm.toString()};
										Process p = new ProcessBuilder().inheritIO().command(commands).start();
										p.waitFor();
									}
									if (!Files.exists(mp4) || Files.size(mp4) == 0) {
										Preconditions.checkArgument(Files.exists(webm) && Files.size(webm) > 0, webm.toString() + " is required");
										logger.debug("Create " + mp4);
										Files.deleteIfExists(mp4);
										Files.createDirectories(mp4.getParent());
										String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", webm.toString(), "-vf", "crop=((in_w/2)*2):((in_h/2)*2)", mp4.toString()};
										Process p = new ProcessBuilder().inheritIO().command(commands).start();
										p.waitFor();
									}
									if (!Files.exists(originalJpg) || Files.size(originalJpg) == 0) {
										Files.createDirectories(originalJpg.getParent());
										Path tmp = Paths.get("C:/temp/" + System.currentTimeMillis() + ".jpg");
										Files.createDirectories(tmp.getParent());
										String[] commands = {LOCAL_FFMPEG_PATH, "-nostdin", "-i", original.toString(), "-ss", "00:00:02", "-t", "00:00:1", "-r", "1", "-f", "mjpeg", tmp.toString()};
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
										ImageIO.write(b, "jpg", originalJpg.toFile());
										Preconditions.checkArgument(Files.exists(originalJpg) && Files.size(originalJpg)>0, originalJpg.toString() + " does not exist (or is 0 byte)");
									}
								}
							}
							if (width == 0 || height == 0 || !Files.exists(jpg) || Files.size(jpg) == 0 || !Files.exists(webp) || Files.size(webp) == 0) {
								GlobalFunctions.createWebImagesAndUpdateDb(c, id);
							}
						} catch (Exception e) {
							warnings.add("id=" + id + ", path=" + original.toString() + ": " + e.getMessage());
						}
					});
				}
			}
			executor.shutdown();
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			for (String warning : warnings) {
				logger.warn(warning);
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	public int addMovie(Connection c, Path src, int idPhotographerUserId, int idUploaderUserId, Map<Integer, Long> idProblemMsMap, List<Integer> inPhoto) throws SQLException, IOException {
		Preconditions.checkArgument(Files.exists(src), src.toString() + " does not exist");
		Preconditions.checkArgument(idPhotographerUserId > 0, "Invalid idPhotographerUserId=" + idPhotographerUserId);
		Preconditions.checkArgument(idUploaderUserId > 0, "Invalid idUploaderUserId=" + idUploaderUserId);
		Preconditions.checkArgument(!idProblemMsMap.isEmpty(), idProblemMsMap + " is empty");
		Preconditions.checkArgument(!inPhoto.isEmpty(), inPhoto + " is empty");
		// DB - add media
		int idMedia = 0;
		final String suffix = com.google.common.io.Files.getFileExtension(src.getFileName().toString()).toLowerCase();
		Preconditions.checkArgument(suffix.equals("mp4") || suffix.equals("mov"), "Invalid suffix on " + src.toString() + ": " + suffix);
		PreparedStatement ps = c.prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created) VALUES (1, ?, ?, ?, NOW())", PreparedStatement.RETURN_GENERATED_KEYS);
		ps.setString(1, suffix);
		ps.setInt(2, idPhotographerUserId);
		ps.setInt(3, idUploaderUserId);
		ps.executeUpdate();
		ResultSet rst = ps.getGeneratedKeys();
		if (rst != null && rst.next()) {
			idMedia = rst.getInt(1);
		}
		rst.close();
		ps.close();
		ps = null;
		Preconditions.checkArgument(idMedia>0);
		// DB - connect to problem
		ps = c.prepareStatement("INSERT INTO media_problem (media_id, problem_id, milliseconds) VALUES (?, ?, ?)");
		for (int idProblem : idProblemMsMap.keySet()) {
			ps.setInt(1, idMedia);
			ps.setInt(2, idProblem);
			ps.setLong(3, idProblemMsMap.get(idProblem));
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		ps = null;
		// DB - add inPhoto
		ps = c.prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)");
		for (int idUser : inPhoto) {
			ps.setInt(1, idMedia);
			ps.setInt(2, idUser);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		ps = null;
		// IO
		final Path root = GlobalFunctions.getPathRoot();
		final Path dst = root.resolve("original/mp4").resolve(String.valueOf(idMedia/100*100)).resolve(idMedia + "." + suffix);
		Preconditions.checkArgument(!Files.exists(dst), dst.toString() + " already exists");
		Preconditions.checkArgument(Files.exists(dst.getParent().getParent()), dst.getParent().getParent().toString() + " does not exist");
		Files.createDirectories(dst.getParent());
		Files.copy(src, dst);
		return idMedia;
	}
}

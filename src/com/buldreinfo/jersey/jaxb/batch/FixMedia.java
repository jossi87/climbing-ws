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

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.base.Preconditions;

public class FixMedia {
	private static Logger logger = LogManager.getLogger();
	public final static Path root = Paths.get( "D:/gdrive/web/buldreinfo/buldreinfo_media");
	private final static String LOCAL_LIB_WEBC_PATH = "D:/gdrive/web/buldreinfo/sw/libwebp-1.1.0-windows-x64/bin/cwebp.exe";
	private final static String LOCAL_FFMPEG_PATH = "D:/gdrive/web/buldreinfo/sw/ffmpeg-2020-11-22-git-0066bf4d1a-full_build/bin/ffmpeg.exe";
	private final static String LOCAL_YOUTUBE_DL_PATH = "D:/gdrive/web/buldreinfo/sw/youtube-dl/youtube-dl.exe";

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			c.getConnection().setAutoCommit(true);
			FixMedia service = new FixMedia();
			// Add movie
			//			final int idUploaderUserId = 1;			
			//			Path src = Paths.get("C:/Users/joste/OneDrive/Skrivebord/new/.mp4");
			//			int idPhotographerUserId = ;
			//			Map<Integer, Long> idProblemMsMap = new LinkedHashMap<>();
			//			idProblemMsMap.put(, 0l);
			//			List<Integer> inPhoto = Lists.newArrayList();
			//			service.addMovie(c.getConnection(), src, idPhotographerUserId, idUploaderUserId, idProblemMsMap, inPhoto);
			//			for (int idProblem : idProblemMsMap.keySet()) {
			//				c.getBuldreinfoRepo().fillActivity(idProblem);
			//			}
			// Create all formats and set checksum
			List<String> warnings = service.fixMovies(c.getConnection());
			for (String warning : warnings) {
				logger.warn(warning);
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	public FixMedia() {
	}

	public void addMovie(Connection c, Path src, int idPhotographerUserId, int idUploaderUserId, Map<Integer, Long> idProblemMsMap, List<Integer> inPhoto) throws SQLException, IOException {
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
		final Path dst = root.resolve("original/mp4").resolve(String.valueOf(idMedia/100*100)).resolve(idMedia + "." + suffix);
		Preconditions.checkArgument(!Files.exists(dst), dst.toString() + " already exists");
		Preconditions.checkArgument(Files.exists(dst.getParent().getParent()), dst.getParent().getParent().toString() + " does not exist");
		Files.createDirectories(dst.getParent());
		Files.copy(src, dst);
	}

	private List<String> fixMovies(Connection c) throws Exception {
		List<String> warnings = new ArrayList<>();
		PreparedStatement ps = c.prepareStatement("SELECT id, width, height, suffix, is_movie, embed_url FROM media WHERE");
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			final int id = rst.getInt("id");
			final int width = rst.getInt("width");
			final int height = rst.getInt("height");
			final String suffix = rst.getString("suffix");
			final boolean isMovie = rst.getBoolean("is_movie");
			final String embedUrl = rst.getString("embed_url");
			final String originalFolder = isMovie? "original/mp4" : "original/jpg";
			final Path original = root.resolve(originalFolder).resolve(String.valueOf(id/100*100)).resolve(id + "." + suffix);
			final Path jpg = root.resolve("web/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
			final Path mp4 = root.resolve("web/mp4").resolve(String.valueOf(id/100*100)).resolve(id + ".mp4");
			final Path webm = root.resolve("web/webm").resolve(String.valueOf(id/100*100)).resolve(id + ".webm");
			final Path webp = root.resolve("web/webp").resolve(String.valueOf(id/100*100)).resolve(id + ".webp");
			try {
				if (embedUrl != null) {
					Preconditions.checkNotNull(suffix, "suffix cannot be null");
					// Only download original if embedded video
					if (!Files.exists(original)) {
						Files.createDirectories(original.getParent());
						String[] commands = {LOCAL_YOUTUBE_DL_PATH, embedUrl, "-o", original.toString()};
						Process p = new ProcessBuilder().inheritIO().command(commands).start();
						p.waitFor();
						logger.debug("Embedded video " + embedUrl + " downloaded to " + original.toString());
					}
					if (!Files.exists(original) || Files.size(original) == 0) {
						warnings.add(original.toString() + " does not exist (or is 0 bytes)");
					}
				}
				else {
					if (!Files.exists(original) || Files.size(original) == 0) {
						warnings.add(original.toString() + " does not exist (or is 0 bytes)");
					}
					else {
						if (isMovie) {
							if (!Files.exists(webm) || Files.size(webm) == 0) {
								logger.debug("Create " + webm);
								Files.deleteIfExists(webm);
								Files.createDirectories(webm.getParent());
								String[] commands = {LOCAL_FFMPEG_PATH, "-i", original.toString(), "-codec:v", "libvpx", "-quality", "good", "-cpu-used", "0", "-b:v", "500k", "-qmin", "10", "-qmax", "42", "-maxrate", "500k", "-bufsize", "1000k", "-threads", "4", "-vf", "scale=-1:1080", "-codec:a", "libvorbis", "-b:a", "128k", webm.toString()};
								Process p = new ProcessBuilder().inheritIO().command(commands).start();
								p.waitFor();
								// Set checksum
								PreparedStatement ps2 = c.prepareStatement("UPDATE media SET checksum=? WHERE id=?");
								ps2.setInt(1, GlobalFunctions.getCrc32(webm));
								ps2.setInt(2, id);
								ps2.execute();
								ps2.close();
							}
							if (!Files.exists(mp4) || Files.size(mp4) == 0) {
								Preconditions.checkArgument(Files.exists(webm) && Files.size(webm) > 0, webm.toString() + " is required");
								logger.debug("Create " + mp4);
								Files.deleteIfExists(mp4);
								Files.createDirectories(mp4.getParent());
								String[] commands = {LOCAL_FFMPEG_PATH, "-i", webm.toString(), "-vf", "crop=((in_w/2)*2):((in_h/2)*2)", mp4.toString()};
								Process p = new ProcessBuilder().inheritIO().command(commands).start();
								p.waitFor();
							}
							if (!Files.exists(jpg) || Files.size(jpg) == 0) {
								logger.debug("Create " + jpg);
								Files.createDirectories(jpg.getParent());
								Path tmp = Paths.get("C:/temp/" + System.currentTimeMillis() + ".jpg");
								Files.createDirectories(tmp.getParent());
								String[] commands = {LOCAL_FFMPEG_PATH, "-i", original.toString(), "-ss", "00:00:02", "-t", "00:00:1", "-r", "1", "-f", "mjpeg", tmp.toString()};
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
								ImageIO.write(b, "jpg", jpg.toFile());
								Preconditions.checkArgument(Files.exists(jpg) && Files.size(jpg)>0, jpg.toString() + " does not exist (or is 0 byte)");
							}
							if (!Files.exists(webp) || Files.size(webp) == 0) {
								logger.debug("Create " + webp);
								Files.createDirectories(webp.getParent());
								String cmd = "cmd /c " + LOCAL_LIB_WEBC_PATH + " \"" + jpg.toString() + "\" -o \"" + webp.toString() + "\"";
								Process process = Runtime.getRuntime().exec(cmd);
								process.waitFor();
								Preconditions.checkArgument(Files.exists(webp), "WebP does not exist. Command=" + cmd);
								logger.debug(webp.toString() + " saved");
							}
						}
						else if (width == 0 || height == 0 || !Files.exists(jpg) || Files.size(jpg) == 0 || !Files.exists(webp) || Files.size(webp) == 0) {
							Files.deleteIfExists(jpg);
							Files.deleteIfExists(webp);
							Files.createDirectories(jpg.getParent());
							Files.createDirectories(webp.getParent());

							// IO JPG
							BufferedImage bOriginal = ImageIO.read(original.toFile());
							final int newWidth = bOriginal.getWidth();
							final int newHeight = bOriginal.getHeight();
							BufferedImage bScaled = Scalr.resize(bOriginal, 2560, 1440, Scalr.OP_ANTIALIAS);
							ImageIO.write(bScaled, "jpg", jpg.toFile());
							bOriginal.flush();
							bOriginal = null;
							bScaled.flush();
							bScaled = null;
							Preconditions.checkArgument(Files.exists(jpg) && Files.size(jpg)>0);
							logger.debug(jpg.toString() + " saved");

							// IO WEBP
							String cmd = "cmd /c " + LOCAL_LIB_WEBC_PATH + " \"" + jpg.toString() + "\" -o \"" + webp.toString() + "\"";
							Process process = Runtime.getRuntime().exec(cmd);
							process.waitFor();
							Preconditions.checkArgument(Files.exists(webp), "WebP does not exist. Command=" + cmd);
							logger.debug(webp.toString() + " saved");

							// DB
							try (PreparedStatement ps2 = c.prepareStatement("UPDATE media SET checksum=?, width=?, height=? WHERE id=?")) {
								ps2.setInt(1, GlobalFunctions.getCrc32(webp));
								ps2.setInt(2, newWidth);
								ps2.setInt(3, newHeight);
								ps2.setInt(4, id);
								ps2.execute();
							}
						}
					}
				}
			} catch (Exception e) {
				warnings.add("id=" + id + ", path=" + original.toString() + ": " + e.getMessage());
			}
		}
		rst.close();
		ps.close();
		return warnings;
	}
}

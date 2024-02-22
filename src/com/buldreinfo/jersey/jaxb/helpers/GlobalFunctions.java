package com.buldreinfo.jersey.jaxb.helpers;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.thumbnailcreator.ExifOrientation;
import com.buldreinfo.jersey.jaxb.thumbnailcreator.ThumbnailCreation;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class GlobalFunctions {
	private static final Logger logger = LogManager.getLogger();

	public static void createWebImagesAndUpdateDb(DbConnection c, int id) throws IOException, InterruptedException, SQLException {
		logger.debug("createWebImagesAndUpdateDb(id={}) - initialized", id);
		final Path originalJpg = getPathMediaOriginalJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
		final Path webp = getPathMediaWebWebp().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		final Path jpg = getPathMediaWebJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
		final Path webm = getPathMediaWebWebm().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webm");
		Files.deleteIfExists(webp);
		Files.deleteIfExists(jpg);
		if (!Files.exists(webp.getParent())) {
			Files.createDirectories(webp.getParent());
			setFilePermission(webp.getParent());
		}
		if (!Files.exists(jpg.getParent())) {
			Files.createDirectories(jpg.getParent());
			setFilePermission(jpg.getParent());
		}
		Preconditions.checkArgument(Files.exists(originalJpg), originalJpg.toString() + " does not exist");
		Preconditions.checkArgument(!Files.exists(webp), webp.toString() + " does already exist");
		Preconditions.checkArgument(!Files.exists(jpg), jpg.toString() + " does already exist");
		// Scaled JPG
		BufferedImage bOriginal = ImageIO.read(originalJpg.toFile());
		final int width = bOriginal.getWidth();
		final int height = bOriginal.getHeight();
		BufferedImage bScaled = Scalr.resize(bOriginal, Scalr.Method.ULTRA_QUALITY, 2560, 1440, Scalr.OP_ANTIALIAS);
		JpgWriter.write(bScaled, jpg);
		bOriginal.flush();
		bOriginal = null;
		bScaled.flush();
		bScaled = null;
		Preconditions.checkArgument(Files.exists(jpg));
		setFilePermission(jpg);
		logger.debug("createWebImagesAndUpdateDb(id={}) - scaled jpg saved", id);
		// Scaled WebP
		String[] cmd = null;
		if (!SystemUtils.IS_OS_WINDOWS) {
			cmd = new String[] { "/bin/bash", "-c", "cwebp \"" + jpg.toString() + "\" -o \"" + webp.toString() + "\"" };
		}
		else {
			cmd = new String[] { "\"G:/My Drive/web/buldreinfo/sw/libwebp-1.3.2-windows-x64/bin/cwebp.exe\"", "\"" + jpg.toString() + "\"", "-o", "\"" + webp.toString() + "\""};
		}
		Process process = Runtime.getRuntime().exec(cmd);
		process.waitFor();
		Preconditions.checkArgument(Files.exists(webp), "WebP does not exist. Command=" + Lists.newArrayList(cmd));
		setFilePermission(webp);
		logger.debug("createWebImagesAndUpdateDb(id={}) - scaled webp saved", id);
		// Update db
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET date_taken=?, checksum=?, width=?, height=? WHERE id=?")) {
			ps.setString(1, getDateTaken(originalJpg));
			ps.setInt(2, getCrc32(Files.exists(webm)? webm : webp));
			ps.setInt(3, width);
			ps.setInt(4, height);
			ps.setInt(5, id);
			ps.execute();
		}
		logger.debug("createWebImagesAndUpdateDb(id={}) - DB done", id);
	}

	public static void downloadJpgFromEmbedVideo(int idMedia, String embedUrl) throws IOException, InterruptedException {
		Preconditions.checkArgument(idMedia > 0, "idMedia = 0");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(embedUrl), "embedUrl is null");
		final Path p = getPathMediaOriginalJpg().resolve(String.valueOf(idMedia / 100 * 100)).resolve(idMedia + ".jpg");
		Preconditions.checkArgument(!Files.exists(p), p.toString() + " already exists");
		if (!Files.exists(p.getParent())) {
			Files.createDirectories(p.getParent());
			setFilePermission(p.getParent());
		}
		String imgUrl = null;
		if (embedUrl.startsWith("https://www.youtube.com/embed/")) {
			String id = embedUrl.replace("https://www.youtube.com/embed/", "");
			imgUrl = "https://img.youtube.com/vi/" + id + "/0.jpg";
		}
		else if (embedUrl.startsWith("https://player.vimeo.com/video/")) {
			String id = embedUrl.replace("https://player.vimeo.com/video/", "");
			try (HttpClient client = HttpClient.newHttpClient()) {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create("https://vimeo.com/api/v2/video/" + id + ".json"))
						.GET()
						.build();
				HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
				Preconditions.checkArgument(response.statusCode() == HttpURLConnection.HTTP_OK, "HTTP-" + response.statusCode());
				try (InputStream is = response.body()) {
					try (Reader targetReader = new InputStreamReader(is, Charset.forName("UTF-8"))) {
						JsonArray arr = new Gson().fromJson(targetReader, JsonArray.class);
						JsonObject obj = arr.get(0).getAsJsonObject();
						imgUrl = obj.get("thumbnail_large").getAsString();
					}
				}
			}
		}
		Preconditions.checkArgument(imgUrl != null, "imgUrl is null");
		try (InputStream is = URI.create(imgUrl).toURL().openStream()){
			BufferedImage b = ImageIO.read(is);
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
			JpgWriter.write(b, p);
			b.flush();
			setFilePermission(p);
		}
		if (Files.exists(p) && Files.size(p) == 0) {
			Files.delete(p);
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
	}

	public static void downloadOriginalJpgFromImage(int idMedia, String name, FormDataMultiPart multiPart) throws IOException {
		Preconditions.checkArgument(idMedia > 0, "idMedia = 0");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name is null");
		Preconditions.checkArgument(multiPart != null, "multiPart is null");
		final Path p = getPathMediaOriginalJpg().resolve(String.valueOf(idMedia / 100 * 100)).resolve(idMedia + ".jpg");
		Preconditions.checkArgument(!Files.exists(p), p.toString() + " already exists");
		if (!Files.exists(p.getParent())) {
			Files.createDirectories(p.getParent());
			setFilePermission(p.getParent());
		}
		try (InputStream is = multiPart.getField(name).getValueAs(InputStream.class)) {
			if (name.toLowerCase().endsWith("jpg")) {
				Files.copy(is, p);
			}
			else {
				BufferedImage src = ImageIO.read(is);
				BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
				dst.createGraphics().drawImage(src, 0, 0, Color.WHITE, null);
				JpgWriter.write(dst, p);
				src.flush();
				dst.flush();
			}
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");

		// Rotate (if EXIF-rotated)
		try (ThumbnailCreation creation = ThumbnailCreation.image(p.toFile())) {
			ExifOrientation orientation = creation.getExifRotation();
			if (orientation != null && orientation != ExifOrientation.HORIZONTAL_NORMAL) {
				logger.info("Rotating " + p.toString() + " using " + orientation);
				creation.rotate(orientation).preserveExif().saveTo(com.google.common.io.Files.asByteSink(p.toFile()));
			}
		}
		
		if (Files.exists(p) && Files.size(p) == 0) {
			Files.delete(p);
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
	}

	public static boolean downloadUserImage(int userId, String url) throws IOException {
		try {
			Path original = getPathOriginalUsers().resolve(userId + ".jpg");
			if (!Files.exists(original.getParent())) {
				Files.createDirectories(original.getParent());
				setFilePermission(original.getParent());
			}
			try (InputStream in = URI.create(url).toURL().openStream()) {
				Files.copy(in, original, StandardCopyOption.REPLACE_EXISTING);
				in.close();
				setFilePermission(original);
				// Resize avatar
				Path resized = getPathWebUsers().resolve(userId + ".jpg");
				Files.createDirectories(resized.getParent());
				Files.deleteIfExists(resized);
				BufferedImage bOriginal = ImageIO.read(original.toFile());
				BufferedImage bScaled = Scalr.resize(bOriginal, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
				JpgWriter.write(bScaled, resized);
				bOriginal.flush();
				bOriginal = null;
				bScaled.flush();
				bScaled = null;
				Preconditions.checkArgument(Files.exists(resized));
				setFilePermission(resized);
				return true;
			}
		} catch (Exception e) {
			logger.fatal(e.getMessage(), e);
			return false;
		}
	}

	public static String getFilename(String purpose, String ext) {
		purpose = removeIllegalCharacters(purpose);
		final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return String.format("%s_Buldreinfo_BratteLinjer_%s.%s", dateTime, purpose, ext);
	}

	public static Path getPathLeafletPrint() throws IOException {
		Path res = Paths.get("/var/lib/jenkins/workspace/climbing-web/leaflet-puppeteer-print/index.js");
		if (!Files.exists(res)) {
			throw new RuntimeException(res.toString() + " does not exists");
		}
		return res;
	}
	
	public static Path getPathMediaWebJpg() throws IOException {
		return getPathRoot().resolve("web/jpg");
	}

	public static Path getPathMediaWebWebp() throws IOException {
		return getPathRoot().resolve("web/webp");
	}
	
	public static Path getPathRoot() throws IOException {
		Path root = null;
		if (!SystemUtils.IS_OS_WINDOWS) {
			root = Paths.get("/mnt/buldreinfo/media");
		}
		else {
			root = Paths.get("G:/My Drive/web/buldreinfo/buldreinfo_media");
		}
		Preconditions.checkArgument(root != null && Files.exists(root), "Invalid root: " + root);
		return root;
	}

	public static Path getPathTemp() throws IOException {
		return getPathRoot().resolve("temp");
	}

	public static String getUrlJpgToImage(int id) {
		return "https://brattelinjer.no/buldreinfo_media/jpg/" + String.valueOf(id / 100 * 100) + "/" + id + ".jpg";
	}

	public static WebApplicationException getWebApplicationExceptionBadRequest(Exception e) {
		logger.warn(e.getMessage(), e);
		return new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(e.getMessage()).build());
	}

	public static WebApplicationException getWebApplicationExceptionInternalError(Exception e) {
		logger.fatal(e.getMessage(), e);
		return new WebApplicationException(Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).entity(e.getMessage()).build());
	}

	public static void rotateMedia(DbConnection c, int idMedia, int degrees) throws IOException, InterruptedException, SQLException {
		final Path original = getPathMediaOriginalJpg().resolve(String.valueOf(idMedia / 100 * 100)).resolve(idMedia + ".jpg");
		Preconditions.checkArgument(Files.exists(original), "Could not find " + original.toString());
		Rotation r = null;
		switch (degrees) {
		case 90:
			r = Rotation.CW_90;
			break;
		case 180:
			r = Rotation.CW_180;
			break;
		case 270:
			r = Rotation.CW_270;
			break;
		default:
			throw getWebApplicationExceptionBadRequest(new IllegalArgumentException("Cannot rotate image " + degrees + " degrees (legal degrees = 90, 180, 270)"));
		}
		BufferedImage bOriginal = ImageIO.read(original.toFile());
		BufferedImage bRotated = Scalr.rotate(bOriginal, r, Scalr.OP_ANTIALIAS);
		bOriginal.flush();
		Files.delete(original);
		JpgWriter.write(bRotated, original);
		bRotated.flush();
		createWebImagesAndUpdateDb(c, idMedia);
	}

	private static int getCrc32(Path p) throws IOException {
		return com.google.common.io.Files.asByteSource(p.toFile()).hash(Hashing.crc32()).asInt();
	}

	private static String getDateTaken(Path p) {
		if (Files.exists(p) && p.getFileName().toString().toLowerCase().endsWith(".jpg")) {
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(p.toFile());
				ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				java.util.Date date = directory.getDateOriginal(TimeZone.getDefault());
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
				return sdf.format(date.getTime());
			} catch (Exception e) {
			}
		}
		return null;
	}
	
	private static Path getPathMediaOriginalJpg() throws IOException {
		return getPathRoot().resolve("original/jpg");
	}
	
	private static Path getPathMediaWebWebm() throws IOException {
		return getPathRoot().resolve("web/webm");
	}
	
	private static Path getPathOriginalUsers() throws IOException {
		return getPathRoot().resolve("original/users");
	}
	
	private static Path getPathWebUsers() throws IOException {
		return getPathRoot().resolve("web/users");
	}
	
	private static String removeIllegalCharacters(String str) {
		return str.trim().replaceAll("[\\\\/:*?\"<>|] ", "_");
	}
	
	private static void setFilePermission(Path p) {
		if (!SystemUtils.IS_OS_WINDOWS) {
			try {
				Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.OTHERS_READ);
				perms.add(PosixFilePermission.OTHERS_WRITE);
				perms.add(PosixFilePermission.GROUP_READ);
				perms.add(PosixFilePermission.GROUP_WRITE);
				Files.setPosixFilePermissions(p, perms);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}
}
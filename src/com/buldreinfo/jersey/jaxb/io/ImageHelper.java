package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.db.Dao;
import com.google.common.base.Preconditions;

public class ImageHelper {
	private static Logger logger = LogManager.getLogger();

	public static void rotateImage(Dao dao, Connection c, int idMedia, Rotation rotation) throws IOException, SQLException, InterruptedException {
		Path original = IOHelper.getPathMediaOriginalJpg(idMedia);
		byte[] bytes = Files.readAllBytes(original);
		ExifReader exifReader = new ExifReader(bytes);
		ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withMetadata(exifReader.getOutputSet())
		.withPathOriginalJpg(original)
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), exifReader.getDateTaken());
		IOHelper.deleteResizedCache(idMedia);
	}

	public static void saveAvatar(int userId, InputStream is) {
		try {
			Path original = IOHelper.getPathOriginalUsers(userId);
			IOHelper.createDirectories(original.getParent());
			Files.copy(is, original, StandardCopyOption.REPLACE_EXISTING);
			IOHelper.setFilePermission(original);
			Path resized = IOHelper.getPathWebUsers(userId);
			saveAvatarThumb(original, resized);
		} catch (IOException e) {
			logger.warn("saveAvatar(userId={}) failed: {}", userId, e.toString());
		}
	}

	public static void saveAvatarThumb(Path original, Path resized) {
		try {
			IOHelper.deleteIfExistsCreateParent(resized);
			BufferedImage b = ImageReader.newBuilder().withPath(original).build().getJpgBufferedImage();
			int targetSize = 50;
			if (b.getWidth() > targetSize && b.getHeight() > targetSize) {
				var mode = b.getWidth() > b.getHeight() ? Scalr.Mode.FIT_TO_HEIGHT : Scalr.Mode.FIT_TO_WIDTH;
				b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, mode, targetSize, Scalr.OP_ANTIALIAS);
			}
			if (b.getWidth() > targetSize || b.getHeight() > targetSize) {
				int x = Math.max(0, (b.getWidth() - targetSize) / 2);
		        int y = Math.max(0, (b.getHeight() - targetSize) / 2);
		        int cropWidth = Math.min(targetSize, b.getWidth() - x);
		        int cropHeight = Math.min(targetSize, b.getHeight() - y);
		        b = Scalr.crop(b, x, y, cropWidth, cropHeight);
			}
			Preconditions.checkArgument(ImageIO.write(b, "jpg", resized.toFile()));
			IOHelper.setFilePermission(resized);
		} catch (IOException | InterruptedException e) {
			logger.warn("saveAvatarThumb(original={}, resized={}) failed: {}", original, resized, e.toString());
		}
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage) throws IOException, SQLException {
		ImageSaver.newBuilder()
		.withBufferedImage(bufferedImage)
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, byte[] bytes) throws IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withMetadata(exifReader.getOutputSet())
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), exifReader.getDateTaken());
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, SQLException, InterruptedException {
		ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), null);
	}
}
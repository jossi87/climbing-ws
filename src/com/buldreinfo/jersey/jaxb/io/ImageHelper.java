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
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), exifReader.getDateTaken());
	}

	public static void saveAvatar(int userId, InputStream is, boolean replaceOriginal) {
		try {
			Path original = IOHelper.getPathOriginalUsers(userId);
			boolean createResized = false;
			if (replaceOriginal || !Files.exists(original)) {
				IOHelper.createDirectories(original.getParent());
				Files.copy(is, original, StandardCopyOption.REPLACE_EXISTING);
				IOHelper.setFilePermission(original);
				createResized = true;
			}
			if (Files.exists(original) ) {
				Path resized = IOHelper.getPathWebUsers(userId);
				if (createResized || !Files.exists(resized)) {
					IOHelper.deleteIfExistsCreateParent(resized);
					BufferedImage b = ImageReader.newBuilder().withPath(original).build().getJpgBufferedImage();
					b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, 35, 35, Scalr.OP_ANTIALIAS);
					Preconditions.checkArgument(ImageIO.write(b, "jpg", resized.toFile()));
					IOHelper.setFilePermission(resized);
				}
			}
		} catch (IOException | InterruptedException e) {
			logger.warn("saveAvatar(userId={}, replaceOriginal={}) failed: {}", userId, replaceOriginal, e.toString());
		}
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage) throws IOException, SQLException {
		ImageSaver.newBuilder()
		.withBufferedImage(bufferedImage)
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, bufferedImage.getHeight(), bufferedImage.getWidth(), null);
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
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), exifReader.getDateTaken());
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, SQLException, InterruptedException {
		ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withPathOriginalJpg(IOHelper.getPathMediaOriginalJpg(idMedia))
		.withPathWebJpg(IOHelper.getPathMediaWebJpg(idMedia))
		.withPathWebWebP(IOHelper.getPathMediaWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getHeight(), imageReader.getJpgBufferedImage().getWidth(), null);
	}
}
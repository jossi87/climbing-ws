package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.db.Dao;

public class ImageHelper {
	private static Logger logger = LogManager.getLogger();

	public static void rotateImage(Dao dao, Connection c, int idMedia, Rotation rotation) throws IOException, SQLException, InterruptedException {
		StorageManager storage = StorageManager.getInstance();
		String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
		byte[] bytes = storage.downloadBytes(originalKey);
		ExifReader exifReader = new ExifReader(bytes);
		ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withMetadata(exifReader.getOutputSet())
		.withKeyOriginalJpg(originalKey)
		.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
		.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), exifReader.getDateTaken());
		storage.deleteResizedCache(S3KeyGenerator.getWebJpgResizedPrefix(idMedia));
	}

	public static void saveAvatar(int userId, InputStream is) {
		try {
			StorageManager storage = StorageManager.getInstance();
			byte[] originalBytes = is.readAllBytes();
			storage.uploadBytes(S3KeyGenerator.getOriginalUserAvatar(userId), originalBytes, StorageType.JPG);
			saveAvatarThumb(userId, originalBytes);
		} catch (IOException e) {
			logger.warn("saveAvatar(userId={}) failed: {}", userId, e.toString());
		}
	}

	public static void saveAvatarThumb(int userId, byte[] originalBytes) {
		try {
			StorageManager storage = StorageManager.getInstance();
			BufferedImage b = ImageReader.newBuilder().withBytes(originalBytes).build().getJpgBufferedImage();
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
			storage.uploadImage(S3KeyGenerator.getWebUserAvatar(userId), b, StorageType.JPG);
			b.flush();
		} catch (IOException | InterruptedException e) {
			logger.warn("saveAvatarThumb(userId={}) failed: {}", userId, e.toString());
		}
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage) throws SQLException {
		ImageSaver.newBuilder()
		.withBufferedImage(bufferedImage)
		.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
		.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
		.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
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
		.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
		.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
		.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), exifReader.getDateTaken());
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, SQLException, InterruptedException {
		ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build();
		ImageSaver.newBuilder()
		.withBufferedImage(imageReader.getJpgBufferedImage())
		.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
		.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
		.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, imageReader.getJpgBufferedImage().getWidth(), imageReader.getJpgBufferedImage().getHeight(), null);
	}
}
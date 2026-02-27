package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.db.Dao;

public class ImageHelper {
	public static void rotateImage(Dao dao, Connection c, int idMedia, Rotation rotation) throws IOException, SQLException, InterruptedException {
		StorageManager storage = StorageManager.getInstance();
		String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
		byte[] bytes = storage.downloadBytes(originalKey);
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			ImageSaver.newBuilder()
			.withBufferedImage(image)
			.withMetadata(exifReader.getOutputSet())
			.withKeyOriginalJpg(originalKey)
			.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
			.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
			.save();
			dao.setMediaMetadata(c, idMedia, image.getWidth(), image.getHeight(), exifReader.getDateTaken());
		}
		storage.deleteResizedCache(S3KeyGenerator.getWebJpgResizedPrefix(idMedia));
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
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			ImageSaver.newBuilder()
			.withBufferedImage(image)
			.withMetadata(exifReader.getOutputSet())
			.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
			.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
			.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
			.save();
			dao.setMediaMetadata(c, idMedia, image.getWidth(), image.getHeight(), exifReader.getDateTaken());
		}
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, SQLException, InterruptedException {
		try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			ImageSaver.newBuilder()
			.withBufferedImage(image)
			.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
			.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
			.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
			.save();
			dao.setMediaMetadata(c, idMedia, image.getWidth(), image.getHeight(), null);
		}
	}
}
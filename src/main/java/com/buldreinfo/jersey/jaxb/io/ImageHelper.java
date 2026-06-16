package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.helpers.ImageClassifier;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;

public class ImageHelper {
	private static final Logger logger = LogManager.getLogger();
	
	private static void analyzeAndSaveAsync(int idMedia, byte[] imgBytes, int width, int height, String logPrefix) {
		DatabaseContext.runAsync(() -> {
			DatabaseContext.runSql((dao, c) -> {
				try {
					var result = ImageClassifier.analyze(imgBytes);
					dao.getMediaRepo().saveMediaAnalysis(c, idMedia, width, height, result.hexColor(), result.labels(), result.objects(), false);
				} catch (Exception e) {
					logger.warn("AI Analysis failed{} for media {}: {}", logPrefix, idMedia, e.getMessage());
				}
			});
		});
	}

	private static byte[] getJpgBytes(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}
	}
	
	public static void rotateImage(Dao dao, Connection c, int idMedia, Rotation rotation) throws SQLException, IOException, InterruptedException {
		StorageManager storage = StorageManager.getInstance();
		String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
		byte[] bytes = storage.downloadBytes(originalKey);
		ExifReader exifReader = new ExifReader(bytes);
		
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			
			dao.getMediaRepo().deleteMediaAnalysis(c, idMedia);
			ImageSaver.save(image, originalKey, S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			dao.getMediaRepo().setMediaMetadata(c, idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
			byte[] rotatedBytes = getJpgBytes(image);
			analyzeAndSaveAsync(idMedia, rotatedBytes, width, height, " after rotation");
		} finally {
			try {
				S3KeyGenerator.getGeneratedMediaPrefixes(idMedia).forEach(storage::invalidateCache);
			} catch (Exception e) {
				logger.error("Failed to invalidate CDN storage cache for media {}", idMedia, e);
			}
		}
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage) throws SQLException, IOException {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		ImageSaver.save(bufferedImage, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
		dao.getMediaRepo().setMediaMetadata(c, idMedia, width, height, null, false);
		
		byte[] imgBytes = getJpgBytes(bufferedImage);
		analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, byte[] bytes) throws IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			dao.getMediaRepo().setMediaMetadata(c, idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
			analyzeAndSaveAsync(idMedia, bytes, width, height, "");
		}
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, InterruptedException, SQLException {
		try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
			dao.getMediaRepo().setMediaMetadata(c, idMedia, width, height, null, false);
			
			byte[] imgBytes = getJpgBytes(image);
			analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
		}
	}
}
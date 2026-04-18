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
import com.buldreinfo.jersey.jaxb.db.Dao;
import com.buldreinfo.jersey.jaxb.helpers.ImageClassifier;

public class ImageHelper {
	private static final Logger logger = LogManager.getLogger();
	
	public static void rotateImage(Dao dao, Connection c, int idMedia, boolean hasTaggedUser, Rotation rotation) throws SQLException, IOException, InterruptedException {
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
	        dao.deleteMediaAnalysis(c, idMedia);
	        ImageSaver.newBuilder()
	            .withBufferedImage(image)
	            .withMetadata(exifReader.getOutputSet())
	            .withKeyOriginalJpg(originalKey)
	            .withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
	            .withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
	            .save();
	        dao.setMediaMetadata(c, idMedia, width, height, exifReader.getDateTaken());
	        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	            ImageIO.write(image, "jpg", baos);
	            byte[] rotatedBytes = baos.toByteArray();
	            var result = ImageClassifier.analyze(rotatedBytes);
	            dao.saveMediaAnalysis(c, idMedia, width, height, hasTaggedUser, result.hexColor(), result.labels(), result.objects(), false);
	        } catch (Exception e) {
	            logger.warn("AI Re-Analysis failed after rotation for media {}: {}", idMedia, e.getMessage());
	        }
	    }
	    S3KeyGenerator.getGeneratedMediaPrefixes(idMedia).forEach(storage::invalidateCache);
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, BufferedImage bufferedImage, boolean hasTaggedUser) throws SQLException {
		int width = bufferedImage.getWidth();
	    int height = bufferedImage.getHeight();
		ImageSaver.newBuilder()
		.withBufferedImage(bufferedImage)
		.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
		.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
		.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
		.save();
		dao.setMediaMetadata(c, idMedia, width, height, null);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	        ImageIO.write(bufferedImage, "jpg", baos);
	        byte[] bytes = baos.toByteArray();
	        var result = ImageClassifier.analyze(bytes);
	        dao.saveMediaAnalysis(c, idMedia, width, height, hasTaggedUser, result.hexColor(), result.labels(), result.objects(), false);
	    } catch (Exception e) {
	        logger.warn("AI Analysis failed for media {}: {}. Batch script will pick this up later.", idMedia, e.getMessage());
	    }
	}

	public static void saveImage(Dao dao, Connection c, int idMedia, byte[] bytes, boolean hasTaggedUser) throws IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
		    int height = image.getHeight();
			ImageSaver.newBuilder()
			.withBufferedImage(image)
			.withMetadata(exifReader.getOutputSet())
			.withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
			.withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
			.withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
			.save();
			dao.setMediaMetadata(c, idMedia, width, height, exifReader.getDateTaken());
			try {
	            var result = ImageClassifier.analyze(bytes);
	            dao.saveMediaAnalysis(c, idMedia, width, height, hasTaggedUser, result.hexColor(), result.labels(), result.objects(), false);
	        } catch (Exception e) {
	            logger.warn("AI Analysis failed for media {}: {}. Batch script will pick this up later.", idMedia, e.getMessage());
	        }
		}
	}

	public static void saveImageFromEmbedVideo(Dao dao, Connection c, int idMedia, String embedVideoUrl) throws IOException, InterruptedException, SQLException {
	    try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build()) {
	        BufferedImage image = imageReader.getJpgBufferedImage();
	        int width = image.getWidth();
	        int height = image.getHeight();
	        ImageSaver.newBuilder()
	            .withBufferedImage(image)
	            .withKeyOriginalJpg(S3KeyGenerator.getOriginalJpg(idMedia))
	            .withKeyWebJpg(S3KeyGenerator.getWebJpg(idMedia))
	            .withKeyWebWebP(S3KeyGenerator.getWebWebp(idMedia))
	            .save();
	        dao.setMediaMetadata(c, idMedia, width, height, null);
	        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	            ImageIO.write(image, "jpg", baos);
	            byte[] bytes = baos.toByteArray();
	            var result = ImageClassifier.analyze(bytes);
	            boolean hasTaggedUser = true;
	            dao.saveMediaAnalysis(c, idMedia, width, height, hasTaggedUser, result.hexColor(), result.labels(), result.objects(), false);
	        } catch (Exception e) {
	            logger.warn("AI Analysis failed for embed media {}: {}. Batch script will pick this up later.", idMedia, e.getMessage());
	        }
	    }
	}
}
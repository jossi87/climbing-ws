package com.buldreinfo.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.service.ImageClassifierService;

public class ImageHelper {
	private static final Logger logger = LogManager.getLogger();
	
	private static void analyzeAndSaveAsync(ImageClassifierService imageClassifierService, ClimbingTransactionManager txManager, MediaRepository mediaRepo, int idMedia, byte[] imgBytes, int width, int height, String logPrefix) {
		Thread.startVirtualThread(() -> {
			try {
				var result = imageClassifierService.analyze(imgBytes);
				txManager.executeInTransaction(() -> {
					mediaRepo.saveMediaAnalysis(idMedia, width, height, result.hexColor(), result.labels(), result.objects(), false);
					return null;
				});
			} catch (Exception e) {
				logger.warn("AI Analysis failed{} for media {}: {}", logPrefix, idMedia, e.getMessage());
			}
		});
	}

	private static byte[] getJpgBytes(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}
	}
	
	public static void rotateImage(ImageClassifierService imageClassifierService, StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, int idMedia, Rotation rotation) throws SQLException, IOException, InterruptedException {
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
			
			mediaRepo.deleteMediaAnalysis(idMedia);
			ImageSaver.save(storage, image, originalKey, S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			mediaRepo.setMediaMetadata(idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
			byte[] rotatedBytes = getJpgBytes(image);
			analyzeAndSaveAsync(imageClassifierService, txManager, mediaRepo, idMedia, rotatedBytes, width, height, " after rotation");
		} finally {
			try {
				S3KeyGenerator.getGeneratedMediaPrefixes(idMedia).forEach(storage::invalidateCache);
			} catch (Exception e) {
				logger.error("Failed to invalidate CDN storage cache for media {}", idMedia, e);
			}
		}
	}

	public static void saveImage(ImageClassifierService imageClassifierService, StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, int idMedia, BufferedImage bufferedImage) throws SQLException, IOException {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		ImageSaver.save(storage, bufferedImage, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
		mediaRepo.setMediaMetadata(idMedia, width, height, null, false);
		
		byte[] imgBytes = getJpgBytes(bufferedImage);
		analyzeAndSaveAsync(imageClassifierService, txManager, mediaRepo, idMedia, imgBytes, width, height, "");
	}

	public static void saveImage(ImageClassifierService imageClassifierService, StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, int idMedia, byte[] bytes) throws IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			mediaRepo.setMediaMetadata(idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
			analyzeAndSaveAsync(imageClassifierService, txManager, mediaRepo, idMedia, bytes, width, height, "");
		}
	}

	public static void saveImageFromEmbedVideo(ImageClassifierService imageClassifierService, StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, int idMedia, String embedVideoUrl) throws IOException, InterruptedException, SQLException {
		try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build()) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
			mediaRepo.setMediaMetadata(idMedia, width, height, null, false);
			
			byte[] imgBytes = getJpgBytes(image);
			analyzeAndSaveAsync(imageClassifierService, txManager, mediaRepo, idMedia, imgBytes, width, height, "");
		}
	}
}
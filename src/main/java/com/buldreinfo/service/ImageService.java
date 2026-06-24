package com.buldreinfo.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.io.ExifReader;
import com.buldreinfo.io.ImageReader;
import com.buldreinfo.io.ImageSaver;
import com.buldreinfo.io.StorageManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImageService {
	private static final Logger logger = LogManager.getLogger();
	private final ObjectMapper objectMapper;
	private final ImageClassifierService imageClassifierService;
	private final StorageManager storage;
	private final ClimbingTransactionManager txManager;
	private final MediaRepository mediaRepo;

	public ImageService(ObjectMapper objectMapper, ImageClassifierService imageClassifierService, StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo) {
		this.objectMapper = objectMapper;
		this.imageClassifierService = imageClassifierService;
		this.storage = storage;
		this.txManager = txManager;
		this.mediaRepo = mediaRepo;
	}

	private void analyzeAndSaveAsync(int idMedia, byte[] imgBytes, int width, int height, String logPrefix) {
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

	private byte[] getJpgBytes(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}
	}
	
	public void rotateImage(int idMedia, Rotation rotation) throws SQLException, IOException, InterruptedException {
		String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
		byte[] bytes = storage.downloadBytes(originalKey);
		ExifReader exifReader = new ExifReader(bytes);
		
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(rotation)
				.build(objectMapper)) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			
			mediaRepo.deleteMediaAnalysis(idMedia);
			ImageSaver.save(storage, image, originalKey, S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			mediaRepo.setMediaMetadata(idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
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

	public void saveImage(int idMedia, BufferedImage bufferedImage) throws SQLException, IOException {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		ImageSaver.save(storage, bufferedImage, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
		mediaRepo.setMediaMetadata(idMedia, width, height, null, false);
		
		byte[] imgBytes = getJpgBytes(bufferedImage);
		analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
	}

	public void saveImage(int idMedia, byte[] bytes) throws IOException, SQLException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build(objectMapper)) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			mediaRepo.setMediaMetadata(idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());
			
			analyzeAndSaveAsync(idMedia, bytes, width, height, "");
		}
	}

	public void saveImageFromEmbedVideo(int idMedia, String embedVideoUrl) throws IOException, InterruptedException, SQLException {
		try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build(objectMapper)) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
			mediaRepo.setMediaMetadata(idMedia, width, height, null, false);
			
			byte[] imgBytes = getJpgBytes(image);
			analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
		}
	}
}
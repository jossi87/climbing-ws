package com.buldreinfo.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.io.ExifReader;
import com.buldreinfo.io.ImageReader;
import com.buldreinfo.io.ImageSaver;
import com.buldreinfo.io.StorageManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImageService {
	private static final Logger logger = LogManager.getLogger();
	private final TaskExecutor taskExecutor;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final ImageClassifierService imageClassifierService;
	private final StorageManager storage;
	private final MediaRepository mediaRepo;

	public ImageService(ObjectMapper objectMapper, HttpClient httpClient, ImageClassifierService imageClassifierService, TaskExecutor taskExecutor, StorageManager storage, MediaRepository mediaRepo) {
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
		this.imageClassifierService = imageClassifierService;
		this.taskExecutor = taskExecutor;
		this.storage = storage;
		this.mediaRepo = mediaRepo;
	}

	private void analyzeAndSaveAsync(int idMedia, byte[] imgBytes, int width, int height, String logPrefix) {
		Thread.startVirtualThread(() -> {
			try {
				var result = imageClassifierService.analyze(imgBytes);
				mediaRepo.saveMediaAnalysis(idMedia, width, height, result.hexColor(), result.labels(), result.objects(), false);
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

	public void rotateImage(int idMedia, Rotation rotation) {
		try {
			String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
			byte[] bytes = storage.downloadBytes(originalKey);
			ExifReader exifReader = new ExifReader(bytes);

			try (ImageReader imageReader = ImageReader.newBuilder()
					.withBytes(bytes)
					.withRotation(rotation)
					.build(objectMapper, httpClient)) {
				BufferedImage image = imageReader.getJpgBufferedImage();
				int width = image.getWidth();
				int height = image.getHeight();

				mediaRepo.deleteMediaAnalysis(idMedia);
				ImageSaver.save(taskExecutor, storage, image, originalKey, S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
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
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void saveImage(int idMedia, BufferedImage bufferedImage) throws IOException {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		ImageSaver.save(taskExecutor, storage, bufferedImage, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
		mediaRepo.setMediaMetadata(idMedia, width, height, null, false);

		byte[] imgBytes = getJpgBytes(bufferedImage);
		analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
	}

	public void saveImage(int idMedia, byte[] bytes) throws IOException, InterruptedException {
		ExifReader exifReader = new ExifReader(bytes);
		try (ImageReader imageReader = ImageReader.newBuilder()
				.withBytes(bytes)
				.withRotation(exifReader.getRotation())
				.build(objectMapper, httpClient)) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(taskExecutor, storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), exifReader.getOutputSet());
			mediaRepo.setMediaMetadata(idMedia, width, height, exifReader.getDateTaken(), exifReader.is360());

			analyzeAndSaveAsync(idMedia, bytes, width, height, "");
		}
	}

	public void saveImageFromEmbedVideo(int idMedia, String embedVideoUrl) throws IOException, InterruptedException {
		try (ImageReader imageReader = ImageReader.newBuilder().withEmbedVideoUrl(embedVideoUrl).build(objectMapper, httpClient)) {
			BufferedImage image = imageReader.getJpgBufferedImage();
			int width = image.getWidth();
			int height = image.getHeight();
			ImageSaver.save(taskExecutor, storage, image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia));
			mediaRepo.setMediaMetadata(idMedia, width, height, null, false);

			byte[] imgBytes = getJpgBytes(image);
			analyzeAndSaveAsync(idMedia, imgBytes, width, height, "");
		}
	}
}
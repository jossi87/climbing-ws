package com.buldreinfo.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.AsyncConfig;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.io.ExifReader;
import com.buldreinfo.io.ExifReader.ImageMetadataInfo;
import com.buldreinfo.io.ExifReader.ImageRotation;
import com.buldreinfo.io.StorageManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImageService {
	public static final int IMAGE_WEB_HEIGHT = 1440;
	public static final int IMAGE_WEB_WIDTH = 2560;
	private static final Logger logger = LogManager.getLogger();
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
	private static final Pattern VIMEO_PATTERN = Pattern.compile("vimeo\\.com/(?:video/)?([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern YT_PATTERN = Pattern.compile("(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})", Pattern.CASE_INSENSITIVE);

	private final ExifReader exifService;
	private final HttpClient httpClient;
	private final ImageClassifierService imageClassifierService;
	private final MediaRepository mediaRepo;
	private final ObjectMapper objectMapper;
	private final StorageManager storage;
	private final TaskExecutor imageProcessingExecutor;

	public ImageService(
			ObjectMapper objectMapper,
			HttpClient httpClient,
			ImageClassifierService imageClassifierService, 
			MediaRepository mediaRepo,
			ExifReader exifService,
			@Qualifier(AsyncConfig.IMAGE_EXECUTOR_BEAN_NAME) TaskExecutor imageProcessingExecutor,
			StorageManager storage) {
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
		this.imageClassifierService = imageClassifierService;
		this.mediaRepo = mediaRepo;
		this.exifService = exifService;
		this.imageProcessingExecutor = imageProcessingExecutor;
		this.storage = storage;
	}

	public void analyzeAndSaveAsync(int idMedia, byte[] imgBytes, int width, int height) {
		imageProcessingExecutor.execute(() -> {
			try {
				var result = imageClassifierService.analyze(imgBytes);
				mediaRepo.saveMediaAnalysis(idMedia, width, height, result.hexColor(), result.labels(), result.objects(), false);
			} catch (Exception e) {
				logger.warn("AI Analysis failed for media {}: {}", idMedia, e.getMessage());
			}
		});
	}

	public BufferedImage crop(BufferedImage src, int x, int y, int width, int height) {
		BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = cropped.createGraphics();
		try {
			g.drawImage(src, 0, 0, width, height, x, y, x + width, y + height, null);
		} finally {
			g.dispose();
		}
		return cropped;
	}

	public BufferedImage readFromEmbedUrl(String embedVideoUrl) throws IOException, InterruptedException {
		if (embedVideoUrl == null || embedVideoUrl.length() > 2048) {
			throw new IllegalArgumentException("Invalid URL length");
		}
		String imgUrl = null;
		Matcher ytMatcher = YT_PATTERN.matcher(embedVideoUrl);
		Matcher vimeoMatcher = VIMEO_PATTERN.matcher(embedVideoUrl);
		if (ytMatcher.find()) {
			imgUrl = "https://img.youtube.com/vi/" + ytMatcher.group(1) + "/0.jpg";
		} else if (vimeoMatcher.find()) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://vimeo.com/api/oembed.json?url=https://vimeo.com/" + vimeoMatcher.group(1)))
					.header("User-Agent", USER_AGENT)
					.timeout(Duration.ofSeconds(10))
					.GET().build();
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				imgUrl = objectMapper.readTree(response.body()).path("thumbnail_url").asText();
			}
		}
		if (imgUrl == null || imgUrl.isEmpty()) {
			throw new IllegalArgumentException("Could not extract image from: " + embedVideoUrl);
		}
		HttpRequest imgRequest = HttpRequest.newBuilder()
				.uri(URI.create(imgUrl))
				.header("User-Agent", USER_AGENT)
				.timeout(Duration.ofSeconds(15))
				.GET().build();
		HttpResponse<byte[]> imgResponse = httpClient.send(imgRequest, BodyHandlers.ofByteArray());
		String contentType = imgResponse.headers().firstValue("Content-Type").orElse("");
		if (!contentType.startsWith("image/")) {
			throw new IOException("Invalid image content type: " + contentType);
		}
		try (ByteArrayInputStream bais = new ByteArrayInputStream(imgResponse.body())) {
			BufferedImage image = ImageIO.read(bais);
			if (image == null) throw new IOException("Failed to decode image");
			return prepareAndRotate(image, null);
		}
	}

	public BufferedImage readImage(byte[] bytes, ImageRotation rotation) throws IOException {
		try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
			BufferedImage image = ImageIO.read(stream);
			Objects.requireNonNull(image, "BufferedImage could not be read");
			return prepareAndRotate(image, rotation);
		}
	}

	public BufferedImage resize(BufferedImage src, int width, int height) {
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resized.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(src, 0, 0, width, height, null);
		} finally {
			g.dispose();
		}
		return resized;
	}

	public void rotateImage(int idMedia, ImageRotation rotation) {
		try {
			String originalKey = S3KeyGenerator.getOriginalJpg(idMedia);
			byte[] bytes = storage.downloadBytes(originalKey);
			ImageMetadataInfo metadata = exifService.extractMetadata(bytes);
			BufferedImage image = readImage(bytes, rotation);
			try {
				mediaRepo.deleteMediaAnalysis(idMedia);
				saveImagesConcurrently(image, originalKey, S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), metadata.nativeMetadata());
				mediaRepo.setMediaMetadata(idMedia, image.getWidth(), image.getHeight(), metadata.dateTaken(), metadata.is360());
				analyzeAndSaveAsync(idMedia, getJpgBytes(image), image.getWidth(), image.getHeight());
			} finally {
				image.flush();
				S3KeyGenerator.getGeneratedMediaPrefixes(idMedia).forEach(storage::invalidateCache);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void saveImage(int idMedia, BufferedImage bufferedImage) throws IOException {
		saveImagesConcurrently(bufferedImage, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), null);
		mediaRepo.setMediaMetadata(idMedia, bufferedImage.getWidth(), bufferedImage.getHeight(), null, false);
		analyzeAndSaveAsync(idMedia, getJpgBytes(bufferedImage), bufferedImage.getWidth(), bufferedImage.getHeight());
	}

	public void saveImage(int idMedia, byte[] bytes) throws Exception {
		ImageMetadataInfo metadata = exifService.extractMetadata(bytes);
		BufferedImage image = readImage(bytes, metadata.rotation());
		try {
			saveImagesConcurrently(image, S3KeyGenerator.getOriginalJpg(idMedia), S3KeyGenerator.getWebJpg(idMedia), S3KeyGenerator.getWebWebp(idMedia), metadata.nativeMetadata());
			mediaRepo.setMediaMetadata(idMedia, image.getWidth(), image.getHeight(), metadata.dateTaken(), metadata.is360());
			analyzeAndSaveAsync(idMedia, bytes, image.getWidth(), image.getHeight());
		} finally {
			image.flush();
		}
	}

	public void saveImagesConcurrently(BufferedImage bufferedImage, String keyOriginalJpg, String keyWebJpg, String keyWebWebP, IIOMetadata nativeMetadata) {
		var originalFuture = CompletableFuture.runAsync(() -> {
			try {
				if (nativeMetadata == null) {
					storage.uploadImage(keyOriginalJpg, bufferedImage, StorageType.JPG);
				} else {
					storage.uploadBytes(keyOriginalJpg, writeImageWithMetadata(bufferedImage, nativeMetadata, "jpg"), StorageType.JPG);
				}
			} catch (Exception e) {
				throw new RuntimeException("Original upload failed", e);
			}
		}, imageProcessingExecutor);
		var webFuture = CompletableFuture.runAsync(() -> {
			try {
				BufferedImage webImage = bufferedImage;
				if (bufferedImage.getWidth() > IMAGE_WEB_WIDTH || bufferedImage.getHeight() > IMAGE_WEB_HEIGHT) {
					double ratio = Math.min((double) IMAGE_WEB_WIDTH / bufferedImage.getWidth(), (double) IMAGE_WEB_HEIGHT / bufferedImage.getHeight());
					int targetWidth = (int) Math.round(bufferedImage.getWidth() * ratio);
					int targetHeight = (int) Math.round(bufferedImage.getHeight() * ratio);

					webImage = resize(bufferedImage, targetWidth, targetHeight);
				}
				try {
					storage.uploadImage(keyWebJpg, webImage, StorageType.JPG);
					storage.uploadImage(keyWebWebP, webImage, StorageType.WEBP);
				} finally {
					if (webImage != bufferedImage) webImage.flush();
				}
			} catch (Exception e) {
				throw new RuntimeException("Web upload failed", e);
			}
		}, imageProcessingExecutor);
		CompletableFuture.allOf(originalFuture, webFuture).join();
	}

	private byte[] getJpgBytes(BufferedImage image) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}
	}

	private BufferedImage prepareAndRotate(BufferedImage src, ImageRotation rotation) {
		BufferedImage standardized = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g1 = standardized.createGraphics();
		try {
			g1.drawImage(src, 0, 0, Color.WHITE, null);
		} finally {
			g1.dispose();
		}
		if (src != standardized) src.flush();
		if (rotation == null) return standardized;

		double angle = switch (rotation) {
		case CW_90 -> 90;
		case CW_180 -> 180;
		case CW_270 -> 270;
		};

		int width = standardized.getWidth(), height = standardized.getHeight();
		int newWidth = (rotation == ImageRotation.CW_90 || rotation == ImageRotation.CW_270) ? height : width;
		int newHeight = (rotation == ImageRotation.CW_90 || rotation == ImageRotation.CW_270) ? width : height;

		BufferedImage dest = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = dest.createGraphics();
		try {
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			AffineTransform at = new AffineTransform();
			at.translate((newWidth - width) / 2.0, (newHeight - height) / 2.0);
			at.rotate(Math.toRadians(angle), width / 2.0, height / 2.0);
			g2.setTransform(at);
			g2.drawImage(standardized, 0, 0, null);
		} finally {
			g2.dispose();
		}
		standardized.flush();
		return dest;
	}

	private byte[] writeImageWithMetadata(BufferedImage image, IIOMetadata metadata, String format) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
		if (!writers.hasNext()) throw new IOException("No writer for: " + format);
		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.9f);
			}
			writer.write(null, new IIOImage(image, null, metadata), param);
			return baos.toByteArray();
		} finally {
			writer.dispose();
		}
	}
}
package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.beans.StorageType;

public class ImageSaver {
	public static final int IMAGE_WEB_WIDTH = 2560;
	public static final int IMAGE_WEB_HEIGHT = 1440;

	private final BufferedImage bufferedImage;
	private final String keyOriginalJpg;
	private final String keyWebJpg;
	private final String keyWebWebP;
	private final TiffOutputSet metadata;

	protected static class ImageSaverBuilder {
		private BufferedImage bufferedImage;
		private String keyOriginalJpg;
		private String keyWebJpg;
		private String keyWebWebP;
		private TiffOutputSet metadata;

		protected void save() {
			new ImageSaver(this).execute();
		}

		protected ImageSaverBuilder withBufferedImage(BufferedImage bufferedImage) {
			this.bufferedImage = bufferedImage;
			return this;
		}

		protected ImageSaverBuilder withMetadata(TiffOutputSet metadata) {
			this.metadata = metadata;
			return this;
		}

		protected ImageSaverBuilder withKeyOriginalJpg(String keyOriginalJpg) {
			this.keyOriginalJpg = keyOriginalJpg;
			return this;
		}

		protected ImageSaverBuilder withKeyWebJpg(String keyWebJpg) {
			this.keyWebJpg = keyWebJpg;
			return this;
		}

		protected ImageSaverBuilder withKeyWebWebP(String keyWebWebP) {
			this.keyWebWebP = keyWebWebP;
			return this;
		}
	}

	protected static ImageSaverBuilder newBuilder() {
		return new ImageSaverBuilder();
	}

	private ImageSaver(ImageSaverBuilder builder) {
		this.bufferedImage = Objects.requireNonNull(builder.bufferedImage, "BufferedImage cannot be null");
		this.keyOriginalJpg = Objects.requireNonNull(builder.keyOriginalJpg, "Original JPG key is required");
		this.keyWebJpg = Objects.requireNonNull(builder.keyWebJpg, "Web JPG key is required");
		this.keyWebWebP = Objects.requireNonNull(builder.keyWebWebP, "Web WebP key is required");
		this.metadata = builder.metadata;
	}

	private void execute() {
		StorageManager storage = StorageManager.getInstance();

		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var originalFuture = CompletableFuture.runAsync(() -> {
				try {
					if (metadata == null) {
						storage.uploadImage(keyOriginalJpg, bufferedImage, StorageType.JPG);
					} else {
						try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
							 ByteArrayOutputStream finalOs = new ByteArrayOutputStream()) {
							ImageIO.write(bufferedImage, "jpg", baos);
							new ExifRewriter().updateExifMetadataLossless(baos.toByteArray(), finalOs, metadata);
							storage.uploadBytes(keyOriginalJpg, finalOs.toByteArray(), StorageType.JPG);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Original upload failed: " + e.getMessage(), e);
				}
			}, executor);

			var webFuture = CompletableFuture.runAsync(() -> {
				try {
					BufferedImage webImage = bufferedImage;
					if (bufferedImage.getWidth() > IMAGE_WEB_WIDTH || bufferedImage.getHeight() > IMAGE_WEB_HEIGHT) {
						webImage = Scalr.resize(bufferedImage, Scalr.Method.ULTRA_QUALITY, 
								Scalr.Mode.AUTOMATIC, IMAGE_WEB_WIDTH, IMAGE_WEB_HEIGHT, Scalr.OP_ANTIALIAS);
					}
					try {
						storage.uploadImage(keyWebJpg, webImage, StorageType.JPG);
						storage.uploadImage(keyWebWebP, webImage, StorageType.WEBP);
					} finally {
						if (webImage != bufferedImage) {
							webImage.flush();
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Web upload failed: " + e.getMessage(), e);
				}
			}, executor);

			CompletableFuture.allOf(originalFuture, webFuture).join();
		}
	}
}
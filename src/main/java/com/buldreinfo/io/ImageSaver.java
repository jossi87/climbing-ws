package com.buldreinfo.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.imgscalr.Scalr;

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.controller.BaseController;

public class ImageSaver {
	public static final int IMAGE_WEB_WIDTH = 2560;
	public static final int IMAGE_WEB_HEIGHT = 1440;

	public static void save(BufferedImage bufferedImage, String keyOriginalJpg, String keyWebJpg, String keyWebWebP) {
		new ImageSaver(bufferedImage, keyOriginalJpg, keyWebJpg, keyWebWebP, null).execute();
	}

	public static void save(BufferedImage bufferedImage, String keyOriginalJpg, String keyWebJpg, String keyWebWebP, TiffOutputSet metadata) {
		new ImageSaver(bufferedImage, keyOriginalJpg, keyWebJpg, keyWebWebP, metadata).execute();
	}

	private final BufferedImage bufferedImage;
	private final String keyOriginalJpg;
	private final String keyWebJpg;
	private final String keyWebWebP;
	private final TiffOutputSet metadata;

	private ImageSaver(BufferedImage bufferedImage, String keyOriginalJpg, String keyWebJpg, String keyWebWebP, TiffOutputSet metadata) {
		this.bufferedImage = prepareImageForJpg(Objects.requireNonNull(bufferedImage, "BufferedImage cannot be null"));
		this.keyOriginalJpg = Objects.requireNonNull(keyOriginalJpg, "Original JPG key is required");
		this.keyWebJpg = Objects.requireNonNull(keyWebJpg, "Web JPG key is required");
		this.keyWebWebP = Objects.requireNonNull(keyWebWebP, "Web WebP key is required");
		this.metadata = metadata;
	}

	private BufferedImage prepareImageForJpg(BufferedImage src) {
		if (src.getType() == BufferedImage.TYPE_INT_RGB) {
			return src;
		}
		BufferedImage rgbImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgbImage.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, src.getWidth(), src.getHeight());
			g.drawImage(src, 0, 0, null);
		} finally {
			g.dispose();
		}
		return rgbImage;
	}

	private void execute() {
		StorageManager storage = StorageManager.getInstance();
		Executor executor = BaseController.executor;

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
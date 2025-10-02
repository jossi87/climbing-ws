package com.buldreinfo.jersey.jaxb.io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.lang3.SystemUtils;
import org.imgscalr.Scalr;

import com.google.common.base.Preconditions;

public class ImageSaver {
	public static int IMAGE_WEB_WIDTH = 2560;
	public static int IMAGE_WEB_HEIGHT = 1440;
	
	protected static class ImageSaverBuilder {
		private BufferedImage bufferedImage;
		private Path pathOriginalJpg;
		private Path pathWebJpg;
		private Path pathWebWebP;
		private TiffOutputSet metadata;
		protected ImageSaver save() {
			return new ImageSaver(this);
		}
		protected ImageSaverBuilder withBufferedImage(BufferedImage bufferedImage) {
			this.bufferedImage = bufferedImage;
			return this;
		}
		protected ImageSaverBuilder withMetadata(TiffOutputSet metadata) {
			this.metadata = metadata;
			return this;
		}
		protected ImageSaverBuilder withPathOriginalJpg(Path pathOriginalJpg) {
			this.pathOriginalJpg = pathOriginalJpg;
			return this;
		}
		protected ImageSaverBuilder withPathWebJpg(Path pathWebJpg) {
			this.pathWebJpg = pathWebJpg;
			return this;
		}
		protected ImageSaverBuilder withPathWebWebP(Path pathWebWebP) {
			this.pathWebWebP = pathWebWebP;
			return this;
		}
	}

	protected static ImageSaverBuilder newBuilder() {
		return new ImageSaverBuilder();
	}

	private ImageSaver(ImageSaverBuilder builder) {
		Preconditions.checkNotNull(builder.bufferedImage);
		if (builder.pathOriginalJpg != null) {
			Preconditions.checkNotNull(builder.pathOriginalJpg);
			Preconditions.checkNotNull(builder.pathWebJpg);
			Preconditions.checkNotNull(builder.pathWebWebP);
			// Original jpg
			CompletableFuture<Void> fOriginal = CompletableFuture.runAsync(() -> {
				try {
					IOHelper.deleteIfExistsCreateParent(builder.pathOriginalJpg);
					if (builder.metadata == null) {
						ImageIO.write(builder.bufferedImage, "jpg", builder.pathOriginalJpg.toFile());
					}
					else {
						try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
							ImageIO.write(builder.bufferedImage, "jpg", baos);
							try (OutputStream os = new FileOutputStream(builder.pathOriginalJpg.toFile())) {
								new ExifRewriter().updateExifMetadataLossless(baos.toByteArray(), os, builder.metadata);
							}
						}
					}
					IOHelper.setFilePermission(builder.pathOriginalJpg);
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			});
			// Scaled jpg and webp
			CompletableFuture<Void> fScaled = CompletableFuture.runAsync(() -> {
				try {
					// JPG
					IOHelper.deleteIfExistsCreateParent(builder.pathWebJpg);
					if (builder.bufferedImage.getWidth() > IMAGE_WEB_WIDTH && builder.bufferedImage.getHeight() > IMAGE_WEB_HEIGHT) {
						BufferedImage b = Scalr.resize(builder.bufferedImage, Scalr.Method.ULTRA_QUALITY, IMAGE_WEB_WIDTH, IMAGE_WEB_HEIGHT, Scalr.OP_ANTIALIAS);
						ImageIO.write(b, "jpg", builder.pathWebJpg.toFile());
					}
					else {
						ImageIO.write(builder.bufferedImage, "jpg", builder.pathWebJpg.toFile());
					}
					IOHelper.setFilePermission(builder.pathWebJpg);
					// WebP
					IOHelper.deleteIfExistsCreateParent(builder.pathWebWebP);
					String[] cmd = null;
					if (!SystemUtils.IS_OS_WINDOWS) {
						cmd = new String[] { "/bin/bash", "-c", "cwebp \"" + builder.pathWebJpg.toString() + "\" -o \"" + builder.pathWebWebP.toString() + "\"" };
					}
					else {
						cmd = new String[] { "\"G:/My Drive/web/buldreinfo/sw/libwebp-1.3.2-windows-x64/bin/cwebp.exe\"", "\"" + builder.pathWebJpg.toString() + "\"", "-o", "\"" + builder.pathWebWebP.toString() + "\""};
					}
					Process process = Runtime.getRuntime().exec(cmd);
					process.waitFor();
					IOHelper.setFilePermission(builder.pathWebWebP);
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			});
			// Run tasks
			CompletableFuture.allOf(fOriginal, fScaled).join();
		}
		else {
			throw new RuntimeException("Invalid builder");
		}
	}
}
package com.buldreinfo.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for writing JPEG images with resilience against corrupted Huffman tables
 * in source image metadata. The JDK's native JPEG writer (and by extension the
 * TwelveMonkeys plugin) can throw "Missing Huffman code table entry" when the
 * source image's native metadata contains corrupted Huffman tables.
 */
public final class JpegWriter {
	private static final Logger logger = LoggerFactory.getLogger(JpegWriter.class);

	/**
	 * Writes a BufferedImage to JPEG bytes. If the write fails due to a Huffman
	 * table error, retries without native metadata (forcing fresh Huffman tables).
	 * Also proactively flattens any alpha channel since JPEG does not support it.
	 */
	public static byte[] writeJpeg(BufferedImage image, IIOMetadata nativeMetadata) throws IOException {
		BufferedImage imageToProcess = ensureNoAlpha(image);

		try {
			return writeJpegInternal(imageToProcess, nativeMetadata);
		} catch (IIOException e) {
			if (e.getMessage() != null && e.getMessage().contains("Huffman")) {
				logger.warn("JPEG Huffman table error detected, retrying without native metadata: {}", e.getMessage());
				return writeJpegInternal(imageToProcess, null);
			}
			throw e;
		}
	}

	/**
	 * Writes a BufferedImage to JPEG bytes with compression quality 0.9.
	 */
	private static byte[] writeJpegInternal(BufferedImage image, IIOMetadata metadata) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
		if (!writers.hasNext()) throw new IOException("No JPEG writer found");
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

	/**
	 * If the image has an alpha channel, flattens it onto a white background
	 * (JPEG does not support alpha). Returns the original image if no alpha.
	 */
	public static BufferedImage ensureNoAlpha(BufferedImage image) {
		if (image.getColorModel().hasAlpha()) {
			BufferedImage flattened = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = flattened.createGraphics();
			try {
				g.drawImage(image, 0, 0, Color.WHITE, null);
			} finally {
				g.dispose();
			}
			return flattened;
		}
		return image;
	}

	private JpegWriter() {
	}
}

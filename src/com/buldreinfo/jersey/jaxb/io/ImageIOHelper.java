package com.buldreinfo.jersey.jaxb.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.imgscalr.Scalr;

import com.google.common.base.Preconditions;

public class ImageIOHelper {
	public static String writeImageFileWithExif(byte[] bytes, Path dst) throws IOException, ImageReadException, ImageWriteException, ParseException {
		BufferedImage b = createImageFromBytes(bytes);
		ExifReader exif = new ExifReader(bytes);
		if (exif.getRotation() != null) {
			b = Scalr.rotate(b, exif.getRotation(), Scalr.OP_ANTIALIAS);
		}
		if (exif.getOutputSet() == null) {
			writeToPath(b, dst);
		}
		else {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(b, "jpg", baos);
				try (OutputStream os = new FileOutputStream(dst.toFile())) {
					new ExifRewriter().updateExifMetadataLossless(baos.toByteArray(), os, exif.getOutputSet());
				}
			}
		}
		return exif.getDateTaken();
	}
	
	private static BufferedImage createImageFromBytes(byte[] bytes) throws IOException {
	    try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
	    	return ImageIO.read(stream);
	    }
	}
	
	public static byte[] writeToByteArray(BufferedImage b) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		boolean ok = ImageIO.write(b, "jpg", baos);
		if (!ok) {
			boolean isPng = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(b), "png").hasNext();
			Preconditions.checkArgument(isPng, "No writer available");
			BufferedImage newImage = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
			newImage.createGraphics().drawImage(b, 0, 0, Color.BLACK, null);
			Preconditions.checkArgument(ImageIO.write(newImage, "jpg", baos));
		}
		Preconditions.checkArgument(baos != null);
		return baos.toByteArray();
	}
	
	public static void writeToPath(BufferedImage b, Path dst) throws IOException {
		Preconditions.checkArgument(Files.exists(dst.getParent()), dst.getParent().toString() + " does not exist");
		Preconditions.checkArgument(!Files.exists(dst), dst.toString() + " already exists");
		boolean ok = ImageIO.write(b, "jpg", dst.toFile());
		if (!ok) {
			boolean isPng = ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(b), "png").hasNext();
			Preconditions.checkArgument(isPng, "No writer available, cannot save " + dst.toString());
			
			BufferedImage newImage = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_RGB);
			newImage.createGraphics().drawImage(b, 0, 0, Color.BLACK, null);
			ok = ImageIO.write(newImage, "jpg", dst.toFile());
			newImage.flush();
			Preconditions.checkArgument(ok, "Could not save " + dst.toString());
		}
	}
}

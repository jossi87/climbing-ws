package com.buldreinfo.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;

@Service
public class ExifReader {
	public enum ImageRotation {CW_180, CW_270, CW_90}
	public record ImageMetadataInfo(ImageRotation rotation, LocalDateTime dateTaken, boolean is360, IIOMetadata nativeMetadata) {}

	private static final byte[] EQUIRECTANGULAR_BYTES = "equirectangular".getBytes(StandardCharsets.ISO_8859_1);
	
	public ImageMetadataInfo extractMetadata(byte[] bytes) throws Exception {
		try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
			Metadata metadata = ImageMetadataReader.readMetadata(is);

			ImageRotation rotation = getRotation(metadata);
			LocalDateTime dateTaken = getDateTaken(metadata);
			boolean is360 = checkIs360(bytes, metadata);
			IIOMetadata nativeMetadata = getNativeMetadata(bytes);

			return new ImageMetadataInfo(rotation, dateTaken, is360, nativeMetadata);
		}
	}

	private boolean checkIs360(byte[] bytes, Metadata metadata) {
		JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
		if (jpegDir != null) {
			try {
				int width = jpegDir.getInt(JpegDirectory.TAG_IMAGE_WIDTH);
				int height = jpegDir.getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
				if (width > 0 && height > 0 && width == height * 2) {
					return true;
				}
			} catch (Exception _) {}
		}
		return containsSequence(bytes, EQUIRECTANGULAR_BYTES);
	}

	private boolean containsSequence(byte[] source, byte[] target) {
		if (target.length == 0 || source == null || source.length < target.length) return false;
		for (int i = 0; i <= source.length - target.length; i++) {
			boolean match = true;
			for (int j = 0; j < target.length; j++) {
				if (source[i + j] != target[j]) {
					match = false;
					break;
				}
			}
			if (match) return true;
		}
		return false;
	}

	private LocalDateTime getDateTaken(Metadata metadata) {
		ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
		if (directory != null) {
			Date date = directory.getDate(ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
			if (date != null) {
				return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			}
		}
		return null;
	}

	private IIOMetadata getNativeMetadata(byte[] bytes) {
		try (ByteArrayInputStream is = new ByteArrayInputStream(bytes);
				ImageInputStream iis = ImageIO.createImageInputStream(is)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				reader.setInput(iis, true);
				IIOMetadata metadata = reader.getImageMetadata(0);
				reader.dispose();
				return metadata;
			}
		} catch (IOException _) {}
		return null;
	}

	private ImageRotation getRotation(Metadata metadata) {
		ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
		if (directory != null && directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
			try {
				int orientation = directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
				return switch (orientation) {
				case 3, 4 -> ImageRotation.CW_180;
				case 5, 8 -> ImageRotation.CW_270;
				case 6, 7 -> ImageRotation.CW_90;
				default -> null;
				};
			} catch (Exception _) {}
		}
		return null;
	}
}
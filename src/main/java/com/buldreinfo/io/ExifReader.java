package com.buldreinfo.io;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;

public class ExifReader {
	private static final Logger logger = LogManager.getLogger();
	private static final String EXIF_DATE_PATTERN = "yyyy:MM:dd HH:mm:ss";
	private static final byte[] EQUIRECTANGULAR_BYTES = "equirectangular".getBytes(StandardCharsets.ISO_8859_1);
	
	private final Rotation rotation;
	private final TiffOutputSet outputSet;
	private final LocalDateTime dateTaken;
	private final boolean is360;
	
	public ExifReader(byte[] bytes) throws IOException {
		TiffImageMetadata imageMetadata = getTiffImageMetadata(bytes);
		if (imageMetadata != null) {
			this.rotation = getExifOrientation(imageMetadata);
			TiffOutputSet parsedSet = imageMetadata.getOutputSet();
			if (parsedSet == null) {
				parsedSet = new TiffOutputSet();
			}
			parsedSet.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			this.outputSet = parsedSet;
			this.dateTaken = getExifDateValue(imageMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
		} else {
			this.rotation = null;
			this.outputSet = null;
			this.dateTaken = null;
		}
		this.is360 = checkIs360(bytes);
	}

	private boolean checkIs360(byte[] bytes) {
		try {
			Dimension dimension = Imaging.getImageSize(bytes);
			if (dimension.width == dimension.height * 2) {
				return true;
			}

			String xmpXml = Imaging.getXmpXml(bytes);
			if (xmpXml != null && xmpXml.contains("equirectangular")) {
				return true;
			}

			return containsSequence(bytes, EQUIRECTANGULAR_BYTES);
		} catch (Exception e) {
			logger.warn("Failed to check if image is 360 panorama: {}", e.getMessage(), e);
		}
		return false;
	}

	private boolean containsSequence(byte[] source, byte[] target) {
		if (target.length == 0) return false;
		int limit = Math.min(source.length, 128 * 1024) - target.length;
		for (int i = 0; i <= limit; i++) {
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

	private LocalDateTime getExifDateValue(ImageMetadata imageMetadata, TagInfo tagInfo) throws ImagingException {
		if (imageMetadata == null) {
			return null;
		}
		String exifDateStr = getExifStringValue(imageMetadata, tagInfo);
		if (exifDateStr == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(exifDateStr, DateTimeFormatter.ofPattern(EXIF_DATE_PATTERN));
		} catch (DateTimeParseException e) {
			logger.warn("Failed to parse EXIF date string '{}': {}", exifDateStr, e.getMessage());
			return null;
		}
	}

	private Rotation getExifOrientation(ImageMetadata imageMetadata) throws ImagingException {
		TiffField field = getTiffField(imageMetadata, TiffTagConstants.TIFF_TAG_ORIENTATION);
		if (field != null) {
			int value = field.getIntValue();
			if (value == 0 || value == 256 || value == TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL || value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL) {
				return null;
			} else if (value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_180 || value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL) {
				return Rotation.CW_180;
			} else if (value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW || value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW) {
				return Rotation.CW_270;
			} else if (value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW || value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW) {
				return Rotation.CW_90;
			} else {
				throw new RuntimeException("Invalid exif orientation: " + value);
			}
		}
		return null;
	}

	private String getExifStringValue(ImageMetadata imageMetadata, TagInfo tagInfo) throws ImagingException {
		TiffField field = getTiffField(imageMetadata, tagInfo);
		if (field == null) {
			return null;
		}
		
		String exifStr = null;
		try {
			exifStr = field.getStringValue();
		} catch (Exception _) {
			Object val = field.getValue();
			if (val instanceof String[] arr && arr.length > 0) {
				exifStr = arr[0];
			} else if (val != null) {
				exifStr = val.toString();
			}
		}
		
		if (exifStr != null) {
			int nullIdx = exifStr.indexOf('\u0000');
			if (nullIdx != -1) {
				exifStr = exifStr.substring(0, nullIdx);
			}
			exifStr = exifStr.trim();
		}
		return exifStr;
	}

	private TiffField getTiffField(ImageMetadata imageMetadata, TagInfo tagInfo) throws ImagingException {
		if (imageMetadata == null) {
			return null;
		}
		if (imageMetadata instanceof JpegImageMetadata jpegMetadata) {
			return jpegMetadata.findExifValueWithExactMatch(tagInfo);
		} else if (imageMetadata instanceof TiffImageMetadata tiffMetadata) {
			return tiffMetadata.findField(tagInfo, true);
		}
		return null;
	}

	private TiffImageMetadata getTiffImageMetadata(byte[] imageBytes) {
		try {
			ImageMetadata imageMetadata = Imaging.getMetadata(imageBytes);
			if (imageMetadata == null) {
				return null;
			}
			if (imageMetadata instanceof JpegImageMetadata jpegMetadata) {
				return jpegMetadata.getExif();
			} else if (imageMetadata instanceof TiffImageMetadata tiffMetadata) {
				return tiffMetadata;
			}
		} catch (Exception e) {
			logger.warn("Failed to retrieve image TIFF metadata properties: {}", e.getMessage());
		}
		return null;
	}

	public LocalDateTime getDateTaken() {
		return dateTaken;
	}

	public TiffOutputSet getOutputSet() {
		return outputSet;
	}

	public Rotation getRotation() {
		return rotation;
	}

	public boolean is360() {
		return is360;
	}
}
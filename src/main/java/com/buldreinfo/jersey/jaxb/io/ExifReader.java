package com.buldreinfo.jersey.jaxb.io;

import java.io.IOException;
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
	private static Logger logger = LogManager.getLogger();
	private static final String EXIF_DATE_PATTERN = "yyyy:MM:dd HH:mm:ss";
	private final Rotation rotation;
	private final TiffOutputSet outputSet;
	private final LocalDateTime dateTaken;

	protected ExifReader(byte[] bytes) throws IOException {
		TiffImageMetadata imageMetadata = getTiffImageMetadata(bytes);
		if (imageMetadata != null) {
			// Read exif orientation and remove from metadata. Save rotated image instead of keeping exif orientation in file.
			this.rotation = getExifOrientation(imageMetadata);
			TiffOutputSet outputSet = imageMetadata.getOutputSet();
			outputSet.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			this.outputSet = outputSet;
			this.dateTaken = getExifDateValue(imageMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
		}
		else {
			this.rotation = null;
			this.outputSet = null;
			this.dateTaken = null;
		}
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
			// E.g. "Text '0000:00:00 00:00:00' could not be parsed: Invalid value for YearOfEra (valid values 1 - 999999999/1000000000): 0"
			logger.warn(e.getMessage(), e);
			return null;
		}
	}

	private Rotation getExifOrientation(ImageMetadata imageMetadata) throws ImagingException {
		TiffField field = getTiffField(imageMetadata, TiffTagConstants.TIFF_TAG_ORIENTATION);
		if (field != null) {
			int value = field.getIntValue();
			if (value == 0 || value == 256 || value == TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL || value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL) {
				return null;
			}
			else if (value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_180 || value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL) {
				return Rotation.CW_180;
			}
			else if (value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW || value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW) {
				return Rotation.CW_270;
			}
			else if (value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW || value == TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW) {
				return Rotation.CW_90;
			}
			else {
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
		} catch (ImagingException e) {
			exifStr = ((String[])field.getValue())[0];
		}
		if (exifStr != null) {
			int nullIdx = exifStr.indexOf('\u0000');
			if (nullIdx != -1) {
				exifStr = exifStr.substring(0, nullIdx);
			}
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
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return null;
	}

	protected LocalDateTime getDateTaken() {
		return dateTaken;
	}

	protected TiffOutputSet getOutputSet() {
		return outputSet;
	}

	protected Rotation getRotation() {
		return rotation;
	}
}
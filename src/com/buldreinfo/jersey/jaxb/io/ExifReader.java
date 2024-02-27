package com.buldreinfo.jersey.jaxb.io;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.imgscalr.Scalr.Rotation;

public class ExifReader {
	private static final String EXIF_DATE_PATTERN = "yyyy:MM:dd HH:mm:ss";
	private final Rotation rotation;
	private final TiffOutputSet outputSet;
	private final Date dateTaken;

	protected ExifReader(byte[] bytes) throws IOException, ImageReadException, ImageWriteException, ParseException {
		TiffImageMetadata imageMetadata = getTiffImageMetadata(bytes);
		if (imageMetadata != null) {
			// Read exif orientation and remove from metadata. Save rotated image instead of keeping exif orientation in file.
			this.rotation = getExifOrientation(imageMetadata);
			TiffOutputSet outputSet = imageMetadata.getOutputSet();
			outputSet.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
			this.outputSet = outputSet;
			this.dateTaken = getEXIFDateValue(imageMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, ExifTagConstants.EXIF_TAG_SUB_SEC_TIME_ORIGINAL);
		}
		else {
			this.rotation = null;
			this.outputSet = null;
			this.dateTaken = null;
		}
	}
	
	private Date getEXIFDateValue(ImageMetadata imageMetadata, TagInfo tagInfo, TagInfo subTagInfo) throws ParseException, ImageReadException {
		if (imageMetadata == null) {
			return null;
		}
		String exifDateStr = getExifStringValue(imageMetadata, tagInfo);
		if (exifDateStr == null) {
			return null;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat(EXIF_DATE_PATTERN);
		Date date = dateFormat.parse(exifDateStr);
		if (subTagInfo != null) {
			String subSec = getExifStringValue(imageMetadata, subTagInfo);
			if (subSec != null && !subSec.isEmpty()) {
				date = new Date(date.getTime() + (Integer.parseInt(subSec) * 10));
			}
		}
		return date;
	}

	private Rotation getExifOrientation(ImageMetadata imageMetadata) throws ImageReadException {
		TiffField field = getTiffField(imageMetadata, TiffTagConstants.TIFF_TAG_ORIENTATION);
		if (field != null) {
			int value = field.getIntValue();
			if (value == 0 || value == TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL || value == TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL) {
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

	private String getExifStringValue(ImageMetadata imageMetadata, TagInfo tagInfo) throws ImageReadException {
		TiffField field = getTiffField(imageMetadata, tagInfo);
		if (field == null) {
			return null;
		}
		String exifStr = field.getStringValue();
		if (exifStr != null) {
			int nullIdx = exifStr.indexOf('\u0000');
			if (nullIdx != -1) {
				exifStr = exifStr.substring(0, nullIdx);
			}
		}
		return exifStr;
	}

	private TiffField getTiffField(ImageMetadata imageMetadata, TagInfo tagInfo) throws ImageReadException {
		if (imageMetadata == null) {
			return null;
		}
		if (imageMetadata instanceof JpegImageMetadata jpegMetadata) {
			return jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
		} else if (imageMetadata instanceof TiffImageMetadata tiffMetadata) {
			return tiffMetadata.findField(tagInfo, true);
		}
		return null;
	}

	private TiffImageMetadata getTiffImageMetadata(byte[] imageBytes) throws ImageReadException, IOException {
		ImageMetadata imageMetadata = Imaging.getMetadata(imageBytes);
		if (imageMetadata == null) {
			return null;
		}
		if (imageMetadata instanceof JpegImageMetadata jpegMetadata) {
			return jpegMetadata.getExif();
		} else if (imageMetadata instanceof TiffImageMetadata tiffMetadata) {
			return tiffMetadata;
		}
		return null;
	}

	protected String getDateTaken() {
		if (dateTaken == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(dateTaken);
	}

	protected TiffOutputSet getOutputSet() {
		return outputSet;
	}

	protected Rotation getRotation() {
		return rotation;
	}
}
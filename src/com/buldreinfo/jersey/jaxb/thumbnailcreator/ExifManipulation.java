package com.buldreinfo.jersey.jaxb.thumbnailcreator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

class ExifManipulation {
    /**
     * Represents an interface for processing metadata.
     */
    public interface MetadataProcessor {
        public void process(TiffOutputSet tiffMetadata) throws ImageReadException, ImageWriteException;
    }

    /**
     * Retrieve the rotation specified by EXIF.
     * @return The EXIF rotation, or NULL if not specified.
     */
    public static ExifOrientation getExifRotation(String path) throws IOException {
        return getExifRotation(Files.asByteSource(new File(path)));
    }

    /**
     * Retrieve the rotation specified by EXIF.
     * @return The EXIF rotation, or NULL if not specified.
     */
    public static ExifOrientation getExifRotation(ByteSource source) throws IOException {
        try {
        	ImageMetadata metadata = ExifManipulation.getMetadata(source);

            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                TiffField field = jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_ORIENTATION);
                Object value = field != null ? field.getValue() : null;

                return value instanceof Number ? ExifOrientation.fromTagConstant(((Number) value).shortValue()) : null;
            }
            // No rotation at all
            return null;
        } catch (ImageReadException e) {
            throw new IOException("Cannot read image.", e);
        }
    }

	/**
	 * Combine the EXIF tags from the EXIF source, with the image and EXIF data from the image source, and save 
	 * it as a picture of the same type to the destination.
     * </p>
     * The destination byte sink will only be opened once.
	 * @param exifSource - the source of the EXIF data to copy
	 * @param imageSource - image source, with additional EXIF data.
	 * @param destination - image destination.
	 * @param processor - metadata processor.
	 * @throws java.io.IOException General IO failure.
	 * @throws IllegalStateException If the two files differ in byte order.
	 */
	public static void copyExifData(ByteSource exifSource, ByteSource imageSource, ByteSink destination, MetadataProcessor processor) throws IOException {
        try {
            TiffOutputSet sourceSet = getImagingOutputSet(exifSource, TiffConstants.DEFAULT_TIFF_BYTE_ORDER);
            TiffOutputSet destSet = getImagingOutputSet(imageSource, sourceSet.byteOrder);

            // If the EXIF data endianess of the source and destination files
            // differ then fail. This only happens if the source and
            // destination images were created on different devices. It's
            // technically possible to copy this data by changing the byte
            // order of the data, but handling this case is outside the scope
            // of this implementation
            if (sourceSet.byteOrder != destSet.byteOrder) {
                throw new IllegalStateException("Cannot copy EXIF data to a file with a different byte order.");
            }
            destSet.getOrCreateExifDirectory();

            // Go through the source directories
            List<?> sourceDirectories = sourceSet.getDirectories();
            for (int i = 0; i < sourceDirectories.size(); i++) {
                TiffOutputDirectory sourceDirectory = (TiffOutputDirectory) sourceDirectories.get(i);
                TiffOutputDirectory destinationDirectory = getOrCreateDirectory(destSet, sourceDirectory, ByteOrder.nativeOrder());

                if (destinationDirectory == null) {
                    continue; // failed to create
                }

                // Loop the fields
                List<?> sourceFields = sourceDirectory.getFields();
                for (int j = 0; j < sourceFields.size(); j++) {
                    // Get the source field
                    TiffOutputField sourceField = (TiffOutputField) sourceFields.get(j);

                    // Remove old field
                    destinationDirectory.removeField(sourceField.tag);
                    // Add field
                    destinationDirectory.add(sourceField);
                }
            }

            if (processor != null) {
                processor.process(destSet);
            }

            // Save data to destination
            try (InputStream inputStream = imageSource.openBufferedStream();
                 OutputStream outputStream = destination.openStream()) {
                new ExifRewriter().updateExifMetadataLossless(inputStream, outputStream, destSet);
            }
        } catch (ImageReadException exception) {
            throw new IOException("Cannot read underlying image.", exception);
        } catch (ImageWriteException exception) {
            throw new IOException("Cannot write to destination image.", exception);
        }
	}

	/**
	 * Combine the EXIF tags from the EXIF source, with the image and EXIF data from the image source, and save
	 * it as a picture of the same type to the destination.
	 * <p>
	 * Note that the destination source might be opened twice.
	 * @param exifSource - byte source with EXIF data to copy, or NULL.
	 * @param imageSource - image source, with additional EXIF data.
	 * @param destination - the image output. Caller is responsible for closing it.
	 * @param metadataProcessor metadata processor, or NULL to ignore.
	 * @throws java.io.IOException General IO failure.
	 * @throws IllegalStateException If the two files differ in byte order.
	 */
	public static void copyExifData(ByteSource exifSource, ByteSource imageSource, OutputStream destination, MetadataProcessor metadataProcessor) throws IOException {
        copyExifData(exifSource, imageSource, new SingleByteSink(destination), metadataProcessor);
	}

	/**
	 * Retrieve the TIFF image meta data from given byte source.
	 * @param source the byte source.
	 * @param defaultByteOrder default byte order.
	 * @return The TIFF image meta data.
     */
	public static TiffOutputSet getImagingOutputSet(ByteSource source, ByteOrder defaultByteOrder)
			throws IOException, ImageReadException, ImageWriteException {
		TiffImageMetadata exif = null;
		TiffOutputSet outputSet = null;
		
		// Metadata
		JpegImageMetadata jpegMetadata = source != null ? (JpegImageMetadata) getMetadata(source) : null;
		if (jpegMetadata != null) {
			exif = jpegMetadata.getExif();

			if (exif != null) {
				outputSet = exif.getOutputSet();
			}
		}

		// If JPEG file contains no EXIF metadata, create an empty set
		// of EXIF metadata. Otherwise, use existing EXIF metadata to
		// keep all other existing tags
		if (outputSet == null) {
			outputSet = new TiffOutputSet(exif == null ? defaultByteOrder : exif.contents.header.byteOrder);
		}
		return outputSet;
	}
	
	/**
	 * Retrieve an image metadata from the given byte source.
	 * @param source - the source.
	 * @return The underlying metadata.
	 * @throws ImageReadException Cannot read the image.
	 * @throws java.io.IOException A general IO failure.
	 */
	public static ImageMetadata getMetadata(ByteSource source) throws ImageReadException, IOException {
		ImageMetadata metadata = null;
		InputStream input = null;
		boolean swallow = true;

		try {
			input = source.openStream();
			metadata = Imaging.getMetadata(input, null); // Assume JPEG
			
			swallow = false;
			return metadata;
		} finally {
			if (input != null) {
				Closeables.close(input, swallow);
			}
		}
	}

	private static TiffOutputDirectory getOrCreateDirectory(TiffOutputSet outputSet,
                                                            TiffOutputDirectory outputDirectory, ByteOrder order) {
		TiffOutputDirectory result = outputSet.findDirectory(outputDirectory.type);
		if (result != null)
			return result;
		result = new TiffOutputDirectory(outputDirectory.type, order);
		try {
			outputSet.addDirectory(result);
		} catch (ImageWriteException e) {
			return null;
		}
		return result;
	}
}

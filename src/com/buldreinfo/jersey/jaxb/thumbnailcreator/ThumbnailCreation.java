package com.buldreinfo.jersey.jaxb.thumbnailcreator;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.logging.Logger;

public class ThumbnailCreation implements AutoCloseable {
	protected static Logger logger = Logger.getLogger(ThumbnailCreation.class.getSimpleName());
	
	/**
	 * The default JPEG quality.
	 */
	protected static final int DEFAULT_JPEG_QUALITY = 94;

	protected interface ImageReaderProcessor<T> {
		/**
		 * Process an image reader.
		 * @param reader the reader.
		 * @return The corresponding result.
         * @throws IOException Cannot process the given reader.
		 */
		public T process(ImageReader reader) throws IOException;
	}
	
	// Image source
	protected final ByteSource bitmapSource;

	// Resize operation
	protected Rectangle2D originalSize;

    // Current scale or rotation
	protected AffineTransform matrix = new AffineTransform();

	/** TRUE if the creator is closed. */
	protected boolean closed = false;
    /** TRUE if saveTo() is prohibited. */
	protected boolean dryRun = false;
    /** TRUE if the transformation from the bitmap source is the identity transform. */
    protected boolean bitmapIdentity = true;
    /** TRUE if the current bitmap is also stored in another thumbnail instance, FALSE otherwise */
    protected boolean sharedBitmap = false;

    // Working bitmap
	protected BufferedImage bitmap;

	// Whether or not to preserve exif
	protected boolean preserveExif;
	protected boolean keepOrientation = true;
	protected File preserveFile;
	
	// Whether or not to use sub-sampling, which will improve memory use
	protected boolean useSubsampling = false;

    /**
	 * Construct a new thumbnail creation chain.
	 * @param bitmapSource - the byte source.
	 */
	public ThumbnailCreation(ByteSource bitmapSource) {
		this.bitmapSource = bitmapSource;
	}

    /**
     * Construct a copy of the given creator.
     * <p>
     * Unlike {@link #fork()}, this will not attempt to load the image into memory.
     * @param other the other creator.
     */
    public ThumbnailCreation(ThumbnailCreation other) {
        this.bitmapSource = other.bitmapSource;
        this.dryRun = other.dryRun;
        this.closed = other.closed;
        this.bitmapIdentity = other.bitmapIdentity;
        this.bitmap = other.bitmap;
        this.sharedBitmap = true;
        this.preserveExif = other.preserveExif;
        this.preserveFile = other.preserveFile;
        this.keepOrientation = other.keepOrientation;
        this.useSubsampling = other.useSubsampling;

        // Clone non-immutable reference objects
        this.originalSize = (Rectangle2D) other.originalSize.clone();
        this.matrix = new AffineTransform(other.matrix);
    }

	/**
	 * Automatically rotate the given image.
	 * @param image the image to rotate.
     * @throws IOException Cannot automatically resize image.
	 */
	public static void autoRotate(File image) throws IOException {
		try (ThumbnailCreation creation = ThumbnailCreation.image(image)) {
			ExifOrientation orientation = creation.getExifRotation();

			if (orientation != null && orientation != ExifOrientation.HORIZONTAL_NORMAL) {
				logger.info("Rotating " + image + " using " + orientation);

				creation.
					rotate(orientation).
					preserveExif().
					saveTo(image);
			}
		}
	}

	/**
	 * Operate on the image found in the given file.
	 * @param imageFile - the image file.
	 * @return Thumbnail creation for this byte source.
	 */
	public static ThumbnailCreation image(File imageFile) {
		return new ThumbnailCreation(Files.asByteSource(imageFile));
	}
	
	/**
	 * Operate on the image found in the given byte source.
	 * @param source - the byte source.
	 * @return Thumbnail creation for this byte source.
	 */
	public static ThumbnailCreation image(ByteSource source) {
		return new ThumbnailCreation(source);
	}

    /**
     * Resize the current image such that it fits within the given bounding box.
     * @param boundingBox the bounding box.
     * @param upscale whether or not to scale the image outwards.
     * @return This thumbnail creation, for chaining.
     * @throws java.io.IOException Cannot resize image.
     */
    public ThumbnailCreation resize(Rectangle2D boundingBox, boolean upscale) throws IOException {
        return resize(boundingBox.getWidth(), boundingBox.getHeight(), upscale);
    }

    /**
     * Resize the current image such that it fits within the given square bounding box.
     * @param boundingSideLength side length of the bounding square box.
     * @param upscale whether or not to scale the image outwards.
     * @return This thumbnail creation, for chaining.
     * @throws java.io.IOException Cannot resize image.
     */
    public ThumbnailCreation resize(double boundingSideLength, boolean upscale) throws IOException {
        return resize(boundingSideLength, boundingSideLength, upscale);
    }

    /**
     * Resize the current image such that it fits within the given bounding box.
     * @param boundingWidth the width of the bounding box.
     * @param boundingHeight the height of the bounding bow.
     * @param upscale whether or not to scale the image outwards.
     * @return This thumbnail creation, for chaining.
     * @throws java.io.IOException Cannot resize image.
     */
    public ThumbnailCreation resize(double boundingWidth, double boundingHeight, boolean upscale) throws IOException {
        Preconditions.checkArgument(boundingWidth > 0, "width cannot be less than 1.");
        Preconditions.checkArgument(boundingHeight > 0, "height cannot be less than 1.");
        Rectangle2D original = getCurrentSize();

        double factor = Math.max(
            boundingWidth / original.getWidth(),
            boundingHeight / original.getHeight());

        // In case we should not upscale
        if (!upscale && factor > 1) {
            logger.fine("Ignoring resize.");
            return this;
        }
        return scale(factor, factor);
    }
	
	/**
	 * Resize the current image to the provided dimensions.
	 * @param width - new width. Cannot be less than 1.
	 * @param height - new height. Cannot be less than 1.
	 * @return This thumbnail creation, for chaining. 
	 * @throws java.io.IOException Cannot resize image exactly.
	 */
	public ThumbnailCreation resizeExact(int width, int height) throws IOException {
		Preconditions.checkArgument(width > 0, "width cannot be less than 1.");
		Preconditions.checkArgument(height > 0, "height cannot be less than 1.");
        Rectangle2D original = getCurrentSize();

        matrix.scale(
                width / original.getWidth(),
                height / original.getHeight());
        logger.fine("Resizing to [" + width + ", " + height + "]");
	    return this;
	}

    /**
     * Perform the given rotation on the actual image.
     * <p>
     * Note that this will reset the TIFF orientation field.
     * @param orientation - the orientation. NULL to ignore.
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation rotate(ExifOrientation orientation) {
        if (orientation != null) {
            orientation.apply(matrix);
            this.keepOrientation = false;
        }
        return this;
    }

    /**
     * Scale the pixels of the image using the given factors.
     * @param sx the factor by which pixels are scaled along the
     * X axis direction
     * @param sy the factor by which pixels are scaled along the
     * Y axis direction
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation scale(double sx, double sy) {
        matrix.scale(sx, sy);
        return this;
    }

    /**
     * Shear the pixels of the image using the given factors.
     * @param shx the multiplier by which pixels are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate.
     * @param shy the multiplier by which pixels are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate.
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation shear(double shx, double shy) {
        matrix.shear(shx, shy);
        return this;
    }
    
    /**
     * Scale the image using the given factors.
     * @param sx the factor by which the image is scaled along the
     * X axis direction.
     * @param sy the factor by which the image is scaled along the
     * Y axis direction
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation rotate(double sx, double sy) {
        matrix.scale(sx, sy);
        return this;
    }
    
    /**
     * Rotate the image.
     * @param degree - the image to rotate.
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation rotate(float degree) {
        matrix.rotate(Math.toRadians(degree));
        return this;
    }

	/**
	 * Indicate that the thumbnail creator may use pixel subsampling on the source image to preserve memory.
	 * <p>
	 * This may sacrifice quality.
	 * @param useSubsampling TRUE to use subsampling, FALSE otherwise.
	 * @return This thumbnail creation, for chaining.
	 */
	public ThumbnailCreation useSubsampling(boolean useSubsampling) {
		useSubsampling =  true;
		return this;
	}
    
	/**
	 * Indicate that EXIF information should be preserved.
	 * <p>
	 * This will perform the EXIF transfer in memory.
	 * @return This thumbnail creation, for chaining.
	 */
	public ThumbnailCreation preserveExif() {
		return preserveExif(null);
	}
	
	/**
	 * Indicate that EXIF information should be preserved.
	 * <p>
	 * The temporary file will be used
	 * @param temporaryFile - a temporary file to use during creation, or NULL to perform everything in memory.
	 * @return This thumbnail creation, for chaining.
	 */
	public ThumbnailCreation preserveExif(File temporaryFile) {
		preserveExif = true;
		preserveFile = temporaryFile;
		return this;
	}

    /**
     * Discard all EXIF information. This is the default mode.
     * @return This thumbnail creation, for chaining.
     */
    public ThumbnailCreation discardExif() {
        preserveExif = false;
        preserveFile = null;
        return this;
    }

    /**
     * Determine if the current creation is just the identity operation.
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean isIdentity() {
        return bitmapIdentity && matrix.isIdentity();
    }

    /**
     * Load the current transformation into memory, and create a new creator from the current state.
     * <p>
     * This is useful if you need to transform the same source file into multiple versions,
     * without having to load each one.
     * @return Another thumbnail creator.
     * @throws IOException Cannot fork creator.
     */
    public ThumbnailCreation fork() throws IOException {
        // Load current state
        executeTransform();
        return new ThumbnailCreation(this);
    }

    /**
     * Load bitmap into memory, and the transform it according to the current matrix.
     * @throws IOException Cannot execute image load and transform.
     */
    protected void executeTransform() throws IOException {
        this.bitmap = getTransformedBitmap(bitmapSource, this.bitmap, matrix);
        this.originalSize = new Rectangle2D.Double(0, 0, bitmap.getWidth(), bitmap.getHeight());
        this.bitmapIdentity &= matrix.isIdentity();
        this.sharedBitmap = false;
        this.matrix = new AffineTransform();
    }

    /**
	 * Retrieve the original size of the image source.
	 * @return Original size.
	 * @throws java.io.IOException Cannot read the underlying image size.
	 */
	public Rectangle2D getOriginalSize() throws IOException {
		// Determine the original size first
		if (originalSize == null) {
			originalSize = getBounds(bitmapSource);
		}
		return originalSize;
	}

    /**
     * Retrieve the current image size in the pipeline.
     * @return Current image size.
     * @throws IOException Cannot retrieve current size.
     */
    public Rectangle2D getCurrentSize() throws IOException {
    	return getCurrentSize(true);
    }
	
    /**
     * Retrieve the current image size in the pipeline.
     * @param normalize whether or not to normalize the position of this rectangle.
     * @return Current image size.
     * @throws IOException Cannot retrieve current size.
     */
    protected Rectangle2D getCurrentSize(boolean normalize) throws IOException {
    	Rectangle2D size = getOriginalSize();

    	// Corners
    	double[] points = new double[] { 
			size.getMinX(), size.getMinY(), // LT
			size.getMaxX(), size.getMinY(), // RT
			size.getMaxX(), size.getMaxY(), // RB
			size.getMinX(), size.getMaxY()  // LB
    	};
    	matrix.transform(points, 0, points, 0, 4);
    
    	// Assume the first point is the minimum
    	double minX = points[0], maxX = minX;
    	double minY = points[1], maxY = minY;
    	
    	// Check point 1 to 4
    	for (int i = 1; i < 4; i++) {
    		minX = Math.min(minX, points[2 * i]);
    		maxX = Math.max(maxX, points[2 * i]);
    		minY = Math.min(minY, points[2 * i + 1]);
    		maxY = Math.max(maxY, points[2 * i + 1]);
		}
    	// Normalize image size
    	if (normalize) {
    		return new Rectangle2D.Double(0, 0, Math.abs(maxX - minX), Math.abs(maxY - minY));
    	} else {
    		return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    	}
    }

    /**
     * Retrieve the rotation specified by EXIF.
     * @return The EXIF rotation, or NULL if not specified.
     * @throws IOException Cannot retrieve EXIF information.
    */
    public ExifOrientation getExifRotation() throws IOException {
        return ExifManipulation.getExifRotation(bitmapSource);
    }
	
	/**
	 * Retrieve the actual size of the image represented by the given byte source.
	 * @param source - the original image.
	 * @return The size.
	 * @throws java.io.IOException If we cannot read this size.
	 */
	private Rectangle2D getBounds(ByteSource source) throws IOException {
		return processImage(source, false, new ImageReaderProcessor<Rectangle2D>() {
			@Override
			public Rectangle2D process(ImageReader reader) throws IOException {
				return new Rectangle2D.Double(0, 0, reader.getWidth(0), reader.getHeight(0)); 
			}
		});
	}

	/**
	 * Process an image represented by an image source.
	 * @param source the image byte source.
	 * @param ignoreMetadata whether or not to ignore metadata.
	 * @param processor the image processor.
	 * @return The corresponding processed result.
	 * @throws IOException Cannot process image.
	 */
	protected <T> T processImage(ByteSource source, boolean ignoreMetadata, ImageReaderProcessor<T> processor) throws IOException {
		try (InputStream sourceStream = source.openBufferedStream();
			 ImageInputStream in = ImageIO.createImageInputStream(sourceStream)) {
			if (in == null) {
				// Throw a more descriptive error
				throw new IOException("Unable to find a valid image reader for " + source);
			}
		    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
		    
		    if (readers.hasNext()) {
		        ImageReader reader = readers.next();
		        try {
		            reader.setInput(in, false, ignoreMetadata);
		            return processor.process(reader);
		        } finally {
		            reader.dispose();
		        }
		    }
		}
		throw new IllegalArgumentException("Unable to open " + source + " as an image.");
	}
	
	/**
	 * Create a bitmap by applying the current scale operation.
     * @param bitmapSource - source of the bitmap.
	 * @param existing - a existing loaded bitmap. This will be recycled if it is not returned.
	 * @param inputMatrix - transformation matrix to apply to the bitmap.
	 * @return The new bitmap, or NULL if we are in a dry run.
	 * @throws java.io.IOException Cannot retrieve transformed bitmap.
	 */
	protected BufferedImage getTransformedBitmap(ByteSource bitmapSource, BufferedImage existing, AffineTransform inputMatrix) throws IOException {
		if (dryRun) {
			return null;
		}
        // Current transformation
		final AffineTransform transformation = new AffineTransform(inputMatrix);
        final Rectangle2D currentSize = getCurrentSize(false);
		
		// Fetch bitmap from the underlying byte source
		if (existing == null) {
	        final Rectangle2D original = getOriginalSize();
			
			existing = processImage(bitmapSource, true, new ImageReaderProcessor<BufferedImage>() {
				@Override
				public BufferedImage process(ImageReader reader) throws IOException {
			    	ImageReadParam param = reader.getDefaultReadParam();
					
				    if (currentSize != null && useSubsampling) {
		                // Use sample size to optimize this
				    	int sampleSize = Math.max(
		                    (int) (original.getWidth() / currentSize.getWidth()),
		                    (int) (original.getHeight() / currentSize.getHeight())
				    	);
		                int scale = Integer.highestOneBit(sampleSize);
		                
		                if (scale > 1) {
		                	// Adjust transformation
			                AffineTransform scaling = new AffineTransform();
			                scaling.scale(scale, scale);
					    	transformation.preConcatenate(scaling);
					    	
					    	// Use sub-sampling
					    	param.setSourceSubsampling(scale, scale, 0, 0);
		                }
				    }
				    return reader.read(0, param);
				}
			});
		}

	    // Perform the final resize
	    if (!transformation.isIdentity()) {
	    	BufferedImage resized = new BufferedImage((int) currentSize.getWidth(), (int) currentSize.getHeight(), existing.getType());
	    	Graphics2D g = resized.createGraphics();
	    	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
	    	    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    	g.translate(-currentSize.getX(), -currentSize.getY());
	    	g.transform(transformation);
	    	g.drawImage(existing, 0, 0, existing.getWidth(), existing.getHeight(), null);
	    	g.dispose();

            // Do not flush a shared instance
            if (!sharedBitmap) {
                existing.flush();
            }
	    	return resized;
	    } else {
	    	return existing;
	    }
	}

	/**
	 * Write a compressed version of the bitmap to the specified byte sink using JPEG at 90 quality.
	 * <p>
	 * The creation instance is automatically closed and cleaned up after this call.
	 * @param destination - the destination byte sink.
     * @return This creator, for chaining.
	 * @throws java.io.IOException Cannot save image.
	 */
	public ThumbnailCreation saveTo(ByteSink destination) throws IOException {
		return saveTo(destination, "jpg", DEFAULT_JPEG_QUALITY);
	}

    /**
     * Write a compressed version of the bitmap to the specified byte sink using JPEG at 90 quality.
     * <p>
     * The creation instance is automatically closed and cleaned up after this call.
     * @param destination - the destination byte sink.
     * @return This creator, for chaining.
     * @throws java.io.IOException Cannot save image.
     */
    public ThumbnailCreation saveTo(File destination) throws IOException {
        return saveTo(destination, "jpg", DEFAULT_JPEG_QUALITY);
    }

    /**
     * Write a compressed version of the bitmap to the specified byte sink.
     * <p>
     * The creation instance is automatically closed and cleaned up after this call.
     * <p>
     * This operation is atomic - a file will only be overwritten if the operation succeeds.
     * @param destination - the destination byte sink.
     * @param format - the format name of the compressed image.
     * @param quality - hint to the compressor, 0 - 100. 0 meaning compress for small size, 100 meaning compress for max quality.
     * @return This creator, for chaining.
     * @throws java.io.IOException Cannot save image.
     */
    public ThumbnailCreation saveTo(File destination, String format, int quality) throws IOException {
        File temporary = File.createTempFile("temp", "." + format, destination.getParentFile());

        try {
            saveTo(Files.asByteSink(temporary), format, quality);

            // Overwrite existing file
            if ((destination.exists() && !destination.delete()) || !temporary.renameTo(destination)) {
                throw new IOException("Unable to overwrite file " + destination);
            }
        } finally {
            // Always delete temporary
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
        }
        return this;
    }

	/**
	 * Write a compressed version of the bitmap to the specified byte sink.
     * <p>
     * Also note that the destination byte sink will only be opened once.
	 * @param destination - the destination byte sink.
	 * @param format - the format name of the compressed image.
	 * @param quality - hint to the compressor, 0 - 100. 0 meaning compress for small size, 100 meaning compress for max quality. 
	 * @return This creator, for chaining.
     * @throws java.io.IOException Cannot save image.
	 */
	public ThumbnailCreation saveTo(ByteSink destination, String format, int quality) throws IOException {
		if (closed)
			throw new IllegalStateException("Thumbnail creation has already been closed.");
		if (dryRun)
			throw new IllegalStateException("Cannot create image in a dry run.");

        // Transform the bitmap into memory, and reset the current transformation
        executeTransform();

        // Also note that we ALWAYS call destination.openStream() at the end, to enable writing to the
        // same input and output source
        if (preserveExif) {
            // Remove tags we do not need
            final ExifManipulation.MetadataProcessor tagFilter = new ExifManipulation.MetadataProcessor() {
                @Override
                public void process(TiffOutputSet tiffMetadata) {
                    if (!keepOrientation) {
                        // Remove all orientation tags
                        for (TiffOutputDirectory directory : tiffMetadata.getDirectories()) {
                            if (directory != null) {
                                directory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
                            }
                        }
                    }
                }
            };

            // Use a temporary file or memory
            if (preserveFile != null) {
                OutputStream output = null;
                boolean swallow = true;

                try {
                    if (!preserveFile.delete()) {
                        throw new IOException("Unable to delete existing file.");
                    }
                    compressTo(format, quality, output = new FileOutputStream(preserveFile));

                    ExifManipulation.copyExifData(bitmapSource, Files.asByteSource(preserveFile), destination, tagFilter);
                    swallow = false;

                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    preserveFile.delete();

                    if (output != null) {
                        Closeables.close(output, swallow);
                    }
                }
            } else {
                // Store everything in a byte array
                ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream();

                compressTo(format, quality, memoryBuffer);
                ExifManipulation.copyExifData(bitmapSource,
                    ByteSource.wrap(memoryBuffer.toByteArray()), destination, tagFilter);
            }
        } else {
            try (OutputStream output = destination.openStream()) {
                compressTo(format, quality, output);
            }
        }
        return this;
	}
	
	/**
	 * Write a compressed version of the bitmap to the specified output stream.
	 * <p>
	 * The caller is responsible for closing the provided stream.
	 * @param destination - the destination output stream.
	 * @param format - the format name of the compressed image.
	 * @param quality - hint to the compressor, 0 - 100. 0 meaning compress for small size, 100 meaning compress for max quality.
     * @return This creator, for chaining.
	 * @throws java.io.IOException Cannot save image.
	 */
	public ThumbnailCreation saveTo(OutputStream destination, String format, int quality) throws IOException {
        if (closed)
            throw new IllegalStateException("Thumbnail creation has already been closed.");
        if (dryRun)
            throw new IllegalStateException("Cannot create image in a dry run.");

        // Save to stream
        return saveTo(new SingleByteSink(destination), format, quality);
	}
	
	/**
	 * Compress the current bitmap to the given output stream.
	 * @param formatName the format name, typically JPEG.
	 * @param quality quality, if this is a JPEG.
	 * @param output the destination output stream.
	 * @throws IOException Cannot compress image.
	 */
	protected void compressTo(String formatName, int quality, OutputStream output) throws IOException {
		final ImageWriter writer = ImageIO.getImageWritersByFormatName(formatName).next();
		ImageWriteParam params = null;
		
		// Include quality
		if (formatName.equalsIgnoreCase("jpg") || formatName.equalsIgnoreCase("jpeg")) {
			final JPEGImageWriteParam jpegParams = (JPEGImageWriteParam) writer.getDefaultWriteParam();
			jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegParams.setCompressionQuality(quality / 100.0f);
			params = jpegParams;
		}
		ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output);
		writer.setOutput(imageOutput);
		writer.write(null, new IIOImage(bitmap, null, null), params);
	}

	/**
	 * Set whether or not we will ignore image creation in this run.
	 * @param dryRun - TRUE to ignore it, FALSE otherwise.
	 * @return This instance, for chaining.
	 */
	public ThumbnailCreation dryRun(boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}
	
	/**
	 * Determine if the thumbnail has already been created.
	 * @return TRUE if it has, FALSE othewise.
	 */
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * Close the current instance, cleaning up any associated resources.
	 */
	@Override
	public void close() {
        if (!closed) {
            // Do not flush the bitmap of another instance - let it handle it
            if (!sharedBitmap && bitmap != null) {
                bitmap.flush();
            }
            closed = true;
        }
    }
}

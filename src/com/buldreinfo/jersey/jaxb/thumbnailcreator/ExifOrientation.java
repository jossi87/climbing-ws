package com.buldreinfo.jersey.jaxb.thumbnailcreator;

import java.awt.geom.AffineTransform;

import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

public enum ExifOrientation {
    HORIZONTAL_NORMAL(
            TiffTagConstants.ORIENTATION_VALUE_HORIZONTAL_NORMAL, 0, false),
    MIRROR_HORIZONTAL(
            TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL, 0, true),
    ROTATE_180(
            TiffTagConstants.ORIENTATION_VALUE_ROTATE_180, 180, false),
    MIRROR_VERTICAL(
            TiffTagConstants.ORIENTATION_VALUE_MIRROR_VERTICAL, 180, true),
    MIRROR_HORIZONTAL_AND_ROTATE_270_CW(
            TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_270_CW, 270, true),
    ROTATE_90_CW(
            TiffTagConstants.ORIENTATION_VALUE_ROTATE_90_CW, 90, false),
    MIRROR_HORIZONTAL_AND_ROTATE_90_CW(
            TiffTagConstants.ORIENTATION_VALUE_MIRROR_HORIZONTAL_AND_ROTATE_90_CW, 90, true),
    ROTATE_270_CW(
            TiffTagConstants.ORIENTATION_VALUE_ROTATE_270_CW, 270, false);

    private static final ExifOrientation[] VALUES = values();

    private final int tagConstant;
    private final int rotation;
    private final boolean flip;

    private ExifOrientation(int tagConstant, int rotation, boolean flip) {
        this.tagConstant = tagConstant;
        this.rotation = rotation;
        this.flip = flip;
    }

    /**
     * Apply the current rotation to the matrix.
     * @param matrix - the destination matrix.
     */
    public void apply(AffineTransform matrix) {
        matrix.rotate(Math.toRadians(rotation));

        if (flip) {
        	matrix.scale(-1, 1);
        }
    }

    /**
     * Retrieve the number of degrees to rotate the image clockwise.
     * @return The number of degrees.
     */
    public int getRotation() {
        return rotation;
    }

    /**
     * Determine if the orientation is flipped. This is usually not used.
     * @return TRUE if it is flipped, FALSE otherwise.
     */
    public boolean isFlipped() {
        return flip;
    }

    /**
     * Retrieve a copy of the underlying orientation matrix.
     * @return The orientation matrix.
     */
    public AffineTransform copyMatrix() {
        AffineTransform transform = new AffineTransform();
        apply(transform);
        return transform;
    }

    /**
     * Perform the given number of clockwise right angle rotations
     * @param rightAngles the number.
     * @return The corresponding orientation.
     */
    public ExifOrientation rotateClockwise(int rightAngles) {
        int normalized = rightAngles % 4;
        ExifOrientation current = this;

        if (normalized < 0) {
            normalized += 4;
        }
        for (int i = 0; i < normalized; i++) {
            switch (current) {
                case HORIZONTAL_NORMAL:
                    current = ROTATE_90_CW; break;
                case MIRROR_HORIZONTAL:
                    current = MIRROR_HORIZONTAL_AND_ROTATE_90_CW; break;
                case ROTATE_180:
                    current = ROTATE_270_CW; break;
                case MIRROR_VERTICAL:
                    current = MIRROR_HORIZONTAL_AND_ROTATE_270_CW; break;
                case MIRROR_HORIZONTAL_AND_ROTATE_270_CW:
                    current = MIRROR_HORIZONTAL; break;
                case ROTATE_90_CW:
                    current = ROTATE_180; break;
                case MIRROR_HORIZONTAL_AND_ROTATE_90_CW:
                    current = MIRROR_VERTICAL; break;
                case ROTATE_270_CW:
                    current = HORIZONTAL_NORMAL; break;
            }
        }
        return current;
    }

    /**
     * Determine if the given orientation performs a rotation or a mirroring.
     * @param orientation - the orientation.
     * @return TRUE if it does, FALSE otherwise.
     */
    public static boolean isRotationOrMirror(ExifOrientation orientation) {
        return orientation != null && orientation != HORIZONTAL_NORMAL;
    }

    /**
     * Retrieve the tag constant associated with this enum.
     * @return The tag constant.
     */
    public int getTagConstant() {
        return tagConstant;
    }

    /**
     * Retrieve the orientation associated with the given tag constant.
     * @param value - the value.
     * @return The tag constant, or NULL if not found.
     */
    public static ExifOrientation fromTagConstant(int value) {
        for (ExifOrientation orientation : VALUES) {
            if (orientation.getTagConstant() == value) {
                return orientation;
            }
        }
        return null;
    }
}
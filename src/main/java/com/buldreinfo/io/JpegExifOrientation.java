package com.buldreinfo.io;

/**
 * Normalises the EXIF orientation tag of the JPEG files this application writes.
 * <p>
 * Every stored image keeps its pixels in display orientation: an upload is rotated by the EXIF
 * orientation of the uploaded file and a rotate request is applied to the pixels as well. An orientation
 * tag left in the file is therefore a lie, and a costly one — EXIF-aware viewers (every browser, unlike
 * the WebGL panorama viewer, which ignores EXIF) rotate the original a second time, while the derived web
 * images, which are written without metadata, keep showing the pixels. That is how an original ends up
 * disagreeing with its own thumbnails.
 * <p>
 * The image is patched, not re-encoded: only the four value bytes of the orientation entry in IFD0 change,
 * so the rest of the EXIF block (date taken, camera, GPS) and the JPEG payload survive untouched.
 */
public final class JpegExifOrientation {
	/** IFD0 entry id and type of the tags this class has to understand. */
	private static final int TAG_ORIENTATION = 0x0112;
	private static final int TYPE_SHORT = 3;
	/** The value for "pixels are already upright", which is what every image we write is. */
	private static final int ORIENTATION_UPRIGHT = 1;
	private static final int IFD_ENTRY_LENGTH = 12;
	private static final int TIFF_MAGIC = 42;
	/** EXIF marker: an APP1 segment that starts with this six byte header. XMP also uses APP1. */
	private static final byte[] EXIF_HEADER = { 'E', 'x', 'i', 'f', 0, 0 };
	private static final int MARKER_PREFIX = 0xFF;
	private static final int APP1 = 0xE1;
	private static final int START_OF_SCAN = 0xDA;

	/**
	 * Returns {@code jpeg} with the orientation tag of its EXIF block set to "upright" ({@code 1}). Files
	 * without EXIF, without an orientation tag, or that are not in a JPEG layout this class understands
	 * are returned untouched. The array is patched in place.
	 */
	public static byte[] normalizeOrientation(byte[] jpeg) {
		if (jpeg == null || jpeg.length < 4) {
			return jpeg;
		}
		int position = 2; // The SOI marker has no length field, so the first segment starts right after it.
		while (position + 4 <= jpeg.length) {
			if (u8(jpeg[position]) != MARKER_PREFIX) {
				return jpeg; // Not a marker: something we do not understand, so leave the bytes alone.
			}
			int marker = u8(jpeg[position + 1]);
			if (marker == MARKER_PREFIX) {
				position++; // Fill byte before a marker.
				continue;
			}
			if (marker == START_OF_SCAN) {
				return jpeg; // Entropy coded data follows, every metadata segment is behind us.
			}
			if (isStandalone(marker)) {
				position += 2;
				continue;
			}
			int length = (u8(jpeg[position + 2]) << 8) | u8(jpeg[position + 3]); // Segment lengths are big endian.
			if (length < 2 || position + 2 + length > jpeg.length) {
				return jpeg; // Truncated segment, do not touch what we cannot read completely.
			}
			if (marker == APP1) {
				narrowOrientation(jpeg, position + 4, position + 2 + length);
			}
			position += 2 + length;
		}
		return jpeg;
	}

	/** TEM, the restart markers, SOI and EOI carry no length field. */
	private static boolean isStandalone(int marker) {
		return marker == 0x01 || (marker >= 0xD0 && marker <= 0xD9);
	}

	/** Sets the orientation entry of one APP1 segment to {@link #ORIENTATION_UPRIGHT}, if it has one. */
	private static void narrowOrientation(byte[] jpeg, int segmentStart, int segmentEnd) {
		for (int i = 0; i < EXIF_HEADER.length; i++) {
			if (segmentStart + i >= segmentEnd || jpeg[segmentStart + i] != EXIF_HEADER[i]) {
				return; // Not an EXIF segment (an XMP block is also an APP1 segment), or too short to be one.
			}
		}
		int tiff = segmentStart + EXIF_HEADER.length;
		if (tiff + 8 > segmentEnd) {
			return;
		}
		boolean bigEndian;
		if (jpeg[tiff] == 'I' && jpeg[tiff + 1] == 'I') {
			bigEndian = false;
		} else if (jpeg[tiff] == 'M' && jpeg[tiff + 1] == 'M') {
			bigEndian = true;
		} else {
			return;
		}
		if (readShort(jpeg, tiff + 2, bigEndian) != TIFF_MAGIC) {
			return;
		}
		int ifd0 = tiff + readInt(jpeg, tiff + 4, bigEndian);
		if (ifd0 < tiff || ifd0 + 2 > segmentEnd) {
			return; // The IFD is not inside this segment, so there is nothing we can safely patch.
		}
		int entryCount = readShort(jpeg, ifd0, bigEndian);
		for (int i = 0; i < entryCount; i++) {
			int entry = ifd0 + 2 + i * IFD_ENTRY_LENGTH;
			if (entry + IFD_ENTRY_LENGTH > segmentEnd) {
				return;
			}
			if (readShort(jpeg, entry, bigEndian) != TAG_ORIENTATION
					|| readShort(jpeg, entry + 2, bigEndian) != TYPE_SHORT
					|| readInt(jpeg, entry + 4, bigEndian) < 1) {
				continue;
			}
			// A single SHORT value is left justified in the four value bytes, in the file's own byte order.
			jpeg[entry + 8] = bigEndian ? (byte) (ORIENTATION_UPRIGHT >> 8) : (byte) ORIENTATION_UPRIGHT;
			jpeg[entry + 9] = bigEndian ? (byte) ORIENTATION_UPRIGHT : 0;
			return;
		}
	}

	private static int u8(byte value) {
		return value & 0xFF;
	}

	private static int readShort(byte[] data, int offset, boolean bigEndian) {
		int first = u8(data[offset]);
		int second = u8(data[offset + 1]);
		return bigEndian ? (first << 8) | second : (second << 8) | first;
	}

	private static int readInt(byte[] data, int offset, boolean bigEndian) {
		int first = u8(data[offset]);
		int second = u8(data[offset + 1]);
		int third = u8(data[offset + 2]);
		int fourth = u8(data[offset + 3]);
		return bigEndian
				? (first << 24) | (second << 16) | (third << 8) | fourth
				: (fourth << 24) | (third << 16) | (second << 8) | first;
	}

	private JpegExifOrientation() {
	}
}

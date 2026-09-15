package com.buldreinfo.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.buldreinfo.io.ExifReader.ImageMetadataInfo;
import com.buldreinfo.io.ExifReader.ImageRotation;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * Covers the normalisation of the EXIF orientation tag on the JPEGs we store. The pixels are always kept in
 * display orientation, so a tag that still describes the orientation of the uploaded file makes EXIF-aware
 * viewers rotate the stored original a second time — while the derived web images, which are written
 * without metadata, keep showing the pixels. That is how an original ends up disagreeing with its own
 * thumbnails, and why a rotation used to fix the thumbnail and break the original.
 */
public class JpegExifOrientationTest {
	private static final int WIDTH = 40;
	private static final int HEIGHT = 20;
	private static final int APP1_MARKER = 0xE1;
	private static final int TAG_ORIENTATION = 0x0112;
	private static final int TAG_RESOLUTION_UNIT = 0x0128;
	private static final int TYPE_SHORT = 3;

	@Test
	public void orientationIsSetToUpright() throws Exception {
		assertEquals(1, readOrientation(normalize(jpegWithExif(false, new int[][] { { TAG_ORIENTATION, 6 } }))));
		assertEquals(1, readOrientation(normalize(jpegWithExif(false, new int[][] { { TAG_ORIENTATION, 8 } }))));
	}

	@Test
	public void bigEndianExifIsPatchedToo() throws Exception {
		assertEquals(1, readOrientation(normalize(jpegWithExif(true, new int[][] { { TAG_ORIENTATION, 6 } }))));
	}

	@Test
	public void neighboursOfTheOrientationEntrySurvive() throws Exception {
		byte[] exif = jpegWithExif(false, new int[][] { { TAG_ORIENTATION, 6 }, { TAG_RESOLUTION_UNIT, 2 } });

		byte[] patched = normalize(exif);

		assertEquals(exif.length, patched.length);
		assertEquals(1, readOrientation(patched));
		ExifIFD0Directory directory = readExifDirectory(patched);
		assertTrue(directory.containsTag(TAG_RESOLUTION_UNIT));
		assertEquals(2, directory.getInt(TAG_RESOLUTION_UNIT));
	}

	@Test
	public void pixelsSurviveThePatch() throws Exception {
		byte[] patched = normalize(jpegWithExif(false, new int[][] { { TAG_ORIENTATION, 6 } }));

		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(patched));

		assertNotNull(decoded);
		assertEquals(WIDTH, decoded.getWidth());
		assertEquals(HEIGHT, decoded.getHeight());
	}

	@Test
	public void jpegWithoutExifIsLeftAlone() throws Exception {
		byte[] plain = baseJpeg();

		assertArrayEquals(plain, normalize(plain));
	}

	@Test
	public void exifWithoutOrientationEntryIsLeftAlone() throws Exception {
		byte[] exif = jpegWithExif(false, new int[][] { { TAG_RESOLUTION_UNIT, 2 } });

		assertArrayEquals(exif, normalize(exif));
	}

	@Test
	public void nonExifApp1SegmentsAreLeftAlone() throws Exception {
		// XMP lives in an APP1 segment as well, so the patcher must not walk into it.
		byte[] xmp = jpegWithApp1("http://ns.adobe.com/xap/1.0/\0<x:xmpmeta/>".getBytes(StandardCharsets.ISO_8859_1));

		assertArrayEquals(xmp, normalize(xmp));
	}

	/**
	 * The regression behind the fix: {@link ImageService} rotates the pixels into display orientation and
	 * writes the original with the metadata of the uploaded file. The writer keeps that file's EXIF block —
	 * orientation tag included — so the tag has to be reset to upright before the bytes are stored.
	 */
	@Test
	public void orientationOfTheSourceDoesNotSurviveTheWriteOfAnOriginal() throws Exception {
		byte[] uploaded = jpegWithExif(false, new int[][] { { TAG_ORIENTATION, 6 } });
		ImageMetadataInfo metadata = new ExifReader().extractMetadata(uploaded);
		assertEquals(ImageRotation.CW_90, metadata.rotation());
		BufferedImage pixelsRotatedToUpright = ImageIO.read(new ByteArrayInputStream(uploaded));

		byte[] written = JpegWriter.writeJpeg(pixelsRotatedToUpright, metadata.nativeMetadata());

		assertEquals(6, readOrientation(written));
		assertEquals(1, readOrientation(normalize(written)));
	}

	private static byte[] normalize(byte[] jpeg) {
		return JpegExifOrientation.normalizeOrientation(jpeg);
	}

	private static Integer readOrientation(byte[] jpeg) throws Exception {
		ExifIFD0Directory directory = readExifDirectory(jpeg);
		return directory == null ? null : directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
	}

	private static ExifIFD0Directory readExifDirectory(byte[] jpeg) throws Exception {
		Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(jpeg));
		return metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	}

	private static byte[] baseJpeg() throws IOException {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		}
	}

	/** A JPEG whose APP1 segment holds an EXIF block with the given IFD0 entries, each a single SHORT. */
	private static byte[] jpegWithExif(boolean bigEndian, int[][] entries) throws IOException {
		return jpegWithApp1(exifPayload(bigEndian, entries));
	}

	private static byte[] exifPayload(boolean bigEndian, int[][] entries) {
		byte[] payload = new byte[6 + 8 + 2 + entries.length * 12 + 4];
		payload[0] = 'E';
		payload[1] = 'x';
		payload[2] = 'i';
		payload[3] = 'f';
		int tiff = 6;
		payload[tiff] = (byte) (bigEndian ? 'M' : 'I');
		payload[tiff + 1] = (byte) (bigEndian ? 'M' : 'I');
		writeShort(payload, tiff + 2, bigEndian, 42);
		writeInt(payload, tiff + 4, bigEndian, 8);
		int ifd0 = tiff + 8;
		writeShort(payload, ifd0, bigEndian, entries.length);
		for (int i = 0; i < entries.length; i++) {
			int entry = ifd0 + 2 + i * 12;
			writeShort(payload, entry, bigEndian, entries[i][0]);
			writeShort(payload, entry + 2, bigEndian, TYPE_SHORT);
			writeInt(payload, entry + 4, bigEndian, 1);
			writeShort(payload, entry + 8, bigEndian, entries[i][1]);
		}
		return payload;
	}

	private static byte[] jpegWithApp1(byte[] app1Payload) throws IOException {
		byte[] base = baseJpeg();
		byte[] result = new byte[2 + 4 + app1Payload.length + base.length - 2];
		result[0] = base[0];
		result[1] = base[1];
		result[2] = (byte) 0xFF;
		result[3] = (byte) APP1_MARKER;
		writeShort(result, 4, true, app1Payload.length + 2);
		System.arraycopy(app1Payload, 0, result, 6, app1Payload.length);
		System.arraycopy(base, 2, result, 6 + app1Payload.length, base.length - 2);
		return result;
	}

	private static void writeShort(byte[] data, int offset, boolean bigEndian, int value) {
		data[offset] = (byte) (bigEndian ? value >> 8 : value);
		data[offset + 1] = (byte) (bigEndian ? value : value >> 8);
	}

	private static void writeInt(byte[] data, int offset, boolean bigEndian, int value) {
		for (int i = 0; i < 4; i++) {
			data[offset + i] = (byte) (bigEndian ? value >> (8 * (3 - i)) : value >> (8 * i));
		}
	}
}

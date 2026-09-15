package com.buldreinfo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.buldreinfo.io.ExifReader.ImageRotation;

/**
 * Covers how a rotate request is turned into a pixel rotation ({@link ImageService#composedRotationDegrees}).
 * A viewer shows a stored original as its pixels plus the EXIF orientation of the file, so a request of
 * "90 degrees" means "rotate what I am looking at": the stored orientation goes on top of the request. The
 * result is written upright, which is what keeps an original and its thumbnails in sync.
 */
public class ImageServiceRotateTest {

	@Test
	public void requestIsTheWholeRotationWithoutStoredOrientation() {
		assertEquals(90, ImageService.composedRotationDegrees(90, null));
		assertEquals(180, ImageService.composedRotationDegrees(180, null));
		assertEquals(270, ImageService.composedRotationDegrees(270, null));
	}

	@Test
	public void storedOrientationIsAddedToTheRequest() {
		assertEquals(180, ImageService.composedRotationDegrees(90, ImageRotation.CW_90));
		assertEquals(270, ImageService.composedRotationDegrees(90, ImageRotation.CW_180));
		assertEquals(90, ImageService.composedRotationDegrees(270, ImageRotation.CW_180));
	}

	/** An orientation that cancels the request out leaves the pixels alone; the file is still normalised. */
	@Test
	public void oppositeRotationsCancelOut() {
		assertEquals(0, ImageService.composedRotationDegrees(90, ImageRotation.CW_270));
		assertEquals(0, ImageService.composedRotationDegrees(180, ImageRotation.CW_180));
		assertEquals(0, ImageService.composedRotationDegrees(270, ImageRotation.CW_90));
	}

	@Test
	public void resultIsAlwaysWithinAFullTurn() {
		assertEquals(180, ImageService.composedRotationDegrees(270, ImageRotation.CW_270));
		assertEquals(90, ImageService.composedRotationDegrees(180, ImageRotation.CW_270));
		assertEquals(270, ImageService.composedRotationDegrees(180, ImageRotation.CW_90));
	}
}

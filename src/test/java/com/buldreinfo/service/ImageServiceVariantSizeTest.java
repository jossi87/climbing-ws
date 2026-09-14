package com.buldreinfo.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Covers the size decisions behind {@link ImageService#processResize}: which source a requested size is
 * resized from, whether the image shrinks at all (no shrink means "redirect to what we already have"
 * instead of downloading the source and generating a copy) and how big the generated file would be.
 *
 * These are the numbers the live frontend asks for, so the cases mirror real requests: tiles use
 * {@code minDimension}, the media modal uses {@code targetWidth=1920} and the zoom viewer asks for the
 * original width and above.
 */
public class ImageServiceVariantSizeTest {

	/** iPhone portrait: the standard web image is capped by height, so it is only 1080px wide. */
	private static final int PORTRAIT_W = 3024;
	private static final int PORTRAIT_H = 4032;

	/** Classic 4:3 landscape: the standard web image ends up exactly 1920x1440. */
	private static final int LANDSCAPE_W = 4032;
	private static final int LANDSCAPE_H = 3024;

	@Test
	public void webImageIsCappedByHeightForPortraitPhotos() {
		assertArrayEquals(new int[] { 1080, 1440 }, ImageService.webDimensions(PORTRAIT_W, PORTRAIT_H));
	}

	@Test
	public void webImageIsCappedByWidthForWidePhotos() {
		assertArrayEquals(new int[] { 2160, 1440 }, ImageService.webDimensions(6000, 4000));
	}

	@Test
	public void webImageIsNeverUpscaled() {
		assertArrayEquals(new int[] { 800, 600 }, ImageService.webDimensions(800, 600));
	}

	@Test
	public void sizesUpToTheWebWidthResizeFromTheWebImage() {
		assertTrue(ImageService.resizesFromWebImage(ImageService.IMAGE_WEB_WIDTH, 0));
		assertTrue(ImageService.resizesFromWebImage(0, ImageService.IMAGE_WEB_WIDTH));
		// Larger requests are served from the original, which is a different (bigger) source.
		assertFalse(ImageService.resizesFromWebImage(ImageService.IMAGE_WEB_WIDTH + 1, 0));
		assertFalse(ImageService.resizesFromWebImage(0, ImageService.IMAGE_WEB_WIDTH + 1));
	}

	@Test
	public void modalRequestForPortraitPhotoDoesNotShrinkAnything() {
		// 1920 is wider than the 1080px web image, so there is nothing to resize.
		int[] web = ImageService.webDimensions(PORTRAIT_W, PORTRAIT_H);
		int[] target = ImageService.targetDimensions(web[0], web[1], 1920, 0);
		assertArrayEquals(web, target);
	}

	@Test
	public void modalRequestEqualToWebWidthDoesNotShrinkAnything() {
		// Boundary case: landscape photos end up exactly 1920 wide, and the modal asks for exactly 1920.
		int[] web = ImageService.webDimensions(LANDSCAPE_W, LANDSCAPE_H);
		assertArrayEquals(new int[] { 1920, 1440 }, web);
		int[] target = ImageService.targetDimensions(web[0], web[1], 1920, 0);
		assertArrayEquals(web, target);
	}

	@Test
	public void tileRequestStillResizes() {
		int[] web = ImageService.webDimensions(PORTRAIT_W, PORTRAIT_H);
		int[] target = ImageService.targetDimensions(web[0], web[1], 0, 188);
		// 188 is the short side, so the long side scales with it.
		assertArrayEquals(new int[] { 188, 251 }, target);
		assertTrue(target[0] < web[0] && target[1] < web[1]);
	}

	@Test
	public void zoomRequestAboveOriginalWidthDoesNotShrinkAnything() {
		// The zoom viewer emits candidate widths up to the original width, and sizes above it must not
		// turn into an upscaled copy of the original.
		assertFalse(ImageService.resizesFromWebImage(5120, 0));
		int[] target = ImageService.targetDimensions(LANDSCAPE_W, LANDSCAPE_H, 5120, 0);
		assertArrayEquals(new int[] { LANDSCAPE_W, LANDSCAPE_H }, target);
	}

	@Test
	public void zoomRequestEqualToOriginalWidthDoesNotShrinkAnything() {
		int[] target = ImageService.targetDimensions(LANDSCAPE_W, LANDSCAPE_H, LANDSCAPE_W, 0);
		assertArrayEquals(new int[] { LANDSCAPE_W, LANDSCAPE_H }, target);
	}

	@Test
	public void zoomRequestBelowOriginalWidthResizesFromTheOriginal() {
		assertFalse(ImageService.resizesFromWebImage(3000, 0));
		int[] target = ImageService.targetDimensions(LANDSCAPE_W, LANDSCAPE_H, 3000, 0);
		assertArrayEquals(new int[] { 3000, 2250 }, target);
	}

	@Test
	public void smallSourceRequestNeverUpscales() {
		// A source smaller than the requested size keeps its own dimensions, so nothing is generated.
		int[] target = ImageService.targetDimensions(800, 600, 1920, 0);
		assertArrayEquals(new int[] { 800, 600 }, target);
	}

	@Test
	public void targetWidthWinsOverSourceWhenItShrinks() {
		int[] target = ImageService.targetDimensions(2560, 1440, 300, 0);
		assertArrayEquals(new int[] { 300, 169 }, target);
	}
}

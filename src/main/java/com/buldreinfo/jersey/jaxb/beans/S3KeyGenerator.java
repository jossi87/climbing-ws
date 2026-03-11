package com.buldreinfo.jersey.jaxb.beans;

import java.util.List;

public final class S3KeyGenerator {
	public static List<String> getCachedMediaPrefixes(int idMedia) {
		return List.of(getWebJpgResizedPrefix(idMedia),
				S3KeyGenerator.getWebWebpResizedPrefix(idMedia),
				S3KeyGenerator.getWebJpgRegionPrefix(idMedia),
				S3KeyGenerator.getWebWebpRegionPrefix(idMedia));
	}

	public static String getOriginalJpg(int id) {
		return "original/jpg/%s/%d.jpg".formatted(getFolderName(id), id);
	}

	public static String getOriginalMp4(int id) {
		return "original/mp4/%s/%d.mp4".formatted(getFolderName(id), id);
	}

	public static String getWebJpg(int id) {
		return "web/jpg/%s/%d.jpg".formatted(getFolderName(id), id);
	}

	public static String getWebJpgRegion(int id, int x, int y, int width, int height) {
		return getWebJpgRegionPrefix(id) + "%d_%d_%d_%d.jpg".formatted(x, y, width, height);
	}
	
	public static String getWebJpgResized(int id, int targetWidth, int minDimension) {
		return getWebJpgResizedPrefix(id) + "w%d_m%d.jpg".formatted(targetWidth, minDimension);
	}

	public static String getWebMp4(int id) {
		return "web/mp4/%s/%d.mp4".formatted(getFolderName(id), id);
	}

	public static String getWebWebm(int id) {
		return "web/webm/%s/%d.webm".formatted(getFolderName(id), id);
	}

	public static String getWebWebp(int id) {
		return "web/webp/%s/%d.webp".formatted(getFolderName(id), id);
	}

	public static String getWebWebpRegion(int id, int x, int y, int width, int height) {
		return getWebWebpRegionPrefix(id) + "%d_%d_%d_%d.webp".formatted(x, y, width, height);
	}

	public static String getWebWebpResized(int id, int targetWidth, int minDimension) {
		return getWebWebpResizedPrefix(id) + "w%d_m%d.webp".formatted(targetWidth, minDimension);
	}
	
	private static String getFolderName(int id) {
		return String.valueOf((id / 100) * 100);
	}

	private static String getWebJpgRegionPrefix(int id) {
		return "web/jpg_region/%s/%d/".formatted(getFolderName(id), id);
	}

	private static String getWebJpgResizedPrefix(int id) {
		return "web/jpg_resized/%s/%d/".formatted(getFolderName(id), id);
	}

	private static String getWebWebpRegionPrefix(int id) {
		return "web/webp_region/%s/%d/".formatted(getFolderName(id), id);
	}

	private static String getWebWebpResizedPrefix(int id) {
		return "web/webp_resized/%s/%d/".formatted(getFolderName(id), id);
	}

	private S3KeyGenerator() {
	}
}
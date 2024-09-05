package com.buldreinfo.jersey.jaxb.beans;

import java.util.Arrays;

public record ImageRegion(int x, int y, int width, int height) {
	public static ImageRegion fromString(String region) {
		if (region != null) {
			int[] parts = Arrays.asList(region.split(",")).stream()
					  .map(String::trim)
					  .mapToInt(Integer::parseInt)
					  .toArray();
			return new ImageRegion(parts[0], parts[1], parts[2], parts[3]);
		}
		return null;
	}
}

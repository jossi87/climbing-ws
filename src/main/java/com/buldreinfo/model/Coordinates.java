package com.buldreinfo.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Coordinates(int id, double latitude, double longitude, double elevation, String elevationSource, double distance) {
	public static final String ELEVATION_SOURCE_GOOGLE = "Google Elevation API";

	public Coordinates withId(int id) {
		return new Coordinates(id, latitude, longitude, elevation, elevationSource, distance);
	}

	public Coordinates withElevation(double elevation, String elevationSource) {
		return new Coordinates(id, latitude, longitude, elevation, elevationSource, distance);
	}

	public Coordinates withDistance(double distance) {
		return new Coordinates(id, latitude, longitude, elevation, elevationSource, distance);
	}

	public Coordinates roundTo10Digits() {
		return new Coordinates(
				id,
				round(latitude, 10),
				round(longitude, 10),
				elevation,
				elevationSource,
				distance
				);
	}

	private static double round(double value, int places) {
		return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
	}
}
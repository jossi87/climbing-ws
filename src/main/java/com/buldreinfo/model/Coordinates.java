package com.buldreinfo.model;

import com.buldreinfo.util.GeoUtils;

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
		return new Coordinates(id, GeoUtils.roundToTenDigits(latitude), GeoUtils.roundToTenDigits(longitude), elevation, elevationSource, distance);
	}
}

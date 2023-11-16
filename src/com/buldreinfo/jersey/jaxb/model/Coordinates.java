package com.buldreinfo.jersey.jaxb.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Coordinates {
	public final static String ELEVATION_SOURCE_GOOGLE = "Google Elevation API";
	public final static String ELEVATION_SOURCE_GPX = "GPX";
	public final static String ELEVATION_SOURCE_TCX = "TCX";
	private int id;
	private double latitude;
	private double longitude;
	private double elevation;
	private String elevationSource;
	private double distance;
	
	public Coordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Coordinates(int id, double latitude, double longitude, double elevation, String elevationSource) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		this.elevationSource = elevationSource;
	}

	public double getDistance() {
		return distance;
	}

	public double getElevation() {
		return elevation;
	}

	public String getElevationSource() {
		return elevationSource;
	}
	
	public int getId() {
		return id;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}

	public void roundCoordinatesToMaximum10digitsAfterComma() {
		this.latitude = round(latitude, 10);
		this.longitude = round(longitude, 10);
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public void setElevation(double elevation, String elevationSource) {
		this.elevation = elevation;
		this.elevationSource = elevationSource;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "Coordinates [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", elevation="
				+ elevation + ", elevationSource=" + elevationSource + ", distance=" + distance + "]";
	}

	private double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
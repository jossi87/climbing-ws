package com.buldreinfo.jersey.jaxb.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Coordinate {
	private int id;
	private double latitude;
	private double longitude;
	private double elevation;
	
	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Coordinate(int id, double latitude, double longitude, double elevation) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
	}

	public double getElevation() {
		return elevation;
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
	
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public void roundCoordinatesToMaximum10digitsAfterComma() {
		this.latitude = round(latitude, 10);
		this.longitude = round(longitude, 10);
	}
	
	private double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}

	@Override
	public String toString() {
		return "Coordinate [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", elevation="
				+ elevation + "]";
	}
}
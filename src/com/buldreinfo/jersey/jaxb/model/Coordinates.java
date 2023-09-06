package com.buldreinfo.jersey.jaxb.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Coordinates {
	private int id;
	private double latitude;
	private double longitude;
	private double elevation;
	private double distance;
	
	public Coordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Coordinates(int id, double latitude, double longitude, double elevation) {
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
	
	public double getDistance() {
		return distance;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return "Coordinates [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", elevation="
				+ elevation + ", distance=" + distance + "]";
	}
}
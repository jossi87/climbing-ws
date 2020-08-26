package com.buldreinfo.jersey.jaxb.leafletprint.beans;

public class Marker {
	private final double lat;
	private final double lng;
	private final boolean isParking;
	private final String label;
	
	public Marker(double lat, double lng, boolean isParking, String label) {
		this.lat = lat;
		this.lng = lng;
		this.isParking = isParking;
		this.label = label;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public boolean isParking() {
		return isParking;
	}

	public String getLabel() {
		return label;
	}
}
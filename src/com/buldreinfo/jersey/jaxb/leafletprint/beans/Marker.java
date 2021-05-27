package com.buldreinfo.jersey.jaxb.leafletprint.beans;

public class Marker {
	public static enum ICON_TYPE { DEFAULT, PARKING, ROCK };
	private final double lat;
	private final double lng;
	private final ICON_TYPE iconType;
	private final String label;
	
	public Marker(double lat, double lng, ICON_TYPE iconType, String label) {
		this.lat = lat;
		this.lng = lng;
		this.iconType = iconType;
		this.label = label;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public ICON_TYPE getIconType() {
		return iconType;
	}

	public String getLabel() {
		return label;
	}
}
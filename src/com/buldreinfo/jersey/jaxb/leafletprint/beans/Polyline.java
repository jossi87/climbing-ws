package com.buldreinfo.jersey.jaxb.leafletprint.beans;

public class Polyline {
	private final String name;
	private final String polyline;
	
	public Polyline(String name, String polyline) {
		this.name = name;
		this.polyline = polyline;
	}

	public String getName() {
		return name;
	}

	public String getPolyline() {
		return polyline;
	}
}
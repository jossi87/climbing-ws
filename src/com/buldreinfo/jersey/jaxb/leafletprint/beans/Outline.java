package com.buldreinfo.jersey.jaxb.leafletprint.beans;

public class Outline {
	private final String name;
	private final String polygonCoords;
	
	public Outline(String name, String polygonCoords) {
		this.name = name;
		this.polygonCoords = polygonCoords;
	}

	public String getName() {
		return name;
	}

	public String getPolygonCoords() {
		return polygonCoords;
	}
}
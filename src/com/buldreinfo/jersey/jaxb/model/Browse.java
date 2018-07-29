package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

public class Browse {
	private final String title;
	private final Collection<Area> areas;
	private final double defaultLat;
	private final double defaultLng;
	private final int defaultZoom;
	
	public Browse(String title, Collection<Area> areas, double defaultLat, double defaultLng, int defaultZoom) {
		this.title = title;
		this.areas = areas;
		this.defaultLat = defaultLat;
		this.defaultLng = defaultLng;
		this.defaultZoom = defaultZoom;
	}

	public Collection<Area> getAreas() {
		return areas;
	}

	public double getDefaultLat() {
		return defaultLat;
	}

	public double getDefaultLng() {
		return defaultLng;
	}

	public int getDefaultZoom() {
		return defaultZoom;
	}

	public String getTitle() {
		return title;
	}
}
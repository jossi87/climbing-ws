package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

public class Browse {
	private final String title;
	private final Collection<Area> areas;
	private final LatLng defaultCenter;
	private final int defaultZoom;
	
	public Browse(String title, Collection<Area> areas, LatLng defaultCenter, int defaultZoom) {
		this.title = title;
		this.areas = areas;
		this.defaultCenter = defaultCenter;
		this.defaultZoom = defaultZoom;
	}

	public Collection<Area> getAreas() {
		return areas;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}

	public String getTitle() {
		return title;
	}
}
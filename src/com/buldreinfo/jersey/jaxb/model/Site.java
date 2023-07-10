package com.buldreinfo.jersey.jaxb.model;

public class Site {
	private final String group;
	private final String name;
	private final String url;
	private final String polygonCoords;
	private final boolean active;
	
	public Site(String group, String name, String url, String polygonCoords, boolean active) {
		this.group = group;
		this.name = name;
		this.url = url;
		this.polygonCoords = polygonCoords;
		this.active = active;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public String getPolygonCoords() {
		return polygonCoords;
	}

	public String getUrl() {
		return url;
	}

	public boolean isActive() {
		return active;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Site {
	private final String group;
	private final String name;
	private final String url;
	private List<Coordinate> outline;
	private final boolean active;
	
	public Site(String group, String name, String url, boolean active) {
		this.group = group;
		this.name = name;
		this.url = url;
		this.active = active;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public List<Coordinate> getOutline() {
		return outline;
	}

	public String getUrl() {
		return url;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setOutline(List<Coordinate> outline) {
		this.outline = outline;
	}
}
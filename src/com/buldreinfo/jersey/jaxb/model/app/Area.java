package com.buldreinfo.jersey.jaxb.model.app;

import java.util.ArrayList;
import java.util.List;

public class Area {
	private final int regionId;
	private final int id;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final List<Sector> sectors = new ArrayList<>();
	
	public Area(int regionId, int id, String name, String comment, double lat, double lng) {
		this.regionId = regionId;
		this.id = id;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
	}
	
	public int getRegionId() {
		return regionId;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getComment() {
		return comment;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public List<Sector> getSectors() {
		return sectors;
	}
}
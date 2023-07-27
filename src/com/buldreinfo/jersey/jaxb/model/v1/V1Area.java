package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.ArrayList;
import java.util.List;

public class V1Area {
	private final int regionId;
	private final int id;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final List<V1Sector> sectors = new ArrayList<>();
	
	public V1Area(int regionId, int id, String name, String comment, double lat, double lng) {
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

	public List<V1Sector> getSectors() {
		return sectors;
	}
}
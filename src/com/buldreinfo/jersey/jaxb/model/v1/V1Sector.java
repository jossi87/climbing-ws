package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.ArrayList;
import java.util.List;

public class V1Sector {
	private final int areaId;
	private final int id;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final List<V1Media> media = new ArrayList<>();
	private final List<V1Problem> problems = new ArrayList<>();
	
	public V1Sector(int areaId, int id, String name, String comment, double lat, double lng) {
		this.areaId = areaId;
		this.id = id;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
	}
	
	public int getAreaId() {
		return areaId;
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

	public List<V1Media> getMedia() {
		return media;
	}

	public List<V1Problem> getProblems() {
		return problems;
	}
}
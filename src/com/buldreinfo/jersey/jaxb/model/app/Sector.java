package com.buldreinfo.jersey.jaxb.model.app;

import java.util.ArrayList;
import java.util.List;

public class Sector {
	private final int areaId;
	private final int id;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final List<Media> media = new ArrayList<>();
	private final List<Problem> problems = new ArrayList<>();
	
	public Sector(int areaId, int id, String name, String comment, double lat, double lng) {
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

	public List<Media> getMedia() {
		return media;
	}

	public List<Problem> getProblems() {
		return problems;
	}
}
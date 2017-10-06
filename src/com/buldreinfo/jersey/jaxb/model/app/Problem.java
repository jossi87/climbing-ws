package com.buldreinfo.jersey.jaxb.model.app;

import java.util.ArrayList;
import java.util.List;

public class Problem {
	private final int sectorId;
	private final int id;
	private final int nr;
	private final String name;
	private final String comment;
	private final int grade;
	private final String fa;
	private final double lat;
	private final double lng;
	private final List<Media> media = new ArrayList<>();
	
	public Problem(int sectorId, int id, int nr, String name, String comment, int grade, String fa, double lat, double lng) {
		this.sectorId = sectorId;
		this.id = id;
		this.nr = nr;
		this.name = name;
		this.comment = comment;
		this.grade = grade;
		this.fa = fa;
		this.lat = lat;
		this.lng = lng;
	}
	
	public int getSectorId() {
		return sectorId;
	}

	public int getId() {
		return id;
	}

	public int getNr() {
		return nr;
	}

	public String getName() {
		return name;
	}

	public String getComment() {
		return comment;
	}

	public int getGrade() {
		return grade;
	}

	public String getFa() {
		return fa;
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
}
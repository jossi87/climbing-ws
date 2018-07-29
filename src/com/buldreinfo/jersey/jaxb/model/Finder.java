package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Finder {
	private final String title;
	private final List<Problem> problems;
	private final LatLng defaultCenter;
	private final boolean isBouldering;
	
	public Finder(String title, List<Problem> problems, LatLng defaultCenter, boolean isBouldering) {
		this.title = title;
		this.problems = problems;
		this.defaultCenter = defaultCenter;
		this.isBouldering = isBouldering;
	}

	public String getTitle() {
		return title;
	}

	public List<Problem> getProblems() {
		return problems;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public boolean isBouldering() {
		return isBouldering;
	}
}
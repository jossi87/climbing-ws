package com.buldreinfo.jersey.jaxb.model;

public class Site {
	private final String name;
	private final String url;
	private final String polygonCoords;
	private final int numProblems;
	
	public Site(String name, String url, String polygonCoords, int numProblems) {
		this.name = name;
		this.url = url;
		this.polygonCoords = polygonCoords;
		this.numProblems = numProblems;
	}
	
	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getPolygonCoords() {
		return polygonCoords;
	}
	
	public int getNumProblems() {
		return numProblems;
	}
}
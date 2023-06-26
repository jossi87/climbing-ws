package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.helpers.Setup.GRADE_SYSTEM;

public class SitesRegion {
	private final String name;
	private final String url;
	private final String polygonCoords;
	private final int numProblems;
	private final GRADE_SYSTEM system;
	
	public SitesRegion(String name, String url, String polygonCoords, int numProblems, GRADE_SYSTEM system) {
		this.name = name;
		this.url = url;
		this.polygonCoords = polygonCoords;
		this.numProblems = numProblems;
		this.system = system;
	}
	
	public String getName() {
		return name;
	}

	public int getNumProblems() {
		return numProblems;
	}

	public String getPolygonCoords() {
		return polygonCoords;
	}
	
	public GRADE_SYSTEM getSystem() {
		return system;
	}
	
	public String getUrl() {
		return url;
	}
}
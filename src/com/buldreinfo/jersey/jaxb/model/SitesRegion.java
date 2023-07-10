package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.helpers.Setup.GRADE_SYSTEM;

public class SitesRegion {
	private final String shortName;
	private final String name;
	private final String url;
	private final String polygonCoords;
	private final int numProblems;
	private final GRADE_SYSTEM system;
	private final boolean active;
	
	public SitesRegion(String shortName, String name, String url, String polygonCoords, int numProblems, GRADE_SYSTEM system, boolean active) {
		this.shortName = shortName;
		this.name = name;
		this.url = url;
		this.polygonCoords = polygonCoords;
		this.numProblems = numProblems;
		this.system = system;
		this.active = active;
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
	
	public String getShortName() {
		return shortName;
	}

	public GRADE_SYSTEM getSystem() {
		return system;
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean isActive() {
		return active;
	}
}
package com.buldreinfo.jersey.jaxb.model.app;

import java.util.ArrayList;
import java.util.List;

public class Region {
	private final int id;
	private final String name;
	private final List<Area> areas = new ArrayList<>();
	
	public Region(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public List<Area> getAreas() {
		return areas;
	}
}
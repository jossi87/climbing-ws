package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.ArrayList;
import java.util.List;

public class V1Region {
	private final int id;
	private final String name;
	private final List<V1Area> areas = new ArrayList<>();
	
	public V1Region(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public List<V1Area> getAreas() {
		return areas;
	}
}
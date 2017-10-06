package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final int id;
	private final int visibility;
	private final String value;
	
	public Search(int id, int visibility, String value) {
		this.id = id;
		this.visibility = visibility;
		this.value = value;
	}

	public int getId() {
		return id;
	}
	
	public String getValue() {
		return value;
	}

	public int getVisibility() {
		return visibility;
	}

	@Override
	public String toString() {
		return "Search [id=" + id + ", visibility=" + visibility + ", value=" + value + "]";
	}
}
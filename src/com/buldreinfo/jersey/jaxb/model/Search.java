package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String url;
	private final int id;
	private final int visibility;
	private final String value;
	
	public Search(String url, int id, int visibility, String value) {
		this.url = url;
		this.id = id;
		this.visibility = visibility;
		this.value = value;
	}

	public String getUrl() {
		return url;
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
		return "Search [url=" + url + ", id=" + id + ", visibility=" + visibility + ", value=" + value + "]";
	}
}
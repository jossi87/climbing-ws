package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String url;
	private final int visibility;
	private final String value;
	
	public Search(String url, int visibility, String value) {
		this.url = url;
		this.visibility = visibility;
		this.value = value;
	}

	public String getUrl() {
		return url;
	}
	
	public String getValue() {
		return value;
	}

	public int getVisibility() {
		return visibility;
	}

	@Override
	public String toString() {
		return "Search [url=" + url + ", visibility=" + visibility + ", value=" + value + "]";
	}
}
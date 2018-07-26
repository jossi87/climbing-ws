package com.buldreinfo.jersey.jaxb.model;

public class Metadata {
	private final String title;
	private final String description;
	
	public Metadata(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}
}
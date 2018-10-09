package com.buldreinfo.jersey.jaxb.model;

public class FindResult {
	private final String title;
	private final String description;
	private final String image;
	private final String url;
	private final int visibility;
	
	public FindResult(String title, String description, String image, String url, int visibility) {
		this.title = title;
		this.description = description;
		this.image = image;
		this.url = url;
		this.visibility = visibility;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getImage() {
		return image;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int getVisibility() {
		return visibility;
	}
}
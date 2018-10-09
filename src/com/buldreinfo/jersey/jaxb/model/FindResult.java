package com.buldreinfo.jersey.jaxb.model;

public class FindResult {
	private final String title;
	private final String image;
	private final String avatar;
	private final String url;
	private final int visibility;
	
	public FindResult(String title, String image, String avatar, String url, int visibility) {
		this.title = title;
		this.image = image;
		this.avatar = avatar;
		this.url = url;
		this.visibility = visibility;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getImage() {
		return image;
	}
	
	public String getAvatar() {
		return avatar;
	}
	
	public String getUrl() {
		return url;
	}
	
	public int getVisibility() {
		return visibility;
	}
}
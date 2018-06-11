package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String avatar;
	private final String url;
	private final String value;
	private final int visibility;
	
	public Search(String avatar, String url, String value, int visibility) {
		this.avatar = avatar;
		this.url = url;
		this.value = value;
		this.visibility = visibility;
	}
	
	public String getAvatar() {
		return avatar;
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
		return "Search [avatar=" + avatar + ", url=" + url + ", value=" + value + ", visibility=" + visibility + "]";
	}
}
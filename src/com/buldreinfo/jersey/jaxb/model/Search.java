package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String avatar;
	private final String url;
	private final String value;
	
	public Search(String avatar, String url, String value) {
		this.avatar = avatar;
		this.url = url;
		this.value = value;
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

	@Override
	public String toString() {
		return "Search [avatar=" + avatar + ", url=" + url + ", value=" + value + "]";
	}
}
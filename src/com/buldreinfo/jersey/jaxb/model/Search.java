package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String title;
	private final String description;
	private final String url;
	private final String mediaurl;
	private final int mediaid;
	private final int visibility;
	
	public Search(String title, String description, String url, String mediaurl, int mediaid, int visibility) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.mediaurl = mediaurl;
		this.mediaid = mediaid;
		this.visibility = visibility;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getMediaId() {
		return mediaid;
	}

	public String getMediaUrl() {
		return mediaurl;
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
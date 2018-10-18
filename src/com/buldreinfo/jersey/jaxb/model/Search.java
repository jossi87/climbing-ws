package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String title;
	private final String description;
	private final String url;
	private final String mediaUrl;
	private final int mediaId;
	private final int visibility;
	
	public Search(String title, String description, String url, String mediaUrl, int mediaId, int visibility) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.mediaUrl = mediaUrl;
		this.mediaId = mediaId;
		this.visibility = visibility;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getMediaId() {
		return mediaId;
	}

	public String getMediaUrl() {
		return mediaUrl;
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
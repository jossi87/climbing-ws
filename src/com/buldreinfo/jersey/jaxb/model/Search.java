package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String title;
	private final String description;
	private final String url;
	private final String mediaurl;
	private final int mediaid;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	
	public Search(String title, String description, String url, String mediaurl, int mediaid, boolean lockedAdmin, boolean lockedSuperadmin) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.mediaurl = mediaurl;
		this.mediaid = mediaid;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public String getMediaurl() {
		return mediaurl;
	}

	public int getMediaid() {
		return mediaid;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class Search {
	private final String title;
	private final String description;
	private final String url;
	private final String mediaurl;
	private final int mediaid;
	private final int crc32;
	private final boolean lockedadmin;
	private final boolean lockedsuperadmin;
	
	public Search(String title, String description, String url, String mediaurl, int mediaid, int crc32, boolean lockedAdmin, boolean lockedSuperadmin) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.mediaurl = mediaurl;
		this.mediaid = mediaid;
		this.crc32 = crc32;
		this.lockedadmin = lockedAdmin;
		this.lockedsuperadmin = lockedSuperadmin;
	}

	public int getCrc32() {
		return crc32;
	}

	public String getDescription() {
		return description;
	}

	public int getMediaid() {
		return mediaid;
	}

	public String getMediaurl() {
		return mediaurl;
	}
	
	public String getTitle() {
		return title;
	};

	public String getUrl() {
		return url;
	}

	public boolean isLockedadmin() {
		return lockedadmin;
	}

	public boolean isLockedsuperadmin() {
		return lockedsuperadmin;
	}
}
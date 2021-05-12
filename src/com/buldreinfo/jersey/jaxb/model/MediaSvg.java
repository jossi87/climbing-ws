package com.buldreinfo.jersey.jaxb.model;

public class MediaSvg {
	private final int id;
	private final int mediaId;
	private final String path;
	
	public MediaSvg(int id, int mediaId, String path) {
		this.id = id;
		this.mediaId = mediaId;
		this.path = path;
	}

	public int getId() {
		return id;
	}

	public int getMediaId() {
		return mediaId;
	}

	public String getPath() {
		return path;
	}
}
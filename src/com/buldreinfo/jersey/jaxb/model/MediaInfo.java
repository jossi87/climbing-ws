package com.buldreinfo.jersey.jaxb.model;

public class MediaInfo {
	private final int mediaId;
	private final String description;
	private final int pitch;
	private final boolean trivia;
	
	public MediaInfo(int mediaId, String description, int pitch, boolean trivia) {
		this.mediaId = mediaId;
		this.description = description;
		this.pitch = pitch;
		this.trivia = trivia;
	}

	public int getMediaId() {
		return mediaId;
	}

	public String getDescription() {
		return description;
	}

	public int getPitch() {
		return pitch;
	}

	public boolean isTrivia() {
		return trivia;
	}
}
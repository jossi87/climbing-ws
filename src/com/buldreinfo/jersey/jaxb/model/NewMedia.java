package com.buldreinfo.jersey.jaxb.model;

public class NewMedia {
	private final String name;
	private final String photographer;
	private final String inPhoto;
	private final int pitch;
	private final boolean trivia;
	private final String description;
	private final String embedVideoUrl;
	private final String embedThumbnailUrl;
	private final long embedMilliseconds;
	
	public NewMedia(String name, String photographer, String inPhoto, int pitch, boolean trivia, String description, String embedVideoUrl, String embedThumbnailUrl, long embedMilliseconds) {
		this.name = name;
		this.photographer = photographer;
		this.inPhoto = inPhoto;
		this.pitch = pitch;
		this.trivia = trivia;
		this.description = description;
		this.embedVideoUrl = embedVideoUrl;
		this.embedThumbnailUrl = embedThumbnailUrl;
		this.embedMilliseconds = embedMilliseconds;
	}
	
	public String getDescription() {
		return description;
	}
	
	public long getEmbedMilliseconds() {
		return embedMilliseconds;
	}
	
	public String getEmbedThumbnailUrl() {
		return embedThumbnailUrl;
	}
	
	public String getEmbedVideoUrl() {
		return embedVideoUrl;
	}
	
	public String getInPhoto() {
		return inPhoto;
	}
	
	public String getName() {
		return name;
	}

	public String getPhotographer() {
		return photographer;
	}
	
	public int getPitch() {
		return pitch;
	}

	public boolean isTrivia() {
		return trivia;
	}

	@Override
	public String toString() {
		return "NewMedia [name=" + name + ", photographer=" + photographer + ", inPhoto=" + inPhoto + ", pitch=" + pitch
				+ ", description=" + description + ", embedVideoUrl=" + embedVideoUrl + ", embedThumbnailUrl="
				+ embedThumbnailUrl + ", embedMilliseconds=" + embedMilliseconds + "]";
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class NewMedia {
	private final String name;
	private final String photographer;
	private final String inPhoto;
	private final int pitch;
	private final String description;
	private final String embedUrl;
	private final String embedThumbnailUrl;
	
	public NewMedia(String name, String photographer, String inPhoto, int pitch, String description, String embedUrl, String embedThumbnailUrl) {
		this.name = name;
		this.photographer = photographer;
		this.inPhoto = inPhoto;
		this.pitch = pitch;
		this.description = description;
		this.embedUrl = embedUrl;
		this.embedThumbnailUrl = embedThumbnailUrl;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getEmbedThumbnailUrl() {
		return embedThumbnailUrl;
	}
	
	public String getEmbedUrl() {
		return embedUrl;
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

	@Override
	public String toString() {
		return "NewMedia [name=" + name + ", photographer=" + photographer + ", inPhoto=" + inPhoto + ", pitch=" + pitch
				+ ", description=" + description + ", embedUrl=" + embedUrl + ", embedThumbnailUrl=" + embedThumbnailUrl
				+ "]";
	}
}
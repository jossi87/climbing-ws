package com.buldreinfo.jersey.jaxb.model;

public class ActivityMedia {
	private final int id;
	private final int crc32;
	private final boolean movie;
	private final String embedUrl;
	
	public ActivityMedia(int id, int crc32, boolean movie, String embedUrl) {
		this.id = id;
		this.crc32 = crc32;
		this.movie = movie;
		this.embedUrl = embedUrl;
	}
	
	public int getCrc32() {
		return crc32;
	}
	
	public String getEmbedUrl() {
		return embedUrl;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isMovie() {
		return movie;
	}

	@Override
	public String toString() {
		return "Media [id=" + id + ", movie=" + movie + ", embedUrl=" + embedUrl + "]";
	}
}
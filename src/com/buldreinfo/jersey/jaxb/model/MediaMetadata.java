package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public class MediaMetadata {
	private final String dateCreated;
	private final String dateTaken;
	private final String capturer;
	private final String tagged;
	private final String description;
	private final String location;
	private String alt;
	
	public MediaMetadata(String dateCreated, String dateTaken, String capturer, String tagged, String description, String location) {
		this.dateCreated = dateCreated;
		this.dateTaken = dateTaken;
		this.capturer = capturer;
		this.tagged = tagged;
		this.description = description;
		this.location = location;
		this.alt = location;
		if (!Strings.isNullOrEmpty(description)) {
			alt += " (" + description + ")";
		}
		if (!Strings.isNullOrEmpty(capturer)) {
			alt += ", captured by: " + tagged;
		}
		if (!Strings.isNullOrEmpty(tagged)) {
			alt += ", in photo: " + tagged;
		}
	}
	
	public String getAlt() {
		return alt;
	}

	public String getCapturer() {
		return capturer;
	}

	public String getDateCreated() {
		return dateCreated;
	}

	public String getDateTaken() {
		return dateTaken;
	}
	
	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public String getTagged() {
		return tagged;
	}
}
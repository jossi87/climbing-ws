package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public class MediaMetadata {
	private final String dateCreated;
	private final String dateTaken;
	private final String capturer;
	private final String tagged;
	private final String description;
	private final String alt;
	
	public MediaMetadata(String dateCreated, String dateTaken, String capturer, String tagged, String description) {
		this.dateCreated = dateCreated;
		this.dateTaken = dateTaken;
		this.capturer = capturer;
		this.tagged = tagged;
		this.description = description;
		this.alt = "Captured by " + capturer + (!Strings.isNullOrEmpty(tagged)? ", in photo: " + tagged : "") + (!Strings.isNullOrEmpty(description)? " - " + description : "");
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

	public String getTagged() {
		return tagged;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public class MediaMetadata {
	private final String dateCreated;
	private final String dateTaken;
	private final String capturer;
	private final String tagged;
	private final String alt;
	
	public MediaMetadata(String dateCreated, String dateTaken, String capturer, String tagged) {
		this.dateCreated = dateCreated;
		this.dateTaken = dateTaken;
		this.capturer = capturer;
		this.tagged = tagged;
		this.alt = "Captured by " + capturer + (!Strings.isNullOrEmpty(tagged)? ", in photo: " + tagged : "");
	}

	public String getDateCreated() {
		return dateCreated;
	}

	public String getDateTaken() {
		return dateTaken;
	}

	public String getCapturer() {
		return capturer;
	}

	public String getTagged() {
		return tagged;
	}

	public String getAlt() {
		return alt;
	}
}
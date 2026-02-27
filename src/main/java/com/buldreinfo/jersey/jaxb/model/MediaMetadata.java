package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public record MediaMetadata(String dateCreated, String dateTaken, String capturer, String tagged, String description, String location, String alt) {
	public static MediaMetadata from(String dateCreated, String dateTaken, String capturer, String tagged, String description, String location) {
		String alt = location;
		if (!Strings.isNullOrEmpty(description)) {
			alt += " (" + description + ")";
		}
		if (!Strings.isNullOrEmpty(capturer)) {
			alt += ", captured by: " + capturer;
		}
		if (!Strings.isNullOrEmpty(tagged)) {
			alt += ", in photo: " + tagged;
		}
		return new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location, alt);
	}
}
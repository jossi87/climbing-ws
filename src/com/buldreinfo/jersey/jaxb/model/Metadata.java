package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLd;

public class Metadata {
	private final String title;
	private final String description;
	private final JsonLd jsonLd;
	
	public Metadata(String title, String description, JsonLd jsonLd) {
		this.title = title;
		this.description = description;
		this.jsonLd = jsonLd;
	}

	public String getDescription() {
		return description;
	}
	
	public JsonLd getJsonLd() {
		return jsonLd;
	}

	public String getTitle() {
		return title;
	}
}
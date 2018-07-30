package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLd;

public class Metadata {
	private final String title;
	private String description;
	private JsonLd jsonLd;
	private int defaultZoom;
	private LatLng defaultCenter;
	private boolean isBouldering;
	private List<Grade> grades;
	private List<Type> types;
	
	public Metadata(String title) {
		this.title = title;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public JsonLd getJsonLd() {
		return jsonLd;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Type> getTypes() {
		return types;
	}

	public boolean isBouldering() {
		return isBouldering;
	}

	public Metadata setDefaultCenter(LatLng defaultCenter) {
		this.defaultCenter = defaultCenter;
		return this;
	}

	public Metadata setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Metadata setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public Metadata setGrades(List<Grade> grades) {
		this.grades = grades;
		return this;
	}

	public Metadata setIsBouldering(boolean isBouldering) {
		this.isBouldering = isBouldering;
		return this;
	}
	
	public Metadata setJsonLd(JsonLd jsonLd) {
		this.jsonLd = jsonLd;
		return this;
	}

	public Metadata setTypes(List<Type> types) {
		this.types = types;
		return this;
	}
}
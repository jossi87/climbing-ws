package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Media {
	private final int id;
	private final String description;
	private final int idType;
	private final String t;
	private final List<Svg> svgs;
	
	public Media(int id, String description, int idType, String t, List<Svg> svgs) {
		this.id = id;
		this.description = description;
		this.idType = idType;
		this.t = t;
		this.svgs = svgs;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getId() {
		return id;
	}
	
	public int getIdType() {
		return idType;
	}
	
	public String getT() {
		return t;
	}
	
	public List<Svg> getSvgs() {
		return svgs;
	}
}
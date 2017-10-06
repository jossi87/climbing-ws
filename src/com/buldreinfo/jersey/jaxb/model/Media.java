package com.buldreinfo.jersey.jaxb.model;

public class Media {
	private final int id;
	private final String description;
	private final int idType;
	private final String t;
	
	public Media(int id, String description, int idType, String t) {
		this.id = id;
		this.description = description;
		this.idType = idType;
		this.t = t;
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
}
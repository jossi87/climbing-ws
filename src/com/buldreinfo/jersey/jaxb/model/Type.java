package com.buldreinfo.jersey.jaxb.model;

public class Type {
	private final int id;
	private final String type;
	private final String subType;
	
	public Type(int id, String type, String subType) {
		this.id = id;
		this.type = type;
		this.subType = subType;
	}

	public int getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}
}
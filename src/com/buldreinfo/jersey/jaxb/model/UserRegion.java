package com.buldreinfo.jersey.jaxb.model;

public class UserRegion {
	private final int id;
	private final String name;
	private final boolean readOnly;
	
	public UserRegion(int id, String name, boolean readOnly) {
		this.id = id;
		this.name = name;
		this.readOnly = readOnly;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
}
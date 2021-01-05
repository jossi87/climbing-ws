package com.buldreinfo.jersey.jaxb.model;

public class TodoPartner {
	private final int id;
	private final String name;
	
	public TodoPartner(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
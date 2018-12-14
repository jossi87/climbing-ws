package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class TodoUser {
	private final String name;
	private final String picture;
	private final boolean readOnly;
	private final List<Todo> todo;
	
	public TodoUser(String name, String picture, boolean readOnly, List<Todo> todo) {
		this.name = name;
		this.picture = picture;
		this.readOnly = readOnly;
		this.todo = todo;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPicture() {
		return picture;
	}

	public List<Todo> getTodo() {
		return todo;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
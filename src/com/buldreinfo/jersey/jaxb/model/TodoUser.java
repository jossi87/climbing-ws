package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class TodoUser implements IMetadata {
	private final int id;
	private final String name;
	private final String picture;
	private final List<Todo> todo;
	private Metadata metadata;
	
	public TodoUser(int id, String name, String picture, List<Todo> todo) {
		this.id = id;
		this.name = name;
		this.picture = picture;
		this.todo = todo;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
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
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
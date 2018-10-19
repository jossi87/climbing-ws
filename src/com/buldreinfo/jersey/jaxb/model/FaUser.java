package com.buldreinfo.jersey.jaxb.model;

public class FaUser {
	private final int id;
	private final String name;
	private final String picture;
	
	public FaUser(int id, String name, String picture) {
		this.id = id;
		this.name = name;
		this.picture = picture;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPicture() {
		return picture;
	}

	@Override
	public String toString() {
		return "FaUser [id=" + id + ", name=" + name + ", picture=" + picture + "]";
	}
}
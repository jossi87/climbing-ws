package com.buldreinfo.jersey.jaxb.model;

public record User(int id, String name, String picture) {
	public static User from(int id, String name) {
		return new User(id, name, null);
	}
	
	public static User from(int id, String name, String picture) {
		return new User(id, name, picture);
	}
}
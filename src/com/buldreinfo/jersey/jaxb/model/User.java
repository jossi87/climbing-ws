package com.buldreinfo.jersey.jaxb.model;

public record User(int id, String name, long avatarCrc32) {
	public static User from(int id, String name) {
		return new User(id, name, 0);
	}
	
	public static User from(int id, String name, long avatarCrc32) {
		return new User(id, name, avatarCrc32);
	}
}
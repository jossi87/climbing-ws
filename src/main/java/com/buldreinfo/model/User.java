package com.buldreinfo.model;

public record User(int id, String name, MediaIdentity mediaIdentity) {
	public static User from(int id, String name, MediaIdentity mediaIdentity) {
		return new User(id, name, mediaIdentity);
	}

	public static User from(int id, String name) {
		return new User(id, name, null);
	}
}
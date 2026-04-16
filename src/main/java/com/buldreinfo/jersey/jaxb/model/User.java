package com.buldreinfo.jersey.jaxb.model;

public record User(int id, String name, MediaIdentity mediaIdentity) {
	public User withIdAsNameSuffix() {
		return new User(this.id, this.name + " (id=" + this.id + ")", this.mediaIdentity);
	}
	
	public static User from(int id, String name, MediaIdentity mediaIdentity) {
		return new User(id, name, mediaIdentity);
	}

	public static User from(int id, String name) {
		return new User(id, name, null);
	}
}
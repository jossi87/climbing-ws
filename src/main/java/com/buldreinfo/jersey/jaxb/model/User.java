package com.buldreinfo.jersey.jaxb.model;

public record User(int id, String name, int mediaId, long mediaVersionStamp) {
	public User withIdAsNameSuffix() {
		return new User(this.id, this.name + " (id=" + this.id + ")", this.mediaId, this.mediaVersionStamp);
	}
	
	public static User from(int id, String name, int mediaId, long mediaVersionStamp) {
		return new User(id, name, mediaId, mediaVersionStamp);
	}

	public static User from(int id, String name) {
		return new User(id, name, 0, 0l);
	}
}
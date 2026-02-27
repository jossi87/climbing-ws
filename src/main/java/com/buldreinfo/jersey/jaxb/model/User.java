package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public record User(int id, String name, int mediaId, long mediaVersionStamp) {
	public static User from(int id, String firstname, String lastname) {
		String name = Strings.isNullOrEmpty(lastname) ? firstname : firstname + " " + lastname;
		return new User(id, name, 0, 0l);
	}
	
	public User withIdAsNameSuffix() {
		return new User(this.id, this.name + " (id=" + this.id + ")", this.mediaId, this.mediaVersionStamp);
	}
	
	public static User from(int id, String name, int mediaId, long mediaVersionStamp) {
		return new User(id, name, mediaId, mediaVersionStamp);
	}
}
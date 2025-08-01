package com.buldreinfo.jersey.jaxb.model;

import com.google.common.base.Strings;

public record User(int id, String name, long avatarCrc32) {
	public static User from(int id, String firstname, String lastname) {
		String name = Strings.isNullOrEmpty(lastname) ? firstname : firstname + " " + lastname;
		return new User(id, name, 0);
	}
	
	public User withIdAsNameSuffix() {
		return new User(this.id, this.name + " (id=" + this.id + ")", this.avatarCrc32);
	}
	
	public static User from(int id, String name, long avatarCrc32) {
		return new User(id, name, avatarCrc32);
	}
}
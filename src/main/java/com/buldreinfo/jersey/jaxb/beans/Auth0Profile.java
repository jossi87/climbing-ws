package com.buldreinfo.jersey.jaxb.beans;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public record Auth0Profile(String email, String firstname, String lastname, String fullname, String picture) {
	public static Auth0Profile from(Map<String, Object> values) {
		String email = Preconditions.checkNotNull((String)values.get("email"));
		// Firstname
		String firstname = (String) values.get("given_name");
		if (firstname == null) {
			firstname = (String) values.get("https://buldreinfo.com/firstname");
		}
		if (firstname == null) {
			firstname = (String) values.get("name");
		}
		if (firstname == null) {
			firstname = email;
		}
		Preconditions.checkNotNull(firstname);
		// Lastname
		String lastname = (String) values.get("family_name");
		if (lastname == null) {
			lastname = (String) values.get("https://buldreinfo.com/lastname");
		}
		// Fullname
		String fullname = Strings.emptyToNull((Strings.nullToEmpty(firstname) + " " + Strings.nullToEmpty(lastname)).trim().toLowerCase());
		// Picture
		String picture = (String) values.get("picture");
		return new Auth0Profile(email, firstname, lastname, fullname, picture);
	}
}
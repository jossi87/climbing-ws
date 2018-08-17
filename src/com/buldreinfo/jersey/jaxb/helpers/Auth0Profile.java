package com.buldreinfo.jersey.jaxb.helpers;

import java.util.Map;

import jersey.repackaged.com.google.common.base.Preconditions;

public class Auth0Profile {
	private final String email;
	private final String firstname;
	private final String lastname;
	
	public Auth0Profile(Map<String, Object> values) {
		this.email = Preconditions.checkNotNull((String)values.get("email"));
		String firstname = (String) values.get("given_name");
		String lastname = (String) values.get("family_name");
		String name = (String) values.get("name");
		if (name == null) {
			name = email;
		}
		this.firstname = firstname != null? firstname : name;
		this.lastname = lastname != null? lastname : name;
	}

	public String getEmail() {
		return email;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	@Override
	public String toString() {
		return "Auth0Profile [email=" + email + ", firstname=" + firstname + ", lastname=" + lastname + "]";
	}
}
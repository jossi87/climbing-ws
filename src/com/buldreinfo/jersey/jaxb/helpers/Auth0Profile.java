package com.buldreinfo.jersey.jaxb.helpers;

import java.util.Map;

import jersey.repackaged.com.google.common.base.Preconditions;

public class Auth0Profile {
	private final String email;
	private final String username;
	private final String firstname;
	private final String lastname;
	
	public Auth0Profile(Map<String, Object> values) {
		this.email = Preconditions.checkNotNull((String)values.get("email"));
		String username = (String) values.get("nickname");
		String firstname = (String) values.get("given_name");
		String lastname = (String) values.get("family_name");
		String name = (String) values.get("name");
		if (name == null) {
			name = email;
		}
		this.username = username != null? username : email;
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

	public String getUsername() {
		return username;
	}
}
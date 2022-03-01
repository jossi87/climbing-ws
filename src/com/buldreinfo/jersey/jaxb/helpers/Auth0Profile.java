package com.buldreinfo.jersey.jaxb.helpers;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Auth0Profile {
	private final String email;
	private final String firstname;
	private final String lastname;
	private final String picture;
	
	public Auth0Profile(Map<String, Object> values) {
		this.email = Preconditions.checkNotNull((String)values.get("email"));
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
		this.firstname = Preconditions.checkNotNull(firstname);
		// Lastname
		String lastname = (String) values.get("family_name");
		if (lastname == null) {
			lastname = (String) values.get("https://buldreinfo.com/lastname");
		}
		this.lastname = lastname;
		// Picture
		this.picture = (String) values.get("picture");
	}

	public String getEmail() {
		return email.toLowerCase();
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}
	
	public String getName() {
		return Strings.emptyToNull((Strings.nullToEmpty(firstname) + " " + Strings.nullToEmpty(lastname)).trim().toLowerCase());
	}

	public String getPicture() {
		return picture;
	}

	@Override
	public String toString() {
		return "Auth0Profile [email=" + email + ", firstname=" + firstname + ", lastname=" + lastname + ", picture="
				+ picture + "]";
	}
}
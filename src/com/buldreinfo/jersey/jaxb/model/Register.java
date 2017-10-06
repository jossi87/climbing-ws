package com.buldreinfo.jersey.jaxb.model;

public class Register {
	private final String firstname;
	private final String lastname;
	private final String username;
	private final String password;

	public Register(String firstname, String lastname, String username, String password) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.username = username;
		this.password = password;
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

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "Register [firstname=" + firstname + ", lastname=" + lastname + ", username=" + username + ", password="
				+ password + "]";
	}
}
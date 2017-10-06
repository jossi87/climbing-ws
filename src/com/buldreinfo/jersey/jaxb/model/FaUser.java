package com.buldreinfo.jersey.jaxb.model;

public class FaUser {
	private final int id;
	private final String firstname;
	private final String surname;
	private final String initials;
	
	public FaUser(int id, String firstname, String surname, String initials) {
		this.id = id;
		this.firstname = firstname;
		this.surname = surname;
		this.initials = initials;
	}

	public int getId() {
		return id;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getSurname() {
		return surname;
	}

	public String getInitials() {
		return initials;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", firstname=" + firstname + ", surname=" + surname + ", initials=" + initials + "]";
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class FaUser {
	private final int id;
	private final String picture;
	private final String firstname;
	private final String surname;
	private final String initials;
	
	public FaUser(int id, String picture, String firstname, String surname, String initials) {
		this.id = id;
		this.picture = picture;
		this.firstname = firstname;
		this.surname = surname;
		this.initials = initials;
	}

	public String getFirstname() {
		return firstname;
	}

	public int getId() {
		return id;
	}

	public String getInitials() {
		return initials;
	}

	public String getPicture() {
		return picture;
	}
	
	public String getSurname() {
		return surname;
	}

	@Override
	public String toString() {
		return "FaUser [id=" + id + ", picture=" + picture + ", firstname=" + firstname + ", surname=" + surname
				+ ", initials=" + initials + "]";
	}
}
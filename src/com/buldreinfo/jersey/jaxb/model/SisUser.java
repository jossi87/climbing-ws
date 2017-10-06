package com.buldreinfo.jersey.jaxb.model;

public class SisUser {
	private final int id;
	private final String name;
	private final String email;
	private final boolean isAdmin;
	
	public SisUser(int id, String name, String email, boolean isAdmin) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.isAdmin = isAdmin;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public boolean isAdmin() {
		return isAdmin;
	}
}
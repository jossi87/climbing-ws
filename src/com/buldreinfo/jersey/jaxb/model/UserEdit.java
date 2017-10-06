package com.buldreinfo.jersey.jaxb.model;

public class UserEdit {
	private final int regionId;
	private final int id;
	private final String username;
	private final String firstname;
	private final String lastname;
	private final String currentPassword;
	private final String newPassword;
	
	public UserEdit(int regionId, int id, String username, String firstname, String lastname, String currentPassword, String newPassword) {
		this.regionId = regionId;
		this.id = id;
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
		this.currentPassword = currentPassword;
		this.newPassword = newPassword;
	}

	public int getId() {
		return id;
	}
	
	public int getRegionId() {
		return regionId;
	}

	public String getUsername() {
		return username;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public String getCurrentPassword() {
		return currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class ManagementUser {
	private final int userId;
	private final String name;
	private final String picture;
	private final String lastLogin;
	private final int write;
	
	public ManagementUser(int userId, String name, String picture, String lastLogin, int write) {
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.lastLogin = lastLogin;
		this.write = write;
	}

	public int getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public String getPicture() {
		return picture;
	}

	public String getLastLogin() {
		return lastLogin;
	}

	public int getWrite() {
		return write;
	}
}
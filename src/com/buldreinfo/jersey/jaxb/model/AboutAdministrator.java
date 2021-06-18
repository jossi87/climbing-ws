package com.buldreinfo.jersey.jaxb.model;

public class AboutAdministrator {
	private final int userId;
	private final String name;
	private final String picture;
	private final String lastLogin;
	
	public AboutAdministrator(int userId, String name, String picture, String lastLogin) {
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.lastLogin = lastLogin;
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
}
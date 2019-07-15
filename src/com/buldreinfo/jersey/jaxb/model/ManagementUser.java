package com.buldreinfo.jersey.jaxb.model;

public class ManagementUser {
	private final int userId;
	private final String name;
	private final String picture;
	private final String lastLogin;
	private final int write;
	private final boolean readOnly;
	
	public ManagementUser(int userId, String name, String picture, String lastLogin, int write, boolean readOnly) {
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.lastLogin = lastLogin;
		this.write = write;
		this.readOnly = readOnly;
	}
	
	public String getLastLogin() {
		return lastLogin;
	}

	public String getName() {
		return name;
	}

	public String getPicture() {
		return picture;
	}

	public int getUserId() {
		return userId;
	}

	public int getWrite() {
		return write;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
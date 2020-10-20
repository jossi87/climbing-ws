package com.buldreinfo.jersey.jaxb.model;

public class PermissionUser {
	private final int userId;
	private final String name;
	private final String picture;
	private final String lastLogin;
	private final boolean adminRead;
	private final boolean adminWrite;
	private final boolean superadminRead;
	private final boolean superadminWrite;
	private final boolean readOnly;
	
	public PermissionUser(int userId, String name, String picture, String lastLogin, boolean adminRead, boolean adminWrite, boolean superadminRead, boolean superadminWrite, boolean readOnly) {
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.lastLogin = lastLogin;
		this.adminRead = adminRead;
		this.adminWrite = adminWrite;
		this.superadminRead = superadminRead;
		this.superadminWrite = superadminWrite;
		this.readOnly = readOnly;
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

	public boolean isAdminRead() {
		return adminRead;
	}

	public boolean isAdminWrite() {
		return adminWrite;
	}

	public boolean isSuperadminRead() {
		return superadminRead;
	}

	public boolean isSuperadminWrite() {
		return superadminWrite;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
}
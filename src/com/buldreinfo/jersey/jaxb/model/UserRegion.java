package com.buldreinfo.jersey.jaxb.model;

public class UserRegion {
	private final int id;
	private final String name;
	private final String role;
	private final boolean enabled;
	private final boolean readOnly;
	
	public UserRegion(int id, String name, String role, boolean enabled, boolean readOnly) {
		this.id = id;
		this.name = name;
		this.role = role;
		this.enabled = enabled;
		this.readOnly = readOnly;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public String getRole() {
		return role;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class UserRegion {
	private final int id;
	private final String name;
	private final boolean enabled;
	private final boolean readOnly;
	
	public UserRegion(int id, String name, boolean enabled, boolean readOnly) {
		this.id = id;
		this.name = name;
		this.enabled = enabled;
		this.readOnly = readOnly;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
}
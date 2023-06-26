package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Profile {
	private final int id;
	private final String picture;
	private final String firstname;
	private final String lastname;
	private final List<UserRegion> userRegions;

	public Profile(int id, String picture, String firstname, String lastname, List<UserRegion> userRegions) {
		this.id = id;
		this.picture = picture;
		this.firstname = firstname;
		this.lastname = lastname;
		this.userRegions = userRegions;
	}
	
	public String getFirstname() {
		return firstname;
	}

	public int getId() {
		return id;
	}
	
	public String getLastname() {
		return lastname;
	}

	public String getPicture() {
		return picture;
	}

	public List<UserRegion> getUserRegions() {
		return userRegions;
	}
}
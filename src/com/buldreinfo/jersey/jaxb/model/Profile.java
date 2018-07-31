package com.buldreinfo.jersey.jaxb.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
	@SerializedName("user_id")
	private final int userId;
	
	public Profile(int userId) {
		this.userId = userId;
	}
	
	public int getUserId() {
		return userId;
	}
}
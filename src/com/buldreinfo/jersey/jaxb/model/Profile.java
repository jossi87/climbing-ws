package com.buldreinfo.jersey.jaxb.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
	@SerializedName("user_id")
	private final int userId;
	private final String email;
	private final String nickname;
	
	public Profile(int userId, String email, String nickname) {
		this.userId = userId;
		this.email = email;
		this.nickname = nickname;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getNickname() {
		return nickname;
	}
}
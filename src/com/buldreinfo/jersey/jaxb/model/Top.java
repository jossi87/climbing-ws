package com.buldreinfo.jersey.jaxb.model;

public class Top {
	private final int userId;
	private final String name;
	private final String picture;
	private final int percentage;
	
	public Top(int userId, String name, String picture, int percentage) {
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.percentage = percentage;
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

	public int getPercentage() {
		return percentage;
	}
}
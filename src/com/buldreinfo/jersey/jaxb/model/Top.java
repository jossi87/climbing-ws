package com.buldreinfo.jersey.jaxb.model;

public class Top {
	private final int rank;
	private final int userId;
	private final String name;
	private final String picture;
	private final double percentage;
	
	public Top(int rank, int userId, String name, String picture, double percentage) {
		this.rank = rank;
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.percentage = percentage;
	}
	
	public int getRank() {
		return rank;
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

	public double getPercentage() {
		return percentage;
	}
}
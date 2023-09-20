package com.buldreinfo.jersey.jaxb.model;

public class Top {
	private final int rank;
	private final int userId;
	private final String name;
	private final String picture;
	private final double percentage;
	private final boolean mine;
	
	public Top(int rank, int userId, String name, String picture, double percentage, boolean mine) {
		this.rank = rank;
		this.userId = userId;
		this.name = name;
		this.picture = picture;
		this.percentage = percentage;
		this.mine = mine;
	}
	
	public String getName() {
		return name;
	}

	public double getPercentage() {
		return percentage;
	}

	public String getPicture() {
		return picture;
	}

	public int getRank() {
		return rank;
	}

	public int getUserId() {
		return userId;
	}
	
	public boolean isMine() {
		return mine;
	}
}
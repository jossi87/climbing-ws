package com.buldreinfo.jersey.jaxb.model;

public class SisTick {
	private final int problemId;
	private final int userId;
	private final String name;
	private final int stars;
	
	public SisTick(int problemId, int userId, String name, int stars) {
		this.problemId = problemId;
		this.userId = userId;
		this.name = name;
		this.stars = stars;
	}
	
	public int getProblemId() {
		return problemId;
	}

	public int getUserId() {
		return userId;
	}
	
	public String getName() {
		return name;
	}
	
	public int getStars() {
		return stars;
	}
}
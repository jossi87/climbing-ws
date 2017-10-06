package com.buldreinfo.jersey.jaxb.model;

public class SisTick {
	private final int problemId;
	private final int userId;
	private final String name;
	
	public SisTick(int problemId, int userId, String name) {
		this.problemId = problemId;
		this.userId = userId;
		this.name = name;
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
}
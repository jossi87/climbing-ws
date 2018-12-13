package com.buldreinfo.jersey.jaxb.model;

public class Todo {
	private final int id;
	private final int userId;
	private final int problemId;
	private final int priority;
	private final boolean delete;
	
	public Todo(int id, int userId, int problemId, int priority, boolean delete) {
		this.id = id;
		this.userId = userId;
		this.problemId = problemId;
		this.priority = priority;
		this.delete = delete;
	}

	public int getId() {
		return id;
	}

	public int getUserId() {
		return userId;
	}

	public int getProblemId() {
		return problemId;
	}

	public int getPriority() {
		return priority;
	}

	public boolean isDelete() {
		return delete;
	}
}
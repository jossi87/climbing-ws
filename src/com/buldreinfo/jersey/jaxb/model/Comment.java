package com.buldreinfo.jersey.jaxb.model;

public class Comment {
	private final int id;
	private final int idProblem;
	private final String comment;
	private final boolean danger;
	private final boolean resolved;
	private final boolean delete;
	
	public Comment(int id, int idProblem, String comment, boolean danger, boolean resolved, boolean delete) {
		this.id = id;
		this.idProblem = idProblem;
		this.comment = comment;
		this.danger = danger;
		this.resolved = resolved;
		this.delete = delete;
	}
	
	public boolean isDelete() {
		return delete;
	}
	
	public int getId() {
		return id;
	}
	
	public String getComment() {
		return comment;
	}
	
	public int getIdProblem() {
		return idProblem;
	}

	public boolean isDanger() {
		return danger;
	}

	public boolean isResolved() {
		return resolved;
	}
}
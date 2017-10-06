package com.buldreinfo.jersey.jaxb.model;

public class Comment {
	private final int idProblem;
	private final String comment;
	
	public Comment(int idProblem, String comment) {
		this.idProblem = idProblem;
		this.comment = comment;
	}

	public int getIdProblem() {
		return idProblem;
	}

	public String getComment() {
		return comment;
	}
}
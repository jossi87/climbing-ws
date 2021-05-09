package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Comment {
	private final int id;
	private final int idProblem;
	private final String comment;
	private final boolean danger;
	private final boolean resolved;
	private final boolean delete;
	private final List<NewMedia> newMedia;
	
	public Comment(int id, int idProblem, String comment, boolean danger, boolean resolved, boolean delete, List<NewMedia> newMedia) {
		this.id = id;
		this.idProblem = idProblem;
		this.comment = comment;
		this.danger = danger;
		this.resolved = resolved;
		this.delete = delete;
		this.newMedia = newMedia;
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
	
	public List<NewMedia> getNewMedia() {
		return newMedia;
	}
}
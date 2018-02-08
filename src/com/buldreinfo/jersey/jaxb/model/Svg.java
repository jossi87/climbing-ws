package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final int id;
	private final int problemId;
	private final int nr;
	private final String path;
	private final boolean hasAnchor;
	
	public Svg(int id, int problemId, int nr, String path, boolean hasAnchor) {
		this.id = id;
		this.problemId = problemId;
		this.nr = nr;
		this.path = path;
		this.hasAnchor = hasAnchor;
	}

	public int getId() {
		return id;
	}

	public int getNr() {
		return nr;
	}

	public String getPath() {
		return path;
	}
	
	public int getProblemId() {
		return problemId;
	}

	public boolean isHasAnchor() {
		return hasAnchor;
	}
}
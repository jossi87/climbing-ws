package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final boolean delete;
	private final int id;
	private final int problemId;
	private final int nr;
	private final String path;
	private final boolean hasAnchor;
	
	public Svg(boolean delete, int id, int problemId, int nr, String path, boolean hasAnchor) {
		this.delete = delete;
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
	
	public boolean isDelete() {
		return delete;
	}

	public boolean isHasAnchor() {
		return hasAnchor;
	}
}
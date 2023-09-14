package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final boolean delete;
	private final int id;
	private final int problemId;
	private final String problemName;
	private final String problemGrade;
	private final int problemGradeGroup;
	private final String problemSubtype;
	private final int nr;
	private final String path;
	private final boolean hasAnchor;
	private final String texts;
	private final String anchors;
	private final boolean primary;
	private final boolean ticked;
	private final boolean todo;
	private final boolean dangerous;
	
	public Svg(boolean delete, int id, int problemId, String problemName, String problemGrade, int problemGradeGroup, String problemSubtype,
			int nr, String path, boolean hasAnchor, String texts, String anchors, boolean primary, boolean ticked,
			boolean todo, boolean dangerous) {
		this.delete = delete;
		this.id = id;
		this.problemId = problemId;
		this.problemName = problemName;
		this.problemGrade = problemGrade;
		this.problemGradeGroup = problemGradeGroup;
		this.problemSubtype = problemSubtype;
		this.nr = nr;
		this.path = path;
		this.hasAnchor = hasAnchor;
		this.texts = texts;
		this.anchors = anchors;
		this.primary = primary;
		this.ticked = ticked;
		this.todo = todo;
		this.dangerous = dangerous;
	}

	public String getAnchors() {
		return anchors;
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

	public String getProblemGrade() {
		return problemGrade;
	}

	public int getProblemGradeGroup() {
		return problemGradeGroup;
	}
	
	public int getProblemId() {
		return problemId;
	}

	public String getProblemName() {
		return problemName;
	}

	public String getProblemSubtype() {
		return problemSubtype;
	}

	public String getTexts() {
		return texts;
	}

	public boolean isDangerous() {
		return dangerous;
	}

	public boolean isDelete() {
		return delete;
	}

	public boolean isHasAnchor() {
		return hasAnchor;
	}

	public boolean isPrimary() {
		return primary;
	}

	public boolean isTicked() {
		return ticked;
	}

	public boolean isTodo() {
		return todo;
	}
}
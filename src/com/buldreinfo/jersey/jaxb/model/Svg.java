package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final boolean delete;
	private final int id;
	private final int problemId;
	private final String problemName;
	private final String problemGrade;
	private final int problemGradeGroup;
	private final int nr;
	private final String path;
	private final boolean hasAnchor;
	private final String texts;
	private final String anchors;
	private final boolean primary;
	private final boolean isTicked;
	private final boolean isTodo;
	private final boolean isDangerous;
	
	public Svg(boolean delete, int id, int problemId, String problemName, String problemGrade, int problemGradeGroup,
			int nr, String path, boolean hasAnchor, String texts, String anchors, boolean primary, boolean isTicked,
			boolean isTodo, boolean isDangerous) {
		this.delete = delete;
		this.id = id;
		this.problemId = problemId;
		this.problemName = problemName;
		this.problemGrade = problemGrade;
		this.problemGradeGroup = problemGradeGroup;
		this.nr = nr;
		this.path = path;
		this.hasAnchor = hasAnchor;
		this.texts = texts;
		this.anchors = anchors;
		this.primary = primary;
		this.isTicked = isTicked;
		this.isTodo = isTodo;
		this.isDangerous = isDangerous;
	}

	public boolean isDelete() {
		return delete;
	}

	public int getId() {
		return id;
	}

	public int getProblemId() {
		return problemId;
	}

	public String getProblemName() {
		return problemName;
	}

	public String getProblemGrade() {
		return problemGrade;
	}
	
	public int getProblemGradeGroup() {
		return problemGradeGroup;
	}

	public int getNr() {
		return nr;
	}

	public String getPath() {
		return path;
	}

	public boolean isHasAnchor() {
		return hasAnchor;
	}

	public String getTexts() {
		return texts;
	}

	public String getAnchors() {
		return anchors;
	}

	public boolean isPrimary() {
		return primary;
	}

	public boolean isTicked() {
		return isTicked;
	}

	public boolean isTodo() {
		return isTodo;
	}

	public boolean isDangerous() {
		return isDangerous;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class Todo {
	private final int id;
	private final int priority;
	private final String areaName;
	private final String sectorName;
	private final int problemId;
	private final String problemName;
	private final String problemGrade;
	private final int problemVisibility;
	private final boolean isDelete = false;
	
	public Todo(int id, int priority, String areaName, String sectorName, int problemId, String problemName, String problemGrade, int problemVisibility) {
		this.id = id;
		this.priority = priority;
		this.areaName = areaName;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.problemName = problemName;
		this.problemGrade = problemGrade;
		this.problemVisibility = problemVisibility;
	}

	public int getId() {
		return id;
	}

	public int getPriority() {
		return priority;
	}

	public String getAreaName() {
		return areaName;
	}

	public String getSectorName() {
		return sectorName;
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

	public int getProblemVisibility() {
		return problemVisibility;
	}

	public boolean isDelete() {
		return isDelete;
	}
}
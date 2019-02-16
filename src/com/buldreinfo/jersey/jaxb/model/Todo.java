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
	private final double problemLat;
	private final double problemLng;
	private final int randomMediaId;
	private boolean isDelete;

	public Todo(int id, int priority, String areaName, String sectorName, int problemId, String problemName,
			String problemGrade, int problemVisibility, double problemLat, double problemLng, int randomMediaId) {
		this.id = id;
		this.priority = priority;
		this.areaName = areaName;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.problemName = problemName;
		this.problemGrade = problemGrade;
		this.problemVisibility = problemVisibility;
		this.problemLat = problemLat;
		this.problemLng = problemLng;
		this.randomMediaId = randomMediaId;
	}
	
	public String getAreaName() {
		return areaName;
	}
	
	public int getId() {
		return id;
	}
	
	public int getPriority() {
		return priority;
	}

	public String getProblemGrade() {
		return problemGrade;
	}
	
	public int getProblemId() {
		return problemId;
	}

	public double getProblemLat() {
		return problemLat;
	}

	public double getProblemLng() {
		return problemLng;
	}

	public String getProblemName() {
		return problemName;
	}

	public int getProblemVisibility() {
		return problemVisibility;
	}

	public int getRandomMediaId() {
		return randomMediaId;
	}

	public String getSectorName() {
		return sectorName;
	}

	public boolean isDelete() {
		return isDelete;
	}

	public void setDelete(boolean isDelete) {
		this.isDelete = isDelete;
	}
}
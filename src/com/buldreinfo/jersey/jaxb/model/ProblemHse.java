package com.buldreinfo.jersey.jaxb.model;

public class ProblemHse {
	private final int areaId;
	private final int areaVisibility;
	private final String areaName;
	private final int sectorId;
	private final int sectorVisibility;
	private final String sectorName;
	private final int problemId;
	private final int problemVisibility;
	private final String problemName;
	private final String comment;
	
	public ProblemHse(int areaId, int areaVisibility, String areaName, int sectorId, int sectorVisibility, String sectorName, int problemId, int problemVisibility, String problemName, String comment) {
		this.areaId = areaId;
		this.areaVisibility = areaVisibility;
		this.areaName = areaName;
		this.sectorId = sectorId;
		this.sectorVisibility = sectorVisibility;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.problemVisibility = problemVisibility;
		this.problemName = problemName;
		this.comment = comment;
	}

	public int getAreaId() {
		return areaId;
	}

	public int getAreaVisibility() {
		return areaVisibility;
	}

	public String getAreaName() {
		return areaName;
	}

	public int getSectorId() {
		return sectorId;
	}

	public int getSectorVisibility() {
		return sectorVisibility;
	}

	public String getSectorName() {
		return sectorName;
	}

	public int getProblemId() {
		return problemId;
	}

	public int getProblemVisibility() {
		return problemVisibility;
	}

	public String getProblemName() {
		return problemName;
	}

	public String getComment() {
		return comment;
	}
}
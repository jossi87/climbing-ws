package com.buldreinfo.jersey.jaxb.model;

public class PublicAscent {
	private final String areaName;
	private final int areaVisibility;
	private final String sectorName;
	private final int sectorVisibility;
	private final int problemId;
	private final String problemGrade;
	private final String problemName;
	private final int problemVisibility;
	private final String date;
	private final String name;
	
	public PublicAscent(String areaName, int areaVisibility, String sectorName, int sectorVisibility, int problemId, String problemGrade, String problemName, int problemVisibility, String date, String name) {
		this.areaName = areaName;
		this.areaVisibility = areaVisibility;
		this.sectorName = sectorName;
		this.sectorVisibility = sectorVisibility;
		this.problemId = problemId;
		this.problemGrade = problemGrade;
		this.problemName = problemName;
		this.problemVisibility = problemVisibility;
		this.date = date;
		this.name = name;
	}
	public String getAreaName() {
		return areaName;
	}
	public int getAreaVisibility() {
		return areaVisibility;
	}
	public String getSectorName() {
		return sectorName;
	}
	public int getSectorVisibility() {
		return sectorVisibility;
	}
	public int getProblemId() {
		return problemId;
	}
	public String getProblemGrade() {
		return problemGrade;
	}
	public String getProblemName() {
		return problemName;
	}
	public int getProblemVisibility() {
		return problemVisibility;
	}
	public String getDate() {
		return date;
	}
	public String getName() {
		return name;
	}
}
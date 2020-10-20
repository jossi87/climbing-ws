package com.buldreinfo.jersey.jaxb.model;

public class PublicAscent {
	private final String areaName;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final String sectorName;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private final int problemId;
	private final String problemGrade;
	private final String problemName;
	private final boolean problemLockedAdmin;
	private final boolean problemLockedSuperadmin;
	private final String date;
	private final String name;
	
	public PublicAscent(String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String sectorName,
			boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, int problemId, String problemGrade,
			String problemName, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String date, String name) {
		this.areaName = areaName;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.sectorName = sectorName;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.problemId = problemId;
		this.problemGrade = problemGrade;
		this.problemName = problemName;
		this.problemLockedAdmin = problemLockedAdmin;
		this.problemLockedSuperadmin = problemLockedSuperadmin;
		this.date = date;
		this.name = name;
	}

	public String getAreaName() {
		return areaName;
	}

	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}

	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}

	public String getSectorName() {
		return sectorName;
	}

	public boolean isSectorLockedAdmin() {
		return sectorLockedAdmin;
	}

	public boolean isSectorLockedSuperadmin() {
		return sectorLockedSuperadmin;
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

	public boolean isProblemLockedAdmin() {
		return problemLockedAdmin;
	}

	public boolean isProblemLockedSuperadmin() {
		return problemLockedSuperadmin;
	}

	public String getDate() {
		return date;
	}

	public String getName() {
		return name;
	}
}
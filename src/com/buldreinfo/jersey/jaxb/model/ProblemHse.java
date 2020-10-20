package com.buldreinfo.jersey.jaxb.model;

public class ProblemHse {
	private final int areaId;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final String areaName;
	private final int sectorId;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private final String sectorName;
	private final int problemId;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final String problemName;
	private final String comment;
	
	public ProblemHse(int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaName, int sectorId,
			boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, String sectorName, int problemId,
			boolean lockedAdmin, boolean lockedSuperadmin, String problemName, String comment) {
		this.areaId = areaId;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.areaName = areaName;
		this.sectorId = sectorId;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.problemName = problemName;
		this.comment = comment;
	}

	public int getAreaId() {
		return areaId;
	}

	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}

	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}

	public String getAreaName() {
		return areaName;
	}

	public int getSectorId() {
		return sectorId;
	}

	public boolean isSectorLockedAdmin() {
		return sectorLockedAdmin;
	}

	public boolean isSectorLockedSuperadmin() {
		return sectorLockedSuperadmin;
	}

	public String getSectorName() {
		return sectorName;
	}

	public int getProblemId() {
		return problemId;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}

	public String getProblemName() {
		return problemName;
	}

	public String getComment() {
		return comment;
	}
}
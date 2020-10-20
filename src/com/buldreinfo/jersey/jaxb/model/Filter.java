package com.buldreinfo.jersey.jaxb.model;

public class Filter {
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final String areaName;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private final String sectorName;
	private final int problemId;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final String problemName;
	private final double latitude;
	private final double longitude;
	private final double stars;
	private final String grade;
	private final boolean ticked;
	private final int randomMediaId;
	
	public Filter(boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaName, boolean sectorLockedAdmin,
			boolean sectorLockedSuperadmin, String sectorName, int problemId, boolean lockedAdmin,
			boolean lockedSuperadmin, String problemName, double latitude, double longitude, double stars, String grade,
			boolean ticked, int randomMediaId) {
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.areaName = areaName;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.problemName = problemName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.stars = stars;
		this.grade = grade;
		this.ticked = ticked;
		this.randomMediaId = randomMediaId;
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

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getStars() {
		return stars;
	}

	public String getGrade() {
		return grade;
	}

	public boolean isTicked() {
		return ticked;
	}

	public int getRandomMediaId() {
		return randomMediaId;
	}
}
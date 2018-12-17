package com.buldreinfo.jersey.jaxb.model;

public class Filter {
	private final int areaVisibility;
	private final String areaName;
	private final int sectorVisibility;
	private final String sectorName;
	private final int problemId;
	private final int problemVisibility;
	private final String problemName;
	private final double latitude;
	private final double longitude;
	private final double stars;
	private final String grade;
	private final boolean ticked;
	private final int randomMediaId;
	
	public Filter(int areaVisibility, String areaName, int sectorVisibility, String sectorName, int problemId,
			int problemVisibility, String problemName, double latitude, double longitude, double stars, String grade,
			boolean ticked, int randomMediaId) {
		this.areaVisibility = areaVisibility;
		this.areaName = areaName;
		this.sectorVisibility = sectorVisibility;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.problemVisibility = problemVisibility;
		this.problemName = problemName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.stars = stars;
		this.grade = grade;
		this.ticked = ticked;
		this.randomMediaId = randomMediaId;
	}

	public int getAreaVisibility() {
		return areaVisibility;
	}

	public String getAreaName() {
		return areaName;
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

	@Override
	public String toString() {
		return "Filter [areaVisibility=" + areaVisibility + ", areaName=" + areaName + ", sectorVisibility="
				+ sectorVisibility + ", sectorName=" + sectorName + ", problemId=" + problemId + ", problemVisibility="
				+ problemVisibility + ", problemName=" + problemName + ", latitude=" + latitude + ", longitude="
				+ longitude + ", stars=" + stars + ", grade=" + grade + ", ticked=" + ticked + ", randomMediaId="
				+ randomMediaId + "]";
	}
}
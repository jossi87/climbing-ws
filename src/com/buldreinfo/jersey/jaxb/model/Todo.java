package com.buldreinfo.jersey.jaxb.model;

public class Todo {
	private final int id;
	private final int priority;
	private final String areaName;
	private final String sectorName;
	private final int problemId;
	private final String problemName;
	private final String problemGrade;
	private final boolean problemLockedAdmin;
	private final boolean problemLockedSuperadmin;
	private final double lat;
	private final double lng;
	private final int randomMediaId;
	private boolean isDelete;
	
	public Todo(int id, int priority, String areaName, String sectorName, int problemId, String problemName,
			String problemGrade, boolean problemLockedAdmin, boolean problemLockedSuperadmin, double lat, double lng,
			int randomMediaId) {
		this.id = id;
		this.priority = priority;
		this.areaName = areaName;
		this.sectorName = sectorName;
		this.problemId = problemId;
		this.problemName = problemName;
		this.problemGrade = problemGrade;
		this.problemLockedAdmin = problemLockedAdmin;
		this.problemLockedSuperadmin = problemLockedSuperadmin;
		this.lat = lat;
		this.lng = lng;
		this.randomMediaId = randomMediaId;
	}

	public boolean isDelete() {
		return isDelete;
	}

	public void setDelete(boolean isDelete) {
		this.isDelete = isDelete;
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

	public boolean isProblemLockedAdmin() {
		return problemLockedAdmin;
	}

	public boolean isProblemLockedSuperadmin() {
		return problemLockedSuperadmin;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public int getRandomMediaId() {
		return randomMediaId;
	}
}
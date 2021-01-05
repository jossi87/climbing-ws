package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Todo {
	private final int id;
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
	private List<TodoPartner> partners = new ArrayList<>();
	
	public Todo(int id, String areaName, String sectorName, int problemId, String problemName, String problemGrade, boolean problemLockedAdmin, boolean problemLockedSuperadmin, double lat, double lng, int randomMediaId) {
		this.id = id;
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

	public String getAreaName() {
		return areaName;
	}

	public int getId() {
		return id;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}
	
	public List<TodoPartner> getPartners() {
		return partners;
	}

	public String getProblemGrade() {
		return problemGrade;
	}

	public int getProblemId() {
		return problemId;
	}

	public String getProblemName() {
		return problemName;
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

	public boolean isProblemLockedAdmin() {
		return problemLockedAdmin;
	}

	public boolean isProblemLockedSuperadmin() {
		return problemLockedSuperadmin;
	}

	public void setDelete(boolean isDelete) {
		this.isDelete = isDelete;
	}
}
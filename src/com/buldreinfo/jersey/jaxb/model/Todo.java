package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Todo {
	private final int id;
	private final String areaName;
	private final String areaUrl;
	private final String sectorName;
	private final String sectorUrl;
	private final int problemId;
	private final int problemNr;
	private final String problemName;
	private final String problemUrl;
	private final String problemGrade;
	private final boolean problemLockedAdmin;
	private final boolean problemLockedSuperadmin;
	private final double lat;
	private final double lng;
	private boolean isDelete;
	private List<TodoPartner> partners = new ArrayList<>();
	
	public Todo(int id, String areaName, String areaUrl, String sectorName, String sectorUrl, int problemId, int problemNr, String problemName,
			String problemUrl, String problemGrade, boolean problemLockedAdmin, boolean problemLockedSuperadmin,
			double lat, double lng) {
		this.id = id;
		this.areaName = areaName;
		this.areaUrl = areaUrl;
		this.sectorName = sectorName;
		this.sectorUrl = sectorUrl;
		this.problemId = problemId;
		this.problemNr = problemNr;
		this.problemName = problemName;
		this.problemUrl = problemUrl;
		this.problemGrade = problemGrade;
		this.problemLockedAdmin = problemLockedAdmin;
		this.problemLockedSuperadmin = problemLockedSuperadmin;
		this.lat = lat;
		this.lng = lng;
	}

	public String getAreaName() {
		return areaName;
	}

	public String getAreaUrl() {
		return areaUrl;
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

	public int getProblemNr() {
		return problemNr;
	}

	public String getProblemUrl() {
		return problemUrl;
	}

	public String getSectorName() {
		return sectorName;
	}

	public String getSectorUrl() {
		return sectorUrl;
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
	
	public void setPartners(List<TodoPartner> partners) {
		this.partners = partners;
	}
}
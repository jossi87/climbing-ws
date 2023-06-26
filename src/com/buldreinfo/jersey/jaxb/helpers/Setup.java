package com.buldreinfo.jersey.jaxb.helpers;

import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.google.common.base.Strings;

public class Setup {
	public static enum GRADE_SYSTEM {CLIMBING, BOULDER, ICE};
	private final String domain;
	private int idRegion;
	private GRADE_SYSTEM gradeSystem;
	private String title;
	private String description;
	private LatLng defaultCenter;
	private int defaultZoom;
	private boolean setRobotsDenyAll = false;

	public Setup(String domain, GRADE_SYSTEM gradeSystem) {
		this.domain = domain;
		this.gradeSystem = gradeSystem;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public int getDefaultZoom() {
		return defaultZoom;
	}

	public String getDescription() {
		return description;
	}

	public String getDomain() {
		return domain;
	}

	public GRADE_SYSTEM getGradeSystem() {
		return gradeSystem;
	}

	public int getIdRegion() {
		return idRegion;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return "https://" + domain;
	}
	
	public String getUrl(String suffix) {
		if (Strings.isNullOrEmpty(suffix)) {
			return "https://" + domain;
		}
		return "https://" + domain + suffix;
	}

	public boolean isSetRobotsDenyAll() {
		return setRobotsDenyAll;
	}

	public Setup setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Setup setDescription(String description) {
		this.description = description;
		return this;
	}

	public Setup setIdRegion(int idRegion) {
		this.idRegion = idRegion;
		return this;
	}

	public Setup setLatLng(double lat, double lng) {
		this.defaultCenter = new LatLng(lat, lng);
		return this;
	}

	public Setup setSetRobotsDenyAll() {
		this.setRobotsDenyAll = true;
		return this;
	}

	public Setup setTitle(String title) {
		this.title = title;
		return this;
	}

	public boolean isBouldering() {
		return gradeSystem.equals(Setup.GRADE_SYSTEM.BOULDER);
	}

	@Override
	public String toString() {
		return "Setup [domain=" + domain + ", idRegion=" + idRegion + ", gradeSystem=" + gradeSystem + ", title="
				+ title + ", description=" + description + ", defaultCenter=" + defaultCenter + ", defaultZoom="
				+ defaultZoom + ", setRobotsDenyAll=" + setRobotsDenyAll + "]";
	}
}
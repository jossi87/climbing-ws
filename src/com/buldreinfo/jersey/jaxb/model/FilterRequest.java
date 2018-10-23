package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FilterRequest {
	private final List<Integer> grades;
	private final boolean hideTicked;
	private final boolean onlyWithMedia;
	
	public FilterRequest(List<Integer> grades, boolean hideTicked, boolean onlyWithMedia) {
		this.grades = grades;
		this.hideTicked = hideTicked;
		this.onlyWithMedia = onlyWithMedia;
	}

	public List<Integer> getGrades() {
		return grades;
	}

	public boolean isHideTicked() {
		return hideTicked;
	}

	public boolean isOnlyWithMedia() {
		return onlyWithMedia;
	}

	@Override
	public String toString() {
		return "FilterRequest [grades=" + grades + ", hideTicked=" + hideTicked + ", onlyWithMedia=" + onlyWithMedia
				+ "]";
	}
}
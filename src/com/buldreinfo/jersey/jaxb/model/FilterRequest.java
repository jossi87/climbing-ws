package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FilterRequest {
	private final List<Integer> grades;
	private final int visibility;
	
	public FilterRequest(List<Integer> grades, int visibility) {
		this.grades = grades;
		this.visibility = visibility;
	}

	public List<Integer> getGrades() {
		return grades;
	}
	
	public int getVisibility() {
		return visibility;
	}

	@Override
	public String toString() {
		return "FilterRequest [grades=" + grades + ", visibility=" + visibility + "]";
	}
}
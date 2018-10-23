package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FilterRequest {
	private final List<Integer> grades;
	
	public FilterRequest(List<Integer> grades) {
		this.grades = grades;
	}

	public List<Integer> getGrades() {
		return grades;
	}

	@Override
	public String toString() {
		return "FilterRequest [grades=" + grades + "]";
	}
}
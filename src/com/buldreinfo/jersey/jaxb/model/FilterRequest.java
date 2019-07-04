package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FilterRequest {
	private final List<Integer> grades;
	private final List<Integer> types;
	
	public FilterRequest(List<Integer> grades, List<Integer> types) {
		this.grades = grades;
		this.types = types;
	}

	public List<Integer> getGrades() {
		return grades;
	}
	
	public List<Integer> getTypes() {
		return types;
	}
	
	@Override
	public String toString() {
		return "FilterRequest [grades=" + grades + ", types=" + types + "]";
	}
}
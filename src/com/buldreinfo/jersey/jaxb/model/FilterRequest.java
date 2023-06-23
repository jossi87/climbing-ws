package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FilterRequest {
	private final List<Integer> grades;
	private final List<Integer> disciplines;
	private final List<String> routeTypes;
	
	public FilterRequest(List<Integer> grades, List<Integer> disciplines, List<String> routeTypes) {
		this.grades = grades;
		this.disciplines = disciplines;
		this.routeTypes = routeTypes;
	}

	public List<Integer> getGrades() {
		return grades;
	}
	
	public List<Integer> getDisciplines() {
		return disciplines;
	}
	
	public List<String> getRouteTypes() {
		return routeTypes;
	}
	
	@Override
	public String toString() {
		return "FilterRequest [grades=" + grades + ", disciplines=" + disciplines + ", routeTypes=" + routeTypes + "]";
	}
}
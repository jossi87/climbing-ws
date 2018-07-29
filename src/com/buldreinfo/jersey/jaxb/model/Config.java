package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Config {
	private final String title;
	private List<Grade> grades;
	private List<Type> types;

	public Config(String title) {
		this.title = title;
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Type> getTypes() {
		return types;
	}
	
	public void setGrades(List<Grade> grades) {
		this.grades = grades;
	}
	
	public void setTypes(List<Type> types) {
		this.types = types;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class Grade {
	private final int id;
	private final String grade;
	
	public Grade(int id, String grade) {
		this.id = id;
		this.grade = grade;
	}
	
	public int getId() {
		return id;
	}
	
	public String getGrade() {
		return grade;
	}

	@Override
	public String toString() {
		return "Grade [id=" + id + ", grade=" + grade + "]";
	}
}
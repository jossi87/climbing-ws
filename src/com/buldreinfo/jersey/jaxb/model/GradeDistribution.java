package com.buldreinfo.jersey.jaxb.model;

public class GradeDistribution {
	private final String grade;
	private int num = 0;
	
	public GradeDistribution(String grade) {
		this.grade = grade;
	}

	public String getGrade() {
		return grade;
	}
	
	public int getNum() {
		return num;
	}
	
	public void incrementNum(int plus) {
		num += plus;
	}
}
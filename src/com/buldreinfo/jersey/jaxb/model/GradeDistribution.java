package com.buldreinfo.jersey.jaxb.model;

public class GradeDistribution {
	private final String grade;
	private int num;
	
	public GradeDistribution(String grade) {
		this.grade = grade;
		this.num = 0;
	}
	
	public GradeDistribution(String grade, int num) {
		this.grade = grade;
		this.num = num;
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
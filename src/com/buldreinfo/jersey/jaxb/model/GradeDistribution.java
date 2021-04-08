package com.buldreinfo.jersey.jaxb.model;

public class GradeDistribution {
	private final String grade;
	private final int num;
	private final int prim;
	private final int sec;
	
	public GradeDistribution(String grade, int num, int prim, int sec) {
		this.grade = grade;
		this.num = num;
		this.prim = prim;
		this.sec = sec;
	}

	public String getGrade() {
		return grade;
	}

	public int getNum() {
		return num;
	}

	public int getPrim() {
		return prim;
	}

	public int getSec() {
		return sec;
	}
}
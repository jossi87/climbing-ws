package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Finder implements IMetadata {
	private final int idGrade;
	private final String grade;
	private final List<Problem> problems;
	private Metadata metadata;
	
	public Finder(int idGrade, String grade, List<Problem> problems) {
		this.idGrade = idGrade;
		this.grade = grade;
		this.problems = problems;
	}
	
	public String getGrade() {
		return grade;
	}

	public int getIdGrade() {
		return idGrade;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public List<Problem> getProblems() {
		return problems;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
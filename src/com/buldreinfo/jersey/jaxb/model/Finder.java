package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Finder implements IMetadata {
	private final String grade;
	private final List<Problem> problems;
	private Metadata metadata;
	
	public Finder(String grade, List<Problem> problems) {
		this.grade = grade;
		this.problems = problems;
	}
	
	public String getGrade() {
		return grade;
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
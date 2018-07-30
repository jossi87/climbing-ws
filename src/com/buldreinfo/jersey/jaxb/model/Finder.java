package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Finder implements IMetadata {
	private final String grade;
	private final List<Problem> problems;
	private final LatLng defaultCenter;
	private final boolean isBouldering;
	private Metadata metadata;
	
	public Finder(String grade, List<Problem> problems, LatLng defaultCenter, boolean isBouldering) {
		this.grade = grade;
		this.problems = problems;
		this.defaultCenter = defaultCenter;
		this.isBouldering = isBouldering;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
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

	public boolean isBouldering() {
		return isBouldering;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
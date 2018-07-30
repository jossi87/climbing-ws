package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Finder implements IMetadata {
	private final List<Problem> problems;
	private final LatLng defaultCenter;
	private final boolean isBouldering;
	private Metadata metadata;
	
	public Finder(List<Problem> problems, LatLng defaultCenter, boolean isBouldering) {
		this.problems = problems;
		this.defaultCenter = defaultCenter;
		this.isBouldering = isBouldering;
	}

	public List<Problem> getProblems() {
		return problems;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public boolean isBouldering() {
		return isBouldering;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class TableOfContents implements IMetadata {
	private final Collection<Problem> problems;
	private Metadata metadata;
	
	public TableOfContents(Collection<Problem> problems) {
		this.problems = problems;
	}

	public Collection<Problem> getProblems() {
		return problems;
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
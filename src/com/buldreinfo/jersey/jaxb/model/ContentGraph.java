package com.buldreinfo.jersey.jaxb.model;

import java.util.Collection;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class ContentGraph implements IMetadata {
	private Metadata metadata;
	private final Collection<GradeDistribution> gradeDistribution;
	
	public ContentGraph(Collection<GradeDistribution> gradeDistribution) {
		this.gradeDistribution = gradeDistribution;
	}
	
	public Collection<GradeDistribution> getGradeDistribution() {
		return gradeDistribution;
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
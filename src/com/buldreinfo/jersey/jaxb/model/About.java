package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class About implements IMetadata {
	private Metadata metadata;
	private final List<AboutAdministrator> administrators = new ArrayList<>();
	
	public About() {
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<AboutAdministrator> getAdministrators() {
		return administrators;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Help implements IMetadata {
	private Metadata metadata;
	private final List<HelpAdministrator> administrators = new ArrayList<>();
	
	public Help() {
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<HelpAdministrator> getAdministrators() {
		return administrators;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Ethics implements IMetadata {
	private Metadata metadata;

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
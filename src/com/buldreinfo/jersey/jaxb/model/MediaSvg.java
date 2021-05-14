package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class MediaSvg implements IMetadata {
	private final Media m;
	private Metadata metadata;
	
	public MediaSvg(Media m) {
		this.m = m;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public Media getM() {
		return m;
	}
}
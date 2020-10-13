package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.xml.Camera;

public class Cameras implements IMetadata {
	private Metadata metadata;
	private final List<Camera> cameras;
	
	public Cameras(List<Camera> cameras) {
		this.cameras = cameras;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<Camera> getCameras() {
		return cameras;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

}

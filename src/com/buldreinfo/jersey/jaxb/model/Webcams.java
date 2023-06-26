package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.xml.Webcam;

public class Webcams implements IMetadata {
	private Metadata metadata;
	private final List<Webcam> cameras;
	
	public Webcams(List<Webcam> cameras) {
		this.cameras = cameras;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<Webcam> getCameras() {
		return cameras;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

}

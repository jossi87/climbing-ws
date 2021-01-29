package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup.GRADE_SYSTEM;

public class Sites implements IMetadata {
	private final List<SitesRegion> regions;
	private Metadata metadata;
	private final GRADE_SYSTEM type;
	
	public Sites(List<SitesRegion> regions, GRADE_SYSTEM type) {
		this.regions = regions;
		this.type = type;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public List<SitesRegion> getRegions() {
		return regions;
	}
	
	public GRADE_SYSTEM getType() {
		return type;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
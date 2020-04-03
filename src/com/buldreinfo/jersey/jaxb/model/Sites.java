package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Sites implements IMetadata {
	private final List<SitesRegion> regions;
	private Metadata metadata;
	private final boolean isBouldering;
	
	public Sites(List<SitesRegion> regions, boolean isBouldering) {
		this.regions = regions;
		this.isBouldering = isBouldering;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public List<SitesRegion> getRegions() {
		return regions;
	}
	
	public boolean isBouldering() {
		return isBouldering;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
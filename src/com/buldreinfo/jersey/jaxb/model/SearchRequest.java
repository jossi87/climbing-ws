package com.buldreinfo.jersey.jaxb.model;

public class SearchRequest {
	private final int regionId;
	private final String value;
	
	public SearchRequest(int regionId, String value) {
		this.regionId = regionId;
		this.value = value;
	}
	
	public int getRegionId() {
		return regionId;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "SearchRequest [regionId=" + regionId + ", value=" + value + "]";
	}
}
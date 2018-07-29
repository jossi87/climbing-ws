package com.buldreinfo.jersey.jaxb.model;

public class SearchRequest {
	private final String value;
	
	public SearchRequest(int regionId, String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "SearchRequest [value=" + value + "]";
	}
}
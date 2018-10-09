package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class FindCategory {
	private final String name;
	private final List<FindResult> results;
	
	public FindCategory(String name, List<FindResult> results) {
		this.name = name;
		this.results = results;
	}
	
	public String getName() {
		return name;
	}
	
	public List<FindResult> getResults() {
		return results;
	}
}
package com.buldreinfo.jersey.jaxb.batch.migrate8anu.beans;

import java.util.List;

public class Root {
	private final List<Tick> ascents;

	public Root(List<Tick> ascents) {
		this.ascents = ascents;
	}
	
	public List<Tick> getAscents() {
		return ascents;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class CompassDirection {
	private final int id;
	private final String direction;
	
	public CompassDirection(int id, String direction) {
		this.id = id;
		this.direction = direction;
	}

	public int getId() {
		return id;
	}


	public String getDirection() {
		return direction;
	}
}
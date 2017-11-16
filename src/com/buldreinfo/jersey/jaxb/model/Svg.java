package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final int nr;
	private final String pathD;
	private final String textTransform;
	
	public Svg(int nr, String pathD, String textTransform) {
		this.nr = nr;
		this.pathD = pathD;
		this.textTransform = textTransform;
	}

	public int getNr() {
		return nr;
	}

	public String getPathD() {
		return pathD;
	}

	public String getTextTransform() {
		return textTransform;
	}
}
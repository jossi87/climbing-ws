package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final int nr;
	private final String textTransform;
	private final String linePathD;
	private final String topPathD;
	private final String nrPathD;
	
	public Svg(int nr, String textTransform, String linePathD, String topPathD, String nrPathD) {
		this.nr = nr;
		this.textTransform = textTransform;
		this.linePathD = linePathD;
		this.topPathD = topPathD;
		this.nrPathD = nrPathD;
	}

	public String getLinePathD() {
		return linePathD;
	}

	public int getNr() {
		return nr;
	}

	public String getNrPathD() {
		return nrPathD;
	}

	public String getTextTransform() {
		return textTransform;
	}
	
	public String getTopPathD() {
		return topPathD;
	}
}
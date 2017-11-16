package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final int nr;
	private final String textTransform;
	private final String linePathD;
	private final String topPathD;
	
	public Svg(int nr, String textTransform, String linePathD, String topPathD) {
		this.nr = nr;
		this.textTransform = textTransform;
		this.linePathD = linePathD;
		this.topPathD = topPathD;
	}

	public int getNr() {
		return nr;
	}

	public String getTextTransform() {
		return textTransform;
	}

	public String getLinePathD() {
		return linePathD;
	}

	public String getTopPathD() {
		return topPathD;
	}
}
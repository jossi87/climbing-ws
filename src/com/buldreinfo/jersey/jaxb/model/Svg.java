package com.buldreinfo.jersey.jaxb.model;

public class Svg {
	private final int nr;
	private final int width;
	private final int height;
	private final String textTransform;
	private final String linePathD;
	private final String topPathD;
	
	public Svg(int nr, int width, int height, String textTransform, String linePathD, String topPathD) {
		this.nr = nr;
		this.width = width;
		this.height = height;
		this.textTransform = textTransform;
		this.linePathD = linePathD;
		this.topPathD = topPathD;
	}

	public int getNr() {
		return nr;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
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
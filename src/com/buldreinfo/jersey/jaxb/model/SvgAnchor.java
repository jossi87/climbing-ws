package com.buldreinfo.jersey.jaxb.model;

public class SvgAnchor {
	private final int x;
	private final int y;
	private final String txt;
	
	public SvgAnchor(int x, int y, String txt) {
		this.x = x;
		this.y = y;
		this.txt = txt;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String getTxt() {
		return txt;
	}
}
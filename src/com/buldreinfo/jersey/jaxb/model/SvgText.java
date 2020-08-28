package com.buldreinfo.jersey.jaxb.model;

public class SvgText {
	private final int x;
	private final int y;
	private final String txt;
	
	public SvgText(int x, int y, String txt) {
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
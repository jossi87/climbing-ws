package com.buldreinfo.jersey.jaxb.model;

public class OpenGraphImage {
	private final String http;
	private final String width;
	private final String height;
	
	public OpenGraphImage(String http, String width, String height) {
		this.http = http;
		this.width = width;
		this.height = height;
	}

	public String getHttp() {
		return http;
	}
	
	public String getWidth() {
		return width;
	}

	public String getHeight() {
		return height;
	}
}
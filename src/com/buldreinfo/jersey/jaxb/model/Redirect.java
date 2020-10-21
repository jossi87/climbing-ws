package com.buldreinfo.jersey.jaxb.model;

public class Redirect {
	private final String redirectUrl;

	public Redirect(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class Redirect {
	private final String redirectUrl;
	private final String destination;

	public Redirect(String redirectUrl, int idArea, int idSector, int idProblem) {
		this.redirectUrl = redirectUrl;
		if (idArea > 0) {
			this.destination = "/area/" + idArea;
		} else if (idSector > 0) {
			this.destination = "/sector/" + idSector;
		} else if (idProblem > 0) {
			this.destination = "/problem/" + idProblem;
		} else {
			this.destination = "/";
		}
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public String getDestination() {
		return destination;
	}
}
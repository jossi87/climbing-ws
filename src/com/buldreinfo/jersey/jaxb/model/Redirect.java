package com.buldreinfo.jersey.jaxb.model;

public record Redirect(int idArea, int idSector, String redirectUrl, String destination) {
	public static Redirect fromRedirectUrl(String redirectUrl) {
		return new Redirect(0, 0, redirectUrl, null);
	}
	
	public static Redirect fromRoot() {
		return new Redirect(0, 0, null, "/");
	}
	
	public static Redirect fromIdArea(int idArea) {
		return new Redirect(idArea, 0, null, "/area/" + idArea);
	}
	
	public static Redirect fromIdSector(int idSector) {
		return new Redirect(0, idSector, null, "/sector/" + idSector);
	}
	
	public static Redirect fromIdProblem(int idProblem) {
		return new Redirect(0, 0, null, "/problem/" + idProblem);
	}
}
package com.buldreinfo.jersey.jaxb.model;

public record MediaSvgElement(MediaSvgElementType t, int id, String path, int rappelX, int rappelY) {
	public static MediaSvgElement fromPath(int id, String path) {
		return new MediaSvgElement(MediaSvgElementType.PATH, id, path, 0, 0);
	}
	
	public static MediaSvgElement fromRappel(int id, int rappelX, int rappelY, boolean rappelBolted) {
		MediaSvgElementType t = rappelBolted? MediaSvgElementType.RAPPEL_BOLTED : MediaSvgElementType.RAPPEL_NOT_BOLTED;
		return new MediaSvgElement(t, id, null, rappelX, rappelY);
	}
}
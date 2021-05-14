package com.buldreinfo.jersey.jaxb.model;

public class MediaSvgElement {
	private enum TYPE { PATH  };
	private final TYPE t;
	private final int id;
	private final String path;
	
	public MediaSvgElement(int id, String path) {
		this.t = TYPE.PATH;
		this.id = id;
		this.path = path;
	}
	
	public TYPE getT() {
		return t;
	}

	public int getId() {
		return id;
	}

	public String getPath() {
		return path;
	}
}
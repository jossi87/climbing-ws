package com.buldreinfo.jersey.jaxb.model.app;

public class Media {
	private final int id;
	private final boolean isMovie;
	private final int t;
	
	public Media(int id, boolean isMovie, int t) {
		this.id = id;
		this.isMovie = isMovie;
		this.t = t;
	}

	public int getId() {
		return id;
	}

	public boolean isMovie() {
		return isMovie;
	}

	public int getT() {
		return t;
	}
}
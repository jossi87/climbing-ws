package com.buldreinfo.jersey.jaxb.model.v1;

public class V1Media {
	private final int id;
	private final boolean isMovie;
	private final int t;
	
	public V1Media(int id, boolean isMovie, int t) {
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
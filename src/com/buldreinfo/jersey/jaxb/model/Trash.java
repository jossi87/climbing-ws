package com.buldreinfo.jersey.jaxb.model;

public class Trash {
	private final int idArea;
	private final int idSector;
	private final int idProblem;
	private final int idMedia;
	private final String name;
	private final String when;
	private final String by;

	public Trash(int idArea, int idSector, int idProblem, int idMedia, String name, String when, String by) {
		this.idArea = idArea;
		this.idSector = idSector;
		this.idProblem = idProblem;
		this.idMedia = idMedia;
		this.name = name;
		this.when = when;
		this.by = by;
	}

	public String getBy() {
		return by;
	}

	public int getIdArea() {
		return idArea;
	}

	public int getIdMedia() {
		return idMedia;
	}

	public int getIdProblem() {
		return idProblem;
	}

	public int getIdSector() {
		return idSector;
	}

	public String getName() {
		return name;
	}

	public String getWhen() {
		return when;
	}
}
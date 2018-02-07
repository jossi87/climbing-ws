package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Media {
	private final int id;
	private final int width;
	private final int height;
	private final String description;
	private final int idType;
	private final String t;
	private final int svgProblemId;
	private final List<Svg> svgs;

	public Media(int id, int width, int height, String description, int idType, String t, int svgProblemId, List<Svg> svgs) {
		this.id = id;
		this.width = width;
		this.height = height;
		this.description = description;
		this.idType = idType;
		this.t = t;
		this.svgProblemId = svgProblemId;
		this.svgs = svgs;
	}

	public String getDescription() {
		return description;
	}

	public int getHeight() {
		return height;
	}

	public int getId() {
		return id;
	}

	public int getIdType() {
		return idType;
	}

	public List<Svg> getSvgs() {
		return svgs;
	}
	
	public String getT() {
		return t;
	}

	public int getWidth() {
		return width;
	}
	
	public int getSvgProblemId() {
		return svgProblemId;
	}
}
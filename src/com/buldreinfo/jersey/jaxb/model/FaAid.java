package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class FaAid {
	private final int problemId;
	private final String date;
	private final String dateHr;
	private final String description;
	private final List<FaUser> users = new ArrayList<>();
	
	public FaAid(int problemId, String date, String dateHr, String description) {
		this.problemId = problemId;
		this.date = date;
		this.dateHr = dateHr;
		this.description = description;
	}

	public int getProblemId() {
		return problemId;
	}

	public String getDate() {
		return date;
	}

	public String getDateHr() {
		return dateHr;
	}

	public String getDescription() {
		return description;
	}

	public List<FaUser> getUsers() {
		return users;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class ProfileTodoProblem {
	private final int todoId;
	private final int id;
	private final String url;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final int nr;
	private final String name;
	private final String grade;
	private Coordinates coordinates;
	private final List<User> partners = new ArrayList<>();
	
	public ProfileTodoProblem(int todoId, int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade) {
		this.todoId = todoId;
		this.id = id;
		this.url = url;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.nr = nr;
		this.name = name;
		this.grade = grade;
	}
	
	public Coordinates getCoordinates() {
		return coordinates;
	}
	
	public String getGrade() {
		return grade;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNr() {
		return nr;
	}
	
	public List<User> getPartners() {
		return partners;
	}
	
	public int getTodoId() {
		return todoId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean isLockedAdmin() {
		return lockedAdmin;
	}
	
	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}
	
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}
}
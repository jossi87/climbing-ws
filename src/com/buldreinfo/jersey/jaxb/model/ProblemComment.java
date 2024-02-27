package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class ProblemComment {
	private final int id;
	private final String date;
	private final int idUser;
	private final String picture;
	private final String name;
	private final String message;
	private final boolean danger;
	private final boolean resolved;
	private final List<Media> media;
	private boolean editable = false;
	public ProblemComment(int id, String date, int idUser, String picture, String name, String message, boolean danger, boolean resolved, List<Media> media) {
		this.id = id;
		this.date = date;
		this.idUser = idUser;
		this.picture = picture;
		this.name = name;
		this.message = message;
		this.danger = danger;
		this.resolved = resolved;
		this.media = media;
	}
	public String getDate() {
		return date;
	}
	public int getId() {
		return id;
	}
	public int getIdUser() {
		return idUser;
	}
	public List<Media> getMedia() {
		return media;
	}
	public String getMessage() {
		return message;
	}
	public String getName() {
		return name;
	}
	public String getPicture() {
		return picture;
	}
	public boolean isDanger() {
		return danger;
	}
	public boolean isEditable() {
		return editable;
	}
	public boolean isResolved() {
		return resolved;
	}
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
}
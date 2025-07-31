package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class ProblemComment {
	private final int id;
	private final String date;
	private final int idUser;
	private final long avatarCrc32;
	private final String name;
	private final String message;
	private final boolean danger;
	private final boolean resolved;
	private final List<Media> media;
	private boolean editable = false;
	public ProblemComment(int id, String date, int idUser, long avatarCrc32, String name, String message, boolean danger, boolean resolved, List<Media> media) {
		this.id = id;
		this.date = date;
		this.idUser = idUser;
		this.avatarCrc32 = avatarCrc32;
		this.name = name;
		this.message = message;
		this.danger = danger;
		this.resolved = resolved;
		this.media = media;
	}
	public long getAvatarCrc32() {
		return avatarCrc32;
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
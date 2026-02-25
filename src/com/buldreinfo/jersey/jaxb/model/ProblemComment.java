package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class ProblemComment {
	private final int id;
	private final String date;
	private final int idUser;
	private final int mediaId;
	private final long mediaVersionStamp;
	private final String name;
	private final String message;
	private final boolean danger;
	private final boolean resolved;
	private final List<Media> media;
	private boolean editable = false;
	public ProblemComment(int id, String date, int idUser, int mediaId, long mediaVersionStamp, String name, String message, boolean danger, boolean resolved, List<Media> media) {
		this.id = id;
		this.date = date;
		this.idUser = idUser;
		this.mediaId = mediaId;
		this.mediaVersionStamp = mediaVersionStamp;
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
	public int getMediaId() {
		return mediaId;
	}
	public long getMediaVersionStamp() {
		return mediaVersionStamp;
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
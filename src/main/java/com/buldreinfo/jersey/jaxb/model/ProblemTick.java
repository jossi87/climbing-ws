package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class ProblemTick {
	private List<TickRepeat> repeats;
	private final int id;
	private final int idUser;
	private final MediaIdentity mediaIdentity;
	private final String date;
	private final String name;
	private final String suggestedGrade;
	private final boolean noPersonalGrade;
	private final String comment;
	private final double stars;
	private final boolean writable;
	public ProblemTick(int id, int idUser, MediaIdentity mediaIdentity, String date, String name, String suggestedGrade, boolean noPersonalGrade, String comment, double stars, boolean writable) {
		this.id = id;
		this.idUser = idUser;
		this.mediaIdentity = mediaIdentity;
		this.date = date;
		this.name = name;
		this.suggestedGrade = suggestedGrade;
		this.noPersonalGrade = noPersonalGrade;
		this.comment = comment;
		this.stars = stars;
		this.writable = writable;
	}
	public void addRepeat(int id2, int tickId2, String date2, String comment2) {
		if (repeats == null) {
			repeats = new ArrayList<>();
		}
		repeats.add(new TickRepeat(id2, tickId2, comment2, date2));
	}
	public String getComment() {
		return comment;
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
	public MediaIdentity getMediaIdentity() {
		return mediaIdentity;
	}
	public String getName() {
		return name;
	}
	public List<TickRepeat> getRepeats() {
		return repeats;
	}
	public double getStars() {
		return stars;
	}
	public String getSuggestedGrade() {
		return suggestedGrade;
	}
	public boolean isNoPersonalGrade() {
		return noPersonalGrade;
	}
	public boolean isWritable() {
		return writable;
	}
	@Override
	public String toString() {
		return "ProblemTick [repeats=" + repeats + ", id=" + id + ", idUser=" + idUser + ", mediaId=" + mediaIdentity
				+ ", mediaIdentity=" + mediaIdentity + ", date=" + date + ", name=" + name + ", suggestedGrade="
				+ suggestedGrade + ", noPersonalGrade=" + noPersonalGrade + ", comment=" + comment + ", stars=" + stars
				+ ", writable=" + writable + "]";
	}
}
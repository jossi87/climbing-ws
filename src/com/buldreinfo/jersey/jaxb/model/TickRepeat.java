package com.buldreinfo.jersey.jaxb.model;

public class TickRepeat {
	private final int id;
	private final int tickId;
	private final String comment;
	private final String date;
	
	public TickRepeat(int id, int tickId, String comment, String date) {
		this.id = id;
		this.tickId = tickId;
		this.comment = comment;
		this.date = date;
	}

	public int getId() {
		return id;
	}

	public int getTickId() {
		return tickId;
	}

	public String getComment() {
		return comment;
	}

	public String getDate() {
		return date;
	}

	@Override
	public String toString() {
		return "TickRepeat [id=" + id + ", tickId=" + tickId + ", comment=" + comment + ", date=" + date + "]";
	}
}
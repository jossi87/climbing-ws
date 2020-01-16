package com.buldreinfo.jersey.jaxb.model;

public class NewMedia {
	private final String name;
	private final String photographer;
	private final String inPhoto;
	private final int pitch;
	
	public NewMedia(String name, String photographer, String inPhoto, int pitch) {
		this.name = name;
		this.photographer = photographer;
		this.inPhoto = inPhoto;
		this.pitch = pitch;
	}
	
	public String getName() {
		return name;
	}
	
	public String getInPhoto() {
		return inPhoto;
	}
	
	public String getPhotographer() {
		return photographer;
	}
	
	public int getPitch() {
		return pitch;
	}

	@Override
	public String toString() {
		return "NewMedia [name=" + name + ", photographer=" + photographer + ", inPhoto=" + inPhoto + ", pitch=" + pitch + "]";
	}
}
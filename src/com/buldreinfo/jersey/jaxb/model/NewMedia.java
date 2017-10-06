package com.buldreinfo.jersey.jaxb.model;

public class NewMedia {
	private final String name;
	private final String photographer;
	private final String inPhoto;
	
	public NewMedia(String name, String photographer, String inPhoto) {
		this.name = name;
		this.photographer = photographer;
		this.inPhoto = inPhoto;
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

	@Override
	public String toString() {
		return "NewMedia [name=" + name + ", photographer=" + photographer + ", inPhoto=" + inPhoto + "]";
	}
}
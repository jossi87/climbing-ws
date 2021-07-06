package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Profile implements IMetadata {
	private Metadata metadata;
	private final int id;
	private final String picture;
	private final String name;

	public Profile(int id, String picture, String name) {
		this.id = id;
		this.picture = picture;
		this.name = name;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public int getId() {
		return id;
	}

	public String getPicture() {
		return picture;
	}

	public String getName() {
		return name;
	}
}
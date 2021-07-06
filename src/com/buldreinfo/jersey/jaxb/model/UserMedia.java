package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

@Deprecated
public class UserMedia implements IMetadata {
	private final int id;
	private final String name;
	private final String picture;
	private final List<MediaProblem> media = new ArrayList<>();
	private Metadata metadata;
	
	public UserMedia(int id, String name, String picture) {
		this.id = id;
		this.name = name;
		this.picture = picture;
	}
	
	public int getId() {
		return id;
	}

	public List<MediaProblem> getMedia() {
		return media;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public String getName() {
		return name;
	}
	
	public String getPicture() {
		return picture;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}

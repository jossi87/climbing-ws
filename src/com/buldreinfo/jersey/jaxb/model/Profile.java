package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.model.User.Tick;

public class Profile implements IMetadata {
	private Metadata metadata;
	private final int id;
	private final String picture;
	private final String name;
	private int numImagesCreated;
	private int numVideosCreated;
	private int numImageTags;
	private int numVideoTags;
	private final List<Tick> ticks = new ArrayList<>();
	private final List<UserRegion> userRegions;

	public Profile(int id, String picture, String name, List<UserRegion> userRegions) {
		this.id = id;
		this.picture = picture;
		this.name = name;
		this.userRegions = userRegions;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public String getName() {
		return name;
	}

	public int getNumImagesCreated() {
		return numImagesCreated;
	}

	public int getNumImageTags() {
		return numImageTags;
	}

	public int getNumVideosCreated() {
		return numVideosCreated;
	}

	public int getNumVideoTags() {
		return numVideoTags;
	}

	public String getPicture() {
		return picture;
	}

	public List<Tick> getTicks() {
		return ticks;
	}

	public List<UserRegion> getUserRegions() {
		return userRegions;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public void setNumImagesCreated(int numImagesCreated) {
		this.numImagesCreated = numImagesCreated;
	}

	public void setNumImageTags(int numImageTags) {
		this.numImageTags = numImageTags;
	}

	public void setNumVideosCreated(int numVideosCreated) {
		this.numVideosCreated = numVideosCreated;
	}

	public void setNumVideoTags(int numVideoTags) {
		this.numVideoTags = numVideoTags;
	}
}
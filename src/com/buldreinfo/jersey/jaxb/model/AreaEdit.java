package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class AreaEdit implements IMetadata {
	private final int id;
	private final int visibility;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final List<NewMedia> newMedia = new ArrayList<>();
	private Metadata metadata;
	
	public AreaEdit(int id, int visibility, String name, String comment, double lat, double lng) {
		this.id = id;
		this.visibility = visibility;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
	}
	
	public String getComment() {
		return comment;
	}
	
	public int getId() {
		return id;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public String getName() {
		return name;
	}

	public List<NewMedia> getNewMedia() {
		return newMedia;
	}

	public int getVisibility() {
		return visibility;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Permissions implements IMetadata {
	private Metadata metadata;
	private final List<PermissionUser> users = new ArrayList<>();
	
	public Permissions() {
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<PermissionUser> getUsers() {
		return users;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
package com.buldreinfo.jersey.jaxb.metadata.jsonld;

import com.google.gson.annotations.SerializedName;

public class Item {
	@SerializedName("@id") private final String id;
	private final String name;
	
	public Item(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
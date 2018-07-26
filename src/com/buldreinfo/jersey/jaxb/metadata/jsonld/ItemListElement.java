package com.buldreinfo.jersey.jaxb.metadata.jsonld;

import com.google.gson.annotations.SerializedName;

public class ItemListElement {
	@SerializedName("@type") private final String type = "ListItem";
	private final int position;
	private final Item item;
	
	
	public ItemListElement(int position, Item item) {
		this.position = position;
		this.item = item;
	}

	public String getType() {
		return type;
	}

	public int getPosition() {
		return position;
	}

	public Item getItem() {
		return item;
	}
}
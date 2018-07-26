package com.buldreinfo.jersey.jaxb.metadata.jsonld;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JsonLd {
	@SerializedName("@context") private final String context = "http://schema.org";
	@SerializedName("@type") private final String type = "BreadcrumbList";
	private final List<ItemListElement> itemListElement = new ArrayList<>();
	
	public String getContext() {
		return context;
	}
	
	public String getType() {
		return type;
	}
	
	public List<ItemListElement> getItemListElement() {
		return itemListElement;
	}
}
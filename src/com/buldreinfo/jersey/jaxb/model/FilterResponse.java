package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class FilterResponse implements IMetadata {
	private final List<FilterRow> rows;
	private Metadata metadata;
	
	public FilterResponse(List<FilterRow> rows) {
		super();
		this.rows = rows;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	public List<FilterRow> getRows() {
		return rows;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}

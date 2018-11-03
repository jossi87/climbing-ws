package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Ticks implements IMetadata {
	private Metadata metadata;
	private final List<PublicAscent> ticks;
	private final int currPage;
	private final int numPages;
	
	public Ticks(List<PublicAscent> ticks, int currPage, int numPages) {
		this.ticks = ticks;
		this.currPage = currPage;
		this.numPages = numPages;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public List<PublicAscent> getTicks() {
		return ticks;
	}

	public int getCurrPage() {
		return currPage;
	}

	public int getNumPages() {
		return numPages;
	}

	@Override
	public String toString() {
		return "Ticks [metadata=" + metadata + ", ticks.size()=" + ticks.size() + ", currPage=" + currPage + ", numPages=" + numPages + "]";
	}
}
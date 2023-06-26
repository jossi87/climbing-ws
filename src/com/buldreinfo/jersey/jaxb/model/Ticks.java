package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Ticks {
	private final List<PublicAscent> ticks;
	private final int currPage;
	private final int numPages;
	
	public Ticks(List<PublicAscent> ticks, int currPage, int numPages) {
		this.ticks = ticks;
		this.currPage = currPage;
		this.numPages = numPages;
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
		return "Ticks [ticks.size()=" + ticks.size() + ", currPage=" + currPage + ", numPages=" + numPages + "]";
	}
}
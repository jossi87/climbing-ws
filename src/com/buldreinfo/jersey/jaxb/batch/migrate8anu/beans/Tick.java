package com.buldreinfo.jersey.jaxb.batch.migrate8anu.beans;

public class Tick {
	private final String cragName;
	private final String sectorSlug;
	private final String zlaggableName;
	private final String difficulty;
	private final String date;
	private final boolean secondGo;
	private final String type;
	private final String notes;
	private final int rating;
	private final String countrySlug;
	
	public Tick(String cragName, String sectorSlug, String zlaggableName, String difficulty, String date, boolean secondGo, String type, String notes, int rating, String countrySlug) {
		this.cragName = cragName;
		this.sectorSlug = sectorSlug;
		this.zlaggableName = zlaggableName;
		this.difficulty = difficulty;
		this.date = date;
		this.secondGo = secondGo;
		this.type = type;
		this.notes = notes;
		this.rating = rating;
		this.countrySlug = countrySlug;
	}

	public String getCragName() {
		return cragName;
	}

	public String getSectorSlug() {
		return sectorSlug;
	}

	public String getZlaggableName() {
		return zlaggableName;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public String getDate() {
		return date;
	}

	public boolean isSecondGo() {
		return secondGo;
	}

	public String getType() {
		return type;
	}

	public String getNotes() {
		return notes;
	}

	public int getRating() {
		return rating;
	}

	public String getCountrySlug() {
		return countrySlug;
	}

	@Override
	public String toString() {
		return "Tick [cragName=" + cragName + ", sectorSlug=" + sectorSlug + ", zlaggableName=" + zlaggableName
				+ ", difficulty=" + difficulty + ", date=" + date + ", secondGo=" + secondGo + ", type=" + type
				+ ", notes=" + notes + ", rating=" + rating + ", countrySlug=" + countrySlug + "]";
	}
}
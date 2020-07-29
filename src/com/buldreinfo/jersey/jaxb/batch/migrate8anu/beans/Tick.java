package com.buldreinfo.jersey.jaxb.batch.migrate8anu.beans;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

import jersey.repackaged.com.google.common.base.Joiner;

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
	private final boolean isHard;
	private final boolean isEasy;
	
	public Tick(String cragName, String sectorSlug, String zlaggableName, String difficulty, String date,
			boolean secondGo, String type, String notes, int rating, String countrySlug, boolean isHard,
			boolean isEasy) {
		super();
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
		this.isHard = isHard;
		this.isEasy = isEasy;
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

	public boolean isHard() {
		return isHard;
	}

	public boolean isEasy() {
		return isEasy;
	}

	@Override
	public String toString() {
		return "Tick [cragName=" + cragName + ", sectorSlug=" + sectorSlug + ", zlaggableName=" + zlaggableName
				+ ", difficulty=" + difficulty + ", date=" + date + ", secondGo=" + secondGo + ", type=" + type
				+ ", notes=" + notes + ", rating=" + rating + ", countrySlug=" + countrySlug + ", isHard=" + isHard
				+ ", isEasy=" + isEasy + "]";
	}

	public String getComment() {
		List<String> parts = new ArrayList<>();
		
		if (isEasy) {
			parts.add("Soft");
		}
		else if (isHard) {
			parts.add("Hard");
		}
		
		if (secondGo) {
			parts.add("Second go");
		}
		else if (type.equals("os")) {
			parts.add("OS");
		}
		else if (type.equals("f")) {
			parts.add("Flash");
		}
		
		if (!Strings.isNullOrEmpty(notes)) {
			parts.add(notes);
		}
		
		if (parts.isEmpty()) {
			return null;
		}
		return Joiner.on(" - ").join(parts);
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradeDistribution {
	public class GradeDistributionRow {
		private final String name;
		private int numBoulder = 0;
		private int numSport = 0;
		private int numTrad = 0;
		private int numMixed = 0;
		private int numTopRope = 0;
		private int numAid = 0;
		private int numAidTrad = 0;
		private int numIce = 0;
		public GradeDistributionRow(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public int getNumAid() {
			return numAid;
		}
		public int getNumAidTrad() {
			return numAidTrad;
		}
		public int getNumBoulder() {
			return numBoulder;
		}
		public int getNumIce() {
			return numIce;
		}
		public int getNumMixed() {
			return numMixed;
		}
		public int getNumSport() {
			return numSport;
		}
		public int getNumTopRope() {
			return numTopRope;
		}
		public int getNumTrad() {
			return numTrad;
		}
		public void setNumAid(int numAid) {
			this.numAid = numAid;
		}
		public void setNumAidTrad(int numAidTrad) {
			this.numAidTrad = numAidTrad;
		}
		public void setNumBoulder(int numBoulder) {
			this.numBoulder = numBoulder;
		}
		public void setNumIce(int numIce) {
			this.numIce = numIce;
		}
		public void setNumMixed(int numMixed) {
			this.numMixed = numMixed;
		}
		public void setNumSport(int numSport) {
			this.numSport = numSport;
		}
		public void setNumTopRope(int numTopRope) {
			this.numTopRope = numTopRope;
		}
		public void setNumTrad(int numTrad) {
			this.numTrad = numTrad;
		}
	}
	private final String grade;
	private int num = 0;
	private int prim = 0;
	private int sec = 0;
	private final List<GradeDistributionRow> rows = new ArrayList<>();
	
	public GradeDistribution(String grade) {
		this.grade = grade;
	}
	
	public void addSector(String name, String type, int num) {
		GradeDistributionRow row = null;
		Optional<GradeDistributionRow> optRow = rows.stream().filter(x -> x.getName().equals(name)).findAny();
		if (optRow.isPresent()) {
			row = optRow.get();
		} else {
			row = new GradeDistributionRow(name);
			rows.add(row);		
		}
		switch (type) {
		case "Boulder":
			this.prim += num;
			row.setNumBoulder(num);
			break;
		case "Bolt":
			this.prim += num;
			row.setNumBoulder(num);
			break;
		case "Trad":
			this.sec += num;
			row.setNumBoulder(num);
			break;
		case "Mixed":
			this.sec += num;
			row.setNumBoulder(num);
			break;
		case "Top rope":
			this.sec += num;
			row.setNumBoulder(num);
			break;
		case "Aid":
			this.sec += num;
			row.setNumBoulder(num);
			break;
		case "Aid/Trad":
			this.sec += num;
			row.setNumBoulder(num);
			break;
		case "Ice":
			this.prim += num;
			row.setNumBoulder(num);
			break;
		default: throw new RuntimeException("Invalid type: " + type);
		}
		this.num += num;
	}
	
	public String getGrade() {
		return grade;
	}

	public int getNum() {
		return num;
	}

	public int getPrim() {
		return prim;
	}

	public List<GradeDistributionRow> getRows() {
		return rows;
	}

	public int getSec() {
		return sec;
	}
}
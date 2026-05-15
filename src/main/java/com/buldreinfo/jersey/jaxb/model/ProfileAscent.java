package com.buldreinfo.jersey.jaxb.model;

public class ProfileAscent {
	private final String regionName;
	private final int areaId;
	private final String areaName;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final int sectorId;
	private final String sectorName;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private int num;
	private final int id;
	private final int idTickRepeat;
	private final String subType;
	private final int numPitches;
	private final int idProblem;
	private final int nr;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final String name;
	private final String comment;
	private String date;
	private String dateHr;
	private final double stars;
	private boolean fa;
	private final String grade;
	private final int gradeWeight;
	private final boolean noPersonalGrade;
	private Coordinates coordinates;
	
	public ProfileAscent(String regionName, int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int id, int idTickRepeat, String subType, int numPitches,
			int idProblem, int nr, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String date,
			String dateHr, double stars, boolean fa, String grade, int gradeWeight, boolean noPersonalGrade) {
		this.regionName = regionName;
		this.areaId = areaId;
		this.areaName = areaName;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.sectorId = sectorId;
		this.sectorName = sectorName;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.id = id;
		this.idTickRepeat = idTickRepeat;
		this.subType = subType;
		this.numPitches = numPitches;
		this.idProblem = idProblem;
		this.nr = nr;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.name = name;
		this.comment = comment;
		this.date = date;
		this.dateHr = dateHr;
		this.stars = stars;
		this.fa = fa;
		this.grade = grade;
		this.gradeWeight = gradeWeight;
		this.noPersonalGrade = noPersonalGrade;
	}
	
	public int getAreaId() {
		return areaId;
	}
	
	public String getAreaName() {
		return areaName;
	}
	
	public String getComment() {
		return comment;
	}
	
	public Coordinates getCoordinates() {
		return coordinates;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getDateHr() {
		return dateHr;
	}
	
	public String getGrade() {
		return grade;
	}
	
	public int getGradeWeight() {
		return gradeWeight;
	}

	public int getId() {
		return id;
	}

	public int getIdProblem() {
		return idProblem;
	}
	
	public int getIdTickRepeat() {
		return idTickRepeat;
	}

	public String getName() {
		return name;
	}

	public int getNr() {
		return nr;
	}
	
	public int getNum() {
		return num;
	}
	
	public int getNumPitches() {
		return numPitches;
	}

	public String getRegionName() {
		return regionName;
	}

	public int getSectorId() {
		return sectorId;
	}

	public String getSectorName() {
		return sectorName;
	}

	public double getStars() {
		return stars;
	}

	public String getSubType() {
		return subType;
	}

	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}

	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}
	
	public boolean isFa() {
		return fa;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}

	public boolean isNoPersonalGrade() {
		return noPersonalGrade;
	}

	public boolean isSectorLockedAdmin() {
		return sectorLockedAdmin;
	}

	public boolean isSectorLockedSuperadmin() {
		return sectorLockedSuperadmin;
	}
	
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}
	
	public void setDate(String date) {
		this.date = date;
	}

	public void setDateHr(String dateHr) {
		this.dateHr = dateHr;
	}

	public void setFa(boolean fa) {
		this.fa = fa;
	}

	public void setNum(int num) {
		this.num = num;
	}
}
package com.buldreinfo.jersey.jaxb.model;

public class SectorProblem {
	private final int id;
	private final String broken;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final int nr;
	private final String name;
	private final String rock;
	private final String comment;
	private final int gradeNumber;
	private final String grade;
	private final String fa;
	private final int numPitches;
	private final boolean hasImages;
	private final boolean hasMovies;
	private final boolean hasTopo;
	private final Coordinate coordinate;
	private final int numTicks;
	private final double stars;
	private final boolean ticked;
	private final boolean todo;
	private final Type t;
	private final boolean danger;
	
	public SectorProblem(int id, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, int gradeNumber, String grade, String fa,
			int numPitches,
			boolean hasImages, boolean hasMovies, boolean hasTopo, Coordinate coordinate, int numTicks, double stars, boolean ticked, boolean todo, Type t,
			boolean danger) {
		this.id = id;
		this.broken = broken;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.nr = nr;
		this.name = name;
		this.rock = rock;
		this.comment = comment;
		this.gradeNumber = gradeNumber;
		this.grade = grade;
		this.fa = fa;
		this.numPitches = numPitches;
		this.hasImages = hasImages;
		this.hasMovies = hasMovies;
		this.hasTopo = hasTopo;
		this.coordinate = coordinate;
		this.numTicks = numTicks;
		this.stars = stars;
		this.ticked = ticked;
		this.todo = todo;
		this.t = t;
		this.danger = danger;
	}

	public String getBroken() {
		return broken;
	}
	
	public String getComment() {
		return comment;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public String getFa() {
		return fa;
	}

	public String getGrade() {
		return grade;
	}
	
	public int getGradeNumber() {
		return gradeNumber;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNr() {
		return nr;
	}

	public int getNumPitches() {
		return numPitches;
	}

	public int getNumTicks() {
		return numTicks;
	}

	public String getRock() {
		return rock;
	}

	public double getStars() {
		return stars;
	}

	public Type getT() {
		return t;
	}

	public boolean isDanger() {
		return danger;
	}

	public boolean isHasImages() {
		return hasImages;
	}

	public boolean isHasMovies() {
		return hasMovies;
	}

	public boolean isHasTopo() {
		return hasTopo;
	}
	
	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}

	public boolean isTicked() {
		return ticked;
	}
	
	public boolean isTodo() {
		return todo;
	}
}
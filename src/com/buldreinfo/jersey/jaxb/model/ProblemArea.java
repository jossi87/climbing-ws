package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.SectorSort;

public class ProblemArea {
	public class ProblemAreaProblem {
		private final int id;
		private final String url;
		private final String broken;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String description;
		private final Coordinates coordinates;
		private final String grade;
		private final String fa;
		private final int numTicks;
		private final double stars;
		private final boolean ticked;
		private final Type t;
		private final int numPitches;
		
		public ProblemAreaProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String description, Coordinates coordinates, String grade, String fa, int numTicks, double stars, boolean ticked, Type t, int numPitches) {
			this.id = id;
			this.url = url;
			this.broken = broken;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.description = description;
			this.coordinates = coordinates;
			this.grade = grade;
			this.fa = fa;
			this.numTicks = numTicks;
			this.stars = stars;
			this.ticked = ticked;
			this.t = t;
			this.numPitches = numPitches;
		}
		
		public String getBroken() {
			return broken;
		}
		
		public String getDescription() {
			return description;
		}
		
		public String getFa() {
			return fa;
		}
		
		public String getGrade() {
			return grade;
		}

		public int getId() {
			return id;
		}

		public Coordinates getCoordinates() {
			return coordinates;
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

		public double getStars() {
			return stars;
		}

		public Type getT() {
			return t;
		}

		public String getUrl() {
			return url;
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
	}
	
	public class ProblemAreaSector {
		private final int id;
		private final String url;
		private final String name;
		private final int sorting;
		private final Coordinates parking;
		private List<Coordinates> outline;
		private final String wallDirection;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<ProblemAreaProblem> problems = new ArrayList<>();
		
		public ProblemAreaSector(int id, String url, String name, int sorting, Coordinates parking, String wallDirection, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.sorting = sorting;
			this.parking = parking;
			this.wallDirection = wallDirection;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public ProblemAreaProblem addProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String description, Coordinates coordinates, String grade, String fa, int numTicks, double stars, boolean ticked, Type t, int numPitches) {
			ProblemAreaProblem p = new ProblemAreaProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, description, coordinates, grade, fa, numTicks, stars, ticked, t, numPitches);
			this.problems.add(p);
			return p;
		}
		
		public int getId() {
			return id;
		}
		
		public Coordinates getParking() {
			return parking;
		}
		
		public String getName() {
			return name;
		}
		
		public List<Coordinates> getOutline() {
			return outline;
		}
		
		public List<ProblemAreaProblem> getProblems() {
			return problems;
		}
		
		public int getSorting() {
			return sorting;
		}
		
		public String getUrl() {
			return url;
		}
		
		public String getWallDirection() {
			return wallDirection;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}

		public void setOutline(List<Coordinates> outline) {
			this.outline = outline;
		}
	}
	
	private final int id;
	private final String url;
	private final String name;
	private final Coordinates coordinates;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final int sunFromHour;
	private final int sunToHour;
	private final List<ProblemAreaSector> sectors = new ArrayList<>();
	
	public ProblemArea(int id, String url, String name, Coordinates coordinates, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour) {
		this.id = id;
		this.url = url;
		this.name = name;
		this.coordinates = coordinates;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.sunFromHour = sunFromHour;
		this.sunToHour = sunToHour;
	}
	
	public ProblemAreaSector addSector(int id, String url, String name, int sorting, Coordinates parking, String wallDirection, boolean lockedAdmin, boolean lockedSuperadmin) {
		ProblemAreaSector s = new ProblemAreaSector(id, url, name, sorting, parking, wallDirection, lockedAdmin, lockedSuperadmin);
		this.sectors.add(s);
		return s;
	}

	public int getId() {
		return id;
	}
	
	public Coordinates getCoordinates() {
		return coordinates;
	}
	
	public String getName() {
		return name;
	}

	public List<ProblemAreaSector> getSectors() {
		return sectors;
	}
	
	public int getSunFromHour() {
		return sunFromHour;
	}

	public int getSunToHour() {
		return sunToHour;
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}
	
	public void orderSectors() {
		if (sectors != null) {
			sectors.sort((ProblemAreaSector o1, ProblemAreaSector o2) -> SectorSort.sortSector(o1.getSorting(), o1.getName(), o2.getSorting(), o2.getName()));
		}
	}
}
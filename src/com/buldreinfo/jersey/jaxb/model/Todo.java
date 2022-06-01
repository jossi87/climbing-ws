package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Todo {
	public class Area {
		private final int id;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<Sector> sectors = new ArrayList<>();
		
		public Area(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}
		
		public Sector addSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			Sector s = new Sector(id, name, lockedAdmin, lockedSuperadmin);
			this.sectors.add(s);
			return s;
		}

		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public List<Sector> getSectors() {
			return sectors;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	
	public class Partner {
		private final int id;
		private final String name;
		
		public Partner(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
	
	public class Problem {
		private final int id;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String grade;
		private List<Partner> partners = new ArrayList<>();
		
		public Problem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade) {
			this.id = id;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.grade = grade;
		}
		
		public Partner addPartner(int id, String name) {
			Partner res = new Partner(id, name);
			this.partners.add(res);
			return res;
		}
		
		public String getGrade() {
			return grade;
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
		
		public List<Partner> getPartners() {
			return partners;
		}
		
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
		
		public void setPartners(List<Partner> partners) {
			this.partners = partners;
		}
	}
	
	public class Sector {
		private final int id;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<Problem> problems = new ArrayList<>();
		
		public Sector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public Problem addProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade) {
			Problem p = new Problem(id, lockedAdmin, lockedSuperadmin, nr, name, grade);
			this.problems.add(p);
			return p;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public List<Problem> getProblems() {
			return problems;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	
	private final List<Area> areas = new ArrayList<>();
	
	public Todo() {
	}
	
	public Area addArea(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
		Area a = new Area(id, name, lockedAdmin, lockedSuperadmin);
		this.areas.add(a);
		return a;
	}

	public List<Area> getAreas() {
		return areas;
	}
}
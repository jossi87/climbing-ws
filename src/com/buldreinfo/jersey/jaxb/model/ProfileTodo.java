package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class ProfileTodo {
	public class Area {
		private final int id;
		private final String url;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<Sector> sectors = new ArrayList<>();
		
		public Area(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}
		
		public Sector addSector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			Sector s = new Sector(id, url, name, lockedAdmin, lockedSuperadmin);
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

		public String getUrl() {
			return url;
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
		private final int todoId;
		private final int id;
		private final String url;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String grade;
		private final double lat;
		private final double lng;
		private List<Partner> partners = new ArrayList<>();
		
		public Problem(int todoId, int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, double lat, double lng) {
			this.todoId = todoId;
			this.id = id;
			this.url = url;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.grade = grade;
			this.lat = lat;
			this.lng = lng;
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
		
		public double getLat() {
			return lat;
		}
		
		public double getLng() {
			return lng;
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
		
		public int getTodoId() {
			return todoId;
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
		
		public void setPartners(List<Partner> partners) {
			this.partners = partners;
		}
	}
	
	public class Sector {
		private final int id;
		private final String url;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<Problem> problems = new ArrayList<>();
		
		public Sector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public Problem addProblem(int todoId, int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, double lat, double lng) {
			Problem p = new Problem(todoId, id, url, lockedAdmin, lockedSuperadmin, nr, name, grade, lat, lng);
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

		public String getUrl() {
			return url;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	
	private final List<Area> areas = new ArrayList<>();
	
	public ProfileTodo() {
	}
	
	public Area addArea(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
		Area a = new Area(id, url, name, lockedAdmin, lockedSuperadmin);
		this.areas.add(a);
		return a;
	}

	public List<Area> getAreas() {
		return areas;
	}
}
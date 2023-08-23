package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Dangerous {
	public class DangerousProblem {
		private final int id;
		private final String url;
		private final String broken;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String grade;
		private final String postBy;
		private final String postWhen;
		private final String postTxt;
		
		public DangerousProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {
			this.id = id;
			this.url = url;
			this.broken = broken;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.grade = grade;
			this.postBy = postBy;
			this.postWhen = postWhen;
			this.postTxt = postTxt;
		}
		
		public String getBroken() {
			return broken;
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
		
		public String getPostBy() {
			return postBy;
		}
		
		public String getPostTxt() {
			return postTxt;
		}
		
		public String getPostWhen() {
			return postWhen;
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
	
	public class DangerousSector {
		private final int id;
		private final String url;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<DangerousProblem> problems = new ArrayList<>();
		
		public DangerousSector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public DangerousProblem addProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {
			DangerousProblem p = new DangerousProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, grade, postBy, postWhen, postTxt);
			this.problems.add(p);
			return p;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public List<DangerousProblem> getProblems() {
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
	
	private final int id;
	private final String url;
	private final String name;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final int sunFromHour;
	private final int sunToHour;
	private final List<DangerousSector> sectors = new ArrayList<>();
	
	public Dangerous(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour) {
		this.id = id;
		this.url = url;
		this.name = name;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.sunFromHour = sunFromHour;
		this.sunToHour = sunToHour;
	}
	
	public DangerousSector addSector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
		DangerousSector s = new DangerousSector(id, url, name, lockedAdmin, lockedSuperadmin);
		this.sectors.add(s);
		return s;
	}

	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public List<DangerousSector> getSectors() {
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
}
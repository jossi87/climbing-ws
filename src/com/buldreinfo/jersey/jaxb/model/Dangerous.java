package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Dangerous {
	public class Problem {
		private final int id;
		private final String url;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String grade;
		private final String postBy;
		private final String postWhen;
		private final String postTxt;
		
		public Problem(int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {
			this.id = id;
			this.url = url;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.grade = grade;
			this.postBy = postBy;
			this.postWhen = postWhen;
			this.postTxt = postTxt;
		}
		
		public int getId() {
			return id;
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
		
		public int getNr() {
			return nr;
		}
		
		public String getName() {
			return name;
		}
		
		public String getGrade() {
			return grade;
		}
		
		public String getPostBy() {
			return postBy;
		}
		
		public String getPostWhen() {
			return postWhen;
		}

		public String getPostTxt() {
			return postTxt;
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

		public Problem addProblem(int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {
			Problem p = new Problem(id, url, lockedAdmin, lockedSuperadmin, nr, name, grade, postBy, postWhen, postTxt);
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
	
	private final int id;
	private final String url;
	private final String name;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final List<Sector> sectors = new ArrayList<>();
	
	public Dangerous(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
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
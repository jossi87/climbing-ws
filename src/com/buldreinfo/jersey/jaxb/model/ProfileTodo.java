package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class ProfileTodo {
	public class ProfileTodoArea {
		private final int id;
		private final String url;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<ProfileTodoSector> sectors = new ArrayList<>();
		
		public ProfileTodoArea(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}
		
		public ProfileTodoSector addSector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			ProfileTodoSector s = new ProfileTodoSector(id, url, name, lockedAdmin, lockedSuperadmin);
			this.sectors.add(s);
			return s;
		}

		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public List<ProfileTodoSector> getSectors() {
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
	
	public class ProfileTodoPartner {
		private final int id;
		private final String name;
		
		public ProfileTodoPartner(int id, String name) {
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
	
	public class ProfileTodoProblem {
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
		private List<ProfileTodoPartner> partners = new ArrayList<>();
		
		public ProfileTodoProblem(int todoId, int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, double lat, double lng) {
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
		
		public ProfileTodoPartner addPartner(int id, String name) {
			ProfileTodoPartner res = new ProfileTodoPartner(id, name);
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
		
		public List<ProfileTodoPartner> getPartners() {
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
		
		public void setPartners(List<ProfileTodoPartner> partners) {
			this.partners = partners;
		}
	}
	
	public class ProfileTodoSector {
		private final int id;
		private final String url;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<ProfileTodoProblem> problems = new ArrayList<>();
		
		public ProfileTodoSector(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.url = url;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public ProfileTodoProblem addProblem(int todoId, int id, String url, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, double lat, double lng) {
			ProfileTodoProblem p = new ProfileTodoProblem(todoId, id, url, lockedAdmin, lockedSuperadmin, nr, name, grade, lat, lng);
			this.problems.add(p);
			return p;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public List<ProfileTodoProblem> getProblems() {
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
	
	private final List<ProfileTodoArea> areas = new ArrayList<>();
	
	public ProfileTodo() {
	}
	
	public ProfileTodoArea addArea(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
		ProfileTodoArea a = new ProfileTodoArea(id, url, name, lockedAdmin, lockedSuperadmin);
		this.areas.add(a);
		return a;
	}

	public List<ProfileTodoArea> getAreas() {
		return areas;
	}
}
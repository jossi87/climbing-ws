package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Todo {
	private final List<TodoSector> sectors = new ArrayList<>();
		
	public class TodoPartner {
		private final int id;
		private final String name;
		
		public TodoPartner(int id, String name) {
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
	
	public class TodoProblem {
		private final int id;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String grade;
		private List<TodoPartner> partners = new ArrayList<>();
		
		public TodoProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade) {
			this.id = id;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.grade = grade;
		}
		
		public TodoPartner addPartner(int id, String name) {
			TodoPartner res = new TodoPartner(id, name);
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
		
		public List<TodoPartner> getPartners() {
			return partners;
		}
		
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
		
		public void setPartners(List<TodoPartner> partners) {
			this.partners = partners;
		}
	}
	
	public class TodoSector {
		private final int id;
		private final String name;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final List<TodoProblem> problems = new ArrayList<>();
		
		public TodoSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
			this.id = id;
			this.name = name;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
		}

		public TodoProblem addProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade) {
			TodoProblem p = new TodoProblem(id, lockedAdmin, lockedSuperadmin, nr, name, grade);
			this.problems.add(p);
			return p;
		}
		
		public int getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public List<TodoProblem> getProblems() {
			return problems;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	
	public Todo() {
	}

	public List<TodoSector> getSectors() {
		return sectors;
	}
	
	public TodoSector addSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin) {
		TodoSector s = new TodoSector(id, name, lockedAdmin, lockedSuperadmin);
		this.sectors.add(s);
		return s;
	}
}
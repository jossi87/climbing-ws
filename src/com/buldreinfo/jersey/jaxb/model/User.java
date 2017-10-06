package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class User {
	public class Tick {
		private final int id;
		private final int idProblem;
		private final int visibility;
		private final String name;
		private final String comment;
		private final String date;
		private final double stars;
		private final boolean fa;
		private final String grade;

		public Tick(int id, int idProblem, int visibility, String name, String comment, String date, double stars, boolean fa, String grade) {
			this.id = id;
			this.idProblem = idProblem;
			this.visibility = visibility;
			this.name = name;
			this.comment = comment;
			this.date = date;
			this.stars = stars;
			this.fa = fa;
			this.grade = grade;
		}

		public String getComment() {
			return comment;
		}
		
		public String getDate() {
			return date;
		}

		public String getGrade() {
			return grade;
		}

		public int getId() {
			return id;
		}

		public int getIdProblem() {
			return idProblem;
		}

		public String getName() {
			return name;
		}

		public double getStars() {
			return stars;
		}

		public int getVisibility() {
			return visibility;
		}

		public boolean isFa() {
			return fa;
		}

		@Override
		public String toString() {
			return "Tick [id=" + id + ", idProblem=" + idProblem + ", visibility=" + visibility + ", name=" + name
					+ ", comment=" + comment + ", date=" + date + ", stars=" + stars + ", fa=" + fa + ", grade=" + grade
					+ "]";
		}
	}

	private final boolean readOnly;
	private final int id;
	private final String name;
	private final int numImagesCreated;
	private final int numVideosCreated;
	private final int numImageTags;
	private final int numVideoTags;
	private final List<Tick> ticks = new ArrayList<>();

	public User(boolean readOnly, int id, String name, int numImagesCreated, int numVideosCreated, int numImageTags, int numVideoTags) {
		this.readOnly = readOnly;
		this.id = id;
		this.name = name;
		this.numImagesCreated = numImagesCreated;
		this.numVideosCreated = numVideosCreated;
		this.numImageTags = numImageTags;
		this.numVideoTags = numVideoTags;
	}
	
	public void addTick(int id, int idProblem, int visibility, String name, String comment, String date, double stars, boolean fa, String grade) {
		ticks.add(new Tick(id, idProblem, visibility, name, comment, date, stars, fa, grade));
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNumImagesCreated() {
		return numImagesCreated;
	}
	
	public int getNumImageTags() {
		return numImageTags;
	}

	public int getNumVideosCreated() {
		return numVideosCreated;
	}

	public int getNumVideoTags() {
		return numVideoTags;
	}

	public List<Tick> getTicks() {
		return ticks;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public String toString() {
		return "User [readOnly=" + readOnly + ", id=" + id + ", name=" + name + ", numImagesCreated=" + numImagesCreated
				+ ", numVideosCreated=" + numVideosCreated + ", numImageTags=" + numImageTags + ", numVideoTags="
				+ numVideoTags + ", ticks=" + ticks + "]";
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Activity {
	public class Media {
		private final int id;
		private final boolean isMovie;
		public Media(int id, boolean isMovie) {
			super();
			this.id = id;
			this.isMovie = isMovie;
		}
		public int getId() {
			return id;
		}
		public boolean isMovie() {
			return isMovie;
		}
		@Override
		public String toString() {
			return "Media [id=" + id + ", isMovie=" + isMovie + "]";
		}
	}
	public class User {
		private final int id;
		private final String name;
		private final String picture;
		public User(int id, String name, String picture) {
			super();
			this.id = id;
			this.name = name;
			this.picture = picture;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public String getPicture() {
			return picture;
		}
		@Override
		public String toString() {
			return "User [id=" + id + ", name=" + name + ", picture=" + picture + "]";
		}
	}
	private final String timestamp;
	private final int problemId;
	private final int problemVisiblity;
	private final String problemName;
	private final int problemRandomMediaId;
	private final List<Media> media;
	private final int grade;
	private final int stars;
	private final String name;
	private final String picture;
	private final String description;
	private final List<User> users;
	public Activity(String timestamp, int problemId, int problemVisiblity, String problemName, int problemRandomMediaId,
			List<Media> media, int grade, int stars, String name, String picture, String description,
			List<User> users) {
		super();
		this.timestamp = timestamp;
		this.problemId = problemId;
		this.problemVisiblity = problemVisiblity;
		this.problemName = problemName;
		this.problemRandomMediaId = problemRandomMediaId;
		this.media = media;
		this.grade = grade;
		this.stars = stars;
		this.name = name;
		this.picture = picture;
		this.description = description;
		this.users = users;
	}
	public String getDescription() {
		return description;
	}
	public int getGrade() {
		return grade;
	}
	public List<Media> getMedia() {
		return media;
	}
	public String getName() {
		return name;
	}
	public String getPicture() {
		return picture;
	}
	public int getProblemId() {
		return problemId;
	}
	public String getProblemName() {
		return problemName;
	}
	public int getProblemRandomMediaId() {
		return problemRandomMediaId;
	}
	public int getProblemVisiblity() {
		return problemVisiblity;
	}
	public int getStars() {
		return stars;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public List<User> getUsers() {
		return users;
	}
	@Override
	public String toString() {
		return "Activity [timestamp=" + timestamp + ", problemId=" + problemId + ", problemVisiblity="
				+ problemVisiblity + ", problemName=" + problemName + ", problemRandomMediaId=" + problemRandomMediaId
				+ ", media=" + media + ", grade=" + grade + ", stars=" + stars + ", name=" + name + ", picture="
				+ picture + ", description=" + description + ", users=" + users + "]";
	}
}
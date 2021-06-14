package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Activity {
	public class Media {
		private final int id;
		private final boolean isMovie;
		private final String embedUrl;
		public Media(int id, boolean isMovie, String embedUrl) {
			super();
			this.id = id;
			this.isMovie = isMovie;
			this.embedUrl = embedUrl;
		}
		public String getEmbedUrl() {
			return embedUrl;
		}
		public int getId() {
			return id;
		}
		public boolean isMovie() {
			return isMovie;
		}
		@Override
		public String toString() {
			return "Media [id=" + id + ", isMovie=" + isMovie + ", embedUrl=" + embedUrl + "]";
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
	private final Set<Integer> activityIds;
	private final String timeAgo;
	private final int problemId;
	private final boolean problemLockedAdmin;
	private final boolean problemLockedSuperadmin;
	private final String problemName;
	private final String problemSubType;
	private String grade;
	private int problemRandomMediaId;
	private List<Media> media;
	private int stars;
	private int id;
	private String name;
	private String picture;
	private String description;
	private String message;
	private List<User> users;
	public Activity(Set<Integer> activityIds, String timeAgo, int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubType, String grade) {
		this.activityIds = activityIds;
		this.timeAgo = timeAgo;
		this.problemId = problemId;
		this.problemLockedAdmin = problemLockedAdmin;
		this.problemLockedSuperadmin = problemLockedSuperadmin;
		this.problemName = problemName;
		this.problemSubType = problemSubType;
		this.grade = grade;
	}
	public void addFa(String name, int userId, String picture, String description, int problemRandomMediaId) {
		if (this.users == null) {
			this.users = new ArrayList<>();
		}
		this.users.add(new User(userId>0? userId : 1049, name != null? name : "Unknown", picture));
		this.description = description;
		this.problemRandomMediaId = problemRandomMediaId;
	}
	public void addMedia(int id, boolean isMovie, String embedUrl) {
		if (this.media == null) {
			this.media = new ArrayList<>();
		}
		this.media.add(new Media(id, isMovie, embedUrl));
		if (!isMovie) {
			this.problemRandomMediaId = id;
		}
	}
	public Set<Integer> getActivityIds() {
		return activityIds;
	}
	public String getDescription() {
		return description;
	}
	public String getGrade() {
		return grade;
	}
	public int getId() {
		return id;
	}
	public String getMessage() {
		return message;
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
	public String getProblemSubType() {
		return problemSubType;
	}
	public int getStars() {
		return stars;
	}
	public String getTimeAgo() {
		return timeAgo;
	}
	public boolean isProblemLockedAdmin() {
		return problemLockedAdmin;
	}
	public boolean isProblemLockedSuperadmin() {
		return problemLockedSuperadmin;
	}
	public void setGuestbook(int id, String name, String picture, String message) {
		this.id = id;
		this.name = name;
		this.picture = picture;
		this.message = message;
	}
	public void setTick(int id, String name, String picture, String description, int stars, String personalGrade) {
		this.id = id;
		this.name = name;
		this.picture = picture;
		this.description = description;
		this.stars = stars;
		this.grade = personalGrade;
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Activity {
	private final Set<Integer> activityIds;
	private List<ActivityMedia> activityThumbnails;
	private final String timeAgo;
	private final int areaId;
	private final String areaName;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final int sectorId;
	private final String sectorName;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private final int problemId;
	private final boolean problemLockedAdmin;
	private final boolean problemLockedSuperadmin;
	private final String problemName;
	private final String problemSubtype;
	private String grade;
	private List<ActivityMedia> media;
	private int stars = -1;
	private boolean repeat;
	private int id;
	private String name;
	private String description;
	private String message;
	private List<User> users;
	public Activity(Set<Integer> activityIds, String timeAgo, int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin, int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade) {
		this.activityIds = activityIds;
		this.timeAgo = timeAgo;
		this.areaId = areaId;
		this.areaName = areaName;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.sectorId = sectorId;
		this.sectorName = sectorName;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.problemId = problemId;
		this.problemLockedAdmin = problemLockedAdmin;
		this.problemLockedSuperadmin = problemLockedSuperadmin;
		this.problemName = problemName;
		this.problemSubtype = problemSubtype;
		this.grade = grade;
	}
	public void addMedia(MediaIdentity identity, boolean isMovie, String embedUrl) {
		if (this.media == null) {
			this.media = new ArrayList<>();
		}
		this.media.add(new ActivityMedia(identity, isMovie, embedUrl));
	}
	public void addUser(int userId, String name, MediaIdentity identity) {
		if (this.users == null) {
			this.users = new ArrayList<>();
		}
		this.users.add(new User(userId>0? userId : 1049, name != null? name : "Unknown", identity));
	}
	public void appendActivityThumbnail(MediaIdentity identity) {
		if (this.activityThumbnails == null) {
			this.activityThumbnails = new ArrayList<>();
		}
		if (this.activityThumbnails.size() < 4 && this.activityThumbnails.stream()
				.filter(x -> x.identity().id() == identity.id())
				.findAny()
				.isEmpty()) {
			this.activityThumbnails.add(new ActivityMedia(identity, false, null));
		}
	}
	public Set<Integer> getActivityIds() {
		return activityIds;
	}
	public List<ActivityMedia> getActivityThumbnails() {
		return activityThumbnails;
	}
	public int getAreaId() {
		return areaId;
	}
	public String getAreaName() {
		return areaName;
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
	public List<ActivityMedia> getMedia() {
		return media;
	}
	public String getMessage() {
		return message;
	}
	public String getName() {
		return name;
	}
	public int getProblemId() {
		return problemId;
	}
	public String getProblemName() {
		return problemName;
	}
	public String getProblemSubtype() {
		return problemSubtype;
	}
	public int getSectorId() {
		return sectorId;
	}
	public String getSectorName() {
		return sectorName;
	}
	public int getStars() {
		return stars;
	}
	public String getTimeAgo() {
		return timeAgo;
	}
	public List<User> getUsers() {
		return users;
	}
	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}
	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}
	public boolean isProblemLockedAdmin() {
		return problemLockedAdmin;
	}
	public boolean isProblemLockedSuperadmin() {
		return problemLockedSuperadmin;
	}
	public boolean isRepeat() {
		return repeat;
	}
	public boolean isSectorLockedAdmin() {
		return sectorLockedAdmin;
	}
	public boolean isSectorLockedSuperadmin() {
		return sectorLockedSuperadmin;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setGuestbook(int id, String name, String message) {
		this.id = id;
		this.name = name;
		this.message = message;
	}
	public void setTick(boolean repeat, int id, String name, String description, int stars, String personalGrade) {
		this.repeat = repeat;
		this.id = id;
		this.name = name;
		this.description = description;
		this.stars = stars;
		this.grade = personalGrade;
	}
}
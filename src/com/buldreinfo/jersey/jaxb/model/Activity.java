package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;

public class Activity {
	private final Set<Integer> activityIds;
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
	private boolean noPersonalGrade;
	private int problemRandomMediaId;
	private long problemRandomMediaVersionStamp;
	private List<ActivityMedia> media;
	private int stars;
	private boolean repeat;
	private int id;
	private String name;
	private long avatarCrc32;
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
	public void addFa(String name, int userId, long avatarCrc32, String description, int problemRandomMediaId, long problemRandomMediaVersionStamp) {
		if (this.users == null) {
			this.users = new ArrayList<>();
		}
		this.users.add(new User(userId>0? userId : 1049, name != null? name : "Unknown", avatarCrc32));
		this.description = description;
		this.problemRandomMediaId = problemRandomMediaId;
		this.problemRandomMediaVersionStamp = problemRandomMediaVersionStamp;
	}
	public void addMedia(int id, long versionStamp, boolean isMovie, String embedUrl) {
		if (this.media == null) {
			this.media = new ArrayList<>();
		}
		this.media.add(new ActivityMedia(id, versionStamp, isMovie, embedUrl));
		if (!isMovie) {
			this.problemRandomMediaId = id;
		}
	}
	public Set<Integer> getActivityIds() {
		return activityIds;
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
	public long getAvatarCrc32() {
		return avatarCrc32;
	}
	public int getProblemId() {
		return problemId;
	}
	public String getProblemName() {
		return problemName;
	}
	public long getProblemRandomMediaVersionStamp() {
		return problemRandomMediaVersionStamp;
	}
	public int getProblemRandomMediaId() {
		return problemRandomMediaId;
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
	public boolean isNoPersonalGrade() {
		return noPersonalGrade;
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
	public void setGuestbook(int id, String name, long avatarCrc32, String message) {
		this.id = id;
		this.name = name;
		this.avatarCrc32 = avatarCrc32;
		this.message = message;
	}
	public void setTick(boolean repeat, int id, String name, long avatarCrc32, String description, int stars, String personalGrade) {
		this.repeat = repeat;
		this.id = id;
		this.name = name;
		this.avatarCrc32 = avatarCrc32;
		this.description = description;
		this.stars = stars;
		if (GradeConverter.NO_PERSONAL_GRADE.equals(personalGrade)) {
			this.noPersonalGrade = true;
		}
		else {
			this.grade = personalGrade;
		}
	}
}
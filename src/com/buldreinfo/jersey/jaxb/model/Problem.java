package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Problem {
	private final String redirectUrl;
	private final int areaId;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final String areaName;
	private final String areaAccessInfo;
	private final String areaAccessClosed;
	private final boolean areaNoDogsAllowed;
	private final int areaSunFromHour;
	private final int areaSunToHour;
	private final int sectorId;
	private final boolean sectorLockedAdmin;
	private final boolean sectorLockedSuperadmin;
	private final String sectorName;
	private final String sectorAccessInfo;
	private final String sectorAccessClosed;
	private final int sectorSunFromHour;
	private final int sectorSunToHour;
	private final Coordinates sectorParking;
	private final List<Coordinates> sectorOutline;
	private final CompassDirection sectorWallDirectionCalculated;
	private final CompassDirection sectorWallDirectionManual;
	private final Slope sectorApproach;
	private final Slope sectorDescent;
	private final SectorProblem neighbourPrev;
	private final SectorProblem neighbourNext;
	private final String canonical;
	private final int id;
	private final String broken;
	private final boolean trash;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final int nr;
	private final String name;
	private final String rock;
	private final String comment;
	private final String grade;
	private final String originalGrade;
	private final String faDate;
	private final String faDateHr;
	private final List<User> fa;
	private Coordinates coordinates;
	private final List<Media> media;
	private final int numTicks;
	private final double stars;
	private final boolean ticked;
	private List<ProblemTick> ticks;
	private List<ProblemTodo> todos;
	private List<ProblemComment> comments;
	private final List<NewMedia> newMedia;
	private final Type t;
	private List<ProblemSection> sections;
	private final boolean todo;
	private final List<ExternalLink> externalLinks;
	private final String pageViews;
	private FaAid faAid;
	private final String trivia;
	private final List<Media> triviaMedia;
	private final String startingAltitude;
	private final String aspect;
	private final String routeLength;
	private final String descent;
	
	public Problem(String redirectUrl, int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaName, String areaAccessInfo, String areaAccessClosed, boolean areaNoDogsAllowed, int areaSunFromHour, int areaSunToHour,
			int sectorId, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, String sectorName, String sectorAccessInfo, String sectorAccessClosed,
			int sectorSunFromHour, int sectorSunToHour,
			Coordinates sectorParking, List<Coordinates> sectorOutline, CompassDirection sectorWallDirectionCalculated, CompassDirection sectorWallDirectionManual, Slope sectorApproach, Slope sectorDescent, SectorProblem neighbourPrev, SectorProblem neighbourNext, String canonical, int id, String broken, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, String grade, String originalGrade, String faDate, String faDateHr, List<User> fa, Coordinates coordinates, List<Media> media, int numTics, double stars, boolean ticked, List<NewMedia> newMedia, Type t, boolean todo, List<ExternalLink> externalLinks, String pageViews,
			String trivia, List<Media> triviaMedia, String startingAltitude, String aspect, String routeLength, String descent) {
		this.redirectUrl = redirectUrl;
		this.areaId = areaId;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.areaName = areaName;
		this.areaAccessInfo = areaAccessInfo;
		this.areaAccessClosed = areaAccessClosed;
		this.areaNoDogsAllowed = areaNoDogsAllowed;
		this.areaSunFromHour = areaSunFromHour;
		this.areaSunToHour = areaSunToHour;
		this.sectorId = sectorId;
		this.sectorLockedAdmin = sectorLockedAdmin;
		this.sectorLockedSuperadmin = sectorLockedSuperadmin;
		this.sectorName = sectorName;
		this.sectorAccessInfo = sectorAccessInfo;
		this.sectorAccessClosed = sectorAccessClosed;
		this.sectorSunFromHour = sectorSunFromHour;
		this.sectorSunToHour = sectorSunToHour;
		this.sectorParking = sectorParking;
		this.sectorOutline = sectorOutline;
		this.sectorWallDirectionCalculated = sectorWallDirectionCalculated;
		this.sectorWallDirectionManual = sectorWallDirectionManual;
		this.sectorApproach = sectorApproach;
		this.sectorDescent = sectorDescent;
		this.neighbourPrev = neighbourPrev;
		this.neighbourNext = neighbourNext;
		this.canonical = canonical;
		this.id = id;
		this.broken = broken;
		this.trash = trash;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.nr = nr;
		this.name = name;
		this.rock = rock;
		this.comment = comment;
		this.grade = grade;
		this.originalGrade = originalGrade;
		this.faDate = faDate;
		this.faDateHr = faDateHr;
		this.fa = fa;
		this.coordinates = coordinates;
		this.media = media;
		this.numTicks = numTics;
		this.stars = stars;
		this.ticked = ticked;
		this.newMedia = newMedia;
		this.t = t;
		this.todo = todo;
		this.externalLinks = externalLinks;
		this.pageViews = pageViews;
		this.trivia = trivia;
		this.triviaMedia = triviaMedia;
		this.startingAltitude = startingAltitude;
		this.aspect = aspect;
		this.routeLength = routeLength;
		this.descent = descent;
	}
	
	public ProblemComment addComment(int id, String date, int idUser, int mediaId, long mediaVersionStamp, String name, String message, boolean danger, boolean resolved, List<Media> media) {
		if (comments == null) {
			comments = new ArrayList<>();
		}
		ProblemComment comment = new ProblemComment(id, date, idUser, mediaId, mediaVersionStamp, name, message, danger, resolved, media);
		comments.add(comment);
		return comment;
	}
	
	public void addSection(int id, int nr, String description, String grade, List<Media> media) {
		if (sections == null) {
			sections = new ArrayList<>();
		}
		sections.add(new ProblemSection(id, nr, description, grade, media));
	}
	
	public ProblemTick addTick(int id, int idUser, int mediaId, long mediaVersionStamp, String date, String name, String suggestedGrade, boolean noPersonalGrade, String comment, double stars, boolean writable) {
		if (ticks == null) {
			ticks = new ArrayList<>();
		}
		ProblemTick t = new ProblemTick(id, idUser, mediaId, mediaVersionStamp, date, name, suggestedGrade, noPersonalGrade, comment, stars, writable);
		ticks.add(t);
		return t;
	}
	
	public void addTodo(int idUser, int mediaId, long mediaVersionStamp, String name) {
		if (todos == null) {
			todos = new ArrayList<>();
		}
		todos.add(new ProblemTodo(idUser, mediaId, mediaVersionStamp, name));
	}
	
	public String getAreaAccessClosed() {
		return areaAccessClosed;
	}
	
	public String getAreaAccessInfo() {
		return areaAccessInfo;
	}
	
	public int getAreaId() {
		return areaId;
	}
	
	public String getAreaName() {
		return areaName;
	}
	
	public int getAreaSunFromHour() {
		return areaSunFromHour;
	}
	
	public int getAreaSunToHour() {
		return areaSunToHour;
	}
	
	public String getAspect() {
		return aspect;
	}
	
	public String getBroken() {
		return broken;
	}
	
	public String getCanonical() {
		return canonical;
	}
	
	public String getComment() {
		return comment;
	}
	
	public List<ProblemComment> getComments() {
		return comments;
	}
	
	public Coordinates getCoordinates() {
		return coordinates;
	}
	
	public String getDescent() {
		return descent;
	}
	
	public List<ExternalLink> getExternalLinks() {
		return externalLinks;
	}
	
	public List<User> getFa() {
		return fa;
	}
	
	public FaAid getFaAid() {
		return faAid;
	}
	
	public String getFaDate() {
		return faDate;
	}
	
	public String getFaDateHr() {
		return faDateHr;
	}
	
	public String getGrade() {
		return grade;
	}
	
	public int getId() {
		return id;
	}
	
	public List<Media> getMedia() {
		return media;
	}
	
	public String getName() {
		return name;
	}
	
	public SectorProblem getNeighbourNext() {
		return neighbourNext;
	}
	
	public SectorProblem getNeighbourPrev() {
		return neighbourPrev;
	}

	public List<NewMedia> getNewMedia() {
		return newMedia;
	}
	
	public int getNr() {
		return nr;
	}
	
	public int getNumTicks() {
		return numTicks;
	}
	
	public String getOriginalGrade() {
		return originalGrade;
	}
	
	public String getPageViews() {
		return pageViews;
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
	}
	
	public String getRock() {
		return rock;
	}

	public String getRouteLength() {
		return routeLength;
	}
	
	public List<ProblemSection> getSections() {
		return sections;
	}
	
	public String getSectorAccessClosed() {
		return sectorAccessClosed;
	}
	
	public String getSectorAccessInfo() {
		return sectorAccessInfo;
	}

	public Slope getSectorApproach() {
		return sectorApproach;
	}
	
	public Slope getSectorDescent() {
		return sectorDescent;
	}
	
	public int getSectorId() {
		return sectorId;
	}
	
	public String getSectorName() {
		return sectorName;
	}
	
	public List<Coordinates> getSectorOutline() {
		return sectorOutline;
	}

	public Coordinates getSectorParking() {
		return sectorParking;
	}
	
	public int getSectorSunFromHour() {
		return sectorSunFromHour;
	}

	public int getSectorSunToHour() {
		return sectorSunToHour;
	}
	
	public CompassDirection getSectorWallDirectionCalculated() {
		return sectorWallDirectionCalculated;
	}
	
	public CompassDirection getSectorWallDirectionManual() {
		return sectorWallDirectionManual;
	}
	
	public double getStars() {
		return stars;
	}

	public String getStartingAltitude() {
		return startingAltitude;
	}

	public Type getT() {
		return t;
	}

	public List<ProblemTick> getTicks() {
		return ticks;
	}

	public List<ProblemTodo> getTodos() {
		return todos;
	}

	public String getTrivia() {
		return trivia;
	}
	
	public List<Media> getTriviaMedia() {
		return triviaMedia;
	}

	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}

	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}
	
	public boolean isAreaNoDogsAllowed() {
		return areaNoDogsAllowed;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}

	public boolean isSectorLockedAdmin() {
		return sectorLockedAdmin;
	}
	
	public boolean isSectorLockedSuperadmin() {
		return sectorLockedSuperadmin;
	}
	
	public boolean isTicked() {
		return ticked;
	}

	public boolean isTodo() {
		return todo;
	}

	public boolean isTrash() {
		return trash;
	}
	
	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}

	public void setFaAid(FaAid faAid) {
		this.faAid = faAid;
	}
}
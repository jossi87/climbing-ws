package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.SectorSort;

public class Sector {
	private final String redirectUrl;
	private final boolean orderByGrade;
	private final int areaId;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final String areaAccessInfo;
	private final String areaAccessClosed;
	private final boolean areaNoDogsAllowed;
	private final int areaSunFromHour;
	private final int areaSunToHour;
	private final String areaName;
	private final String canonical;
	private final int id;
	private final boolean trash;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final String name;
	private final String comment;
	private final String accessInfo;
	private final String accessClosed;
	private final int sunFromHour;
	private final int sunToHour;
	private Coordinates parking;
	private final List<Coordinates> outline;
	private final CompassDirection wallDirectionCalculated;
	private final CompassDirection wallDirectionManual;
	private final Slope approach;
	private final Slope descent;
	private final List<Media> media;
	private final List<Media> triviaMedia;
	private final List<SectorJump> sectors = new ArrayList<>();
	private final List<SectorProblem> problems = new ArrayList<>();
	private final List<SectorProblemOrder> problemOrder = new ArrayList<>();
	private final List<NewMedia> newMedia;
	private final List<ExternalLink> externalLinks;
	private final String pageViews;
	
	public Sector(String redirectUrl, boolean orderByGrade, int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaAccessInfo, String areaAccessClosed, boolean areaNoDogsAllowed, int areaSunFromHour, int areaSunToHour, String areaName, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, int sunFromHour, int sunToHour, Coordinates parking, List<Coordinates> outline, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, Slope approach, Slope descent, List<Media> media, List<Media> triviaMedia, List<NewMedia> newMedia, List<ExternalLink> externalLinks, String pageViews) {
		this.redirectUrl = redirectUrl;
		this.orderByGrade = orderByGrade;
		this.areaId = areaId;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.areaAccessInfo = areaAccessInfo;
		this.areaAccessClosed = areaAccessClosed;
		this.areaNoDogsAllowed = areaNoDogsAllowed;
		this.areaSunFromHour = areaSunFromHour;
		this.areaSunToHour = areaSunToHour;
		this.areaName = areaName;
		this.canonical = canonical;
		this.id = id;
		this.trash = trash;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin; 
		this.name = name;
		this.comment = comment;
		this.accessInfo = accessInfo;
		this.accessClosed = accessClosed;
		this.sunFromHour = sunFromHour;
		this.sunToHour = sunToHour;
		this.parking = parking;
		this.outline = outline;
		this.wallDirectionCalculated = wallDirectionCalculated;
		this.wallDirectionManual = wallDirectionManual;
		this.approach = approach;
		this.descent = descent;
		this.media = media;
		this.triviaMedia = triviaMedia;
		this.newMedia = newMedia;
		this.externalLinks = externalLinks;
		this.pageViews = pageViews;
	}
	
	public void addProblem(SectorProblem sp) {
		this.problems.add(sp);
		this.problemOrder.add(new SectorProblemOrder(sp.id(), sp.name(), sp.nr()));
	}
	
	public void addSector(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, int sorting) {
		this.sectors.add(new SectorJump(id, lockedAdmin, lockedSuperadmin, name, sorting));
	}
	
	public String getAccessClosed() {
		return accessClosed;
	}
	
	public String getAccessInfo() {
		return accessInfo;
	}
	
	public Slope getApproach() {
		return approach;
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
	
	public String getCanonical() {
		return canonical;
	}
	
	public String getComment() {
		return comment;
	}
	
	public Slope getDescent() {
		return descent;
	}
	
	public List<ExternalLink> getExternalLinks() {
		return externalLinks;
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
	
	public List<NewMedia> getNewMedia() {
		return newMedia;
	}
	
	public List<Coordinates> getOutline() {
		return outline;
	}
	
	public String getPageViews() {
		return pageViews;
	}
	
	public Coordinates getParking() {
		return parking;
	}
	
	public List<SectorProblemOrder> getProblemOrder() {
		return problemOrder;
	}
	
	public List<SectorProblem> getProblems() {
		return problems;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public List<SectorJump> getSectors() {
		return sectors;
	}
	
	public List<SectorJump> getSiblings() {
		return sectors;
	}
	
	public int getSunFromHour() {
		return sunFromHour;
	}
	
	public int getSunToHour() {
		return sunToHour;
	}
	
	public List<Media> getTriviaMedia() {
		return triviaMedia;
	}
	
	public CompassDirection getWallDirectionCalculated() {
		return wallDirectionCalculated;
	}
	
	public CompassDirection getWallDirectionManual() {
		return wallDirectionManual;
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
	
	public boolean isOrderByGrade() {
		return orderByGrade;
	}
	
	public boolean isTrash() {
		return trash;
	}
	
	public void orderSectors() {
		if (sectors != null) {
			sectors.sort((SectorJump o1, SectorJump o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()));
		}
	}
	
	public void setParking(Coordinates parking) {
		this.parking = parking;
	}
}
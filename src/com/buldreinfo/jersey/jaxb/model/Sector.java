package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.SectorSort;

public class Sector {
	public class SectorJump {
		private final int id;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final String name;
		private final int sorting;
		public SectorJump(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, int sorting) {
			this.id = id;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.sorting = sorting;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public int getSorting() {
			return sorting;
		}
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	public class SectorProblemOrder {
		private final int id;
		private final String name;
		private final int nr;
		public SectorProblemOrder(int id, String name, int nr) {
			this.id = id;
			this.name = name;
			this.nr = nr;
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
	}
	
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
	private final double lat;
	private final double lng;
	private final List<Coordinate> outline;
	private final String wallDirection;
	private final String polyline;
	private final List<Media> media;
	private final List<Media> triviaMedia;
	private final List<SectorJump> sectors = new ArrayList<>();
	private final List<SectorProblem> problems = new ArrayList<>();
	private final List<SectorProblemOrder> problemOrder = new ArrayList<>();
	private final List<NewMedia> newMedia;
	private final long hits;
	
	public Sector(String redirectUrl, boolean orderByGrade, int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String areaAccessInfo, String areaAccessClosed, boolean areaNoDogsAllowed, int areaSunFromHour, int areaSunToHour, String areaName, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, double lat, double lng, List<Coordinate> outline, String wallDirection, String polyline, List<Media> media, List<Media> triviaMedia, List<NewMedia> newMedia, long hits) {
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
		this.lat = lat;
		this.lng = lng;
		this.outline = outline;
		this.wallDirection = wallDirection;
		this.polyline = polyline;
		this.media = media;
		this.triviaMedia = triviaMedia;
		this.newMedia = newMedia;
		this.hits = hits;
	}
	
	public void addProblem(SectorProblem sp) {
		this.problems.add(sp);
		this.problemOrder.add(new SectorProblemOrder(sp.getId(), sp.getName(), sp.getNr()));
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
	
	public long getHits() {
		return hits;
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
	
	public List<Media> getMedia() {
		return media;
	}
	
	public String getName() {
		return name;
	}
	
	public List<NewMedia> getNewMedia() {
		return newMedia;
	}
	
	public List<Coordinate> getOutline() {
		return outline;
	}
	
	public String getPolyline() {
		return polyline;
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
	
	public List<Media> getTriviaMedia() {
		return triviaMedia;
	}
	
	public String getWallDirection() {
		return wallDirection;
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
			sectors.sort((SectorJump o1, SectorJump o2) -> SectorSort.sortSector(o1.getSorting(), o1.getName(), o2.getSorting(), o2.getName()));
		}
	}
}
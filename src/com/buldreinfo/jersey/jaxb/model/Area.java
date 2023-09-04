package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.SectorSort;

public class Area {
	public class AreaSector {
		private final String areaName;
		private final int id;
		private final int sorting;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final String name;
		private final String comment;
		private final String accessInfo;
		private final String accessClosed;
		private final Coordinate parking;
		private List<Coordinate> outline;
		private final String wallDirection;
		private final String polyline;
		private final int randomMediaId;
		private final int randomMediaCrc32;
		private final List<SectorProblem> problems = new ArrayList<>();
		private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();

		public AreaSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, Coordinate parking, String wallDirection, String polyline, int randomMediaId, int randomMediaCrc32) {
			this.areaName = null;
			this.id = id;
			this.sorting = sorting;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.comment = comment;
			this.accessInfo = accessInfo;
			this.accessClosed = accessClosed;
			this.parking = parking;
			this.wallDirection = wallDirection;
			this.polyline = polyline;
			this.randomMediaId = randomMediaId;
			this.randomMediaCrc32 = randomMediaCrc32;
		}

		public String getAccessClosed() {
			return accessClosed;
		}

		public String getAccessInfo() {
			return accessInfo;
		}

		public String getAreaName() {
			return areaName;
		}

		public String getComment() {
			return comment;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getNumProblems() {
			return numProblems;
		}

		public List<Coordinate> getOutline() {
			return outline;
		}
		
		public Coordinate getParking() {
			return parking;
		}

		public String getPolyline() {
			return polyline;
		}

		public List<SectorProblem> getProblems() {
			return problems;
		}

		public int getRandomMediaCrc32() {
			return randomMediaCrc32;
		}

		public int getRandomMediaId() {
			return randomMediaId;
		}

		public int getSorting() {
			return sorting;
		}

		public List<TypeNumTicked> getTypeNumTicked() {
			return typeNumTicked;
		}

		public String getWallDirection() {
			return wallDirection;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}

		public void setOutline(List<Coordinate> outline) {
			this.outline = outline;
		}
	}

	public class AreaSectorOrder {
		private final int id;
		private final String name;
		private final int sorting;
		public AreaSectorOrder(int id, String name, int sorting) {
			this.id = id;
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
	}

	private final String redirectUrl;
	private final int regionId;
	private final String canonical;
	private final int id;
	private final boolean trash;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final boolean forDevelopers;
	private final String accessInfo;
	private final String accessClosed;
	private final boolean noDogsAllowed;
	private final int sunFromHour;
	private final int sunToHour;
	private final String name;
	private final String comment;
	private final Coordinate coordinate;
	private final int numSectors;
	private final int numProblems;
	private final List<AreaSector> sectors;
	private final List<AreaSectorOrder> sectorOrder;
	private final List<Media> media;
	private final List<Media> triviaMedia;
	private final List<NewMedia> newMedia;
	private final long hits;
	private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();

	public Area(String redirectUrl, int regionId, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers, String accessInfo, String accessClosed, boolean noDogsAllowed, int sunFromHour, int sunToHour, String name, String comment, Coordinate coordinate, int numSectors, int numProblems, List<Media> media, List<Media> triviaMedia, List<NewMedia> newMedia, long hits) {
		this.redirectUrl = redirectUrl;
		this.regionId = regionId;
		this.canonical = canonical;
		this.id = id;
		this.trash = trash;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.forDevelopers = forDevelopers;
		this.accessInfo = accessInfo;
		this.accessClosed = accessClosed;
		this.noDogsAllowed = noDogsAllowed;
		this.sunFromHour = sunFromHour;
		this.sunToHour = sunToHour;
		this.name = name;
		this.comment = comment;
		this.coordinate = coordinate;
		this.numSectors = numSectors;
		this.numProblems = numProblems;
		this.sectors = numSectors == -1? new ArrayList<>() : null;
		this.sectorOrder = numSectors == -1? new ArrayList<>() : null;
		this.media = media;
		this.triviaMedia = triviaMedia;
		this.newMedia = newMedia;
		this.hits = hits;
	}

	public AreaSector addSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, Coordinate parking, String wallDirection, String polyline, int randomMediaId, int randomMediaCrc32) {
		AreaSector s = new AreaSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, parking, wallDirection, polyline, randomMediaId, randomMediaCrc32);
		sectors.add(s);
		sectorOrder.add(new AreaSectorOrder(id, name, sorting));
		return s;
	}

	public String getAccessClosed() {
		return accessClosed;
	}

	public String getAccessInfo() {
		return accessInfo;
	}

	public String getCanonical() {
		return canonical;
	}

	public String getComment() {
		return comment;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public long getHits() {
		return hits;
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

	public int getNumProblems() {
		return numProblems;
	}

	public int getNumSectors() {
		return numSectors;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public int getRegionId() {
		return regionId;
	}

	public List<AreaSectorOrder> getSectorOrder() {
		return sectorOrder;
	}

	public List<AreaSector> getSectors() {
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

	public List<TypeNumTicked> getTypeNumTicked() {
		return typeNumTicked;
	}

	public boolean isForDevelopers() {
		return forDevelopers;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}

	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}

	public boolean isNoDogsAllowed() {
		return noDogsAllowed;
	}

	public boolean isTrash() {
		return trash;
	}

	public void orderSectors() {
		if (sectors != null) {
			sectors.sort((AreaSector o1, AreaSector o2) -> SectorSort.sortSector(o1.getSorting(), o1.getName(), o2.getSorting(), o2.getName()));
		}
	}
}
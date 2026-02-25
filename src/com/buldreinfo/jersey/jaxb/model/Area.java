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
		private final int sunFromHour;
		private final int sunToHour;
		private final Coordinates parking;
		private List<Coordinates> outline;
		private final CompassDirection wallDirectionCalculated;
		private final CompassDirection wallDirectionManual;
		private Slope approach;
		private Slope descent;
		private final int randomMediaId;
		private final long randomMediaVersionStamp;
		private final List<SectorProblem> problems = new ArrayList<>();
		private final List<TypeNumTickedTodo> typeNumTickedTodo = new ArrayList<>();

		public AreaSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, int sunFromHour, int sunToHour, Coordinates parking, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, int randomMediaId, long randomMediaVersionStamp) {
			this.areaName = null;
			this.id = id;
			this.sorting = sorting;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.comment = comment;
			this.accessInfo = accessInfo;
			this.accessClosed = accessClosed;
			this.sunFromHour = sunFromHour;
			this.sunToHour = sunToHour;
			this.parking = parking;
			this.wallDirectionCalculated = wallDirectionCalculated;
			this.wallDirectionManual = wallDirectionManual;
			this.randomMediaId = randomMediaId;
			this.randomMediaVersionStamp = randomMediaVersionStamp;
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
		
		public String getAreaName() {
			return areaName;
		}
		
		public String getComment() {
			return comment;
		}
		
		public Slope getDescent() {
			return descent;
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

		public List<Coordinates> getOutline() {
			return outline;
		}

		public Coordinates getParking() {
			return parking;
		}
		
		public List<SectorProblem> getProblems() {
			return problems;
		}

		public long getRandomMediaVersionStamp() {
			return randomMediaVersionStamp;
		}
		
		public int getRandomMediaId() {
			return randomMediaId;
		}

		public int getSorting() {
			return sorting;
		}

		public int getSunFromHour() {
			return sunFromHour;
		}

		public int getSunToHour() {
			return sunToHour;
		}

		public List<TypeNumTickedTodo> getTypeNumTickedTodo() {
			return typeNumTickedTodo;
		}

		public CompassDirection getWallDirectionCalculated() {
			return wallDirectionCalculated;
		}
		
		public CompassDirection getWallDirectionManual() {
			return wallDirectionManual;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}

		public void setApproach(Slope approach) {
			this.approach = approach;
		}
		
		public void setDescent(Slope descent) {
			this.descent = descent;
		}

		public void setOutline(List<Coordinates> outline) {
			this.outline = outline;
		}
	}
	public record AreaSectorOrder(int id, String name, int sorting) {}
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
	private Coordinates coordinates;
	private final int numSectors;
	private final int numProblems;
	private final List<AreaSector> sectors;
	private final List<AreaSectorOrder> sectorOrder;
	private final List<Media> media;
	private final List<Media> triviaMedia;
	private final List<NewMedia> newMedia;
	private final List<ExternalLink> externalLinks;
	private final String pageViews;
	private final List<TypeNumTickedTodo> typeNumTickedTodo = new ArrayList<>();

	public Area(String redirectUrl, int regionId, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers, String accessInfo, String accessClosed, boolean noDogsAllowed, int sunFromHour, int sunToHour, String name, String comment, Coordinates coordinates, int numSectors, int numProblems, List<Media> media, List<Media> triviaMedia, List<NewMedia> newMedia, List<ExternalLink> externalLinks, String pageViews) {
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
		this.coordinates = coordinates;
		this.numSectors = numSectors;
		this.numProblems = numProblems;
		this.sectors = numSectors == -1? new ArrayList<>() : null;
		this.sectorOrder = numSectors == -1? new ArrayList<>() : null;
		this.media = media;
		this.triviaMedia = triviaMedia;
		this.newMedia = newMedia;
		this.externalLinks = externalLinks;
		this.pageViews = pageViews;
	}

	public AreaSector addSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, int sunFromHour, int sunToHour, Coordinates parking, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, int randomMediaId, long randomMediaVersionStamp) {
		AreaSector s = new AreaSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, wallDirectionCalculated, wallDirectionManual, randomMediaId, randomMediaVersionStamp);
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
	
	public Coordinates getCoordinates() {
		return coordinates;
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

	public int getNumProblems() {
		return numProblems;
	}

	public int getNumSectors() {
		return numSectors;
	}

	public String getPageViews() {
		return pageViews;
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
	
	public List<TypeNumTickedTodo> getTypeNumTickedTodo() {
		return typeNumTickedTodo;
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

	public void setCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
	}
}
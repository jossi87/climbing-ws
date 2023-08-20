package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
		private final double lat;
		private final double lng;
		private final String polygonCoords;
		private final String polyline;
		private final int randomMediaId;
		private final int randomMediaCrc32;
		private final List<SectorProblem> problems = new ArrayList<>();
		private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();
		
		public AreaSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, double lat, double lng, String polygonCoords, String polyline, int randomMediaId, int randomMediaCrc32) {
			this.areaName = null;
			this.id = id;
			this.sorting = sorting;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.comment = comment;
			this.accessInfo = accessInfo;
			this.accessClosed = accessClosed;
			this.lat = lat;
			this.lng = lng;
			this.polygonCoords = polygonCoords;
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
		
		public double getLat() {
			return lat;
		}

		public double getLng() {
			return lng;
		}
		
		public String getName() {
			return name;
		}
		
		public int getNumProblems() {
			return numProblems;
		}
		
		public String getPolygonCoords() {
			return polygonCoords;
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
		
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
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
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final int numSectors;
	private final int numProblems;
	private final List<AreaSector> sectors;
	private final List<AreaSectorOrder> sectorOrder;
	private final List<Media> media;
	private final List<Media> triviaMedia;
	private final List<NewMedia> newMedia;
	private final long hits;
	private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();
	
	public Area(String redirectUrl, int regionId, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers, String accessInfo, String accessClosed, boolean noDogsAllowed, String name, String comment, double lat, double lng, int numSectors, int numProblems, List<Media> media, List<Media> triviaMedia, List<NewMedia> newMedia, long hits) {
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
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
		this.numSectors = numSectors;
		this.numProblems = numProblems;
		this.sectors = numSectors == -1? new ArrayList<>() : null;
		this.sectorOrder = numSectors == -1? new ArrayList<>() : null;
		this.media = media;
		this.triviaMedia = triviaMedia;
		this.newMedia = newMedia;
		this.hits = hits;
	}
	
	public AreaSector addSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, String accessClosed, double lat, double lng, String polygonCoords, String polyline, int randomMediaId, int randomMediaCrc32) {
		AreaSector s = new AreaSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, lat, lng, polygonCoords, polyline, randomMediaId, randomMediaCrc32);
		sectors.add(s);
		sectorOrder.add(new AreaSectorOrder(id, name, sorting));
		return s;
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
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
	
	public int getNumProblems() {
		return numProblems;
	}
	
	public int getNumSectors() {
		return numSectors;
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
			sectors.sort(new Comparator<AreaSector>() {
				@Override
				public int compare(AreaSector o1, AreaSector o2) {
					return getName(o1).compareTo(getName(o2));
				}
				private String getName(AreaSector s) {
					if (s.getSorting() > 0) {
						return String.format("%04d", s.getSorting());
					}
					if (s.getName().toLowerCase().contains("vestre")) {
						return s.getName().toLowerCase();
					}
					return s.getName().toLowerCase()
							.replace("første", "1første")
							.replace("sør", "1sør")
							.replace("vest", "1vest")
							.replace("venstre", "1venstre")
							.replace("andre", "2andre")
							.replace("midt", "2midt")
							.replace("tredje", "3tredje")
							.replace("hoved", "3hoved")
							.replace("fjerde", "4fjerde")
							.replace("høyre", "4høyre")
							.replace("øst", "5øst")
							.replace("nord", "6nord");
				}
			});
		}
	}
}
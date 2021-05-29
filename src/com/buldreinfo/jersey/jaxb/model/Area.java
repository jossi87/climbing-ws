package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Area implements IMetadata {
	public class Sector {
		private final String areaName;
		private final int id;
		private final int sorting;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final String name;
		private final String comment;
		private final double lat;
		private final double lng;
		private final String polygonCoords;
		private final String polyline;
		private final int randomMediaId;
		private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();
		
		public Sector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, double lat, double lng, String polygonCoords, String polyline, int randomMediaId) {
			this.areaName = null;
			this.id = id;
			this.sorting = sorting;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.comment = comment;
			this.lat = lat;
			this.lng = lng;
			this.polygonCoords = polygonCoords;
			this.polyline = polyline;
			this.randomMediaId = randomMediaId;
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
	
	private final int regionId;
	private final String canonical;
	private final int id;
	private final boolean trash;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final boolean forDevelopers;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final int numSectors;
	private final int numProblems;
	private final List<Sector> sectors;
	private final List<Media> media;
	private final List<NewMedia> newMedia;
	private final long hits;
	private final List<TypeNumTicked> typeNumTicked = new ArrayList<>();
	private Metadata metadata;
	
	public Area(int regionId, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers, String name, String comment, double lat, double lng, int numSectors, int numProblems, List<Media> media, List<NewMedia> newMedia, long hits) {
		this.regionId = regionId;
		this.canonical = canonical;
		this.id = id;
		this.trash = trash;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin;
		this.forDevelopers = forDevelopers;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
		this.numSectors = numSectors;
		this.numProblems = numProblems;
		this.sectors = numSectors == -1? new ArrayList<>() : null;
		this.media = media;
		this.newMedia = newMedia;
		this.hits = hits;
	}

	public void addSector(int id, int sorting, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, double lat, double lng, String polygonCoords, String polyline, int randomMediaId) {
		sectors.add(new Sector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, lat, lng, polygonCoords, polyline, randomMediaId));
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
	
	public boolean isTrash() {
		return trash;
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
	
	@Override
	public Metadata getMetadata() {
		return metadata;
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

	public List<Sector> getSectors() {
		return sectors;
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

	public void orderSectors() {
		if (sectors != null) {
			sectors.sort(new Comparator<Sector>() {
				@Override
				public int compare(Sector o1, Sector o2) {
					return getName(o1).compareTo(getName(o2));
				}
				private String getName(Sector s) {
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
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
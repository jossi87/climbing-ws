package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Area {
	public class Sector {
		private final int areaId;
		private final int areaVisibility;
		private final String areaName;
		private final int id;
		private final int visibility;
		private final String name;
		private final String comment;
		private final double lat;
		private final double lng;
		private final String polygonCoords;
		private final int numProblems;
		
		public Sector(int id, int visibility, String name, String comment, double lat, double lng, String polygonCoords, int numProblems) {
			this.areaId = -1;
			this.areaName = null;
			this.areaVisibility = 0;
			this.id = id;
			this.visibility = visibility;
			this.name = name;
			this.comment = comment;
			this.lat = lat;
			this.lng = lng;
			this.polygonCoords = polygonCoords;
			this.numProblems = numProblems;
		}

		public int getAreaId() {
			return areaId;
		}
		
		public String getAreaName() {
			return areaName;
		}

		public int getAreaVisibility() {
			return areaVisibility;
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
		
		public int getVisibility() {
			return visibility;
		}

		@Override
		public String toString() {
			return "Sector [areaId=" + areaId + ", areaVisibility=" + areaVisibility + ", areaName=" + areaName + ", id=" + id
					+ ", visibility=" + visibility + ", name=" + name + ", comment=" + comment + ", lat=" + lat + ", lng=" + lng
					+ ", polygonCoords=" + polygonCoords + ", numProblems=" + numProblems + "]";
		}
	}
	
	private final int regionId;
	private final int id;
	private final int visibility;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final int numSectors;
	private final List<Sector> sectors;
	private final List<Media> media;
	private final List<NewMedia> newMedia;
	
	public Area(int regionId, int id, int visibility, String name, String comment, double lat, double lng, int numSectors, List<Media> media, List<NewMedia> newMedia) {
		this.regionId = regionId;
		this.id = id;
		this.visibility = visibility;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
		this.numSectors = numSectors;
		this.sectors = numSectors == -1? new ArrayList<>() : null;
		this.media = media;
		this.newMedia = newMedia;
	}

	public void addSector(int id, int visibility, String name, String comment, double lat, double lng, String polygonCoords, int numProblems) {
		sectors.add(new Sector(id, visibility, name, comment, lat, lng, polygonCoords, numProblems));
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
	
	public List<Media> getMedia() {
		return media;
	}
	
	public String getName() {
		return name;
	}
	
	public List<NewMedia> getNewMedia() {
		return newMedia;
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
	
	public int getVisibility() {
		return visibility;
	}

	@Override
	public String toString() {
		return "Area [regionId=" + regionId + ", id=" + id + ", visibility=" + visibility + ", name=" + name + ", comment="
				+ comment + ", lat=" + lat + ", lng=" + lng + ", numSectors=" + numSectors + ", sectors=" + sectors
				+ "]";
	}
}
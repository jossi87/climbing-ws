package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Area implements IMetadata {
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
	private final int numProblems;
	private final List<Sector> sectors;
	private final List<Media> media;
	private final List<NewMedia> newMedia;
	private Metadata metadata;
	
	public Area(int regionId, int id, int visibility, String name, String comment, double lat, double lng, int numSectors, int numProblems, List<Media> media, List<NewMedia> newMedia) {
		this.regionId = regionId;
		this.id = id;
		this.visibility = visibility;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
		this.numSectors = numSectors;
		this.numProblems = numProblems;
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
	
	public int getVisibility() {
		return visibility;
	}

	public void orderSectors() {
		sectors.sort(new Comparator<Sector>() {
			@Override
			public int compare(Sector o1, Sector o2) {
				return getName(o1).compareTo(getName(o2));
			}
			private String getName(Sector s) {
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
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "Area [regionId=" + regionId + ", id=" + id + ", visibility=" + visibility + ", name=" + name
				+ ", comment=" + comment + ", lat=" + lat + ", lng=" + lng + ", numSectors=" + numSectors
				+ ", numProblems=" + numProblems + ", sectors=" + sectors + ", media=" + media + ", newMedia="
				+ newMedia + ", metadata=" + metadata + "]";
	}
}
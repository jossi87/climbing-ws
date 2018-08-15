package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Sector implements IMetadata {
	public class Problem {
		private final int id;
		private final int visibility;
		private final int nr;
		private final String name;
		private final String comment;
		private final String grade;
		private final List<FaUser> fa;
		private final int numImages;
		private final int numMovies;
		private final double lat;
		private final double lng;
		private final int numTicks;
		private final double stars;
		private final boolean ticked;
		private final Type t;
		
		public Problem(int id, int visibility, int nr, String name, String comment, String grade, List<FaUser> fa, int numImages, int numMovies, double lat, double lng, int numTicks, double stars, boolean ticked, Type t) {
			this.id = id;
			this.visibility = visibility;
			this.nr = nr;
			this.name = name;
			this.comment = comment;
			this.grade = grade;
			this.fa = fa;
			this.numImages = numImages;
			this.numMovies = numMovies;
			this.lat = lat;
			this.lng = lng;
			this.numTicks = numTicks;
			this.stars = stars;
			this.ticked = ticked;
			this.t = t;
		}
		
		public String getComment() {
			return comment;
		}
		
		public List<FaUser> getFa() {
			return fa;
		}
		
		public String getGrade() {
			return grade;
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
		
		public int getNr() {
			return nr;
		}

		public int getNumImages() {
			return numImages;
		}
		
		public int getNumMovies() {
			return numMovies;
		}

		public int getNumTicks() {
			return numTicks;
		}

		public double getStars() {
			return stars;
		}

		public Type getT() {
			return t;
		}
		
		public int getVisibility() {
			return visibility;
		}
		
		public boolean isTicked() {
			return ticked;
		}

		@Override
		public String toString() {
			return "Problem [id=" + id + ", visibility=" + visibility + ", nr=" + nr + ", name=" + name + ", comment="
					+ comment + ", grade=" + grade + ", fa=" + fa + ", numImages=" + numImages + ", numMovies="
					+ numMovies + ", lat=" + lat + ", lng=" + lng + ", numTicks=" + numTicks + ", stars=" + stars
					+ ", ticked=" + ticked + ", t=" + t + "]";
		}
	}
	
	private final int areaId;
	private final int areaVisibility;
	private final String areaName;
	private final String canonical;
	private final int id;
	private final int visibility;
	private final String name;
	private final String comment;
	private final double lat;
	private final double lng;
	private final String polygonCoords;
	private final List<Media> media;
	private final List<Problem> problems = new ArrayList<>();
	private final List<NewMedia> newMedia;
	private Metadata metadata;
	
	public Sector(int areaId, int areaVisibility, String areaName, String canonical, int id, int visibility, String name, String comment, double lat, double lng, String polygonCoords, List<Media> media, List<NewMedia> newMedia) {
		this.areaId = areaId;
		this.areaVisibility = areaVisibility;
		this.areaName = areaName;
		this.canonical = canonical;
		this.id = id;
		this.visibility = visibility;
		this.name = name;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
		this.polygonCoords = polygonCoords;
		this.media = media;
		this.newMedia = newMedia;
	}
	
	public void addProblem(int id, int visibility, int nr, String name, String comment, String grade, List<FaUser> fa, int numImages, int numMovies, double lat, double lng, int numTicks, double stars, boolean ticked, Type t) {
		this.problems.add(new Problem(id, visibility, nr, name, comment, grade, fa, numImages, numMovies, lat, lng, numTicks, stars, ticked, t));
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
	
	public String getCanonical() {
		return canonical;
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
	
	public String getPolygonCoords() {
		return polygonCoords;
	}
	
	public List<Problem> getProblems() {
		return problems;
	}
	
	public int getVisibility() {
		return visibility;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "Sector [areaId=" + areaId + ", areaVisibility=" + areaVisibility + ", areaName=" + areaName
				+ ", canonical=" + canonical + ", id=" + id + ", visibility=" + visibility + ", name=" + name
				+ ", comment=" + comment + ", lat=" + lat + ", lng=" + lng + ", polygonCoords=" + polygonCoords
				+ ", media=" + media + ", problems=" + problems + ", newMedia=" + newMedia + ", metadata=" + metadata
				+ "]";
	}
}
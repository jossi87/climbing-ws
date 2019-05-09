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
		private final int gradeNumber;
		private final String grade;
		private final String fa;
		private final boolean hasImages;
		private final boolean hasMovies;
		private final double lat;
		private final double lng;
		private final double stars;
		private final boolean ticked;
		private final Type t;
		private final boolean danger;
		
		public Problem(int id, int visibility, int nr, String name, int gradeNumber, String grade, String fa,
				boolean hasImages, boolean hasMovies, double lat, double lng, double stars, boolean ticked, Type t,
				boolean danger) {
			this.id = id;
			this.visibility = visibility;
			this.nr = nr;
			this.name = name;
			this.gradeNumber = gradeNumber;
			this.grade = grade;
			this.fa = fa;
			this.hasImages = hasImages;
			this.hasMovies = hasMovies;
			this.lat = lat;
			this.lng = lng;
			this.stars = stars;
			this.ticked = ticked;
			this.t = t;
			this.danger = danger;
		}

		public int getId() {
			return id;
		}

		public int getVisibility() {
			return visibility;
		}

		public int getNr() {
			return nr;
		}

		public String getName() {
			return name;
		}

		public int getGradeNumber() {
			return gradeNumber;
		}

		public String getGrade() {
			return grade;
		}

		public String getFa() {
			return fa;
		}

		public boolean getHasImages() {
			return hasImages;
		}

		public boolean getHasMovies() {
			return hasMovies;
		}

		public double getLat() {
			return lat;
		}

		public double getLng() {
			return lng;
		}

		public double getStars() {
			return stars;
		}

		public boolean isTicked() {
			return ticked;
		}

		public Type getT() {
			return t;
		}

		public boolean isDanger() {
			return danger;
		}
	}
	
	private final boolean orderByGrade;
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
	private final String polyline;
	private final List<Media> media;
	private final List<Problem> problems = new ArrayList<>();
	private final List<NewMedia> newMedia;
	private Metadata metadata;
	
	public Sector(boolean orderByGrade, int areaId, int areaVisibility, String areaName, String canonical, int id, int visibility, String name, String comment, double lat, double lng, String polygonCoords, String polyline, List<Media> media, List<NewMedia> newMedia) {
		this.orderByGrade = orderByGrade;
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
		this.polyline = polyline;
		this.media = media;
		this.newMedia = newMedia;
	}
	
	public void addProblem(int id, int visibility, int nr, String name, String comment, int gradeNumber, String grade, String fa, boolean hasImages, boolean hasMovies, double lat, double lng, double stars, boolean ticked, Type t, boolean danger) {
		this.problems.add(new Problem(id, visibility, nr, name, gradeNumber, grade, fa, hasImages, hasMovies, lat, lng, stars, ticked, t, danger));
	}

	public boolean isOrderByGrade() {
		return orderByGrade;
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
	
	public String getPolyline() {
		return polyline;
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
				+ ", comment=" + comment + ", lat=" + lat + ", lng=" + lng + ", polygonCoords=" + polygonCoords + ", polyline=" + polyline
				+ ", media=" + media + ", problems=" + problems + ", newMedia=" + newMedia + ", metadata=" + metadata
				+ "]";
	}
}
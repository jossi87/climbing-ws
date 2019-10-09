package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Problem implements IMetadata {
	public class Comment {
		private final int id;
		private final String date;
		private final int idUser;
		private final String picture;
		private final String name;
		private final String message;
		private final boolean danger;
		private final boolean resolved;
		public Comment(int id, String date, int idUser, String picture, String name, String message, boolean danger, boolean resolved) {
			this.id = id;
			this.date = date;
			this.idUser = idUser;
			this.picture = picture;
			this.name = name;
			this.message = message;
			this.danger = danger;
			this.resolved = resolved;
		}
		public String getDate() {
			return date;
		}
		public int getId() {
			return id;
		}
		public int getIdUser() {
			return idUser;
		}
		public String getMessage() {
			return message;
		}
		public String getName() {
			return name;
		}
		public String getPicture() {
			return picture;
		}
		public boolean isDanger() {
			return danger;
		}
		public boolean isResolved() {
			return resolved;
		}
	}
	public class Section {
		private final int id;
		private final int nr;
		private final String description;
		private final String grade;
		public Section(int id, int nr, String description, String grade) {
			this.id = id;
			this.nr = nr;
			this.description = description;
			this.grade = grade;
		}
		public String getDescription() {
			return description;
		}
		public String getGrade() {
			return grade;
		}
		public int getId() {
			return id;
		}
		public int getNr() {
			return nr;
		}
	}
	public class Tick {
		private final int id;
		private final int idUser;
		private final String picture;
		private final String date;
		private final String name;
		private final String suggestedGrade;
		private final String comment;
		private final double stars;
		private final boolean writable;
		public Tick(int id, int idUser, String picture, String date, String name, String suggestedGrade, String comment, double stars, boolean writable) {
			this.id = id;
			this.idUser = idUser;
			this.picture = picture;
			this.date = date;
			this.name = name;
			this.suggestedGrade = suggestedGrade;
			this.comment = comment;
			this.stars = stars;
			this.writable = writable;
		}
		public String getComment() {
			return comment;
		}
		public String getDate() {
			return date;
		}
		public int getId() {
			return id;
		}
		public int getIdUser() {
			return idUser;
		}
		public String getName() {
			return name;
		}
		public String getPicture() {
			return picture;
		}
		public double getStars() {
			return stars;
		}
		public String getSuggestedGrade() {
			return suggestedGrade;
		}
		public boolean isWritable() {
			return writable;
		}
		@Override
		public String toString() {
			return "Tick [id=" + id + ", idUser=" + idUser + ", picture=" + picture + ", date=" + date + ", name="
					+ name + ", suggestedGrade=" + suggestedGrade + ", comment=" + comment + ", stars=" + stars
					+ ", writable=" + writable + "]";
		}
	}
	
	private final int areaId;
	private final int areaVisibility;
	private final String areaName;
	private final int sectorId;
	private final int sectorVisibility;
	private final String sectorName;
	private final double sectorLat;
	private final double sectorLng;
	private final String sectorPolygonCoords;
	private final String sectorPolyline;
	private final String canonical;
	private final int id;
	private final int visibility;
	private final int nr;
	private final String name;
	private final String comment;
	private final String grade;
	private final String originalGrade;
	private final String faDate;
	private final String faDateHr;
	private final List<FaUser> fa;
	private final double lat;
	private final double lng;
	private final List<Media> media;
	private final int numTicks;
	private final double stars;
	private final boolean ticked;
	private List<Tick> ticks;
	private List<Comment> comments;
	private final List<NewMedia> newMedia;
	private final Type t;
	private List<Section> sections;
	private Metadata metadata;
	private final boolean todo;
	private final long hits;
	
	public Problem(int areaId, int areaVisibility, String areaName, int sectorId, int sectorVisibility, String sectorName, double sectorLat, double sectorLng, String sectorPolygonCoords, String sectorPolyline, String canonical, int id, int visibility, int nr, String name, String comment, String grade, String originalGrade, String faDate, String faDateHr, List<FaUser> fa, double lat, double lng, List<Media> media, int numTics, double stars, boolean ticked, List<NewMedia> newMedia, Type t, boolean todo, long hits) {
		this.areaId = areaId;
		this.areaVisibility = areaVisibility;
		this.areaName = areaName;
		this.sectorId = sectorId;
		this.sectorVisibility = sectorVisibility;
		this.sectorName = sectorName;
		this.sectorLat = sectorLat;
		this.sectorLng = sectorLng;
		this.sectorPolygonCoords = sectorPolygonCoords;
		this.sectorPolyline = sectorPolyline;
		this.canonical = canonical;
		this.id = id;
		this.visibility = visibility;
		this.nr = nr;
		this.name = name;
		this.comment = comment;
		this.grade = grade;
		this.originalGrade = originalGrade;
		this.faDate = faDate;
		this.faDateHr = faDateHr;
		this.fa = fa;
		this.lat = lat;
		this.lng = lng;
		this.media = media;
		this.numTicks = numTics;
		this.stars = stars;
		this.ticked = ticked;
		this.newMedia = newMedia;
		this.t = t;
		this.todo = todo;
		this.hits = hits;
	}
	
	public void addComment(int id, String date, int idUser, String picture, String name, String message, boolean danger, boolean resolved) {
		if (comments == null) {
			comments = new ArrayList<>();
		}
		comments.add(new Comment(id, date, idUser, picture, name, message, danger, resolved));
	}
	
	public void addSection(int id, int nr, String description, String grade) {
		if (sections == null) {
			sections = new ArrayList<>();
		}
		sections.add(new Section(id, nr, description, grade));
	}
	
	public void addTick(int id, int idUser, String picture, String date, String name, String suggestedGrade, String comment, double stars, boolean writable) {
		if (ticks == null) {
			ticks = new ArrayList<>();
		}
		ticks.add(new Tick(id, idUser, picture, date, name, suggestedGrade, comment, stars, writable));
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
	
	public List<FaUser> getFa() {
		return fa;
	}
	
	public String getFaDate() {
		return faDate;
	}
	
	public String getFaDateHr() {
		return faDateHr;
	}
	
	public String getGrade() {
		return grade;
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
	
	public int getNr() {
		return nr;
	}
	
	public int getNumTicks() {
		return numTicks;
	}
	
	public String getOriginalGrade() {
		return originalGrade;
	}

	public List<Section> getSections() {
		return sections;
	}
	
	public int getSectorId() {
		return sectorId;
	}
	
	public double getSectorLat() {
		return sectorLat;
	}

	public double getSectorLng() {
		return sectorLng;
	}
	
	public String getSectorName() {
		return sectorName;
	}

	public String getSectorPolygonCoords() {
		return sectorPolygonCoords;
	}

	public String getSectorPolyline() {
		return sectorPolyline;
	}

	public int getSectorVisibility() {
		return sectorVisibility;
	}

	public double getStars() {
		return stars;
	}

	public Type getT() {
		return t;
	}
	
	public List<Tick> getTicks() {
		return ticks;
	}

	public int getVisibility() {
		return visibility;
	}

	public boolean isTicked() {
		return ticked;
	}

	public boolean isTodo() {
		return todo;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "Problem [areaId=" + areaId + ", areaVisibility=" + areaVisibility + ", areaName=" + areaName
				+ ", sectorId=" + sectorId + ", sectorVisibility=" + sectorVisibility + ", sectorName=" + sectorName
				+ ", sectorLat=" + sectorLat + ", sectorLng=" + sectorLng + ", canonical=" + canonical + ", id=" + id
				+ ", visibility=" + visibility + ", nr=" + nr + ", name=" + name + ", comment=" + comment + ", grade="
				+ grade + ", originalGrade=" + originalGrade + ", faDate=" + faDate + ", faDateHr=" + faDateHr + ", fa="
				+ fa + ", lat=" + lat + ", lng=" + lng + ", media=" + media + ", numTicks=" + numTicks + ", stars="
				+ stars + ", ticked=" + ticked + ", ticks=" + ticks + ", comments=" + comments + ", newMedia="
				+ newMedia + ", t=" + t + ", sections=" + sections + ", metadata=" + metadata + "]";
	}
}
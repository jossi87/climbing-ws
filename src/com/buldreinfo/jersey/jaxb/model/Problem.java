package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Problem {
	public class Comment {
		private final String date;
		private final int idUser;
		private final String name;
		private final String message;
		public Comment(String date, int idUser, String name, String message) {
			this.date = date;
			this.idUser = idUser;
			this.name = name;
			this.message = message;
		}
		public String getDate() {
			return date;
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
		@Override
		public String toString() {
			return "Comment [date=" + date + ", idUser=" + idUser + ", name=" + name + ", message=" + message + "]";
		}
	}
	public class Tick {
		private final int id;
		private final int idUser;
		private final String date;
		private final String name;
		private final String suggestedGrade;
		private final String comment;
		private final double stars;
		private final boolean writable;
		public Tick(int id, int idUser, String date, String name, String suggestedGrade, String comment, double stars, boolean writable) {
			this.id = id;
			this.idUser = idUser;
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
			return "Tick [id=" + id + ", idUser=" + idUser + ", date=" + date + ", name=" + name + ", suggestedGrade="
					+ suggestedGrade + ", comment=" + comment + ", stars=" + stars + ", writable=" + writable + "]";
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
	
	public Problem(int areaId, int areaVisibility, String areaName, int sectorId, int sectorVisibility, String sectorName, double sectorLat, double sectorLng, int id, int visibility, int nr, String name, String comment, String grade, String originalGrade, String faDate, String faDateHr, List<FaUser> fa, double lat, double lng, List<Media> media, int numTics, double stars, boolean ticked, List<NewMedia> newMedia, Type t) {
		this.areaId = areaId;
		this.areaVisibility = areaVisibility;
		this.areaName = areaName;
		this.sectorId = sectorId;
		this.sectorVisibility = sectorVisibility;
		this.sectorName = sectorName;
		this.sectorLat = sectorLat;
		this.sectorLng = sectorLng;
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
	}
	
	public void addComment(String date, int idUser, String name, String message) {
		if (comments == null) {
			comments = new ArrayList<>();
		}
		comments.add(new Comment(date, idUser, name, message));
	}
	
	public void addTick(int id, int idUser, String date, String name, String suggestedGrade, String comment, double stars, boolean writable) {
		if (ticks == null) {
			ticks = new ArrayList<>();
		}
		ticks.add(new Tick(id, idUser, date, name, suggestedGrade, comment, stars, writable));
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
	
	public int getNr() {
		return nr;
	}
	
	public int getNumTicks() {
		return numTicks;
	}

	public String getOriginalGrade() {
		return originalGrade;
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

	@Override
	public String toString() {
		return "Problem [areaId=" + areaId + ", areaVisibility=" + areaVisibility + ", areaName=" + areaName
				+ ", sectorId=" + sectorId + ", sectorVisibility=" + sectorVisibility + ", sectorName=" + sectorName
				+ ", sectorLat=" + sectorLat + ", sectorLng=" + sectorLng + ", id=" + id + ", visibility=" + visibility
				+ ", nr=" + nr + ", name=" + name + ", comment=" + comment + ", grade=" + grade + ", originalGrade="
				+ originalGrade + ", faDate=" + faDate + ", faDateHr=" + faDateHr + ", fa=" + fa + ", lat=" + lat
				+ ", lng=" + lng + ", media=" + media + ", numTicks=" + numTicks + ", stars=" + stars + ", ticked="
				+ ticked + ", ticks=" + ticks + ", comments=" + comments + ", newMedia=" + newMedia + ", t=" + t + "]";
	}
}
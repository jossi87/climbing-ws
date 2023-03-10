package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Sector implements IMetadata {
	public class Problem {
		private final int id;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final int nr;
		private final String name;
		private final String rock;
		private final String comment;
		private final int gradeNumber;
		private final String grade;
		private final String fa;
		private final int numPitches;
		private final boolean hasImages;
		private final boolean hasMovies;
		private final boolean hasTopo;
		private final double lat;
		private final double lng;
		private final int numTicks;
		private final double stars;
		private final boolean ticked;
		private final boolean todo;
		private final Type t;
		private final boolean danger;
		
		public Problem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, int gradeNumber, String grade, String fa,
				int numPitches,
				boolean hasImages, boolean hasMovies, boolean hasTopo, double lat, double lng, int numTicks, double stars, boolean ticked, boolean todo, Type t,
				boolean danger) {
			this.id = id;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.nr = nr;
			this.name = name;
			this.rock = rock;
			this.comment = comment;
			this.gradeNumber = gradeNumber;
			this.grade = grade;
			this.fa = fa;
			this.numPitches = numPitches;
			this.hasImages = hasImages;
			this.hasMovies = hasMovies;
			this.hasTopo = hasTopo;
			this.lat = lat;
			this.lng = lng;
			this.numTicks = numTicks;
			this.stars = stars;
			this.ticked = ticked;
			this.todo = todo;
			this.t = t;
			this.danger = danger;
		}

		public String getComment() {
			return comment;
		}
		
		public String getFa() {
			return fa;
		}

		public String getGrade() {
			return grade;
		}

		public int getGradeNumber() {
			return gradeNumber;
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

		public int getNumPitches() {
			return numPitches;
		}

		public int getNumTicks() {
			return numTicks;
		}

		public String getRock() {
			return rock;
		}

		public double getStars() {
			return stars;
		}

		public Type getT() {
			return t;
		}

		public boolean isDanger() {
			return danger;
		}

		public boolean isHasImages() {
			return hasImages;
		}

		public boolean isHasMovies() {
			return hasMovies;
		}

		public boolean isHasTopo() {
			return hasTopo;
		}
		
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}

		public boolean isTicked() {
			return ticked;
		}
		
		public boolean isTodo() {
			return todo;
		}
	}
	public class ProblemOrder {
		private final int id;
		private final String name;
		private final int nr;
		public ProblemOrder(int id, String name, int nr) {
			this.id = id;
			this.name = name;
			this.nr = nr;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public int getNr() {
			return nr;
		}
	}
	public class SectorJump {
		private final int id;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final String name;
		public SectorJump(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name) {
			this.id = id;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public boolean isLockedAdmin() {
			return lockedAdmin;
		}
		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}
	}
	
	private final boolean orderByGrade;
	private final int areaId;
	private final boolean areaLockedAdmin;
	private final boolean areaLockedSuperadmin;
	private final boolean areaNoDogsAllowed;
	private final String areaName;
	private final String canonical;
	private final int id;
	private final boolean trash;
	private final boolean lockedAdmin;
	private final boolean lockedSuperadmin;
	private final String name;
	private final String comment;
	private final String accessInfo;
	private final double lat;
	private final double lng;
	private final String polygonCoords;
	private final String polyline;
	private final List<Media> media;
	private final List<SectorJump> sectors = new ArrayList<>();
	private final List<Problem> problems = new ArrayList<>();
	private final List<ProblemOrder> problemOrder = new ArrayList<>();
	private final List<NewMedia> newMedia;
	private final long hits;
	private Metadata metadata;
	
	public Sector(boolean orderByGrade, int areaId, boolean areaLockedAdmin, boolean areaLockedSuperadmin, boolean areaNoDogsAllowed, String areaName, String canonical, int id, boolean trash, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String accessInfo, double lat, double lng, String polygonCoords, String polyline, List<Media> media, List<NewMedia> newMedia, long hits) {
		this.orderByGrade = orderByGrade;
		this.areaId = areaId;
		this.areaLockedAdmin = areaLockedAdmin;
		this.areaLockedSuperadmin = areaLockedSuperadmin;
		this.areaNoDogsAllowed = areaNoDogsAllowed;
		this.areaName = areaName;
		this.canonical = canonical;
		this.id = id;
		this.trash = trash;
		this.lockedAdmin = lockedAdmin;
		this.lockedSuperadmin = lockedSuperadmin; 
		this.name = name;
		this.comment = comment;
		this.accessInfo = accessInfo;
		this.lat = lat;
		this.lng = lng;
		this.polygonCoords = polygonCoords;
		this.polyline = polyline;
		this.media = media;
		this.newMedia = newMedia;
		this.hits = hits;
	}
	
	public void addProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, int gradeNumber, String grade, String fa, int numPitches, boolean hasImages, boolean hasMovies, boolean hasTopo, double lat, double lng, int numTicks, double stars, boolean ticked, boolean todo, Type t, boolean danger) {
		this.problems.add(new Problem(id, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, gradeNumber, grade, fa, numPitches, hasImages, hasMovies, hasTopo, lat, lng, numTicks, stars, ticked, todo, t, danger));
		this.problemOrder.add(new ProblemOrder(id, name, nr));
	}
	
	public void addSector(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name) {
		this.sectors.add(new SectorJump(id, lockedAdmin, lockedSuperadmin, name));
	}
	
	public String getAccessInfo() {
		return accessInfo;
	}
	
	public int getAreaId() {
		return areaId;
	}
	
	public String getAreaName() {
		return areaName;
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

	public String getPolyline() {
		return polyline;
	}

	public List<ProblemOrder> getProblemOrder() {
		return problemOrder;
	}
	
	public List<Problem> getProblems() {
		return problems;
	}
	
	public List<SectorJump> getSiblings() {
		return sectors;
	}
	
	public boolean isAreaLockedAdmin() {
		return areaLockedAdmin;
	}
	
	public boolean isAreaLockedSuperadmin() {
		return areaLockedSuperadmin;
	}
	
	public boolean isAreaNoDogsAllowed() {
		return areaNoDogsAllowed;
	}

	public boolean isLockedAdmin() {
		return lockedAdmin;
	}
	
	public boolean isLockedSuperadmin() {
		return lockedSuperadmin;
	}
	
	public boolean isOrderByGrade() {
		return orderByGrade;
	}
	
	public boolean isTrash() {
		return trash;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
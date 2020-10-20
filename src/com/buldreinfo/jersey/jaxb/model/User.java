package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class User implements IMetadata {
	public class Tick {
		private final String areaName;
		private final boolean areaLockedAdmin;
		private final boolean areaLockedSuperadmin;
		private final String sectorName;
		private final boolean sectorLockedAdmin;
		private final boolean sectorLockedSuperadmin;
		private int num;
		private final int id;
		private final String subType;
		private final int idProblem;
		private final boolean lockedAdmin;
		private final boolean lockedSuperadmin;
		private final String name;
		private final String comment;
		private final String date;
		private final String dateHr;
		private final double stars;
		private final boolean fa;
		private final String grade;
		private final int gradeNumber;
		
		public Tick(String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String sectorName,
				boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, int id, String subType,
				int idProblem, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String date,
				String dateHr, double stars, boolean fa, String grade, int gradeNumber) {
			this.areaName = areaName;
			this.areaLockedAdmin = areaLockedAdmin;
			this.areaLockedSuperadmin = areaLockedSuperadmin;
			this.sectorName = sectorName;
			this.sectorLockedAdmin = sectorLockedAdmin;
			this.sectorLockedSuperadmin = sectorLockedSuperadmin;
			this.id = id;
			this.subType = subType;
			this.idProblem = idProblem;
			this.lockedAdmin = lockedAdmin;
			this.lockedSuperadmin = lockedSuperadmin;
			this.name = name;
			this.comment = comment;
			this.date = date;
			this.dateHr = dateHr;
			this.stars = stars;
			this.fa = fa;
			this.grade = grade;
			this.gradeNumber = gradeNumber;
		}

		public int getNum() {
			return num;
		}

		public void setNum(int num) {
			this.num = num;
		}

		public String getAreaName() {
			return areaName;
		}

		public boolean isAreaLockedAdmin() {
			return areaLockedAdmin;
		}

		public boolean isAreaLockedSuperadmin() {
			return areaLockedSuperadmin;
		}

		public String getSectorName() {
			return sectorName;
		}

		public boolean isSectorLockedAdmin() {
			return sectorLockedAdmin;
		}

		public boolean isSectorLockedSuperadmin() {
			return sectorLockedSuperadmin;
		}

		public int getId() {
			return id;
		}

		public String getSubType() {
			return subType;
		}

		public int getIdProblem() {
			return idProblem;
		}

		public boolean isLockedAdmin() {
			return lockedAdmin;
		}

		public boolean isLockedSuperadmin() {
			return lockedSuperadmin;
		}

		public String getName() {
			return name;
		}

		public String getComment() {
			return comment;
		}

		public String getDate() {
			return date;
		}

		public String getDateHr() {
			return dateHr;
		}

		public double getStars() {
			return stars;
		}

		public boolean isFa() {
			return fa;
		}

		public String getGrade() {
			return grade;
		}

		public int getGradeNumber() {
			return gradeNumber;
		}
	}
	private final boolean readOnly;
	private final int id;
	private final String picture;
	private final String name;
	private final int numImagesCreated;
	private final int numVideosCreated;
	private final int numImageTags;
	private final int numVideoTags;
	private final List<Tick> ticks = new ArrayList<>();
	private Metadata metadata;
	
	public User(boolean readOnly, int id, String picture, String name, int numImagesCreated, int numVideosCreated, int numImageTags, int numVideoTags) {
		this.readOnly = readOnly;
		this.id = id;
		this.picture = picture;
		this.name = name;
		this.numImagesCreated = numImagesCreated;
		this.numVideosCreated = numVideosCreated;
		this.numImageTags = numImageTags;
		this.numVideoTags = numVideoTags;
	}
	
	public void addTick(String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, int id, String subType, int idProblem, boolean lockedAdmin, boolean lockedSuperadmin, String name, String comment, String date, String dateHr, double stars, boolean fa, String grade, int gradeNumber) {
		ticks.add(new Tick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, subType, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, grade, gradeNumber));
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public String getName() {
		return name;
	}
	
	public int getNumImagesCreated() {
		return numImagesCreated;
	}
	
	public int getNumImageTags() {
		return numImageTags;
	}
	
	public int getNumVideosCreated() {
		return numVideosCreated;
	}
	
	public int getNumVideoTags() {
		return numVideoTags;
	}
	
	public String getPicture() {
		return picture;
	}

	public List<Tick> getTicks() {
		return ticks;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return "User [readOnly=" + readOnly + ", id=" + id + ", picture=" + picture + ", name=" + name
				+ ", numImagesCreated=" + numImagesCreated + ", numVideosCreated=" + numVideosCreated
				+ ", numImageTags=" + numImageTags + ", numVideoTags=" + numVideoTags + ", ticks=" + ticks
				+ ", metadata=" + metadata + "]";
	}
}
package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Frontpage implements IMetadata {
	public class Ascent {
		private final int idProblem;
		private final int visibility;
		private final String problem;
		private final String grade;
		private final String date;
		@Deprecated // TODO Remove when gui is updated
		private final int idUser;
		private final String user;
		private final String picture;
		public Ascent(int idProblem, int visibility, String problem, String grade, String date, int idUser, String user, String picture) {
			this.idProblem = idProblem;
			this.visibility = visibility;
			this.problem = problem;
			this.grade = grade;
			this.date = date;
			this.idUser = idUser;
			this.user = user;
			this.picture = picture;
		}
		public String getDate() {
			return date;
		}
		public String getGrade() {
			return grade;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public int getIdUser() {
			return idUser;
		}
		public String getPicture() {
			return picture;
		}
		public String getProblem() {
			return problem;
		}
		public String getUser() {
			return user;
		}
		public int getVisibility() {
			return visibility;
		}
		@Override
		public String toString() {
			return "Ascent [idProblem=" + idProblem + ", visibility=" + visibility + ", problem=" + problem + ", grade="
					+ grade + ", date=" + date + ", idUser=" + idUser + ", user=" + user + ", picture=" + picture + "]";
		}
	}
	
	public class Comment {
		private final String date;
		private final int idProblem;
		private final int visibility;
		private final String grade;
		private final String problem;
		private final String picture;
		public Comment(String date, int idProblem, int visibility, String grade, String problem, String picture) {
			this.date = date;
			this.idProblem = idProblem;
			this.grade = grade;
			this.visibility = visibility;
			this.problem = problem;
			this.picture = picture;
		}
		public String getDate() {
			return date;
		}
		public String getGrade() {
			return grade;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public String getPicture() {
			return picture;
		}
		public String getProblem() {
			return problem;
		}
		public int getVisibility() {
			return visibility;
		}
	}
	
	public class Fa {
		@Deprecated // TODO Remove when gui is updated
		private final int idArea;
		@Deprecated // TODO Remove when gui is updated
		private final int areaVisibility;
		@Deprecated // TODO Remove when gui is updated
		private final String area;
		@Deprecated // TODO Remove when gui is updated
		private final int idSector;
		@Deprecated // TODO Remove when gui is updated
		private final int sectorVisibility;
		@Deprecated // TODO Remove when gui is updated
		private final String sector;
		private final int idProblem;
		private final int problemVisibility;
		private final String problem;
		private final String grade;
		private final String date;
		private final int randomMediaId;
		public Fa(int idArea, int areaVisibility, String area, int idSector, int sectorVisibility, String sector,
				int idProblem, int problemVisibility, String problem, String grade, String date, int randomMediaId) {
			this.idArea = idArea;
			this.areaVisibility = areaVisibility;
			this.area = area;
			this.idSector = idSector;
			this.sectorVisibility = sectorVisibility;
			this.sector = sector;
			this.idProblem = idProblem;
			this.problemVisibility = problemVisibility;
			this.problem = problem;
			this.grade = grade;
			this.date = date;
			this.randomMediaId = randomMediaId;
		}
		public String getArea() {
			return area;
		}
		public int getAreaVisibility() {
			return areaVisibility;
		}
		public String getDate() {
			return date;
		}
		public String getGrade() {
			return grade;
		}
		public int getIdArea() {
			return idArea;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public int getIdSector() {
			return idSector;
		}
		public String getProblem() {
			return problem;
		}
		public int getProblemVisibility() {
			return problemVisibility;
		}
		public int getRandomMediaId() {
			return randomMediaId;
		}
		public String getSector() {
			return sector;
		}
		public int getSectorVisibility() {
			return sectorVisibility;
		}
		@Override
		public String toString() {
			return "Fa [idArea=" + idArea + ", areaVisibility=" + areaVisibility + ", area=" + area + ", idSector="
					+ idSector + ", sectorVisibility=" + sectorVisibility + ", sector=" + sector + ", idProblem="
					+ idProblem + ", problemVisibility=" + problemVisibility + ", problem=" + problem + ", grade="
					+ grade + ", date=" + date + ", randomMediaId=" + randomMediaId + "]";
		}
	}
	
	public class Media {
		private final int idProblem;
		private final int visibility;
		private final String problem;
		private final String grade;
		@Deprecated // TODO Remove when gui is updated
		private final String type;
		private final int idMedia;
		public Media(int idProblem, int visibility, String problem, String grade, String type, int idMedia) {
			this.idProblem = idProblem;
			this.visibility = visibility;
			this.problem = problem;
			this.grade = grade;
			this.type = type;
			this.idMedia = idMedia;
		}
		public String getGrade() {
			return grade;
		}
		public int getIdMedia() {
			return idMedia;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public String getProblem() {
			return problem;
		}
		public String getType() {
			return type;
		}
		public int getVisibility() {
			return visibility;
		}
		@Override
		public String toString() {
			return "Media [idProblem=" + idProblem + ", visibility=" + visibility + ", problem=" + problem + ", grade="
					+ grade + ", type=" + type + ", idMedia=" + idMedia + "]";
		}
	}
	
	public class RandomMedia {
		private final int idMedia;
		private final int width;
		private final int height;
		private final int idArea;
		private final String area;
		private final int idSector;
		private final String sector;
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final int idCreator;
		private final String creator;
		private final String inPhoto;
		public RandomMedia(int idMedia, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, int idCreator, String creator, String inPhoto) {
			this.idMedia = idMedia;
			this.width = width;
			this.height = height;
			this.idArea = idArea;
			this.area = area;
			this.idSector = idSector;
			this.sector = sector;
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.idCreator = idCreator;
			this.creator = creator;
			this.inPhoto = inPhoto;
		}
		public String getArea() {
			return area;
		}
		public String getCreator() {
			return creator;
		}
		public String getGrade() {
			return grade;
		}
		public int getHeight() {
			return height;
		}
		public int getIdArea() {
			return idArea;
		}
		public int getIdCreator() {
			return idCreator;
		}
		public int getIdMedia() {
			return idMedia;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public int getIdSector() {
			return idSector;
		}
		public String getInPhoto() {
			return inPhoto;
		}
		public String getProblem() {
			return problem;
		}
		public String getSector() {
			return sector;
		}
		public int getWidth() {
			return width;
		}
		@Override
		public String toString() {
			return "RandomMedia [idMedia=" + idMedia + ", width=" + width + ", height=" + height + ", idArea=" + idArea
					+ ", area=" + area + ", idSector=" + idSector + ", sector=" + sector + ", idProblem=" + idProblem
					+ ", problem=" + problem + ", grade=" + grade + ", idCreator=" + idCreator + ", creator=" + creator
					+ ", inPhoto=" + inPhoto + "]";
		}
	}
	
	private int numProblems;
	private int numProblemsWithCoordinates;
	private int numProblemsWithTopo;
	private int numTicks;
	private int numImages;
	private int numMovies;
	private RandomMedia randomMedia;
	@Deprecated // TODO Remove when gui is updated
	private final List<Ascent> ascents = new ArrayList<>();
	@Deprecated // TODO Remove when gui is updated
	private final List<Fa> fas = new ArrayList<>();
	@Deprecated // TODO Remove when gui is updated
	private final List<Media> medias = new ArrayList<>();
	@Deprecated // TODO Remove when gui is updated
	private final List<Comment> comments = new ArrayList<>();
	private final Collection<String> activity;
	private Metadata metadata;
	@Deprecated // TODO Remove when gui is updated
	private final boolean showLogoPlay;
	@Deprecated // TODO Remove when gui is updated
	private final boolean showLogoSis;
	@Deprecated // TODO Remove when gui is updated
	private final boolean showLogoBrv;
	
	public Frontpage(boolean showLogoPlay, boolean showLogoSis, boolean showLogoBrv, Collection<String> activity) {
		this.showLogoPlay = showLogoPlay;
		this.showLogoSis = showLogoSis;
		this.showLogoBrv = showLogoBrv;
		this.activity = activity;
	}
	
	public void addAscent(int idProblem, int visibility, String problem, String grade, String date, int idUser, String user, String picture) {
		this.ascents.add(new Ascent(idProblem, visibility, problem, grade, date, idUser, user, picture));
	}
	
	public void addComment(String date, int idProblem, int visibility, String grade, String problem, String picture) {
		this.comments.add(new Comment(date, idProblem, visibility, grade, problem, picture));
	}
	
	public void addFa(int idArea, int areaVisibility, String area, int idSector, int sectorVisibility, String sector, int idProblem, int problemVisibility, String problem, String grade, String date, int randomMediaId) {
		this.fas.add(new Fa(idArea, areaVisibility, area, idSector, sectorVisibility, sector, idProblem, problemVisibility, problem, grade, date, randomMediaId));
	}
	
	public void addMedia(int idProblem, int visibility, String problem, String grade, String type, int idMedia) {
		this.medias.add(new Media(idProblem, visibility, problem, grade, type, idMedia));
	}
	
	public Collection<String> getActivity() {
		return activity;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public int getNumImages() {
		return numImages;
	}
	
	public int getNumMovies() {
		return numMovies;
	}
	
	public int getNumProblems() {
		return numProblems;
	}
	
	public int getNumProblemsWithCoordinates() {
		return numProblemsWithCoordinates;
	}
	
	public int getNumProblemsWithTopo() {
		return numProblemsWithTopo;
	}

	public int getNumTicks() {
		return numTicks;
	}

	public RandomMedia getRandomMedia() {
		return randomMedia;
	}
	
	public boolean isShowLogoBrv() {
		return showLogoBrv;
	}
	
	public boolean isShowLogoPlay() {
		return showLogoPlay;
	}

	public boolean isShowLogoSis() {
		return showLogoSis;
	}

	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public void setNumImages(int numImages) {
		this.numImages = numImages;
	}

	public void setNumMovies(int numMovies) {
		this.numMovies = numMovies;
	}

	public void setNumProblems(int numProblems) {
		this.numProblems = numProblems;
	}
	
	public void setNumProblemsWithCoordinates(int numProblemsWithCoordinates) {
		this.numProblemsWithCoordinates = numProblemsWithCoordinates;
	}
	
	public void setNumProblemsWithTopo(int numProblemsWithTopo) {
		this.numProblemsWithTopo = numProblemsWithTopo;
	}

	public void setNumTicks(int numTicks) {
		this.numTicks = numTicks;
	}

	public void setRandomMedia(int idMedia, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, int idCreator, String creator, String inPhoto) {
		randomMedia = new RandomMedia(idMedia, width, height, idArea, area, idSector, sector, idProblem, problem, grade, idCreator, creator, inPhoto);
	}

	@Override
	public String toString() {
		return "Frontpage [numProblems=" + numProblems + ", numProblemsWithCoordinates=" + numProblemsWithCoordinates
				+ ", numProblemsWithTopo=" + numProblemsWithTopo + ", numTicks=" + numTicks + ", numImages=" + numImages
				+ ", numMovies=" + numMovies + ", randomMedia=" + randomMedia + ", ascents=" + ascents + ", fas=" + fas
				+ ", medias=" + medias + ", comments=" + comments + ", metadata=" + metadata + "]";
	}
}
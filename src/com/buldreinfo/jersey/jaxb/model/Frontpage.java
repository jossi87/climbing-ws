package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

public class Frontpage {
	public class Ascent {
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final String date;
		private final int idUser;
		private final String user;
		public Ascent(int idProblem, String problem, String grade, String date, int idUser, String user) {
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.date = date;
			this.idUser = idUser;
			this.user = user;
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
		public String getProblem() {
			return problem;
		}
		public String getUser() {
			return user;
		}
		@Override
		public String toString() {
			return "Ascent [idProblem=" + idProblem + ", problem=" + problem + ", grade=" + grade + ", date=" + date
					+ ", idUser=" + idUser + ", user=" + user + "]";
		}
	}
	
	public class Comment {
		private final String date;
		private final int idProblem;
		private final String problem;
		public Comment(String date, int idProblem, String problem) {
			this.date = date;
			this.idProblem = idProblem;
			this.problem = problem;
		}
		public String getDate() {
			return date;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public String getProblem() {
			return problem;
		}
	}
	
	public class Fa {
		private final int idArea;
		private final String area;
		private final int idSector;
		private final String sector;
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final String date;
		public Fa(int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, String date) {
			this.idArea = idArea;
			this.area = area;
			this.idSector = idSector;
			this.sector = sector;
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.date = date;
		}
		public String getArea() {
			return area;
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
		public String getSector() {
			return sector;
		}
		@Override
		public String toString() {
			return "Fa [idArea=" + idArea + ", area=" + area + ", idSector=" + idSector + ", sector=" + sector
					+ ", idProblem=" + idProblem + ", problem=" + problem + ", grade=" + grade + ", date=" + date + "]";
		}
	}
	
	public class Media {
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final String type;
		public Media(int idProblem, String problem, String grade, String type) {
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.type = type;
		}
		public String getGrade() {
			return grade;
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
		@Override
		public String toString() {
			return "Media [idProblem=" + idProblem + ", problem=" + problem + ", grade=" + grade + ", type=" + type
					+ "]";
		}
	}
	public class RandomMedia {
		private final int idMedia;
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final int idCreator;
		private final String creator;
		private final String inPhoto;
		public RandomMedia(int idMedia, int idProblem, String problem, String grade, int idCreator, String creator, String inPhoto) {
			this.idMedia = idMedia;
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.idCreator = idCreator;
			this.creator = creator;
			this.inPhoto = inPhoto;
		}
		public String getCreator() {
			return creator;
		}
		public String getGrade() {
			return grade;
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
		public String getProblem() {
			return problem;
		}
		public String getInPhoto() {
			return inPhoto;
		}
		@Override
		public String toString() {
			return "RandomMedia [idMedia=" + idMedia + ", idProblem=" + idProblem + ", problem=" + problem + ", grade="
					+ grade + ", idCreator=" + idCreator + ", creator=" + creator + ", inPhoto=" + inPhoto + "]";
		}
		
	}
	
	private int numProblems;
	private int numProblemsWithCoordinates;
	private int numTicks;
	private int numImages;
	private int numMovies;
	private RandomMedia randomMedia;
	private final List<Ascent> ascents = new ArrayList<>();
	private final List<Fa> fas = new ArrayList<>();
	private final List<Media> medias = new ArrayList<>();
	private final List<Comment> comments = new ArrayList<>();
	
	public Frontpage() {
	}
	
	public void addAscent(int idProblem, String problem, String grade, String date, int idUser, String user) {
		this.ascents.add(new Ascent(idProblem, problem, grade, date, idUser, user));
	}
	
	public void addComment(String date, int idProblem, String problem) {
		this.comments.add(new Comment(date, idProblem, problem));
	}
	
	public void addFa(int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, String date) {
		this.fas.add(new Fa(idArea, area, idSector, sector, idProblem, problem, grade, date));
	}
	
	public void addMedia(int idProblem, String problem, String grade, String type) {
		this.medias.add(new Media(idProblem, problem, grade, type));
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

	public int getNumTicks() {
		return numTicks;
	}

	public RandomMedia getRandomMedia() {
		return randomMedia;
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

	public void setNumTicks(int numTicks) {
		this.numTicks = numTicks;
	}

	public void setRandomMedia(int idMedia, int idProblem, String problem, String grade, int idCreator, String creator, String inPhoto) {
		randomMedia = new RandomMedia(idMedia, idProblem, problem, grade, idCreator, creator, inPhoto);
	}

	@Override
	public String toString() {
		return "Frontpage [numProblems=" + numProblems + ", numProblemsWithCoordinates=" + numProblemsWithCoordinates
				+ ", numTicks=" + numTicks + ", numImages=" + numImages + ", numMovies=" + numMovies + ", randomMedia="
				+ randomMedia + ", ascents=" + ascents + ", fas=" + fas + ", medias=" + medias + ", comments="
				+ comments + "]";
	}
}
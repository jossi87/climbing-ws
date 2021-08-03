package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.model.Frontpage.RandomMedia.User;

public class Frontpage implements IMetadata {
	public class RandomMedia {
		public class User {
			private final int id;
			private final String name;
			public User(int id, String name) {
				this.id = id;
				this.name = name;
			}
			public int getId() {
				return id;
			}
			public String getName() {
				return name;
			}
		}
		private final int idMedia;
		private final int crc32;
		private final int width;
		private final int height;
		private final int idArea;
		private final String area;
		private final int idSector;
		private final String sector;
		private final int idProblem;
		private final String problem;
		private final String grade;
		private final User photographer;
		private final List<User> tagged;
		public RandomMedia(int idMedia, int crc32, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, User photographer, List<User> tagged) {
			this.idMedia = idMedia;
			this.crc32 = crc32;
			this.width = width;
			this.height = height;
			this.idArea = idArea;
			this.area = area;
			this.idSector = idSector;
			this.sector = sector;
			this.idProblem = idProblem;
			this.problem = problem;
			this.grade = grade;
			this.photographer = photographer;
			this.tagged = tagged;
		}
		public String getArea() {
			return area;
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
		public int getCrc32() {
			return crc32;
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
		public User getPhotographer() {
			return photographer;
		}
		public String getProblem() {
			return problem;
		}
		public String getSector() {
			return sector;
		}
		public List<User> getTagged() {
			return tagged;
		}
		public int getWidth() {
			return width;
		}
		@Override
		public String toString() {
			return "RandomMedia [idMedia=" + idMedia + ", width=" + width + ", height=" + height + ", idArea=" + idArea
					+ ", area=" + area + ", idSector=" + idSector + ", sector=" + sector + ", idProblem=" + idProblem
					+ ", problem=" + problem + ", grade=" + grade + ", photographer=" + photographer + ", tagged="
					+ tagged + "]";
		}
	}
	
	private int numProblems;
	private int numProblemsWithCoordinates;
	private int numProblemsWithTopo;
	private int numTicks;
	private int numImages;
	private int numMovies;
	private RandomMedia randomMedia;
	private Metadata metadata;
	
	public Frontpage() {
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

	public void setRandomMedia(int idMedia, int crc32, int width, int height, int idArea, String area, int idSector, String sector, int idProblem, String problem, String grade, User photographer, List<User> tagged) {
		randomMedia = new RandomMedia(idMedia, crc32, width, height, idArea, area, idSector, sector, idProblem, problem, grade, photographer, tagged);
	}

	@Override
	public String toString() {
		return "Frontpage [numProblems=" + numProblems + ", numProblemsWithCoordinates=" + numProblemsWithCoordinates
				+ ", numProblemsWithTopo=" + numProblemsWithTopo + ", numTicks=" + numTicks + ", numImages=" + numImages
				+ ", numMovies=" + numMovies + ", randomMedia=" + randomMedia + ", metadata=" + metadata + "]";
	}
}
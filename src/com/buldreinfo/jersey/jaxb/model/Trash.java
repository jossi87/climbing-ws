package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Trash implements IMetadata {
	public class TrashItem {
		private final int idArea;
		private final int idSector;
		private final int idProblem;
		private final int idMedia;
		private final String name;
		private final String when;
		private final String by;
		public TrashItem(int idArea, int idSector, int idProblem, int idMedia, String name, String when, String by) {
			this.idArea = idArea;
			this.idSector = idSector;
			this.idProblem = idProblem;
			this.idMedia = idMedia;
			this.name = name;
			this.when = when;
			this.by = by;
		}
		public String getBy() {
			return by;
		}
		public int getIdArea() {
			return idArea;
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
		public String getName() {
			return name;
		}
		public String getWhen() {
			return when;
		}
	}
	private Metadata metadata;
	private List<TrashItem> trash = new ArrayList<>();
	
	public Trash() {
	}

	public void addTrashItem(int idArea, int idSector, int idProblem, int idMedia, String name, String when, String by) {
		trash.add(new TrashItem(idArea, idSector, idProblem, idMedia, name, when, by));
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	public List<TrashItem> getTrash() {
		return trash;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
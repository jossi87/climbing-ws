package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;

public class Trash implements IMetadata {
	public class TrashItem {
		private final int idArea;
		private final int idSector;
		private final int idProblem;
		private final String name;
		private final String when;
		private final String by;
		public TrashItem(int idArea, int idSector, int idProblem, String name, String when, String by) {
			this.idArea = idArea;
			this.idSector = idSector;
			this.idProblem = idProblem;
			this.name = name;
			this.when = when;
			this.by = by;
		}
		public int getIdArea() {
			return idArea;
		}
		public int getIdSector() {
			return idSector;
		}
		public int getIdProblem() {
			return idProblem;
		}
		public String getName() {
			return name;
		}
		public String getWhen() {
			return when;
		}
		public String getBy() {
			return by;
		}
	}
	private Metadata metadata;
	private List<TrashItem> trash = new ArrayList<>();
	
	public Trash() {
	}

	public void addTrashItem(int idArea, int idSector, int idProblem, String name, String when, String by) {
		trash.add(new TrashItem(idArea, idSector, idProblem, name, when, by));
	}
	
	public List<TrashItem> getTrash() {
		return trash;
	}
	
	@Override
	public Metadata getMetadata() {
		return metadata;
	}
	
	@Override
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
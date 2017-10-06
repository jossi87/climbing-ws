package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class SisProblem {
	public class Shape {
		private final float x;
		private final float y;
		private final int r;
		private final String text;
		public Shape(float x, float y, int r, String text) {
			this.x = x;
			this.y = y;
			this.r = r;
			this.text = text;
		}
		public int getR() {
			return r;
		}
		public String getText() {
			return text;
		}
		public float getX() {
			return x;
		}
		public float getY() {
			return y;
		}
	}
	
	private final int id;
	private final String image;
	private final String grade;
	private final String created;
	private final String type;
	private final String creator;
	private final List<Shape> shapes;
	private final boolean deleted;
	private final List<SisTick> ticks;
	
	public SisProblem(int id, String image, String grade, String created, String type, String creator, List<Shape> shapes, boolean deleted, List<SisTick> ticks) {
		this.id = id;
		this.image = image;
		this.grade = grade;
		this.created = created;
		this.type = type;
		this.creator = creator;
		this.shapes = shapes;
		this.deleted = deleted;
		this.ticks = ticks;
	}

	public String getCreated() {
		return created;
	}
	
	public String getCreator() {
		return creator;
	}

	public String getGrade() {
		return grade;
	}

	public int getId() {
		return id;
	}
	
	public String getImage() {
		return image;
	}

	public List<Shape> getShapes() {
		return shapes;
	}
	
	public List<SisTick> getTicks() {
		return ticks;
	}
	
	public String getType() {
		return type;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
}
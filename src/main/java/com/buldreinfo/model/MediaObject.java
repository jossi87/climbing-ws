package com.buldreinfo.model;

import java.util.List;

public record MediaObject(String name, float score, BoundingPoly boundingPoly) {
	public record BoundingPoly(List<NormalizedVertex> normalizedVertices) {
		public NormalizedVertex getNormalizedVertices(int index) {
			return normalizedVertices.get(index);
		}
		
		public List<NormalizedVertex> getNormalizedVerticesList() {
			return normalizedVertices;
		}
	}
	
	public record NormalizedVertex(float x, float y) {
		public float getX() {
			return x;
		}
		
		public float getY() {
			return y;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public BoundingPoly getBoundingPoly() {
		return boundingPoly;
	}
}

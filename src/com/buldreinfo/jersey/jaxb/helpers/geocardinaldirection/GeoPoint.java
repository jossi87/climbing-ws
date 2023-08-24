package com.buldreinfo.jersey.jaxb.helpers.geocardinaldirection;

public class GeoPoint {
	private final double latitude;
	private final double longitude;
	private final double elevation;
	private double distanceToCenter;
	private double neighbourDistance = Double.MAX_VALUE;
	private GeoPoint neighbourPoint;
	
	public GeoPoint(double latitude, double longitude, double elevation) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
	}

	public double getDistanceToCenter() {
		return distanceToCenter;
	}
	
	public double getElevation() {
		return elevation;
	}
	
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public GeoPoint getNeighbourPoint() {
		return neighbourPoint;
	}
	
	public double getNeighbourDistance() {
		return neighbourDistance;
	}
	
	public void setDistanceToCenter(double distanceToCenter) {
		this.distanceToCenter = distanceToCenter;
	}
	
	public void setNeighbour(GeoPoint neighbourPoint, double neighbourDistance) {
		this.neighbourPoint = neighbourPoint;
		this.neighbourDistance = neighbourDistance;
	}

	@Override
	public String toString() {
		return latitude + "," + longitude + " (" + elevation + ")";
	}
}
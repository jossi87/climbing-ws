package com.buldreinfo.jersey.jaxb.helpers.geocardinaldirection;
import java.util.HashMap;
import java.util.Map;

public class GeoPoint {
	private final double latitude;
	private final double longitude;
	private final double elevation;
	private final Map<GeoPoint, Double> neighbours = new HashMap<>();
	
	public GeoPoint(double latitude, double longitude, double elevation) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getElevation() {
		return elevation;
	}
	
	public Map<GeoPoint, Double> getNeighbours() {
		return neighbours;
	}

	@Override
	public String toString() {
		return "GeoPoint [latitude=" + latitude + ", longitude=" + longitude + ", elevation=" + elevation
				+ ", neighbours=" + neighbours.size() + "]";
	}
}
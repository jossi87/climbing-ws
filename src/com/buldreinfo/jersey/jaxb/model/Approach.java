package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;

public class Approach {
	private final List<Coordinates> coordinates;
	private final double calculatedDurationInMinutes;
	private final long distance;
	private final long elevationGain;
	private final long elevationLoss;

	public Approach(List<Coordinates> coordinates) {
		this.coordinates = coordinates;
		this.calculatedDurationInMinutes = GeoHelper.calculateHikingDurationInMinutes(coordinates);
		this.distance = Math.round(coordinates.get(coordinates.size()-1).getDistance());
		double gain = 0, loss = 0;
		for (int i = 1; i < coordinates.size(); i++) {
			double elevationDiff = coordinates.get(i).getElevation() - coordinates.get(i - 1).getElevation();
			if (elevationDiff > 0) {
				gain += elevationDiff;
			}
			else if (elevationDiff < 0) {
				loss -= elevationDiff;
			}
		}
		this.elevationGain = Math.round(gain);
		this.elevationLoss = Math.round(loss);
	}

	public long getElevationGain() {
		return elevationGain;
	}

	public long getElevationLoss() {
		return elevationLoss;
	}

	public List<Coordinates> getCoordinates() {
		return coordinates;
	}

	public double getCalculatedDurationInMinutes() {
		return calculatedDurationInMinutes;
	}

	public long getDistance() {
		return distance;
	}
}
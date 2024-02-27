package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;

public record Approach(List<Coordinates> coordinates, double calculatedDurationInMinutes, long distance, long elevationGain, long elevationLoss) {
	public static Approach from(List<Coordinates> coordinates) {
		double calculatedDurationInMinutes = GeoHelper.calculateHikingDurationInMinutes(coordinates);
		long distance = Math.round(coordinates.get(coordinates.size()-1).getDistance());
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
		long elevationGain = Math.round(gain);
		long elevationLoss = Math.round(loss);
		return new Approach(coordinates, calculatedDurationInMinutes, distance, elevationGain, elevationLoss);
	}
}
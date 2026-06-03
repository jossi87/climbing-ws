package com.buldreinfo.jersey.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;

public record Trail(
		int id, 
		boolean isDescent, 
		boolean delete, 
		String title, 
		String description, 
		List<Coordinates> path, 
		List<TrailMarker> markers, 
		List<Media> media, 
		List<TrailSector> sectors,
		double calculatedDurationInMinutes,
		long distance,
		long elevationGain,
		long elevationLoss
		) {
	public static class TrailBuilder {
		public final int id;
		public final boolean isDescent;
		public final String title;
		public final String description;
		public final List<Coordinates> path = new ArrayList<>();
		public final List<Trail.TrailMarker> markers = new ArrayList<>();
		public TrailBuilder(int id, boolean isDescent, String title, String description) {
			this.id = id;
			this.isDescent = isDescent;
			this.title = title;
			this.description = description;
		}
	}
	public record TrailMarker(Coordinates coordinates, String label) {}
	public record TrailSector(int sectorId, String areaName, String sectorName) {}

	public static Trail withCalculatedStats(
			int id, 
			boolean isDescent, 
			boolean delete, 
			String title, 
			String description, 
			List<Coordinates> path, 
			List<TrailMarker> markers, 
			List<Media> media, 
			List<TrailSector> sectors
			) {
		double calculatedDurationInMinutes = 0;
		long distance = 0;
		long elevationGain = 0;
		long elevationLoss = 0;
		if (path != null && !path.isEmpty()) {
			double totalDistance = 0;
			path.get(0).setDistance(0.0);
			for (int i = 1; i < path.size(); i++) {
				Coordinates prev = path.get(i - 1);
				Coordinates curr = path.get(i);
				// Haversine implementation
				final int R = 6371000;
				double latDistance = Math.toRadians(curr.getLatitude() - prev.getLatitude());
				double lonDistance = Math.toRadians(curr.getLongitude() - prev.getLongitude());
				double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
						+ Math.cos(Math.toRadians(prev.getLatitude())) * Math.cos(Math.toRadians(curr.getLatitude()))
						* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
				double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
				double distanceDelta = R * c; // Horizontal distance in meters
				totalDistance += distanceDelta;
				curr.setDistance(totalDistance);
			}
			distance = Math.round(totalDistance);
			calculatedDurationInMinutes = GeoHelper.calculateHikingDurationInMinutes(path);
			double gain = 0, loss = 0;
			for (int i = 1; i < path.size(); i++) {
				double elevationDiff = path.get(i).getElevation() - path.get(i - 1).getElevation();
				if (elevationDiff > 0) {
					gain += elevationDiff;
				} else if (elevationDiff < 0) {
					loss -= elevationDiff;
				}
			}
			elevationGain = Math.round(gain);
			elevationLoss = Math.round(loss);
		}

		return new Trail(id, isDescent, delete, title, description, path, markers, media, sectors, calculatedDurationInMinutes, distance, elevationGain, elevationLoss);
	}
}
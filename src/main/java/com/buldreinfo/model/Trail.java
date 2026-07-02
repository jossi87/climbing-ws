package com.buldreinfo.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.util.GeoUtils;

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

		List<Coordinates> processedPath = path;

		if (path != null && !path.isEmpty()) {
			List<Coordinates> newPath = new ArrayList<>();
			double totalDistance = 0;

			newPath.add(path.get(0).withDistance(0.0));

			for (int i = 1; i < path.size(); i++) {
				Coordinates prev = newPath.get(i - 1);
				Coordinates curr = path.get(i);

				double distanceDelta = GeoUtils.getHaversineDistanceInMeters(
						prev.latitude(), prev.longitude(), 
						curr.latitude(), curr.longitude()
						);
				totalDistance += distanceDelta;
				newPath.add(curr.withDistance(totalDistance));
			}

			processedPath = newPath;
			distance = Math.round(totalDistance);
			calculatedDurationInMinutes = GeoHelper.calculateHikingDurationInMinutes(processedPath);

			double gain = 0, loss = 0;
			for (int i = 1; i < processedPath.size(); i++) {
				double elevationDiff = processedPath.get(i).elevation() - processedPath.get(i - 1).elevation();
				if (elevationDiff > 0) {
					gain += elevationDiff;
				} else if (elevationDiff < 0) {
					loss -= elevationDiff;
				}
			}
			elevationGain = Math.round(gain);
			elevationLoss = Math.round(loss);
		}

		return new Trail(id, isDescent, delete, title, description, processedPath, markers, media, sectors, 
				calculatedDurationInMinutes, distance, elevationGain, elevationLoss);
	}
}
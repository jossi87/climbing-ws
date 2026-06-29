package com.buldreinfo.helpers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.service.ElevationService;
import com.buldreinfo.util.GeoUtils;

public class GeoHelper {
	public class GeoPoint {
		private final double latitude;
		private final double longitude;
		private final double elevation;
		private double distanceToCenter;
		private double neighbourDistance;
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

		public double getNeighbourDistance() {
			return neighbourDistance;
		}

		public GeoPoint getNeighbourPoint() {
			return neighbourPoint;
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

	private static Logger logger = LogManager.getLogger();
	
	public static CompassDirection calculateCompassDirection(Setup setup, List<Coordinates> outline) {
		final String direction = calculateWallDirection(setup, outline);
		if (direction == null) {
			return null;
		}
		return setup.compassDirections().stream().filter(cd -> cd.direction().equals(direction)).findAny().get();
	}
	
	public static long calculateHikingDurationInMinutes(List<Coordinates> approach) {
		final double e = -3.5; // Exponential growth factor in Tobler's function
		final double a = 0.05; // Slope offset with max speed. 5% down hill will result in max speed -2.86 degrees
		final double c = 1 / Math.exp(e * a); // Horizontal speed to max speed factor: Vmax=C*Vflat
		final int maxLimit = 3; // >30% incline / >40% decline
		double totalCalculatedDistance = 0;
		for (int i = 1; i < approach.size(); i++) {
			Coordinates prevCoord = approach.get(i-1);
			Coordinates coord = approach.get(i);
			double elevation = coord.getElevation()-prevCoord.getElevation();
			double distance = coord.getDistance()-prevCoord.getDistance();
			double distanceMultiplier = 1.0 / (c * Math.exp(e * Math.abs(elevation / distance + a)));
			totalCalculatedDistance += (Math.min(maxLimit, distanceMultiplier) * distance);
		}
		double meterPerSecond = 1.3; // Average hiking speed is between 1.2 and 1.4 m/s
		double durationSeconds = totalCalculatedDistance / meterPerSecond;
		long durationMinutes = Math.round(durationSeconds / 60.0);
		return durationMinutes;
	}
	
	public static int getElevation(ElevationService elevationService, double latitude, double longitude) {
        List<Coordinates> coords = new ArrayList<>(List.of(new Coordinates(latitude, longitude)));
        coords.getFirst().roundCoordinatesToMaximum10digitsAfterComma();
        elevationService.fillElevations(coords);
        return (int) Math.round(coords.getFirst().getElevation());
    }
	
	private static String calculateWallDirection(Setup setup, List<Coordinates> outline) {
		if (!setup.isClimbing() || outline == null || outline.isEmpty() || outline.stream().filter(x -> x.getElevation() == 0).findAny().isPresent()) {
			return null;
		}
		try {
			GeoHelper calc = new GeoHelper();
			return calc.getWallDirection(outline);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return null;
	}
	private List<GeoPoint> geoPoints = new ArrayList<>();
	private GeoPoint firstPointLow;
	private GeoPoint firstPointHigh;
	private GeoPoint secondPointLow;
	private GeoPoint secondPointHigh;
	private double wallBearing;
	private double wallPerpendicularBearing;
	private int sunOffset;

	private long wallDirectionDegrees;

	private GeoHelper() {
	}
	
	private void calculateBoundingBox() {
		// Find bounding box
		List<GeoPoint> boundingBoxPoints = geoPoints.stream()
		        .sorted(Comparator.comparingDouble(GeoPoint::getDistanceToCenter).reversed())
		        .limit(4)
		        .toList();
		if (boundingBoxPoints.size() != 4) throw new IllegalArgumentException("Invalid bounding box");
		// Group the four points in two pairs
		for (int i = 0; i < boundingBoxPoints.size(); i++) {
			GeoPoint a = boundingBoxPoints.get(i);
			for (int j = 0; j < boundingBoxPoints.size(); j++) {
				if (i != j) {
					GeoPoint b = boundingBoxPoints.get(j);
					double distance = GeoUtils.getHaversineDistanceInMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
					if (a.getNeighbourPoint() == null || distance < a.getNeighbourDistance()) {
						a.setNeighbour(b, distance);
					}
				}
			}
		}
		// Set variables
		GeoPoint g11 = boundingBoxPoints.getFirst();
		GeoPoint g12 = g11.getNeighbourPoint();
		GeoPoint g21 = boundingBoxPoints.stream().filter(x -> x != g11 && x != g12).findAny().get();
		GeoPoint g22 = g21.getNeighbourPoint();
		if (g11.getElevation() < g12.getElevation()) {
			firstPointLow = g11;
			firstPointHigh = g12;
		}
		else {
			firstPointLow = g12;
			firstPointHigh = g11;
		}
		if (g21.getElevation() < g22.getElevation()) {
			secondPointLow = g21;
			secondPointHigh = g22;
		}
		else {
			secondPointLow = g22;
			secondPointHigh = g21;
		}
		// Validate that the bounding box is good enough to do calculations on
		double distanceLowToHigh = (GeoUtils.getHaversineDistanceInMeters(firstPointLow.getLatitude(), firstPointLow.getLongitude(), firstPointHigh.getLatitude(), firstPointHigh.getLongitude()) +
				GeoUtils.getHaversineDistanceInMeters(secondPointLow.getLatitude(), secondPointLow.getLongitude(), secondPointHigh.getLatitude(), secondPointHigh.getLongitude())) / 2.0;
		double distanceFirstToSecond = (GeoUtils.getHaversineDistanceInMeters(firstPointHigh.getLatitude(), firstPointLow.getLongitude(), secondPointLow.getLatitude(), secondPointLow.getLongitude()) +
				GeoUtils.getHaversineDistanceInMeters(firstPointHigh.getLatitude(), secondPointLow.getLongitude(), secondPointHigh.getLatitude(), secondPointHigh.getLongitude())) / 2.0;
		if (distanceLowToHigh * 1.5 >= distanceFirstToSecond) {
			throw new RuntimeException("Bounding box is not a rectangle, expecting minimum ratio 1.5:1");
		}
	}

	private void calculateDistanceToCenter() {
		// TODO: This fails when polygon crosses 180th meridian!
		double minLatitude = geoPoints.stream().mapToDouble(GeoPoint::getLatitude).min().getAsDouble();
		double maxLatitude = geoPoints.stream().mapToDouble(GeoPoint::getLatitude).max().getAsDouble();
		double minLongitude = geoPoints.stream().mapToDouble(GeoPoint::getLongitude).min().getAsDouble();
		double maxLongitude = geoPoints.stream().mapToDouble(GeoPoint::getLongitude).max().getAsDouble();
		double centerLatitude = (minLatitude + maxLatitude) / 2.0;
		double centerLongitude = (minLongitude + maxLongitude) / 2.0;
		for (GeoPoint p : geoPoints) {
			p.setDistanceToCenter(GeoUtils.getHaversineDistanceInMeters(centerLatitude, centerLongitude, p.getLatitude(), p.getLongitude()));
		}
	}

	private void calculateSunOffset() {
		// Use points with greatest elevation difference
		wallPerpendicularBearing = getMeanAngle(getBearing(firstPointHigh, firstPointLow), getBearing(secondPointHigh, secondPointLow));
		double diff = ((wallPerpendicularBearing - wallBearing + 360) % 360);
		sunOffset = diff > 180? -90 : 90;
	}

	private String convertFromDegreesToOrdinalName(long bearing) {
		String directions[] = { "North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest"};
		int num = Math.round(bearing * 8f / 360f) % 8;
		return directions[num];
	}

	private int getBearing(GeoPoint g1, GeoPoint g2) {
		double latitude1 = Math.toRadians(g1.getLatitude());
		double latitude2 = Math.toRadians(g2.getLatitude());
		double longDiff = Math.toRadians(g2.getLongitude()-g1.getLongitude());
		double y = Math.sin(longDiff)*Math.cos(latitude2);
		double x = (Math.cos(latitude1)*Math.sin(latitude2)) - (Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff));
		return (int)Math.round((Math.toDegrees(Math.atan2(y, x))+360)%360);
	}

	private int getMeanAngle(double... anglesDeg) {
		double x = 0.0;
		double y = 0.0;
		for (double angleD : anglesDeg) {
			double angleR = Math.toRadians(angleD);
			x += Math.cos(angleR);
			y += Math.sin(angleR);
		}
		double avgR = Math.atan2(y / anglesDeg.length, x / anglesDeg.length);
		int angle = (int)Math.round((Math.toDegrees(avgR)+360)%360);
		return angle;
	}

	private String getWallDirection(List<Coordinates> outline) {
		for (Coordinates coord : outline) {
			geoPoints.add(new GeoPoint(coord.getLatitude(), coord.getLongitude(), coord.getElevation()));
		}
		calculateDistanceToCenter();
		calculateBoundingBox();
		this.wallBearing = getBearing(firstPointLow, secondPointLow);
		// Add or subtract 90 degrees to get perpendicular vector in the walls facing direction
		calculateSunOffset();
		this.wallDirectionDegrees = ((Math.round(wallBearing) + sunOffset)+360) % 360;
		return convertFromDegreesToOrdinalName(wallDirectionDegrees);
	}
}
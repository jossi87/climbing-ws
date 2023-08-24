package com.buldreinfo.jersey.jaxb.helpers.geocardinaldirection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;

public class GeoCardinalDirectionCalculator {
	private static Logger logger = LogManager.getLogger();
	public static void main(String[] args) throws IOException {
		Map<String, String> walls = new TreeMap<>();
		walls.put("Gloppeveggen", new GeoCardinalDirectionCalculator("58.772557064361926,6.29968822002411;58.772488927078584,6.299779415130615;58.77264049758802,6.300629675388336;58.77284212705715,6.301233172416688;58.772893577148054,6.301176846027374;58.77276564705146,6.300640404224397;58.772679433024976,6.300377547740937").getCardinalDirection());
		walls.put("Nedre gulveggen", new GeoCardinalDirectionCalculator("58.93708443437351,5.950142741203309;58.93651146871164,5.950689911842347;58.93649209289499,5.95062553882599;58.9367190660648,5.9503787755966195;58.93705121923254,5.950099825859071").getCardinalDirection());
		walls.put("Sirekrok", new GeoCardinalDirectionCalculator("58.997344884175014,6.9001731276512155;58.99744573690774,6.900326013565064;58.99752586626565,6.90043330192566;58.9976301723056,6.900583505630494;58.99774898409857,6.9007229804992685;58.99793410863329,6.900842338800431;58.99797693580924,6.900890618562699;58.99798384341329,6.900961697101594;58.998053610136395,6.901030093431474;58.998112324595766,6.901174932718278;58.9981510070084,6.901191025972367;58.99819383391457,6.901266127824784;58.99824356831939,6.901409626007081;58.998276724549335,6.901392191648484;58.99823527925689,6.901263445615769;58.99815653306379,6.901148110628129;58.998124758233146,6.901121288537979;58.99807018834682,6.901003271341325;58.99800940153618,6.900934875011445;58.99799696785715,6.90087452530861;58.99777799621828,6.900658607482911;58.997670236793226,6.90057009458542;58.997359390408185,6.900146305561067").getCardinalDirection());
		walls.put("Ålgård", new GeoCardinalDirectionCalculator("58.77264881917226,5.848643928766251;58.77265160027566,5.848128944635392;58.77270583174784,5.848126262426376;58.77270166009914,5.84864929318428").getCardinalDirection());
		for (String wall : walls.keySet()) {
			logger.debug(wall + ": " + walls.get(wall));
		}
	}
	private final List<GeoPoint> geoPoints;
	private double treshold;
	private GeoPoint gNorth;
	private GeoPoint gSouth;
	private final String vector;
	private final double bearing;
	private long wallDirection;
	private String cardinalDirection;

	public GeoCardinalDirectionCalculator(String outline) throws IOException {
		this.geoPoints = parseOutline(outline);
		calculateDistanceBetweenPointsAndSetNorthAndSouthPoints();
		this.vector = String.format("%f,%f;%f,%f", gNorth.getLatitude(), gNorth.getLongitude(), gSouth.getLatitude(), gSouth.getLongitude());
		this.bearing = getBearing(gNorth, gSouth);
		// Add or subtract 90 degrees to get perpendicular vector in the walls facing direction
		int degreesDelta = getPerpendicularDegrees();
		if (degreesDelta == -90 || degreesDelta == 90) {
			this.wallDirection = ((Math.round(bearing) + degreesDelta)+360) % 360;
			this.cardinalDirection = convertFromDegreesToCardinalDirection(wallDirection);
		}
	}

	public String getCardinalDirection() {
		return cardinalDirection;
	}

	public String getVector() {
		return vector;
	}

	private void calculateDistanceBetweenPointsAndSetNorthAndSouthPoints() {
		// Calculate distance between points
		double d = 0;
		GeoPoint g1 = null, g2 = null;
		for (int i = 0; i < geoPoints.size(); i++) {
			GeoPoint a = geoPoints.get(i);
			for (int j = i+1; j < geoPoints.size(); j++) {
				GeoPoint b = geoPoints.get(j);
				double distance = getDistance(a, b, true);
				a.getNeighbours().put(b, distance);
				b.getNeighbours().put(a, distance);
				if (distance > d) {
					d = distance;
					g1 = a;
					g2 = b;
				}
			}
		}
		// Now find point on each side of vector with lowest elevation (the points calculated above are most likely on ground and on top of cliff)
		this.treshold = d/5.0;
		double e = g1.getElevation();
		for (GeoPoint g : g1.getNeighbours().keySet()) {
			if (g1.getNeighbours().get(g) < treshold && g.getElevation() < e) {
				e = g.getElevation();
				g1 = g;
			}
		}
		e = g2.getElevation();
		for (GeoPoint g : g2.getNeighbours().keySet()) {
			if (g2.getNeighbours().get(g) < treshold && g.getElevation() < e) {
				e = g.getElevation();
				g2 = g;
			}
		}
		// Order points
		this.gNorth = g1;
		this.gSouth = g2;
		if (g2.getLongitude() < g1.getLongitude()) {
			gNorth = g2;
			gSouth = g1;
		}
	}

	private String convertFromDegreesToCardinalDirection(long bearing) {
		if (bearing < 0 && bearing > -180) {
			// Normalize to [0,360]
			bearing = 360 + bearing;
		}
		if (bearing > 360 || bearing < -180) {
			throw new RuntimeException("Invalid bearing: " + bearing);
		}
		String directions[] = {
				"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
				"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N"};
		String cardinal = directions[(int) Math.floor(((bearing + 11.25) % 360) / 22.5)];
		return cardinal;
	}

	private long getBearing(GeoPoint g1, GeoPoint g2){
		double longitude1 = g1.getLongitude();
		double longitude2 = g2.getLongitude();
		double latitude1 = Math.toRadians(g1.getLatitude());
		double latitude2 = Math.toRadians(g2.getLatitude());
		double longDiff= Math.toRadians(longitude2-longitude1);
		double y= Math.sin(longDiff)*Math.cos(latitude2);
		double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
		return Math.round(Math.toDegrees(Math.atan2(y, x))+360)%360;
	}

	private double getDistance(GeoPoint g1, GeoPoint g2, boolean ignoreElevation) {
		final int R = 6371; // Radius of the earth
		double latDistance = Math.toRadians(g2.getLatitude() - g1.getLatitude());
		double lngDistance = Math.toRadians(g2.getLongitude() - g1.getLongitude());
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(g1.getLatitude())) * Math.cos(Math.toRadians(g2.getLatitude()))
				* Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters
		double height = ignoreElevation? 0 : g2.getElevation() - g1.getElevation();
		distance = Math.pow(distance, 2) + Math.pow(height, 2);
		return Math.sqrt(distance);
	}

	private int getPerpendicularDegrees() {
		// West/East-facing wall --> use latitude, North/South-facing wall --> use longitude
		boolean useLatitudeNotLongitude = bearing > 90 && bearing < 270;
		int res = getPerpendicularDegrees(gNorth, useLatitudeNotLongitude);
		if (res == 0) {
			res = getPerpendicularDegrees(gSouth, useLatitudeNotLongitude);
		}
		return res;
	}

	private int getPerpendicularDegrees(GeoPoint geoPoint, boolean useLatitudeNotLongitude) {
		GeoPoint first = geoPoint;
		GeoPoint second = geoPoint;
		for (Entry<GeoPoint, Double> e : geoPoint.getNeighbours().entrySet()) {
			GeoPoint g = e.getKey();
			double gCoord = useLatitudeNotLongitude? g.getLatitude() : g.getLongitude();
			if (e.getValue().doubleValue() < treshold) {
				double firstCoord = useLatitudeNotLongitude? first.getLatitude() : first.getLongitude();
				if (first == null || gCoord < firstCoord) {
					first = g;
				}
				double secondCoord = useLatitudeNotLongitude? second.getLatitude() : second.getLongitude();
				if (second == null || gCoord > secondCoord) {
					second = g;
				}
			}
		}
		if (Math.abs(first.getElevation() - second.getElevation()) < 5) {
			logger.debug(first + " og " + second);
			return 0; // Not enough data to give a good suggestion
		}
		int res = 90;
		if ((useLatitudeNotLongitude && first.getElevation() > second.getElevation()) ||
				(!useLatitudeNotLongitude && first.getElevation() < second.getElevation())) {
			res = -90;
		}
		return res;
	}

	private List<GeoPoint> parseOutline(String outline) throws IOException {
		List<GeoPoint> geoPoints = new ArrayList<>();
		String locations = outline.replaceAll(";", "|");
		double latitude = 0, longitude = 0, elevation = 0;
		String apiKey = "AIzaSyDDWmhISdnIqGPA7pJ3goabdueN4M_frGM";
		URL url = new URL(String.format("https://maps.googleapis.com/maps/api/elevation/json?locations=%s&key=%s", locations, apiKey));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		int responseCode = connection.getResponseCode();
		Preconditions.checkArgument(responseCode == 200, "Invalid responseCode: " + responseCode);
		try (InputStream is = connection.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				JsonReader jsonReader = new JsonReader(isr)) {
			jsonReader.beginObject();
			while (jsonReader.hasNext()) {
				String field = jsonReader.nextName();
				if (field.equals("results")) {
					jsonReader.beginArray();
					while (jsonReader.hasNext()) {
						jsonReader.beginObject();
						while (jsonReader.hasNext()) {
							field = jsonReader.nextName();
							if (field.equals("elevation")) {
								elevation = jsonReader.nextDouble();
							}
							else if (field.equals("location")) {
								jsonReader.beginObject();
								while (jsonReader.hasNext()) {
									field = jsonReader.nextName();
									if (field.equals("lat")) {
										latitude = jsonReader.nextDouble();
									}
									else if (field.equals("lng")) {
										longitude = jsonReader.nextDouble();
									}
									else {
										jsonReader.skipValue();
									}
								}
								jsonReader.endObject();
							}
							else {
								jsonReader.skipValue();
							}
						}
						geoPoints.add(new GeoPoint(latitude, longitude, elevation));
						jsonReader.endObject();						
					}
					jsonReader.endArray();
				} else {
					jsonReader.skipValue();
				}

			}
			jsonReader.endObject();
		}
		return geoPoints;
	}
}

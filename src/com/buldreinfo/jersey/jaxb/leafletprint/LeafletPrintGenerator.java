package com.buldreinfo.jersey.jaxb.leafletprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.google.common.base.Joiner;
import com.google.gson.Gson;

public class LeafletPrintGenerator {
	private static Logger logger = LogManager.getLogger();
	public static String getDistance(String polyline) {
		double distance = 0;
		double prevLat = 0;
		double prevLng = 0;
		for (String part : polyline.split(";")) {
			String[] latLng = part.split(",");
			double lat = Double.parseDouble(latLng[0]);
			double lng = Double.parseDouble(latLng[1]);
			if (prevLat > 0 && prevLng > 0) {
				distance += distance(prevLat, lat, prevLng, lng, 0, 0);
			}
			prevLat = lat;
			prevLng = lng;
		}
		long meter = Math.round(distance);
		if (meter > 1000) {
			return meter/1000 + " km";
		}
		return meter + " meter";
	}
	
	public static Path takeSnapshot(Leaflet leaflet) throws IOException, InterruptedException {
		Path res = takeSnapshotWorker(leaflet);
		if (res == null) {
			Thread.sleep(1000);
			res = takeSnapshotWorker(leaflet);
		}
		logger.debug("takeSnapshot(leaflet={}) - res={}", leaflet, res);
		return res;
	}
	
	private static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
	    final int R = 6371; // Radius of the earth

	    double latDistance = Math.toRadians(lat2 - lat1);
	    double lonDistance = Math.toRadians(lon2 - lon1);
	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c * 1000; // convert to meters

	    double height = el1 - el2;

	    distance = Math.pow(distance, 2) + Math.pow(height, 2);

	    return Math.sqrt(distance);
	}
	
	private static Path takeSnapshotWorker(Leaflet leaflet) throws IOException, InterruptedException {
		Path png = GlobalFunctions.getPathTemp().resolve("leafletScreenshot").resolve(System.currentTimeMillis() + "_" + UUID.randomUUID() + ".png");
		Files.createDirectories(png.getParent());
		Path script = GlobalFunctions.getPathLeafletPrint();
		Gson gson = new Gson();
		String json = gson.toJson(leaflet);
		String base64EncodedJson = Base64.getEncoder().encodeToString(json.getBytes());
		ProcessBuilder builder = new ProcessBuilder("node", script.toString(), png.toString(), base64EncodedJson);
		logger.debug("Running: " + Joiner.on(" ").join(builder.command()));
		builder.redirectErrorStream(true);
		final Process process = builder.start();
		watch(process);
		process.waitFor(5, TimeUnit.SECONDS);
		if (!Files.exists(png) || Files.size(png) == 0) {
			logger.warn("takeSnapshot() failed - Files.exists({})={}, json={}", png.toString(), Files.exists(png), json);
			return null;
		}
		return png;
	}
	
	private static void watch(final Process process) {
	    new Thread() {
	        public void run() {
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            String line = null; 
	            try {
	                while ((line = input.readLine()) != null) {
	                    logger.debug(line);
	                }
	            } catch (IOException e) {
	                throw new RuntimeException(e.getMessage(), e);
	            }
	        }
	    }.start();
	}
}

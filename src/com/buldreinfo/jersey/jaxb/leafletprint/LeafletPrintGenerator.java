package com.buldreinfo.jersey.jaxb.leafletprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.google.common.base.Joiner;
import com.google.gson.Gson;

public class LeafletPrintGenerator {
	private static Logger logger = LogManager.getLogger();
	public static LatLng getCenter(Collection<SectorProblem> problems) {
		double x = 0.0;
	    double y = 0.0;
	    double z = 0.0;

	    for (SectorProblem p : problems) {
	        double lat = p.getCoordinates().getLatitude() * Math.PI / 180;
	        double lon = p.getCoordinates().getLongitude() * Math.PI / 180;

	        double a = Math.cos(lat) * Math.cos(lon);
	        double b = Math.cos(lat) * Math.sin(lon);
	        double c = Math.sin(lat);

	        x += a;
	        y += b;
	        z += c;
	    }

	    x /= problems.size();
	    y /= problems.size();
	    z /= problems.size();

	    double lon = Math.atan2(y, x);
	    double hyp = Math.sqrt(x * x + y * y);
	    double lat = Math.atan2(z, hyp);

	    double newX = (lat * 180 / Math.PI);
	    double newY = (lon * 180 / Math.PI);

	    return new LatLng(newX, newY);
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
	
	private static Path takeSnapshotWorker(Leaflet leaflet) throws IOException, InterruptedException {
		Path png = IOHelper.getPathTemp().resolve("leafletScreenshot").resolve(System.currentTimeMillis() + "_" + UUID.randomUUID() + ".png");
		IOHelper.createDirectories(png.getParent());
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

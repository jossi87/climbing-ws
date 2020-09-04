package com.buldreinfo.jersey.jaxb.leafletprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.google.common.base.Joiner;
import com.google.gson.Gson;

public class LeafletPrintGenerator {
	private static final String INIT = "mkdir /mnt/buldreinfo/media/puppeteer && mkdir /mnt/buldreinfo/media/puppeteer/temp && cd /mnt/buldreinfo/media/puppeteer && npm install puppeteer && chmod -R 777 /mnt/buldreinfo/media/puppeteer/temp";
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
		final Path root = Paths.get("/mnt/buldreinfo/media/puppeteer/temp");
		final String filenamePrefix = System.currentTimeMillis() + "_" + UUID.randomUUID();
		final Path js = root.resolve(filenamePrefix + ".js");
		final Path png = root.resolve(filenamePrefix + ".png");
		if (!Files.exists(root)) {
			logger.error("takeSnapshot(leaflet={}) - {} does not exist ({})", leaflet, root, INIT);
			return null;
		}
		Gson gson = new Gson();
		String json = gson.toJson(leaflet);
		String base64EncodedJson = Base64.getEncoder().encodeToString(json.getBytes());
		String url = "https://buldreinfo.com/leaflet-print/" + base64EncodedJson;
		// Write script
		List<String> lines = new ArrayList<>();
		lines.add("const puppeteer = require('puppeteer');");
		lines.add("(async () => {");
		lines.add("  const browser = await puppeteer.launch({");
		lines.add("    args: ['--no-sandbox', '--disable-setuid-sandbox']");
		lines.add("  });");
		lines.add("  const page = await browser.newPage();");
		lines.add("  await page.setViewport({");
		lines.add("    width: 1280,");
		lines.add("    height: 720,");
		lines.add("    deviceScaleFactor: 1,");
		lines.add("  });");
		lines.add("  await page.goto('" + url + "', {waitUntil: 'networkidle2'});");
		// lines.add("  await page.waitFor('my-selector', { timeout: 10000 }).catch(() => console.log('Waiting for my-selector timeouted'));");
		lines.add("  await page.screenshot({path: '" + png + "'});");
		lines.add("");
		lines.add("  await browser.close();");
		lines.add("})();");
		Files.write(js, lines);
		ProcessBuilder builder = new ProcessBuilder("node", js.toString());
		logger.debug("Running: " + Joiner.on(" ").join(builder.command()));
		builder.redirectErrorStream(true);
		final Process process = builder.start();
		watch(process);
		process.waitFor(10, TimeUnit.SECONDS);
		if (!Files.exists(png)) {
			logger.error("takeSnapshot(leaflet={}) - {} does not exist", png);
			return null;
		}
		else if (Files.size(png) == 0) {
			logger.error("takeSnapshot(leaflet={}) - size on {} = 0", png);
			return null;
		}
		return png;
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

package com.buldreinfo.jersey.jaxb.leafletprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Marker;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Outline;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Polyline;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.google.common.base.Joiner;
import com.google.gson.Gson;

/**
	sudo nano /etc/apt/sources.list.d/google-chrome.list
		deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main
	wget https://dl.google.com/linux/linux_signing_key.pub
	sudo apt-key add linux_signing_key.pub
	sudo apt update
	sudo apt install google-chrome-stable
 */
public class LeafletPrintGenerator {
	private static Logger logger = LogManager.getLogger();
	private final String chrome;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		List<Marker> markers = new ArrayList<>();
		markers.add(new Marker(58.9381417663, 5.9510064125, false, "test"));
		List<Outline> outlines = new ArrayList<>();
		outlines.add(new Outline("outline", "58.93507347676487,5.9511566162109375;58.935117766150235,5.951585769653321;58.93458628977521,5.9521222114563;58.93446449195333,5.951843261718751;58.93464165227945,5.951435565948487"));
		List<Polyline> polylines = new ArrayList<>();
		polylines.add(new Polyline("polyline", "58.936468478603715,5.945772081613541;58.93639374315781,5.945734530687333;58.936284407676844,5.945754647254945;58.936097567762175,5.945782810449601;58.93608857174076,5.946037620306016;58.9360512036267,5.9461985528469095;58.93605535564138,5.9464842081069955;58.93605535564138,5.9468382596969604;58.93604428360116,5.947005897760392;58.936013056968555,5.947718024253846;58.93598537682739,5.9481579065322885;58.936085025231684,5.948396623134614;58.93626356123651,5.948546826839448;58.9363728967835,5.948605835437775;58.93640317159683,5.949225425720216;58.93629245215047,5.949654579162598;58.93646960309414,5.949971079826356;58.936452995231804,5.9503787755966195;58.93612602631448,5.95087766647339;58.93580493636318,5.951060056686402;58.935577957181664,5.950995683670045;58.93519042829994,5.951038599014283;58.9348637934347,5.951317548751832"));
		LatLng defaultCenter = new LatLng(58.9381417663, 5.9510064125);
		int defaultZoom = 16;
		Leaflet leaflet = new Leaflet(markers, outlines, polylines, defaultCenter, defaultZoom);
		LeafletPrintGenerator generator = new LeafletPrintGenerator(true);
		Path png = generator.capture(leaflet);
		logger.debug(png);
	}
	
	private String encode(String json) {
		return json
				.replaceAll("\\{", "%7B")
				.replaceAll("\\}", "%7D")
				.replaceAll("\"", "%22");
	}
	
	public LeafletPrintGenerator(boolean windows) {
		this.chrome = !windows? "/usr/bin/google-chrome" : "C:/Program Files (x86)/Google/Chrome/Application/chrome";
	}
	
	public Path capture(Leaflet leaflet) throws IOException, InterruptedException {
		Path res = captureLeaflet(leaflet);
		if (res == null) {
			Thread.sleep(500);
			res = captureLeaflet(leaflet);
		}
		return res;
	}
	
	private Path captureLeaflet(Leaflet leaflet) throws IOException, InterruptedException {
		Path res = Files.createTempFile("leaflet", ".png");
		Gson gson = new Gson();
		String json = encode(gson.toJson(leaflet));
		String url = "https://buldreinfo.com/leaflet-print/" + json;
		ProcessBuilder builder = new ProcessBuilder(chrome, "--headless", "--disable-gpu", "--user-data-dir=" + res.getParent().toString(), "--no-sandbox", "--run-all-compositor-stages-before-draw", "--virtual-time-budget=10000", "--window-size=1280,720", "-screenshot=" + res, url);
		logger.debug("Running: " + Joiner.on(" ").join(builder.command()));
		builder.redirectErrorStream(true);
		final Process process = builder.start();
		watch(process);
		process.waitFor(10, TimeUnit.SECONDS);
		if (!Files.exists(res)) {
			logger.warn("captureLeaflet(leaflet={}) - res=null", leaflet);
			return null;
		}
		return res;
	}
	
	private void watch(final Process process) {
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

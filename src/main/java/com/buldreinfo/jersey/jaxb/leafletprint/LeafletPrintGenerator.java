package com.buldreinfo.jersey.jaxb.leafletprint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.google.gson.Gson;

/**
 * Generates map images by calling the sidecar microservice.
 * Source: https://github.com/jossi87/climbing-leaflet-renderer
 */
public class LeafletPrintGenerator {
	private static final Logger logger = LogManager.getLogger();

	// Internal Docker DNS uses the service name from docker-compose.yml
	private static final String RENDERER_URL = "http://climbing-leaflet-renderer:3000/render";

	// Reusable, thread-safe HTTP client
	private static final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	/**
	 * Calculates the geographic center of a collection of problems.
	 * Original logic preserved.
	 */
	public static LatLng getCenter(Collection<SectorProblem> problems) {
		if (problems == null || problems.isEmpty()) {
			return new LatLng(0, 0);
		}

		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		for (SectorProblem p : problems) {
			double lat = p.coordinates().getLatitude() * Math.PI / 180;
			double lon = p.coordinates().getLongitude() * Math.PI / 180;

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

	/**
	 * Sends the Leaflet data to the Sidecar Node.js service and returns the PNG bytes.
	 * This replaces the old 'takeSnapshot' that relied on local Node installation and paths.
	 */
	public static Optional<byte[]> takeSnapshot(Leaflet leaflet) {
		try {
			Gson gson = new Gson();
			String json = gson.toJson(leaflet);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(RENDERER_URL))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(45)) // Puppeteer needs time for tile loading
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();

			logger.debug("Dispatching map render request to: {}", RENDERER_URL);

			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

			if (response.statusCode() == 200) {
				byte[] pngBytes = response.body();
				logger.debug("Map snapshot generated successfully. Size: {} bytes", pngBytes.length);
				return Optional.of(pngBytes);
			}
			String errorBody = new String(response.body());
			logger.error("Map renderer returned error status: {}. Message: {}", 
					response.statusCode(), errorBody);
			return Optional.empty();

		} catch (IOException e) {
			logger.error("Network error while calling map-renderer. Ensure the container is running. Repo: https://github.com/jossi87/climbing-leaflet-renderer: {}", e.getMessage());
			return Optional.empty();
		} catch (InterruptedException e) {
			logger.error("Map render request was interrupted", e);
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (Exception e) {
			logger.error("Unexpected error during takeSnapshot", e);
			return Optional.empty();
		}
	}
}
package com.buldreinfo.service;

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
import org.springframework.stereotype.Service;

import com.buldreinfo.leafletprint.beans.Leaflet;
import com.buldreinfo.model.LatLng;
import com.buldreinfo.model.Sector.SectorProblem;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generates map images by calling the sidecar microservice.
 * Source: https://github.com/jossi87/climbing-leaflet-renderer
 */
@Service
public class LeafletPrintService {
	private static final Logger logger = LogManager.getLogger();
	private static final String RENDERER_URL = "http://climbing-leaflet-renderer:3000/render";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public LeafletPrintService(HttpClient httpClient, ObjectMapper objectMapper) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	public LatLng getCenter(Collection<SectorProblem> problems) {
		if (problems == null || problems.isEmpty()) return new LatLng(0, 0);

		double x = 0.0, y = 0.0, z = 0.0;
		for (SectorProblem p : problems) {
			double lat = p.coordinates().latitude() * Math.PI / 180;
			double lon = p.coordinates().longitude() * Math.PI / 180;
			x += Math.cos(lat) * Math.cos(lon);
			y += Math.cos(lat) * Math.sin(lon);
			z += Math.sin(lat);
		}

		int size = problems.size();
		double hyp = Math.sqrt((x /= size) * x + (y /= size) * y);
		return new LatLng(Math.atan2(z / size, hyp) * 180 / Math.PI, Math.atan2(y, x) * 180 / Math.PI);
	}

	public Optional<byte[]> takeSnapshot(Leaflet leaflet) {
		try {
			String json = objectMapper.writeValueAsString(leaflet);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(RENDERER_URL))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(15))
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.build();
			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() == 200) {
				return Optional.of(response.body());
			}
			logger.error("Map renderer returned status: {}. Body: {}", response.statusCode(), new String(response.body()));
			return Optional.empty();

		} catch (IOException e) {
			logger.error("Network error while calling map-renderer. Ensure the container is running. Repo: https://github.com/jossi87/climbing-leaflet-renderer: {}", e.getMessage());
			return Optional.empty();
		} catch (InterruptedException e) {
			logger.error("Map render request was interrupted", e);
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}
}
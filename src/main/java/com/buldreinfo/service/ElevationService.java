package com.buldreinfo.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.util.CollectionUtils;
import com.buldreinfo.util.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ElevationService {
	private static final Logger logger = LogManager.getLogger();

	private record GoogleResponse(List<ElevationResult> results) {}
	private record ElevationResult(double elevation, Location location) {}
	private record Location(double lat, double lng) {}

	private final AppConfig appConfig;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public ElevationService(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
		this.appConfig = appConfig;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	public void fillElevations(List<Coordinates> allCoordinates) {
		try {
			for (List<Coordinates> chunk : CollectionUtils.partition(allCoordinates, 500)) {
				String locations = chunk.stream()
						.map(c -> c.latitude() + "," + c.longitude())
						.collect(Collectors.joining("%7C")); 
				String url = String.format("https://maps.googleapis.com/maps/api/elevation/json?locations=%s&key=%s", locations, appConfig.googleApikey());
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.GET()
						.build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() != HttpURLConnection.HTTP_OK) {
					throw new IllegalArgumentException("Google API error: " + response.statusCode());
				}
				GoogleResponse data = objectMapper.readValue(response.body(), GoogleResponse.class);

				for (ElevationResult res : data.results()) {
					double googleLat = GeoUtils.roundToTenDigits(res.location().lat());
					double googleLng = GeoUtils.roundToTenDigits(res.location().lng());
					boolean found = false;
					for (int i = 0; i < chunk.size(); i++) {
						Coordinates c = chunk.get(i);
						if (isClose(c.latitude(), googleLat) && isClose(c.longitude(), googleLng)) {
							chunk.set(i, c.withElevation(res.elevation(), Coordinates.ELEVATION_SOURCE_GOOGLE));
							found = true;
							break;
						}
					}
					if (!found) {
						logger.warn("Google API returned {},{} but no local match found.", googleLat, googleLng);
					}
				}
			}
		} catch (InterruptedException | IOException e) {
			logger.error("Elevation service failed", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private static boolean isClose(double a, double b) {
		return Math.abs(a - b) < 1e-9;
	}
}
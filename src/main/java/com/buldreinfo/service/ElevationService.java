package com.buldreinfo.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.buldreinfo.model.Coordinates;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ElevationService {
	private record GoogleResponse(List<ElevationResult> results) {}
	private record ElevationResult(double elevation, Location location) {}
	private record Location(double lat, double lng) {}

	private final AppConfig appConfig;

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;
	public ElevationService(AppConfig appConfig, ObjectMapper objectMapper) {
		this.appConfig = appConfig;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
	}
	public void fillElevations(List<Coordinates> allCoordinates) {
		try {
			for (List<Coordinates> chunk : partition(allCoordinates, 500)) {
				String locations = chunk.stream()
						.map(c -> c.getLatitude() + "," + c.getLongitude())
						.reduce((a, b) -> a + "|" + b)
						.orElse("");
				String encodedLocations = URLEncoder.encode(locations, StandardCharsets.UTF_8);
				String url = String.format("https://maps.googleapis.com/maps/api/elevation/json?locations=%s&key=%s", encodedLocations, appConfig.googleApikey());
				HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() != HttpURLConnection.HTTP_OK) {
					throw new IllegalArgumentException("Google API error: " + response.statusCode());
				}
				GoogleResponse data = objectMapper.readValue(response.body(), GoogleResponse.class);
				for (ElevationResult res : data.results()) {
					chunk.stream()
					.filter(c -> c.getLatitude() == res.location().lat() && c.getLongitude() == res.location().lng())
					.forEach(c -> c.setElevation(res.elevation(), Coordinates.ELEVATION_SOURCE_GOOGLE));
				}
			}
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> partitions = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			partitions.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return partitions;
	}
}

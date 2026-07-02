package com.buldreinfo.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

	public ElevationService(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
		this.appConfig = appConfig;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	public List<Coordinates> resolveElevations(List<Coordinates> allCoordinates) {
		List<Coordinates> result = new ArrayList<>(allCoordinates);

		// Collect indices that need elevation (elevationSource == null)
		List<Integer> needIndices = new ArrayList<>();
		for (int i = 0; i < result.size(); i++) {
			if (result.get(i).elevationSource() == null) {
				needIndices.add(i);
			}
		}

		if (needIndices.isEmpty()) {
			return result;
		}

		// Fetch elevation in batches of 500
		for (int batchStart = 0; batchStart < needIndices.size(); batchStart += 500) {
			int batchEnd = Math.min(batchStart + 500, needIndices.size());
			String locations = needIndices.subList(batchStart, batchEnd).stream()
					.map(i -> result.get(i))
					.map(c -> c.latitude() + "," + c.longitude())
					.collect(Collectors.joining("%7C"));
			String url = String.format("https://maps.googleapis.com/maps/api/elevation/json?locations=%s&key=%s", locations, appConfig.googleApikey());
			try {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.GET()
						.build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() != HttpURLConnection.HTTP_OK) {
					throw new RuntimeException("Google Elevation API returned status " + response.statusCode());
				}
				GoogleResponse data = objectMapper.readValue(response.body(), GoogleResponse.class);

				for (ElevationResult res : data.results()) {
					double googleLat = res.location().lat();
					double googleLng = res.location().lng();
					for (int j = batchStart; j < batchEnd; j++) {
						int idx = needIndices.get(j);
						Coordinates c = result.get(idx);
						if (isClose(c.latitude(), googleLat) && isClose(c.longitude(), googleLng)) {
							result.set(idx, c.withElevation(res.elevation(), Coordinates.ELEVATION_SOURCE_GOOGLE));
							break;
						}
					}
				}
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException("Elevation service failed", e);
			}
		}

		// Verify all coordinates got elevation
		var missing = result.stream()
				.filter(c -> c.elevationSource() == null)
				.toList();
		if (!missing.isEmpty()) {
			var coordsStr = missing.stream()
					.map(c -> "(" + c.latitude() + ", " + c.longitude() + ")")
					.collect(Collectors.joining(", "));
			throw new RuntimeException("Google Elevation API did not return data for: " + coordsStr);
		}

		return result;
	}

	private static boolean isClose(double a, double b) {
		return Math.abs(a - b) < 1e-9;
	}
}

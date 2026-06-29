package com.buldreinfo.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.buldreinfo.config.AppConfig;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.util.CollectionUtils;
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

    public void fillElevations(List<Coordinates> allCoordinates) {
        try {
            for (List<Coordinates> chunk : CollectionUtils.partition(allCoordinates, 500)) {
                String locations = chunk.stream()
                        .map(c -> c.getLatitude() + "," + c.getLongitude())
                        .collect(Collectors.joining("|"));
                String url = UriComponentsBuilder.fromUriString("https://maps.googleapis.com/maps/api/elevation/json")
                        .queryParam("locations", locations)
                        .queryParam("key", appConfig.googleApikey())
                        .build()
                        .toUriString();
                HttpRequest request = HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    throw new IllegalArgumentException("Google API error: " + response.statusCode());
                }
                GoogleResponse data = objectMapper.readValue(response.body(), GoogleResponse.class);
                for (ElevationResult res : data.results()) {
                    chunk.stream()
                        .filter(c -> Double.compare(c.getLatitude(), res.location().lat()) == 0 && 
                                     Double.compare(c.getLongitude(), res.location().lng()) == 0)
                        .forEach(c -> c.setElevation(res.elevation(), Coordinates.ELEVATION_SOURCE_GOOGLE));
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
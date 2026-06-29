package com.buldreinfo.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Service
public class VegvesenService {
	public record Webcam(String id, String lastUpdated, String name, String urlStillImage, String urlYr, double lat, double lng) {}
	private static final Logger logger = LoggerFactory.getLogger(VegvesenService.class);
	private final String authHeader;
	private final HttpClient httpClient;
	private final XmlMapper xmlMapper;

	public VegvesenService(AppConfig appConfig, HttpClient httpClient) {
		this.httpClient = httpClient;
		this.authHeader = "Basic " + Base64.getEncoder().encodeToString(appConfig.vegvesenAuth().getBytes(StandardCharsets.UTF_8));
		this.xmlMapper = new XmlMapper();
	}

	public List<Webcam> getCameras() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://datex-server-get-v3-1.atlas.vegvesen.no/datexapi/GetCCTVSiteTable/pullsnapshotdata"))
					.header("Authorization", authHeader)
					.GET()
					.build();

			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() != 200) {
				throw new RuntimeException("HTTP error: " + response.statusCode());
			}

			JsonNode root = xmlMapper.readTree(response.body());
			List<JsonNode> cameraNodes = root.findValues("cctvCameraMetadataRecord");

			if (cameraNodes.isEmpty()) {
				logger.warn("Vegvesen feed returned 0 cameras. Check source structure.");
			}

			List<Webcam> results = new ArrayList<>();
			for (JsonNode node : cameraNodes) {
				results.add(new Webcam(
						getText(node, "cctvCameraIdentification"),
						getText(node, "cctvCameraRecordVersionTime"),
						getText(node, "cctvCameraSiteLocalDescription"),
						getText(node, "stillImageUrl"),
						getText(node, "urlYr"),
						getDouble(node, "latitude"),
						getDouble(node, "longitude")
						));
			}
			return results;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch/parse cameras: " + e.getMessage(), e);
		}
	}

	private double getDouble(JsonNode node, String fieldName) {
		JsonNode found = node.findValue(fieldName);
		return (found != null) ? found.asDouble() : 0.0;
	}

	private String getText(JsonNode node, String fieldName) {
		JsonNode found = node.findValue(fieldName);
		return (found != null) ? found.asText() : "";
	}
}
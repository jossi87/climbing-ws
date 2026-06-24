package com.buldreinfo.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class InstagramService {
	public record InstagramMedia(String cdnUrl, boolean isVideo, int mediaIndex) {}
	private static final Pattern ALLOWED_CDN_PATTERN = Pattern.compile("^https://[^/]+\\.(cdninstagram\\.com|fbcdn\\.net)/.*$");
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();
	private static final Logger logger = LoggerFactory.getLogger(InstagramService.class);
	public static String extractInstagramShortcode(String url) {
		String cleanUrl = url.split("\\?")[0];
		if (cleanUrl.endsWith("/")) {
			cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
		}
		String[] segments = cleanUrl.split("/");
		for (int i = 0; i < segments.length - 1; i++) {
			if ("p".equals(segments[i]) || "reel".equals(segments[i]) || "tv".equals(segments[i])) {
				return segments[i + 1];
			}
		}
		return "unknown";
	}
	
	public static URI validateInstagramCdnUrl(String urlString) {
		if (urlString == null) {
			throw new IllegalArgumentException("URL cannot be null");
		}
		var matcher = ALLOWED_CDN_PATTERN.matcher(urlString);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unauthorized or malicious media storage URL");
		}
		return URI.create(matcher.group(0));
	}
	
	private final AppConfig appConfig;
	private final ObjectMapper objectMapper;

	public InstagramService(AppConfig appConfig, ObjectMapper objectMapper) {
		this.appConfig = appConfig;
		this.objectMapper = objectMapper;
	}

	public List<InstagramMedia> resolveMedia(String instagramUrl) throws IOException, InterruptedException {
		if (instagramUrl == null || !instagramUrl.matches("^(https?://)?(www\\.)?instagram\\.com/(p|reel|tv)/.+")) {
			throw new IllegalArgumentException("Invalid Instagram media URL format");
		}
		if (instagramUrl.contains("?")) {
			instagramUrl = instagramUrl.substring(0, instagramUrl.indexOf('?'));
		}
		if (instagramUrl.endsWith("/")) {
			instagramUrl = instagramUrl.substring(0, instagramUrl.length() - 1);
		}

		String apiToken = appConfig.apifyApiToken();
		String startUrl = "https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/runs?token=" + apiToken;

		ObjectNode inputJson = objectMapper.createObjectNode();
		inputJson.putArray("urls").add(instagramUrl);
		inputJson.put("commentsPreviewLimit", 0);

		HttpRequest startRequest = HttpRequest.newBuilder()
				.uri(URI.create(startUrl))
				.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.timeout(Duration.ofSeconds(30))
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(inputJson), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> startResponse = HTTP_CLIENT.send(startRequest, HttpResponse.BodyHandlers.ofString());
		if (startResponse.statusCode() != 200 && startResponse.statusCode() != 201) {
			throw new IOException("Failed to initiate Apify actor run. Status: " + startResponse.statusCode() + " URL: " + instagramUrl);
		}

		JsonNode runData = objectMapper.readTree(startResponse.body()).path("data");
		String runId = runData.path("id").asText();
		String defaultDatasetId = runData.path("defaultDatasetId").asText();

		String runStatusUrl = "https://api.apify.com/v2/actor-runs/" + runId + "?token=" + apiToken;
		HttpRequest statusRequest = HttpRequest.newBuilder().uri(URI.create(runStatusUrl)).timeout(Duration.ofSeconds(15)).GET().build();

		boolean success = false;
		for (int attempt = 1; attempt <= 20; attempt++) {
			Thread.sleep(2500);
			HttpResponse<String> statusResponse = HTTP_CLIENT.send(statusRequest, HttpResponse.BodyHandlers.ofString());
			if (statusResponse.statusCode() == 200) {
				JsonNode statusData = objectMapper.readTree(statusResponse.body()).path("data");
				String status = statusData.path("status").asText();

				if ("SUCCEEDED".equals(status)) {
					success = true;
					break;
				} else if ("FAILED".equals(status) || "ABORTED".equals(status) || "TIMED-OUT".equals(status)) {
					throw new IOException("Apify background execution failed with status: " + status + " for URL: " + instagramUrl);
				}
			}
			logger.warn("Apify execution active, status polling attempt {} for URL: {}", attempt, instagramUrl);
		}

		if (!success) {
			throw new IOException("Apify runner execution tracking timed out out on backend engine for URL: " + instagramUrl);
		}

		String datasetUrl = "https://api.apify.com/v2/datasets/" + defaultDatasetId + "/items?token=" + apiToken;
		HttpRequest datasetRequest = HttpRequest.newBuilder().uri(URI.create(datasetUrl)).timeout(Duration.ofSeconds(15)).GET().build();
		HttpResponse<String> datasetResponse = HTTP_CLIENT.send(datasetRequest, HttpResponse.BodyHandlers.ofString());

		if (datasetResponse.statusCode() != 200) {
			throw new IOException("Failed to retrieve populated dataset items. Code: " + datasetResponse.statusCode());
		}

		JsonNode resultArray = objectMapper.readTree(datasetResponse.body());
		if (resultArray.isMissingNode() || resultArray.isEmpty()) {
			throw new IOException("Apify dataset populated empty on verified run completion for URL: " + instagramUrl);
		}

		List<InstagramMedia> mediaList = new ArrayList<>();
		for (int i = 0; i < resultArray.size(); i++) {
			JsonNode entry = resultArray.get(i);
			if (entry.hasNonNull("download_url")) {
				String cdnUrl = entry.get("download_url").asText().replaceAll("&amp;", "&");
				String mediaType = entry.hasNonNull("media_type") ? entry.get("media_type").asText() : "image";
				boolean isVideo = "video".equalsIgnoreCase(mediaType);
				int apiMediaIndex = entry.hasNonNull("media_index") ? entry.get("media_index").asInt() : i;

				mediaList.add(new InstagramMedia(cdnUrl, isVideo, apiMediaIndex));
			}
		}
		if (mediaList.isEmpty()) {
			throw new IOException("No download_url found in Apify response payload for URL: " + instagramUrl);
		}
		return mediaList;
	}
}
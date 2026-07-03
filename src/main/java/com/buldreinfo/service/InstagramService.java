package com.buldreinfo.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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

	public static URI validateUrl(String urlString, String... allowedSuffixes) {
		if (urlString == null) {
			throw new IllegalArgumentException("URL cannot be null");
		}
		try {
			URI uri = new URI(urlString);
			if (!"https".equalsIgnoreCase(uri.getScheme())) {
				throw new IllegalArgumentException("Only HTTPS scheme is allowed");
			}
			String host = uri.getHost();
			if (host != null) {
				for (String suffix : allowedSuffixes) {
					if (host.equals(suffix) || host.endsWith("." + suffix)) {
						return uri;
					}
				}
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL syntax", e);
		}
		throw new IllegalArgumentException("Unauthorized URL: " + urlString);
	}

	private final AppConfig appConfig;

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	public InstagramService(AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
		this.appConfig = appConfig;
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	public byte[] fetchMediaBytes(URI validatedUri) {
		String host = validatedUri.getHost();
		if (host == null || (!host.endsWith(".cdninstagram.com") && !host.endsWith(".fbcdn.net"))) {
			throw new IllegalArgumentException("Unauthorized host");
		}

		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(validatedUri)
					.GET()
					.build();

			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

			if (response.statusCode() != 200) {
				throw new IOException("Failed to fetch media: " + response.statusCode());
			}

			return response.body();
		} catch (IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UncheckedIOException(new IOException("Media fetch interrupted or failed", e));
		}
	}

	public List<InstagramMedia> resolveMedia(String instagramUrl) {
		try {
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
			ObjectNode inputJson = objectMapper.createObjectNode();
			inputJson.putArray("urls").add(instagramUrl);
			inputJson.put("commentsPreviewLimit", 0);

			HttpRequest startRequest = HttpRequest.newBuilder()
					.uri(URI.create("https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/runs"))
					.header("Authorization", "Bearer " + apiToken)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.timeout(Duration.ofSeconds(30))
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(inputJson), StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> startResponse = httpClient.send(startRequest, HttpResponse.BodyHandlers.ofString());
			if (startResponse.statusCode() != 200 && startResponse.statusCode() != 201) {
				throw new IOException("Failed to initiate Apify actor run. Status: " + startResponse.statusCode() + " URL: " + instagramUrl);
			}

			JsonNode runData = objectMapper.readTree(startResponse.body()).path("data");
			String runId = runData.path("id").asText();
			String defaultDatasetId = runData.path("defaultDatasetId").asText();

			HttpRequest statusRequest = HttpRequest.newBuilder()
					.uri(URI.create("https://api.apify.com/v2/actor-runs/" + runId))
					.header("Authorization", "Bearer " + apiToken)
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build();

			boolean success = false;
			for (int attempt = 1; attempt <= 20; attempt++) {
				Thread.sleep(2500);
				HttpResponse<String> statusResponse = httpClient.send(statusRequest, HttpResponse.BodyHandlers.ofString());
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

			HttpRequest datasetRequest = HttpRequest.newBuilder()
					.uri(URI.create("https://api.apify.com/v2/datasets/" + defaultDatasetId + "/items"))
					.header("Authorization", "Bearer " + apiToken)
					.timeout(Duration.ofSeconds(15))
					.GET()
					.build();
			HttpResponse<String> datasetResponse = httpClient.send(datasetRequest, HttpResponse.BodyHandlers.ofString());

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
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
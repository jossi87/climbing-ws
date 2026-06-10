package com.buldreinfo.jersey.jaxb.helpers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ApifyInstagramResolver {
	public record InstagramMedia(String cdnUrl, boolean isVideo, int mediaIndex) {}
	
	private static final Logger logger = LoggerFactory.getLogger(ApifyInstagramResolver.class);
	
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();
	private static final Gson GSON = new GsonBuilder()
			.disableHtmlEscaping()
			.create();

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
		try {
			URI uri = URI.create(urlString);
			String host = uri.getHost();
			if (host == null || (!host.endsWith(".cdninstagram.com") && !host.endsWith(".fbcdn.net"))) {
				throw new IllegalArgumentException("Invalid media source domain");
			}
			return uri;
		} catch (Exception e) {
			throw new IllegalArgumentException("Malformed or unauthorized media storage URL", e);
		}
	}
	
	public static List<InstagramMedia> resolveMedia(String instagramUrl) throws IOException, InterruptedException {
		if (instagramUrl == null || !instagramUrl.matches("^(https?://)?(www\\.)?instagram\\.com/(p|reel|tv)/.+")) {
			throw new IllegalArgumentException("Invalid Instagram media URL format");
		}
		if (instagramUrl.contains("?")) {
			instagramUrl = instagramUrl.substring(0, instagramUrl.indexOf('?'));
		}
		if (instagramUrl.endsWith("/")) {
			instagramUrl = instagramUrl.substring(0, instagramUrl.length() - 1);
		}

		String apiToken = com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig.getConfig().getProperty(com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig.PROPERTY_KEY_APIFY_API_TOKEN);
		
		String startUrl = "https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/runs?token=" + apiToken;
		
		JsonObject inputJson = new JsonObject();
		JsonArray urlsArray = new JsonArray();
		urlsArray.add(instagramUrl);
		inputJson.add("urls", urlsArray);
		inputJson.addProperty("commentsPreviewLimit", 0);

		HttpRequest startRequest = HttpRequest.newBuilder()
				.uri(URI.create(startUrl))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(30))
				.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(inputJson), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> startResponse = HTTP_CLIENT.send(startRequest, HttpResponse.BodyHandlers.ofString());
		if (startResponse.statusCode() != 200 && startResponse.statusCode() != 201) {
			throw new IOException("Failed to initiate Apify actor run. Status: " + startResponse.statusCode() + " URL: " + instagramUrl);
		}

		JsonObject runData = JsonParser.parseString(startResponse.body()).getAsJsonObject().getAsJsonObject("data");
		String defaultDatasetId = runData.get("defaultDatasetId").getAsString();
		
		String datasetUrl = "https://api.apify.com/v2/datasets/" + defaultDatasetId + "/items?token=" + apiToken;
		HttpRequest datasetRequest = HttpRequest.newBuilder()
				.uri(URI.create(datasetUrl))
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();

		JsonArray resultArray = null;
		long delay = 1500; 
		
		for (int attempt = 1; attempt <= 5; attempt++) {
			Thread.sleep(delay);
			HttpResponse<String> datasetResponse = HTTP_CLIENT.send(datasetRequest, HttpResponse.BodyHandlers.ofString());
			
			if (datasetResponse.statusCode() == 200) {
				JsonArray candidateArray = JsonParser.parseString(datasetResponse.body()).getAsJsonArray();
				if (candidateArray != null && !candidateArray.isEmpty()) {
					resultArray = candidateArray;
					break;
				}
			}
			
			logger.warn("Apify dataset empty on attempt {} for URL: {}. Retrying in {}ms...", attempt, instagramUrl, delay);
			delay += 1000; 
		}

		if (resultArray == null || resultArray.isEmpty()) {
			throw new IOException("Apify dataset failed to populate after explicit polling loops for URL: " + instagramUrl);
		}

		List<InstagramMedia> mediaList = new ArrayList<>();
		for (int i = 0; i < resultArray.size(); i++) {
			JsonObject entry = resultArray.get(i).getAsJsonObject();
			if (entry.has("download_url") && !entry.get("download_url").isJsonNull()) {
				String cdnUrl = entry.get("download_url").getAsString().replaceAll("&amp;", "&");
				String mediaType = entry.has("media_type") && !entry.get("media_type").isJsonNull()
						? entry.get("media_type").getAsString()
						: "image";
				boolean isVideo = "video".equalsIgnoreCase(mediaType);
				int apiMediaIndex = entry.has("media_index") && !entry.get("media_index").isJsonNull()
						? entry.get("media_index").getAsInt()
						: i;

				mediaList.add(new InstagramMedia(cdnUrl, isVideo, apiMediaIndex));
			}
		}
		if (mediaList.isEmpty()) {
			throw new IOException("No download_url found in Apify response payload for URL: " + instagramUrl);
		}
		return mediaList;
	}
}
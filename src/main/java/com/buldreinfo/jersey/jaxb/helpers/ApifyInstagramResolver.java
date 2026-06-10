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
	
	public static void validateInstagramCdnUrl(String urlString) {
		try {
			URI uri = URI.create(urlString);
			String host = uri.getHost();
			if (host == null || (!host.endsWith(".cdninstagram.com") && !host.endsWith(".fbcdn.net"))) {
				throw new IllegalArgumentException("Invalid media source domain");
			}
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
		String url = "https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/run-sync-get-dataset-items?token=" + apiToken;
		JsonObject inputJson = new JsonObject();
		JsonArray urlsArray = new JsonArray();
		urlsArray.add(instagramUrl);
		inputJson.add("urls", urlsArray);
		inputJson.addProperty("commentsPreviewLimit", 0);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(60))
				.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(inputJson), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200 && response.statusCode() != 201) {
			throw new IOException("Apify scrape request failed with HTTP code: " + response.statusCode() + " for URL: " + instagramUrl);
		}
		JsonArray resultArray = JsonParser.parseString(response.body()).getAsJsonArray();
		if (resultArray == null || resultArray.isEmpty()) {
			logger.warn("Apify sync call timed out empty for URL: {}. Initiating 2-second fallback retry delay...", instagramUrl);
			Thread.sleep(2000);
			response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200 || response.statusCode() == 201) {
				resultArray = JsonParser.parseString(response.body()).getAsJsonArray();
			}
		}
		if (resultArray == null || resultArray.isEmpty()) {
			throw new IOException("Apify returned an empty dataset response after retry for URL: " + instagramUrl);
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
package com.buldreinfo.jersey.jaxb.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApifyInstagramResolver {
	public record InstagramMedia(String cdnUrl, boolean isVideo, int mediaIndex) {}

	public static List<InstagramMedia> resolveMedia(String instagramUrl) throws IOException, InterruptedException {
		String apiToken = com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig.getConfig().getProperty(com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig.PROPERTY_KEY_APIFY_API_TOKEN);
		String url = "https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/run-sync-get-dataset-items?token=" + apiToken;
		HttpClient client = HttpClient.newHttpClient();
		JsonObject inputJson = new JsonObject();
		JsonArray urlsArray = new JsonArray();
		urlsArray.add(instagramUrl);
		inputJson.add("urls", urlsArray);
		inputJson.addProperty("commentsPreviewLimit", 0);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(inputJson.toString(), StandardCharsets.UTF_8))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200 && response.statusCode() != 201) {
			throw new IOException("Apify scrape request failed with HTTP code: " + response.statusCode());
		}
		JsonArray resultArray = JsonParser.parseString(response.body()).getAsJsonArray();
		if (resultArray == null || resultArray.isEmpty()) {
			throw new IOException("Apify returned an empty dataset response.");
		}
		List<InstagramMedia> mediaList = new ArrayList<>();
		for (int i = 0; i < resultArray.size(); i++) {
			JsonObject entry = resultArray.get(i).getAsJsonObject();
			if (entry.has("download_url") && !entry.get("download_url").isJsonNull()) {
				String cdnUrl = entry.get("download_url").getAsString();
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
			throw new IOException("No download_url found in Apify response payload.");
		}
		return mediaList;
	}
}
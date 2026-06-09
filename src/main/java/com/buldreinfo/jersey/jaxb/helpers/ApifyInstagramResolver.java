package com.buldreinfo.jersey.jaxb.helpers;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApifyInstagramResolver {
	public record InstagramMedia(String cdnUrl, boolean isVideo) {}

	public static InstagramMedia resolveMedia(String instagramUrl) throws IOException, InterruptedException {
		int targetSlideIndex = extractImageIndex(instagramUrl);
		HttpClient client = HttpClient.newHttpClient();

		JsonObject inputJson = new JsonObject();
		JsonArray urlsArray = new JsonArray();
		urlsArray.add(instagramUrl);
		inputJson.add("urls", urlsArray);
		inputJson.addProperty("commentsPreviewLimit", 0);
		
		String apiToken = BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_APIFY_API_TOKEN);
		String url = "https://api.apify.com/v2/acts/maximedupre~instagram-downloader-api/run-sync-get-dataset-items?token=" + apiToken;

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

		for (int i = 0; i < resultArray.size(); i++) {
			JsonObject entry = resultArray.get(i).getAsJsonObject();
			int apiMediaIndex = entry.has("media_index") && !entry.get("media_index").isJsonNull() 
					? entry.get("media_index").getAsInt() 
					: 0;

			if (apiMediaIndex == targetSlideIndex) {
				return extractMediaFromEntry(entry);
			}
		}

		return extractMediaFromEntry(resultArray.get(0).getAsJsonObject());
	}

	private static InstagramMedia extractMediaFromEntry(JsonObject entry) throws IOException {
		if (entry.has("download_url") && !entry.get("download_url").isJsonNull()) {
			String cdnUrl = entry.get("download_url").getAsString();
			String mediaType = entry.has("media_type") && !entry.get("media_type").isJsonNull()
					? entry.get("media_type").getAsString()
					: "image";
			boolean isVideo = "video".equalsIgnoreCase(mediaType);
			return new InstagramMedia(cdnUrl, isVideo);
		}
		throw new IOException("No download_url found in targeted entry payload.");
	}

	private static int extractImageIndex(String url) {
		try {
			URI uri = URI.create(url);
			String query = uri.getQuery();
			if (query != null) {
				for (String param : query.split("&")) {
					String[] pair = param.split("=");
					if (pair.length == 2 && "img_index".equalsIgnoreCase(pair[0])) {
						return Math.max(0, Integer.parseInt(pair[1]) - 1);
					}
				}
			}
		} catch (Exception _) {
		}
		return 0;
	}
}
package com.buldreinfo.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.model.Webcam;
import com.buldreinfo.util.JsonHelper;

/**
 * Client for the Windy Webcams API v3 (https://api.windy.com/webcams/docs).
 * <p>
 * The image URLs returned by Windy are secured with a token that expires (10 minutes on the free tier,
 * 24 hours on the professional tier), so the result must not be cached for as long as the Vegvesen feed.
 */
@Service
public class WindyService {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String API_URL = "https://api.windy.com/webcams/api/v3/webcams";
	private static final String API_KEY_HEADER = "x-windy-api-key";
	private static final String INCLUDE = "images,location,urls";
	private static final int PAGE_SIZE = 50;
	/**
	 * The free API tier only allows an offset up to 1000. Keeping the total well below that limits the
	 * number of requests used to fill the cache on every refresh.
	 */
	private static final int MAX_WEBCAMS = 200;

	private final String apiKey;
	private final HttpClient httpClient;
	private final JsonHelper jsonHelper;

	public WindyService(AppConfig appConfig, HttpClient httpClient, JsonHelper jsonHelper) {
		this.apiKey = appConfig.windyApiKey();
		this.httpClient = httpClient;
		this.jsonHelper = jsonHelper;
	}

	/**
	 * Returns the most popular webcams in the given country, labelled in the given language. Returns an empty
	 * list when no API key is configured, so a deployment without a Windy key still starts and serves the
	 * Vegvesen feed.
	 *
	 * @param country ISO 3166-1 alpha-2 country code accepted by Windy, e.g. {@code CZ}
	 * @param lang    Windy language code, e.g. {@code cs}
	 */
	@Cacheable(value = CacheConstants.WINDY_WEBCAM_CACHE_NAME, key = "#country + ':' + #lang", sync = true)
	public List<Webcam> getCameras(String country, String lang) {
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn("buldreinfo.windy-api-key is not configured, returning no Windy webcams");
			return List.of();
		}
		if (country == null || country.isBlank() || lang == null || lang.isBlank()) {
			throw new IllegalArgumentException("Windy country and lang are required");
		}
		try {
			List<Webcam> webcams = new ArrayList<>();
			int offset = 0;
			while (offset < MAX_WEBCAMS) {
				WindyResponse response = fetchPage(offset, country, lang);
				if (response == null || response.webcams() == null || response.webcams().isEmpty()) {
					break;
				}
				response.webcams().stream()
						.filter(webcam -> "active".equals(webcam.status()))
						.map(WindyService::toWebcam)
						.filter(Objects::nonNull)
						.forEach(webcams::add);
				offset += response.webcams().size();
				if (offset >= response.total()) {
					break;
				}
			}
			return webcams;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Failed to fetch Windy webcams: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to fetch Windy webcams: " + e.getMessage(), e);
		}
	}

	private WindyResponse fetchPage(int offset, String country, String lang) throws IOException, InterruptedException {
		String url = API_URL
				+ "?countries=" + country
				+ "&limit=" + PAGE_SIZE
				+ "&offset=" + offset
				+ "&sortKey=popularity&sortDirection=desc"
				+ "&lang=" + lang
				+ "&include=" + INCLUDE;
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header(API_KEY_HEADER, apiKey)
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new IOException("Windy API returned HTTP-" + response.statusCode() + ": " + truncate(response.body()));
		}
		return jsonHelper.parseObject(response.body(), WindyResponse.class);
	}

	private static String truncate(String value) {
		if (value == null) {
			return "";
		}
		return value.length() <= 300 ? value : value.substring(0, 300) + "...";
	}

	private static Webcam toWebcam(WindyWebcam webcam) {
		WindyLocation location = webcam.location();
		String urlStillImage = getImageUrl(webcam.images());
		if (location == null || urlStillImage == null) {
			return null;
		}
		String urlOther = webcam.urls() == null ? null : webcam.urls().detail();
		return new Webcam(String.valueOf(webcam.webcamId()), webcam.lastUpdatedOn(), webcam.title(), urlStillImage, null, urlOther, location.latitude(), location.longitude());
	}

	private static String getImageUrl(WindyImages images) {
		if (images == null) {
			return null;
		}
		String url = firstImageUrl(images.current(), "preview", "thumbnail", "icon");
		return url != null ? url : firstImageUrl(images.daylight(), "preview", "thumbnail", "icon");
	}

	private static String firstImageUrl(Map<String, String> images, String... sizes) {
		if (images == null) {
			return null;
		}
		for (String size : sizes) {
			String url = images.get(size);
			if (url != null && !url.isBlank()) {
				return url;
			}
		}
		return null;
	}

	private record WindyResponse(int total, List<WindyWebcam> webcams) {}

	private record WindyWebcam(long webcamId, String title, String status, String lastUpdatedOn, WindyImages images, WindyLocation location, WindyUrls urls) {}

	private record WindyImages(Map<String, String> current, Map<String, String> daylight) {}

	private record WindyLocation(double latitude, double longitude) {}

	private record WindyUrls(String detail) {}
}

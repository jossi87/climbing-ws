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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.buldreinfo.infrastructure.CacheConstants;

@Service
public class VegvesenService {
	public record Webcam(String id, String lastUpdated, String name, String urlStillImage, String urlYr, double lat, double lng) {}
	private static final Logger logger = LogManager.getLogger();
	private final String authHeader;
	private final HttpClient httpClient;
	private final XMLInputFactory xmlInputFactory;

	public VegvesenService(AppConfig appConfig, HttpClient httpClient) {
		this.httpClient = httpClient;
		this.authHeader = "Basic " + Base64.getEncoder().encodeToString(appConfig.vegvesenAuth().getBytes(StandardCharsets.UTF_8));
		this.xmlInputFactory = XMLInputFactory.newInstance();
	}

	@Cacheable(value = CacheConstants.WEBCAM_CACHE_NAME, sync = true)
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

			List<Webcam> results = new ArrayList<>();
			try (InputStream is = response.body()) {
				XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);

				String currentElement = "";
				String id = "", lastUpdated = "", name = "", urlStill = "", urlYr = "";
				double lat = 0.0, lng = 0.0;

				while (reader.hasNext()) {
					int event = reader.next();
					switch (event) {
					case XMLStreamConstants.START_ELEMENT -> currentElement = reader.getLocalName();
					case XMLStreamConstants.CHARACTERS -> {
						String text = reader.getText().trim();
						if (!text.isEmpty()) {
							switch (currentElement) {
							case "cctvCameraIdentification" -> id = text;
							case "cctvCameraRecordVersionTime" -> lastUpdated = text;
							case "cctvCameraSiteLocalDescription" -> name = text;
							case "stillImageUrl" -> urlStill = text;
							case "urlYr" -> urlYr = text;
							case "latitude" -> lat = Double.parseDouble(text);
							case "longitude" -> lng = Double.parseDouble(text);
							default -> {}
							}
						}
					}
					case XMLStreamConstants.END_ELEMENT -> {
						if ("cctvCameraMetadataRecord".equals(reader.getLocalName())) {
							results.add(new Webcam(id, lastUpdated, name, urlStill, urlYr, lat, lng));
							id = lastUpdated = name = urlStill = urlYr = "";
							lat = lng = 0.0;
						}
					}
					}
				}
			}

			if (results.isEmpty()) {
				logger.warn("Vegvesen feed returned 0 cameras.");
			}
			return results;
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch/parse cameras: " + e.getMessage(), e);
		}
	}
}
package com.buldreinfo.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Service
public class VegvesenService {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Webcam(
			@JacksonXmlProperty(localName = "cctvCameraIdentification") String id,
			@JacksonXmlProperty(localName = "cctvCameraRecordVersionTime") String lastUpdated,
			@JacksonXmlProperty(localName = "cctvCameraSiteLocalDescription") String name,
			@JacksonXmlProperty(localName = "stillImageUrl") String urlStillImage,
			@JacksonXmlProperty(localName = "urlYr") String urlYr,
			@JacksonXmlProperty(localName = "latitude") double lat,
			@JacksonXmlProperty(localName = "longitude") double lng
			) {}
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class VegvesenResponse {
		@JacksonXmlElementWrapper(useWrapping = false)
		@JacksonXmlProperty(localName = "cctvCameraMetadataRecord")
		public List<Webcam> cameras;
	}
	private final String authHeader;
	private final HttpClient httpClient;
	private final XmlMapper xmlMapper;

	public VegvesenService(AppConfig appConfig, HttpClient httpClient) {
		this.httpClient = httpClient;
		this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
				appConfig.vegvesenAuth().getBytes(StandardCharsets.UTF_8));
		this.xmlMapper = new XmlMapper();
	}

	public List<Webcam> getCameras() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://datex-server-get-v3-1.atlas.vegvesen.no/datexapi/GetCCTVSiteTable/pullsnapshotdata"))
					.header("Authorization", authHeader)
					.GET()
					.build();
			try (InputStream is = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
				VegvesenResponse response = xmlMapper.readValue(is, VegvesenResponse.class);
				return response.cameras;
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch/parse cameras", e);
		}
	}
}
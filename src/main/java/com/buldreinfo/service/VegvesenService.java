package com.buldreinfo.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.springframework.stereotype.Service;

import com.buldreinfo.config.AppConfig;

@Service
public class VegvesenService {
	public record Webcam(String id, String lastUpdated, String name, String urlStillImage, String urlYr, String urlOther, double lat, double lng) {}
	private static class WebcamBuilder {
		String id, lastUpdated, name, urlStillImage, urlYr;
		double lat, lng;
		Webcam build() { return new Webcam(id, lastUpdated, name, urlStillImage, urlYr, null, lat, lng); }
		WebcamBuilder id(String v) { this.id = v; return this; }
		WebcamBuilder lastUpdated(String v) { this.lastUpdated = v; return this; }
		WebcamBuilder lat(double v) { this.lat = v; return this; }
		WebcamBuilder lng(double v) { this.lng = v; return this; }
		WebcamBuilder name(String v) { this.name = v; return this; }
		WebcamBuilder urlStillImage(String v) { this.urlStillImage = v; return this; }
		WebcamBuilder urlYr(String v) { this.urlYr = v; return this; }
	}

	private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

	static {
		FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
	}

	private final String authHeader;
	private final HttpClient httpClient;

	public VegvesenService(AppConfig appConfig, HttpClient httpClient) {
		this.httpClient = httpClient;
		this.authHeader = "Basic " + Base64.getEncoder().encodeToString(
				appConfig.vegvesenAuth().getBytes(StandardCharsets.UTF_8));
	}

	public List<Webcam> getCameras() {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("https://datex-server-get-v3-1.atlas.vegvesen.no/datexapi/GetCCTVSiteTable/pullsnapshotdata"))
					.header("Authorization", authHeader)
					.GET()
					.build();

			try (InputStream is = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
				return parseCameras(is);
			}
		} catch (IOException | InterruptedException | XMLStreamException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private String getText(XMLEventReader reader) throws XMLStreamException {
		XMLEvent event = reader.nextEvent();
		return event.isCharacters() ? event.asCharacters().getData() : "";
	}

	private List<Webcam> parseCameras(InputStream is) throws XMLStreamException {
		List<Webcam> cameras = new ArrayList<>();
		XMLEventReader eventReader = FACTORY.createXMLEventReader(is);
		WebcamBuilder b = new WebcamBuilder();
		try {
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				if (event.isStartElement()) {
					StartElement se = event.asStartElement();
					String localName = se.getName().getLocalPart();

					switch (localName) {
					case "cctvCameraMetadataRecord" -> b = new WebcamBuilder();
					case "cctvCameraIdentification" -> b.id(getText(eventReader));
					case "cctvCameraRecordVersionTime" -> b.lastUpdated(getText(eventReader));
					case "cctvCameraSiteLocalDescription" -> {
						eventReader.nextEvent(); eventReader.nextEvent();
						b.name(getText(eventReader));
					}
					case "stillImageUrl" -> {
						eventReader.nextEvent();
						b.urlStillImage(getText(eventReader));
						eventReader.nextEvent(); eventReader.nextEvent(); eventReader.nextEvent(); eventReader.nextEvent();
						b.urlYr(getText(eventReader));
					}
					case "latitude" -> b.lat(Double.parseDouble(getText(eventReader)));
					case "longitude" -> b.lng(Double.parseDouble(getText(eventReader)));
					}
				}
				if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("cctvCameraMetadataRecord")) {
					cameras.add(b.build());
				}
			}
		} finally {
			eventReader.close();
		}
		return cameras;
	}
}
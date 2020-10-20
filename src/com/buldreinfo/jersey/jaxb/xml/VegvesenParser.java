package com.buldreinfo.jersey.jaxb.xml;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class VegvesenParser {
	private static Logger logger = LogManager.getLogger();

	public List<Camera> getCameras() throws Exception {
		URL url = new URL("https://www.vegvesen.no/ws/no/vegvesen/veg/trafikkpublikasjon/kamera/2/GetCCTVSiteTable");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		String auth = "TjeDatexBuldreinfo:BZGOqBNXC2s9bAzTZjhA";
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		int responseCode = connection.getResponseCode();
		Preconditions.checkArgument(responseCode == 200, "Invalid responseCode: " + responseCode);
		List<Camera> res = parseCameras(connection.getInputStream());
		logger.debug("getCameras() - res.size()={}", res.size());
		return res;
	}
	
	private List<Camera> parseCameras(InputStream is) throws Exception {
		List<Camera> cameras = Lists.newArrayList();
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
		Camera camera = null;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				// If we have an item element, we create a new item
				String elementName = startElement.getName().getLocalPart();
				switch (elementName) {
				case "cctvCameraMetadataRecord":
					camera = new Camera();
					break;
				case "cctvCameraIdentification":
					event = eventReader.nextEvent();
					camera.setId(event.asCharacters().getData());
					break;
				case "cctvCameraRecordVersionTime":
					event = eventReader.nextEvent();
					camera.setLastUpdated(event.asCharacters().getData());
					break;
				case "cctvCameraSiteLocalDescription":
					eventReader.nextEvent();
					eventReader.nextEvent();
					event = eventReader.nextEvent();
					camera.setName(event.asCharacters().getData());
					break;
				case "stillImageUrl":
					event = eventReader.nextEvent();
					if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("urlLinkAddress")) {
						event = eventReader.nextEvent();
						camera.setUrlStillImage(event.asCharacters().getData());
						event = eventReader.nextEvent();
						if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals("urlLinkAddress")) {
							event = eventReader.nextEvent();
							if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("urlLinkDescription")) {
								event = eventReader.nextEvent();
								if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("values")) {
									event = eventReader.nextEvent();
									if (event.isStartElement() && event.asStartElement().getName().getLocalPart().equals("value")) {
										event = eventReader.nextEvent();
										camera.setUrlYr(event.asCharacters().getData());
									}
								}
							}
						}
					}
					break;
				case "latitude":
					event = eventReader.nextEvent();
					camera.setLat(Double.parseDouble(event.asCharacters().getData()));
					break;
				case "longitude":
					event = eventReader.nextEvent();
					camera.setLng(Double.parseDouble(event.asCharacters().getData()));
					break;
				}
			}
			// If we reach the end of an item element, we add it to the list
			if (event.isEndElement()) {
				EndElement endElement = event.asEndElement();
				if (endElement.getName().getLocalPart().equals("cctvCameraMetadataRecord")) {
					cameras.add(camera);
				}
			}
		}
		return cameras;
	}
}
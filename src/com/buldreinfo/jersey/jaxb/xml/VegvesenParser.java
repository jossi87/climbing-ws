package com.buldreinfo.jersey.jaxb.xml;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class VegvesenParser {
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) throws Exception {
		VegvesenParser parser = new VegvesenParser();
		parser.getCameras();
	}

	public List<Webcam> getCameras() throws Exception {
		URL url = URI.create("https://datex-server-get-v3-1.atlas.vegvesen.no/datexapi/GetCCTVSiteTable/pullsnapshotdata").toURL();
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		String auth = BuldreinfoConfig.getConfig().getProperty(BuldreinfoConfig.PROPERTY_KEY_VEGVESEN_AUTH);
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		String authHeaderValue = "Basic " + new String(encodedAuth);
		connection.setRequestProperty("Authorization", authHeaderValue);

		int responseCode = connection.getResponseCode();
		Preconditions.checkArgument(responseCode == 200, "Invalid responseCode: " + responseCode);
		List<Webcam> res = parseCameras(connection.getInputStream());
		logger.debug("getCameras() - res.size()={}", res.size());
		return res;
	}
	
	private List<Webcam> parseCameras(InputStream is) throws Exception {
		List<Webcam> cameras = Lists.newArrayList();
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		XMLEventReader eventReader = inputFactory.createXMLEventReader(is);
		Webcam camera = null;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				// If we have an item element, we create a new item
				String elementName = startElement.getName().getLocalPart();
				switch (elementName) {
				case "cctvCameraMetadataRecord":
					camera = new Webcam();
					break;
				case "cctvCameraIdentification":
					event = eventReader.nextEvent();
					if (isCharacherString(event)) {
						camera.setId(event.asCharacters().getData());
					}
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
										if (isCharacherString(event)) {
											camera.setUrlYr(event.asCharacters().getData());
										}
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
	
	private boolean isCharacherString(XMLEvent event) {
		if (event.toString().startsWith("<")) {
			return false;
		}
		return true;
	}
}
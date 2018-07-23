package com.buldreinfo.jersey.jaxb.model.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class UrlSet {
	@XmlAttribute private final String xmlns = "http://www.sitemaps.org/schemas/sitemap/0.9";
	private final List<Url> urls;

	public UrlSet(List<Url> urls) {
		this.urls = urls;
	}
	
	@XmlElement(name="url")
	public List<Url> getUrls() {
		return urls;
	}
	
	public String getXmlns() {
		return xmlns;
	}
}

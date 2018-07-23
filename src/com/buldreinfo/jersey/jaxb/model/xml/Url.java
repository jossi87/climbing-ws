package com.buldreinfo.jersey.jaxb.model.xml;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Url {
	private final String loc;

	public Url(String loc) {
		this.loc = loc;
	}
	
	public String getLoc() {
		return loc;
	}
}
package com.buldreinfo.jersey.jaxb;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/")
public class BuldreinfoApplication extends ResourceConfig {
	public BuldreinfoApplication() {
		super(MultiPartFeature.class);
	}
}

package com.buldreinfo.jersey.jaxb;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
	public ApiApplication() {
		super(MultiPartFeature.class);
		register(GlobalExceptionMapper.class);
	}
}

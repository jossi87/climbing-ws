package com.buldreinfo.jersey.jaxb.infrastructure;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.buldreinfo.jersey.jaxb.resources.BaseResource;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    public ApiApplication() {
        super(MultiPartFeature.class);
        register(GlobalExceptionMapper.class);
        register(JsonProvider.class);
        register(CorsFilter.class);
        packages(BaseResource.class.getPackageName());
    }
}

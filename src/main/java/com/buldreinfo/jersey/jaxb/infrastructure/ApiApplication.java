package com.buldreinfo.jersey.jaxb.infrastructure;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.buldreinfo.jersey.jaxb.filters.HitTrackingFilter;

public class ApiApplication extends ResourceConfig {
    public ApiApplication() {
        super(MultiPartFeature.class);
        
        packages(
            "com.buldreinfo.jersey.jaxb.resources",
            "io.swagger.v3.jaxrs2.integration.resources"
        );
        
        register(new DependencyBinder());
        register(CorsFilter.class); 
        register(HitTrackingFilter.class);
        register(GlobalExceptionMapper.class);
        register(JsonProvider.class);
    }
}
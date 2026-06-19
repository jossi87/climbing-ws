package com.buldreinfo.jersey.jaxb.infrastructure;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.buldreinfo.jersey.jaxb.filters.HitTrackingFilter;
import com.buldreinfo.jersey.jaxb.resources.BaseResource;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class ApiApplication extends ResourceConfig {
    public ApiApplication() {
        super(MultiPartFeature.class);
        packages(BaseResource.class.getPackageName());
        register(CorsFilter.class);
        register(HitTrackingFilter.class);
        register(GlobalExceptionMapper.class);
        register(JsonProvider.class);
        register(new DependencyBinder());
    }
}

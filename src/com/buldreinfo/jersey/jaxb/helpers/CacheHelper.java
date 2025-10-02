package com.buldreinfo.jersey.jaxb.helpers;

import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

public class CacheHelper {
	private CacheHelper() {
    }

    /**
     * Applies the optimal long-term, immutable cache headers for static content.
     * This uses a 1-year max-age and manually appends the 'immutable' directive 
     * which is not supported by standard JAX-RS CacheControl, but is essential for 
     * modern browser performance (instant cache hit).
     * * @param builder The ResponseBuilder to modify.
     * @return The modified ResponseBuilder for chaining.
     */
	 public static Response.ResponseBuilder applyImmutableLongTermCache(Response.ResponseBuilder builder) {
	        final long MAX_AGE_SECONDS = TimeUnit.DAYS.toSeconds(365); 
	        CacheControl cc = new CacheControl();
	        cc.setMaxAge((int) MAX_AGE_SECONDS); 
	        cc.setPrivate(false); // Tells all proxies and CDNs (public caches) that they can cache this resource.
	        cc.setNoTransform(true); // // Prevents intermediate proxies from changing the image format.
	        builder.cacheControl(cc); 
	        // Manually construct the full, modern Cache-Control header string.This ensures compatibility and explicitly adds the "immutable" directive.
	        String cacheControlHeader = String.format(
	            "public, max-age=%d, no-transform, immutable", 
	            MAX_AGE_SECONDS
	        );
	        builder.header("Cache-Control", cacheControlHeader);
	        builder.expires(new java.util.Date(System.currentTimeMillis() + (MAX_AGE_SECONDS * 1000L))); // For legacy browsers
	        return builder;
	    }
}
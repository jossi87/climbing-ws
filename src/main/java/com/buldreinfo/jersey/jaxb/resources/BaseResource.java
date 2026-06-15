package com.buldreinfo.jersey.jaxb.resources;

import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;

public abstract class BaseResource {

    protected Response createBadRequestResponse(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", message))
                .build();
    }

    protected Response createNotFoundResponse() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(OpenApiConstants.NOT_FOUND_DESCRIPTION)
                .build();
    }
}
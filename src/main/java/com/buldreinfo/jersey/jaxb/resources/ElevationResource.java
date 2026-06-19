package com.buldreinfo.jersey.jaxb.resources;

import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Elevation")
@Path("/elevation")
public class ElevationResource extends BaseResource {
    @Inject
    public ElevationResource(TransactionManager txManager, RegionRepository regionRepo, UserRepository userRepo) {
        super(txManager, regionRepo, userRepo);
    }

    @Operation(summary = "Get elevation by latitude and longitude", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = Integer.class))}),
            @ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = "Forbidden: Authentication required"),
            @ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getElevation(@Context HttpServletRequest request,
            @Parameter(description = "Latitude (-90 to 90)", required = true) @QueryParam("latitude") double latitude,
            @Parameter(description = "Longitude (-180 to 180)", required = true) @QueryParam("longitude") double longitude) throws Exception {
        
        if (latitude < -90 || latitude > 90) {
            return createBadRequestResponse("Invalid latitude: must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            return createBadRequestResponse("Invalid longitude: must be between -180 and 180");
        }
        if (latitude == 0.0 && longitude == 0.0) {
            return createBadRequestResponse("Invalid coordinates (0,0)");
        }

        return executeAuthenticatedTask(request, (_, authUserId) -> {
            if (authUserId.isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            int elevation = GeoHelper.getElevation(latitude, longitude);
            return Response.ok().entity(String.valueOf(elevation)).build();
        });
    }
}
package com.buldreinfo.jersey.jaxb.resources;

import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Administrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Admin")
@Path("/administrators")
public class AdministratorsResource extends BaseResource {
    private final UserRepository userRepo;

    @Inject
    public AdministratorsResource(TransactionManager txManager, RegionRepository regionRepo, UserRepository userRepo) {
        super(txManager, regionRepo, userRepo);
        this.userRepo = userRepo;
    }

    @Operation(summary = "Get administrators", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))}),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdministrators(@Context HttpServletRequest request) throws Exception {
    	return executeTask(() -> {
            var setup = getSetup(request);
            return Response.ok().entity(userRepo.getAdministrators(setup)).build();
        });
    }
}
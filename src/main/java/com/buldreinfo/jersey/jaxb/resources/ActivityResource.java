package com.buldreinfo.jersey.jaxb.resources;

import com.buldreinfo.jersey.jaxb.dao.ActivityRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

@Tag(name = "Activity")
@Path("/activity")
public class ActivityResource extends BaseResource {
	private final ActivityRepository activityRepo;

	@Inject
	public ActivityResource(TransactionManager txManager, ActivityRepository activityRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.activityRepo = activityRepo;
	}

	@Operation(summary = "Get activity feed", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Activity.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActivity(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector,
			@Parameter(description = "Filter on lower grade", required = false) @QueryParam("lowerGrade") int lowerGrade,
			@Parameter(description = "Include first ascents", required = false) @QueryParam("fa") boolean fa,
			@Parameter(description = "Include comments", required = false) @QueryParam("comments") boolean comments,
			@Parameter(description = "Include ticks (public ascents)", required = false) @QueryParam("ticks") boolean ticks,
			@Parameter(description = "Include new media", required = false) @QueryParam("media") boolean media,
			@Parameter(description = "Offset (see more)", required = false) @QueryParam("offset") int offset) throws Exception {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		if (lowerGrade < 0) {
			return createBadRequestResponse("lowerGrade cannot be negative");
		}
		if (offset < 0) {
			return createBadRequestResponse("offset cannot be negative");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			var activity = activityRepo.getActivity(setup, authUserId, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset);
			return Response.ok().entity(activity).build();
		});
	}
}

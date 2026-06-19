package com.buldreinfo.jersey.jaxb.resources;

import com.buldreinfo.jersey.jaxb.dao.FrontpageRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Frontpage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/frontpage")
public class FrontpageResource extends BaseResource {
	private final FrontpageRepository frontpageRepo;

	@Inject
	public FrontpageResource(TransactionManager txManager, FrontpageRepository frontpageRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.frontpageRepo = frontpageRepo;
	}

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Frontpage.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpage(@Context HttpServletRequest request) throws Exception {
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			var stats = supplyAsync(() -> frontpageRepo.getFrontpageStats(authUserId, setup));
	        var randomMedia = supplyAsync(() -> frontpageRepo.getFrontpageRandomMedia(setup));
	        var firstAscents = supplyAsync(() -> frontpageRepo.getFrontpageFirstAscents(authUserId, setup));
	        var newestComments = supplyAsync(() -> frontpageRepo.getFrontpageNewestAscents(authUserId, setup));
	        var newestMedia = supplyAsync(() -> frontpageRepo.getFrontpageNewestMedia(authUserId, setup));
	        var lastComments = supplyAsync(() -> frontpageRepo.getFrontpageLastComments(authUserId, setup));

			var res = new Frontpage(
					stats.join(),
					randomMedia.join(),
					firstAscents.join(),
					newestComments.join(),
					newestMedia.join(),
					lastComments.join()
					);

			return Response.ok().entity(res).build();
		});
	}
}
package com.buldreinfo.jersey.jaxb.resources;

import java.util.List;

import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Webcam")
@Path("/webcams")
public class WebcamsResource extends BaseResource {
	@Inject
	public WebcamsResource(TransactionManager txManager, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
	}

	@Operation(summary = "Get webcams", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCameras() throws Exception {
		VegvesenParser vegvesenPaser = new VegvesenParser();
		List<Webcam> res = vegvesenPaser.getCameras();
		return Response.ok().entity(res).build();
	}
}

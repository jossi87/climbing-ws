package com.buldreinfo.jersey.jaxb.resources;

import java.util.List;

import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.Administrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

	@Operation(summary = "Get administrators", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdministrators(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (dao, setup, _) -> {
			List<Administrator> administrators = dao.getUserRepo().getAdministrators(setup);
			return Response.ok().entity(administrators).build();
		});
	}
}

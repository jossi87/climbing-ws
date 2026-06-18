package com.buldreinfo.jersey.jaxb.resources;

import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Users")
@Path("/users")
public class UsersResource extends BaseResource {
	
	@Operation(summary = "Search for problem", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = User.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		if (value == null || value.isBlank()) {
			return createBadRequestResponse("Search keyword is required");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, _, authUserId, _) -> {
			List<User> res = dao.getUserRepo().getUserSearch(authUserId, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ticks")
	@Produces(OpenApiConstants.APPLICATION_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, _, authUserId, _) -> {
			byte[] bytes = dao.getUserRepo().getUserTicks(authUserId);
			return Response.ok(bytes, OpenApiConstants.APPLICATION_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("UserTicks", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}
	
	@Operation(summary = "Update visible regions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/regions")
	public Response postUserRegions(@Context HttpServletRequest request,
			@Parameter(description = "Region id", required = true) @QueryParam("regionId") int regionId,
			@Parameter(description = "Delete (TRUE=hide, FALSE=show)", required = true) @QueryParam("delete") boolean delete
			) {
		if (regionId <= 0) {
			return createBadRequestResponse("Invalid regionId=" + regionId);
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, _, authUserId, _) -> {
			dao.getUserRepo().setUserRegion(authUserId, regionId, delete);
			return Response.ok().build();
		});
	}
}
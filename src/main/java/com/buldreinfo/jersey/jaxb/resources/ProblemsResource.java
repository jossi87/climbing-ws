package com.buldreinfo.jersey.jaxb.resources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemSearchResult;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;

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
import jakarta.ws.rs.core.StreamingOutput;

@Tag(name = "Problems")
@Path("/problems")
public class ProblemsResource extends BaseResource {
	private static final Logger logger = LogManager.getLogger();

	@Operation(summary = "Get problem by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Problem.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblems(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, shouldUpdateHits) -> {
			var res = dao.getProblemRepo().getProblem(authUserId, setup, id, showHiddenMedia, shouldUpdateHits);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Get problem PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_PDF, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/pdf")
	@Produces(OpenApiConstants.APPLICATION_PDF)
	public Response getProblemsPdf(@Context HttpServletRequest request, @Parameter(description = "Problem id", required = true) @QueryParam("id") int id) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, shouldUpdateHits) -> {
			final var problem = dao.getProblemRepo().getProblem(authUserId, setup, id, false, shouldUpdateHits);
			final var area = dao.getAreaRepo().getArea(setup, authUserId, problem.areaId(), shouldUpdateHits);
			final var sector = dao.getSectorRepo().getSector(authUserId, false, setup, problem.sectorId(), shouldUpdateHits);
			
			StreamingOutput stream = output -> {
				try (var generator = new PdfGenerator(output)) {
					generator.writeProblem(setup, area, sector, problem);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e.getMessage(), e);
				}
			};
			return Response.ok(stream)
					.type(OpenApiConstants.APPLICATION_PDF)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(problem.name(), "pdf")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Search for problem", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProblemSearchResult.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblemsSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		if (value == null || value.isBlank()) {
			return createBadRequestResponse("Search keyword is required");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, _) -> {
			var res = dao.getProblemRepo().getProblemsSearch(authUserId, setup, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update problem", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblems(@Context HttpServletRequest request, Problem p) {
		if (p == null || p.name() == null || p.name().strip().isEmpty()) {
			return createBadRequestResponse("Problem name is missing or invalid");
		}
		if (p.sectorId() <= 0) {
			return createBadRequestResponse("Invalid sectorId=" + p.sectorId());
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, setup, authUserId, _) -> {
			var res = dao.getProblemRepo().setProblem(authUserId, setup, p);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Update topo line on route/boulder (SVG on sector/problem-image)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@Parameter(description = "Problem section id", required = true) @QueryParam("pitch") int pitch,
			@Parameter(description = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) {
		if (problemId <= 0) {
			return createBadRequestResponse("Invalid problemId=" + problemId);
		}
		if (mediaId <= 0) {
			return createBadRequestResponse("Invalid mediaId=" + mediaId);
		}
		if (svg == null) {
			return createBadRequestResponse("Svg payload is missing");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, _, authUserId, _) -> {
			dao.getMediaRepo().upsertSvg(authUserId, problemId, pitch, mediaId, svg);
			return Response.ok().build();
		});
	}
}
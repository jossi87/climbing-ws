package com.buldreinfo.jersey.jaxb.resources;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemSearchResult;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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

@Tag(name = "/problems/")
@Path("/problems/")
public class ProblemsResource {
	private static Logger logger = LogManager.getLogger();

	public ProblemsResource() {
	}
	
	@Operation(summary = "Get problem by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblem(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			Problem res = dao.getProblemRepo().getProblem(c, authUserId, setup, id, showHiddenMedia, shouldUpdateHits);
			Response response = Response.ok().entity(res).build();
			return response;
		});
	}
	
	@Operation(summary = "Get problem PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/pdf")
	@Produces("application/pdf")
	public Response getProblemPdf(@Context final HttpServletRequest request, @Parameter(description = "Problem id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Problem problem = dao.getProblemRepo().getProblem(c, authUserId, setup, id, false, shouldUpdateHits);
			final Area area = dao.getAreaRepo().getArea(c, setup, authUserId, problem.areaId(), shouldUpdateHits);
			final Sector sector = dao.getSectorRepo().getSector(c, authUserId, false, setup, problem.sectorId(), shouldUpdateHits);
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeProblem(setup, area, sector, problem);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			return Response.ok(stream)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(problem.name(), "pdf")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Search for user", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProblemSearchResult.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblemSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<ProblemSearchResult> res = dao.getProblemRepo().getProblemsSearch(c, authUserId, setup, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update problem", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblem(@Context HttpServletRequest request, Problem p) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.sectorId() > 1);
			Objects.requireNonNull(Strings.emptyToNull(p.name()));
			Redirect res = dao.getProblemRepo().setProblem(c, authUserId, setup, p);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Update topo line on route/boulder (SVG on sector/problem-image)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/svg")
	public Response postProblemSvg(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@Parameter(description = "Problem section id", required = true) @QueryParam("pitch") int pitch,
			@Parameter(description = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Objects.requireNonNull(svg, "Invalid svg=" + svg);
			dao.getMediaRepo().upsertSvg(c, authUserId, problemId, pitch, mediaId, svg);
			return Response.ok().build();
		});
	}
}
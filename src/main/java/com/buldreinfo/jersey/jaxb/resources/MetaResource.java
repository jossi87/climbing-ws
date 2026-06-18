package com.buldreinfo.jersey.jaxb.resources;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Toc;

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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Meta")
@Path("")
public class MetaResource extends BaseResource {

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Frontpage.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpage(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (_, setup, authUserId, _) -> {
			var stats = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageStats(authUserId, setup));
			var randomMedia = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageRandomMedia(setup));
			var firstAscents = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageFirstAscents(authUserId, setup));
			var newestComments = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageNewestAscents(authUserId, setup));
			var newestMedia = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageNewestMedia(authUserId, setup));
			var lastComments = DatabaseContext.submitDaoTask(dao -> dao.getFrontpageRepo().getFrontpageLastComments(authUserId, setup));
			var res = new Frontpage(stats.join(), randomMedia.join(), firstAscents.join(), newestComments.join(), newestMedia.join(), lastComments.join());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get grade distribution by Area Id or Sector Id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/grade/distribution")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGradeDistribution(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		if (idArea == 0 && idSector == 0) {
			return createBadRequestResponse("Either idArea or idSector must be greater than 0");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, _, authUserId, _) -> {
			var res = dao.getHierarchyRepo().getGradeDistribution(authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/graph")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraph(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, _) -> {
			var res = dao.getHierarchyRepo().getContentGraph(authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get metadata", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Meta.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeta(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (_, setup, authUserId, _) -> {
			var res = Meta.from(setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get robots.txt", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (_, setup, _) -> {
			var lines = List.of(
					"User-agent: *",
					"Disallow: */pdf",
					"Sitemap: " + setup.url() + "/sitemap.txt");
			return Response.ok().entity(String.join("\r\n", lines)).build();
		});
	}

	@Operation(summary = "Get sitemap.txt", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getSitemapTxt(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (dao, setup, _) -> {
			var res = dao.getHierarchyRepo().getSitemapTxt(setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Toc.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getToc(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, _) -> {
			return Response.ok().entity(dao.getHierarchyRepo().getToc(authUserId, setup)).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems) as Excel (xlsx)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc/xlsx")
	@Produces(OpenApiConstants.APPLICATION_XLSX)
	public Response getTocXlsx(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, setup, authUserId, _) -> {
			var toc = dao.getHierarchyRepo().getToc(authUserId, setup);
			byte[] bytes;
			try (var workbook = new ExcelWorkbook()) {
				try (var sheet = workbook.addSheet("TOC")) {
					for (var r : toc.regions()) {
						for (var a : r.areas()) {
							for (var s : a.sectors()) {
								for (var p : s.problems()) {
									sheet.incrementRow();
									sheet.writeString("REGION", r.name());
									sheet.writeHyperlink("URL", p.url());
									sheet.writeString("AREA", a.name());
									sheet.writeString("SECTOR", s.name());
									sheet.writeInt("NR", p.nr());
									sheet.writeString("NAME", p.name());
									sheet.writeString("GRADE", p.grade());
									sheet.writeInt("FA_YEAR", p.faYear());
									sheet.writeInt("LENGTH_METER", p.lengthMeter());
									sheet.writeInt("STARTING_ATITUDE", p.startingAltitude());
									var type = p.t().type();
									if (p.t().subType() != null) {
										type += " (" + p.t().subType() + ")";			
									}
									sheet.writeString("TYPE", type);
									if (!setup.isBouldering()) {
										sheet.writeInt("PITCHES", p.numPitches() > 0 ? p.numPitches() : 1);
									}
									if (setup.isBouldering()) {
										sheet.writeString("FA_USER", p.ffaUser());
										sheet.writeInt("FA_YEAR", p.ffaYear());
									}
									else {
										sheet.writeString("FA_USER", p.faUser());
										sheet.writeInt("FA_YEAR", p.faYear());
										sheet.writeString("FFA_USER", p.ffaUser());
										sheet.writeInt("FFA_YEAR", p.ffaYear());
									}
									sheet.writeDouble("STARS", p.stars());
									sheet.writeString("DESCRIPTION", p.description());
								}
							}
						}
					}
				}
				var pitches = dao.getHierarchyRepo().getTocPitches(authUserId, setup);
				if (!pitches.isEmpty()) {
					try (var sheet = workbook.addSheet("TOC_MULTIPITCH_PITCHES")) {
						for (var p : pitches) {
							sheet.incrementRow();
							sheet.writeString("REGION", p.regionName());
							sheet.writeHyperlink("URL", p.url());
							sheet.writeString("AREA", p.areaName());
							sheet.writeString("SECTOR", p.sectorName());
							sheet.writeString("PROBLEM", p.problemName());
							sheet.writeInt("PITCH", p.pitch());
							sheet.writeString("GRADE", p.grade());
							sheet.writeString("DESCRIPTION", p.description());
						}
					}
				}
				try (var os = new ByteArrayOutputStream()) {
					workbook.write(os);
					bytes = os.toByteArray();
				}
			}
			return Response.ok(bytes, OpenApiConstants.APPLICATION_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("TOC", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}
}
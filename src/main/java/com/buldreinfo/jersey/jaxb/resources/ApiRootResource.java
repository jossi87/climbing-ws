package com.buldreinfo.jersey.jaxb.resources;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;

import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.Toc.TocArea;
import com.buldreinfo.jersey.jaxb.model.Toc.TocPitch;
import com.buldreinfo.jersey.jaxb.model.Toc.TocProblem;
import com.buldreinfo.jersey.jaxb.model.Toc.TocRegion;
import com.buldreinfo.jersey.jaxb.model.Toc.TocSector;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Trail;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "General")
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "jwt", scheme = "bearer")
@Path("")
public class ApiRootResource extends BaseResource {

	@Operation(summary = "Get activity feed", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Activity.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/activity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActivity(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector,
			@Parameter(description = "Filter on lower grade", required = false) @QueryParam("lowerGrade") int lowerGrade,
			@Parameter(description = "Include first ascents", required = false) @QueryParam("fa") boolean fa,
			@Parameter(description = "Include comments", required = false) @QueryParam("comments") boolean comments,
			@Parameter(description = "Include ticks (public ascents)", required = false) @QueryParam("ticks") boolean ticks,
			@Parameter(description = "Include new media", required = false) @QueryParam("media") boolean media,
			@Parameter(description = "Offset (see more)", required = false) @QueryParam("offset") int offset) {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<Activity> res = dao.getActivityRepo().getActivity(c, authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get administrators", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/administrators")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdministrators(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, _) -> {
			List<Administrator> administrators = dao.getUserRepo().getAdministrators(c, setup);
			return Response.ok().entity(administrators).build();
		});
	}

	@Operation(summary = "Get webcams", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/webcams")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCameras() {
		return DatabaseContext.buildResponse(() -> {
			VegvesenParser vegvesenPaser = new VegvesenParser();
			List<Webcam> res = vegvesenPaser.getCameras();
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get boulders/routes marked as dangerous", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/dangerous")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDangerous(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<DangerousArea> res = dao.getHierarchyRepo().getDangerous(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get elevation by latitude and longitude", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = Integer.class))}),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/elevation")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getElevation(@Context HttpServletRequest request,
			@QueryParam("latitude") double latitude,
			@QueryParam("longitude") double longitude) {
		if (latitude == 0.0 && longitude == 0.0) {
			return createBadRequestResponse("Invalid coordinates");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (_, _, _, _, _) -> {
			int elevation = GeoHelper.getElevation(latitude, longitude);
			return Response.ok().entity(elevation).build();
		});
	}

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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (_, _, setup, authUserId, _) -> {
			var stats = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageStats(c, authUserId, setup));
			var randomMedia = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageRandomMedia(c, setup));
			var firstAscents = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageFirstAscents(c, authUserId, setup));
			var newestComments = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageNewestAscents(c, authUserId, setup));
			var newestMedia = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageNewestMedia(c, authUserId, setup));
			var lastComments = DatabaseContext.submitDaoTask((dao, c) -> dao.getFrontpageRepo().getFrontpageLastComments(c, authUserId, setup));
			Frontpage res = new Frontpage(stats.join(), randomMedia.join(), firstAscents.join(), newestComments.join(), newestMedia.join(), lastComments.join());
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Collection<GradeDistribution> res = dao.getHierarchyRepo().getGradeDistribution(c, authUserId, idArea, idSector);
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<GradeDistribution> res = dao.getHierarchyRepo().getContentGraph(c, authUserId, setup);
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Meta res = Meta.from(dao, c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get permissions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<PermissionUser> res = dao.getUserRepo().getPermissions(c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/restrictions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRestrictions(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<RestrictionsRegion> res = dao.getHierarchyRepo().getRestrictions(c, authUserId, setup);
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
		return DatabaseContext.buildResponseWithSql(request, (_, _, setup, _) -> {
			List<String> lines = List.of(
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
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, _) -> {
			String res = dao.getHierarchyRepo().getSitemapTxt(c, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Ticks.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTicks(@Context HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 0 returns first page)", required = false) @QueryParam("page") int page
			) {
		if (page < 0) {
			return createBadRequestResponse("Invalid page index");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Ticks res = dao.getTickRepo().getTicks(c, authUserId, setup, page);
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			return Response.ok().entity(dao.getHierarchyRepo().getToc(c, authUserId, setup)).build();
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Toc toc = dao.getHierarchyRepo().getToc(c, authUserId, setup);
			byte[] bytes;
			try (ExcelWorkbook workbook = new ExcelWorkbook()) {
				try (ExcelSheet sheet = workbook.addSheet("TOC")) {
					for (TocRegion r : toc.regions()) {
						for (TocArea a : r.areas()) {
							for (TocSector s : a.sectors()) {
								for (TocProblem p : s.problems()) {
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
									String type = p.t().type();
									if (p.t().subType() != null) {
										type += " (" + p.t().subType() + ")";			
									}
									sheet.writeString("TYPE", type);
									if (!setup.isBouldering()) {
										sheet.writeInt("PITCHES", p.numPitches() > 0? p.numPitches() : 1);
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
				List<TocPitch> pitches = dao.getHierarchyRepo().getTocPitches(c, authUserId, setup);
				if (!pitches.isEmpty()) {
					try (ExcelSheet sheet = workbook.addSheet("TOC_MULTIPITCH_PITCHES")) {
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
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
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

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Todo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTodo(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Todo res = dao.getTodoRepo().getTodo(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Top.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTop(@Context HttpServletRequest request, 
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Top res = dao.getHierarchyRepo().getTop(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrash(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<Trash> res = dao.getTrashRepo().getTrash(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/comments")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postComments(@Context HttpServletRequest request, Comment co) {
		if (co == null || co.idProblem() <= 0 || co.comment() == null || co.comment().strip().isEmpty()) {
			return createBadRequestResponse("Comment payload contains invalid fields or empty body");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			int idGuestbook = dao.getProblemRepo().upsertComment(c, authUserId, setup, co);
			return Response.ok(idGuestbook).build();
		});
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) {
		if (u == null || u.userId() <= 0) {
			return createBadRequestResponse("Invalid or missing userId payload");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getUserRepo().upsertPermissionUser(c, setup, authUserId, u);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) {
		if (sr == null || sr.value() == null || sr.value().strip().isEmpty()) {
			return createBadRequestResponse("Search criteria keyword is missing");
		}
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			String search = sr.value().trim();
			List<Search> res = dao.getHierarchyRepo().getSearch(c, authUserId, setup, search);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) {
		if (t == null || t.idProblem() <= 0) {
			return createBadRequestResponse("Invalid or missing idProblem payload");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getTickRepo().setTick(c, authUserId, setup, t);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/todo")
	public Response postTodo(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("idProblem") int idProblem
			) {
		if (idProblem <= 0) {
			return createBadRequestResponse("Invalid idProblem=" + idProblem);
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getTodoRepo().toggleTodo(c, authUserId, idProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/trails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTrails(@Context HttpServletRequest request, List<Trail> trails) {
		if (trails == null) {
			return createBadRequestResponse("Trails collection payload is missing");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, connection, _, authUserId, _) -> {
			dao.getSectorRepo().upsertTrails(connection, authUserId, trails);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/trash")
	public Response putTrash(@Context HttpServletRequest request,
			@QueryParam("idArea") int idArea,
			@QueryParam("idSector") int idSector,
			@QueryParam("idProblem") int idProblem,
			@QueryParam("idMedia") int idMedia
			) {
		boolean isValidSelection = (idArea > 0 && idSector == 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0 && idMedia > 0);
		if (!isValidSelection) {
			return createBadRequestResponse("Invalid arguments. Exactly one operational identifier target greater than zero must be specified.");
		}
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getTrashRepo().trashRecover(c, setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}
}
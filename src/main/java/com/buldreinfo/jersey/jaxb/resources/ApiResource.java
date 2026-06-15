package com.buldreinfo.jersey.jaxb.resources;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileIdentity;
import com.buldreinfo.jersey.jaxb.model.ProfileAscent;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
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
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Tag(name = "/")
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "jwt", scheme = "bearer")
@Path("/")
public class ApiResource {
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static Logger logger = LogManager.getLogger();

	public ApiResource() {
	}

	@Operation(summary = "Get activity feed", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Activity.class)))}),
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<Activity> res = dao.getActivityRepo().getActivity(c, authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get administrators", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))}),
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

	@Operation(summary = "Get areas", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Area.class)))}),
			@ApiResponse(responseCode = "404", description = "Area not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAreas(@Context HttpServletRequest request,
			@Parameter(description = "Area id", required = false) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			Collection<Area> areas = id > 0?
					Collections.singleton(dao.getAreaRepo().getArea(c, setup, authUserId, id, shouldUpdateHits)) :
						dao.getAreaRepo().getAreaList(c, authUserId, setup.idRegion());
			return Response.ok().entity(areas).build();
		});
	}

	@Operation(summary = "Get area PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Area not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Area area = dao.getAreaRepo().getArea(c, setup, authUserId, id, shouldUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getHierarchyRepo().getGradeDistribution(c, authUserId, area.id(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.AreaSector sector : area.sectors()) {
				Sector s = dao.getSectorRepo().getSector(c, authUserId, orderByGrade, setup, sector.id(), shouldUpdateHits);
				sectors.add(s);
			}
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeArea(setup, area, gradeDistribution, sectors);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			return Response.ok(stream)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(area.name(), "pdf")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get webcams", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/plain", schema = @Schema(implementation = Integer.class))}),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/elevation")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getElevation(@Context HttpServletRequest request,
			@Parameter(description = "latitude", required = true) @QueryParam("latitude") double latitude,
			@Parameter(description = "longitude", required = true) @QueryParam("longitude") double longitude) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (_, _, _, _, _) -> {
			int elevation = GeoHelper.getElevation(latitude, longitude);
			return Response.ok().entity(elevation).build();
		});
	}

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Frontpage.class))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Collection<GradeDistribution> res = dao.getHierarchyRepo().getGradeDistribution(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meta.class))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
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

	@Operation(summary = "Get profile by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Profile.class))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfile(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int reqUserId) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao1, c1, setup, _, _) -> {
			if (reqUserId > 0) {
				dao1.getUserRepo().ensureUserExists(c1, reqUserId);
			}
			var identity = DatabaseContext.submitDaoTask((dao, c) -> dao.getUserRepo().getProfileIdentity(c, setup, reqUserId));
			var kpis = DatabaseContext.submitDaoTask((dao, c) -> dao.getUserRepo().getProfileKpis(c, reqUserId));
			var disciplines = DatabaseContext.submitDaoTask((dao, c) -> dao.getUserRepo().getProfileDisciplines(c, setup, reqUserId));
			Profile res = new Profile(identity.join(), kpis.join(), disciplines.join());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile ascents", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProfileAscent.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/ascents")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileAscents(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getUserRepo().ensureUserExists(c, id);
			List<ProfileAscent> res = dao.getUserRepo().getProfileAscents(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile media by user id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Media.class)))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileMedia(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getUserRepo().ensureUserExists(c, id);
			List<Media> res = dao.getMediaRepo().getProfileMedia(c, authUserId, id, captured);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile todo", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProfileTodo.class))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileTodo(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getUserRepo().ensureUserExists(c, id);
			ProfileTodo res = dao.getUserRepo().getProfileTodo(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (_, _, setup, _) -> {
			List<String> lines = List.of(
					"User-agent: *",
					"Disallow: */pdf", // Disallow all pdf-calls
					"Sitemap: " + setup.url() + "/sitemap.txt");
			return Response.ok().entity(String.join("\r\n", lines)).build();
		});
	}

	@Operation(summary = "Get sector by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Sector.class))}),
			@ApiResponse(responseCode = "404", description = "Sector not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSectors(@Context HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final boolean orderByGrade = setup.isBouldering();
			Sector s = dao.getSectorRepo().getSector(c, authUserId, orderByGrade, setup, id, shouldUpdateHits);
			Response response = Response.ok().entity(s).build();
			return response;
		});
	}

	@Operation(summary = "Get sector PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Sector not found"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request, @Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Sector sector = dao.getSectorRepo().getSector(c, authUserId, false, setup, id, shouldUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getHierarchyRepo().getGradeDistribution(c, authUserId, 0, id);
			final Area area = dao.getAreaRepo().getArea(c, setup, authUserId, sector.areaId(), shouldUpdateHits);
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeArea(setup, area, gradeDistribution, List.of(sector));
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			return Response.ok(stream)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(sector.name(), "pdf")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get sitemap.txt", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Ticks.class))}),
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Ticks res = dao.getTickRepo().getTicks(c, authUserId, setup, page);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Toc.class)))}),
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
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc/xlsx")
	@Produces(MIME_TYPE_XLSX)
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
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("TOC", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Todo.class))}),
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Todo res = dao.getTodoRepo().getTodo(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Top.class))}),
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
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Top res = dao.getHierarchyRepo().getTop(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
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

	@Operation(summary = "Search for problem", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = User.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			List<User> res = dao.getUserRepo().getUserSearch(c, authUserId, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			byte[] bytes = dao.getUserRepo().getUserTicks(c, authUserId);
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("UserTicks", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get Frontpage without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJs(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, _) -> {
			final Optional<Integer> authUserId = Optional.empty();
			var meta = Meta.from(dao, c, setup, authUserId);
			var stats = dao.getFrontpageRepo().getFrontpageStats(c, authUserId, setup);
			FrontpageRandomMedia frontpageRandomMedia = dao.getFrontpageRepo().getFrontpageRandomMedia(c, setup).stream()
					.findAny()
					.orElse(null);
			String description = String.format("%s - %d regions, %d areas, %d %s, %d ticks",
					setup.description(),
					meta.regions().size(),
					stats.areas(),
					stats.problems(),
					(setup.isBouldering()? "boulders" : "routes"),
					stats.ticks());
			String html = getHtml(setup,
					setup.url(),
					setup.title(),
					description,
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.identity().id()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.identity().versionStamp()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.width()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get area by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = "404", description = "Area not found"),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/area/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsArea(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @PathParam("id") int id) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Area a = dao.getAreaRepo().getArea(c, setup, authUserId, id, shouldUpdateHits);
			String description = null;
			if (setup.isBouldering()) {
				description = String.format("Bouldering in %s", a.name());
			}
			else {
				description = String.format("Climbing in %s", a.name());
			}
			Media m = a.media() != null && !a.media().isEmpty()? a.media().getFirst() : null;
			String html = getHtml(setup,
					setup.url() + "/area/" + a.id(),
					a.name(),
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/problem/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblem(@Context HttpServletRequest request, @Parameter(description = "Problem id", required = true) @PathParam("id") int id) {
		return getWithoutJsProblemMedia(request, id, 0);
	}

	@Operation(summary = "Get problem by id and idMedia without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/problem/{id}/{mediaId}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMedia(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Problem p = dao.getProblemRepo().getProblem(c, authUserId, setup, id, false, shouldUpdateHits);
			String title = String.format("%s [%s] (%s / %s)", p.name(), p.grade(), p.areaName(), p.sectorName());
			String description = p.comment();
			if (p.fa() != null && !p.fa().isEmpty()) {
				String fa = p.fa().stream().map(x -> x.name().trim()).collect(Collectors.joining(", "));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.faDateHr())? " (" + p.faDate() + ")" : "");
			}
			Media m = null;
			if (p.media() != null && !p.media().isEmpty()) {
				Optional<Media> optM = p.media()
						.stream()
						.filter(x -> !x.inherited() && (mediaId == 0 || x.identity().id() == mediaId))
						.findFirst();
				if (optM.isPresent()) {
					m = optM.get();
				}
				else {
					m = p.media().getFirst();
				}
			}
			String html = getHtml(setup,
					setup.url() + "/problem/" + p.id(),
					title,
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id, idMedia and pitch without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/problem/{id}/{mediaId}/{pitch}")
	@Produces(MediaType.TEXT_HTML)
	@SuppressWarnings("unused")
	public Response getWithoutJsProblemMediaPitch(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId,
			@Parameter(description = "Pitch", required = true) @PathParam("pitch") int pitch) {
		return getWithoutJsProblemMedia(request, id, mediaId);
	}

	@Operation(summary = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = "404", description = "Sector not found"),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/sector/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsSector(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @PathParam("id") int id) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			final boolean orderByGrade = false;
			Sector s = dao.getSectorRepo().getSector(c, authUserId, orderByGrade, setup, id, shouldUpdateHits);
			String title = String.format("%s (%s)", s.name(), s.areaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.isBouldering()? "Bouldering" : "Climbing"),
					s.areaName(),
					s.name(),
					(s.problems() != null? s.problems().size() : 0),
					(setup.isBouldering()? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.comment())? " | " + s.comment() : ""));
			Media m = s.media() != null && !s.media().isEmpty()? s.media().getFirst() : null;
			String html = getHtml(setup,
					setup.url() + "/sector/" + s.id(),
					title,
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Update area", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAreas(@Context HttpServletRequest request, Area a) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Objects.requireNonNull(Strings.emptyToNull(a.name()));
			Redirect res = dao.getAreaRepo().setArea(c, setup, authUserId, a);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = "200"),
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
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			int idGuestbook = dao.getProblemRepo().upsertComment(c, authUserId, setup, co);
			return Response.ok(idGuestbook).build();
		});
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) {
		Preconditions.checkArgument(u.userId() > 0, "Invalid userId");
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getUserRepo().upsertPermissionUser(c, setup, authUserId, u);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update profile identity", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/profile/identity")
	public Response postProfileIdentity(@Context HttpServletRequest request, ProfileIdentity profile) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getUserRepo().setProfile(c, authUserId, profile);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update theme preference (light/dark)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/profile/theme")
	public Response postProfileTheme(@Context HttpServletRequest request,
			@Parameter(description = "Theme preference (light or dark)", required = true) @QueryParam("themePreference") String themePreference) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getUserRepo().setThemePreference(c, authUserId, themePreference);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) {
		return DatabaseContext.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			String search = Strings.emptyToNull(Strings.nullToEmpty(sr.value()).trim());
			Objects.requireNonNull(search, "Invalid search: " + search);
			List<Search> res = dao.getHierarchyRepo().getSearch(c, authUserId, setup, search);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSectors(@Context HttpServletRequest request, Sector s) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Preconditions.checkArgument(s.areaId() > 1);
			Objects.requireNonNull(Strings.emptyToNull(s.name()));
			Redirect res = dao.getSectorRepo().setSector(c, authUserId, setup, s);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Preconditions.checkArgument(t.idProblem() > 0);
			dao.getTickRepo().setTick(c, authUserId, setup, t);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = "200"),
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
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getTodoRepo().toggleTodo(c, authUserId, idProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = "200"),
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
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, connection, _, authUserId, _) -> {
			dao.getSectorRepo().upsertTrails(connection, authUserId, trails);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update visible regions", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/user/regions")
	public Response postUserRegions(@Context HttpServletRequest request,
			@Parameter(description = "Region id", required = true) @QueryParam("regionId") int regionId,
			@Parameter(description = "Delete (TRUE=hide, FALSE=show)", required = true) @QueryParam("delete") boolean delete
			) {
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.getUserRepo().setUserRegion(c, authUserId, regionId, delete);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/trash")
	public Response putTrash(@Context HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @QueryParam("idSector") int idSector,
			@Parameter(description = "Problem id", required = true) @QueryParam("idProblem") int idProblem,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia
			) {
		Preconditions.checkArgument(
				(idArea > 0 && idSector == 0 && idProblem == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0 && idMedia > 0),
				"Invalid arguments");
		return DatabaseContext.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.getTrashRepo().trashRecover(c, setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	private String getHtml(Setup setup, String pageUrl, String title, String description, int mediaId, long mediaVersionStamp, int mediaWidth, int mediaHeight) {
		String safeTitle = escapeHtml(title);
		String safeDescription = escapeHtml(description);
		String safePageUrl = escapeHtml(pageUrl);
		String ogImageTags = "";
		if (mediaId > 0) {
			String relativePath = StorageManager.getPublicUrl(S3KeyGenerator.getWebJpg(mediaId), mediaVersionStamp);
			String safeAbsoluteImageUrl = escapeHtml(setup.url() + relativePath);
			ogImageTags = """
					<meta property="og:image" content="%s" />
					<meta property="og:image:width" content="%d" />
					<meta property="og:image:height" content="%d" />
					""".formatted(safeAbsoluteImageUrl, mediaWidth, mediaHeight);
		}
		return """
				<!DOCTYPE html>
				<html lang="en">
				<head>
				    <meta charset="UTF-8">
				    <title>%s</title>
				    <meta name="description" content="%s" />
				    <meta property="og:type" content="website" />
				    <meta property="og:description" content="%s" />
				    <meta property="og:url" content="%s" />
				    <meta property="og:title" content="%s" />
				    <meta property="fb:app_id" content="275320366630912" />
				    %s
				</head>
				</html>
				""".formatted(safeTitle, safeDescription, safeDescription, safePageUrl, safeTitle, ogImageTags);
	}
}
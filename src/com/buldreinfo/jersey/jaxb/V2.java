package com.buldreinfo.jersey.jaxb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.MetaHelper;
import com.buldreinfo.jersey.jaxb.helpers.Setup;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumMedia;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumProblems;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumTicks;
import com.buldreinfo.jersey.jaxb.model.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaInfo;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemArea;
import com.buldreinfo.jersey.jaxb.model.ProblemAreaProblem;
import com.buldreinfo.jersey.jaxb.model.ProblemAreaSector;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileStatistics;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.buldreinfo.jersey.jaxb.server.Server;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Tag(name = "/v2/")
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "jwt", scheme = "bearer")
@Path("/v2/")
public class V2 {
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static Logger logger = LogManager.getLogger();

	public V2() {
	}

	@Operation(summary = "Move media to trash")
	@SecurityRequirement(name = "Bearer Authentication")
	@DELETE
	@Path("/media")
	public Response deleteMedia(@Context HttpServletRequest request, @Parameter(description = "Media id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Server.getDao().deleteMedia(c, authUserId, id);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Get activity feed", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Activity.class)))})})
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
			@Parameter(description = "Include new media", required = false) @QueryParam("media") boolean media) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<Activity> res = Server.getDao().getActivity(c, authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get administrators", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))})})
	@GET
	@Path("/administrators")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdministrators(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<Administrator> administrators = Server.getDao().getAdministrators(c, setup.getIdRegion());
			return Response.ok().entity(administrators).build();
		});
	}

	@Operation(summary = "Get areas", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Area.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAreas(@Context HttpServletRequest request,
			@Parameter(description = "Area id", required = false) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Response response = null;
			if (id > 0) {
				Collection<Area> areas = Collections.singleton(Server.getDao().getArea(c, setup, authUserId, id));
				response = Response.ok().entity(areas).build();
			}
			else {
				Collection<Area> areas = Server.getDao().getAreaList(c, authUserId, setup.getIdRegion());
				response = Response.ok().entity(areas).build();
			}
			return response;
		});
	}

	@Operation(summary = "Get area PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Meta meta = Meta.from(c, setup, authUserId);
			final Area area = Server.getDao().getArea(c, setup, authUserId, id);
			final Collection<GradeDistribution> gradeDistribution = Server.getDao().getGradeDistribution(c, authUserId, setup, area.getId(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.AreaSector sector : area.getSectors()) {
				Sector s = Server.getDao().getSector(c, authUserId, orderByGrade, setup, sector.getId());
				sectors.add(s);
			}
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeArea(meta, area, gradeDistribution, sectors);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			String fn = GlobalFunctions.getFilename(area.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		});
	}

	@Operation(summary = "Get webcams", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))})})
	@GET
	@Path("/webcams")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCameras(@Context HttpServletRequest request) {
		return Server.buildResponse(() -> {
			VegvesenParser vegvesenPaser = new VegvesenParser();
			List<Webcam> res = vegvesenPaser.getCameras();
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get boulders/routes marked as dangerous", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/dangerous")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDangerous(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Collection<DangerousArea> res = Server.getDao().getDangerous(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get elevation by latitude and longitude", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = Integer.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/elevation")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getElevation(@Context HttpServletRequest request,
			@Parameter(description = "latitude", required = true) @QueryParam("latitude") double latitude,
			@Parameter(description = "longitude", required = true) @QueryParam("longitude") double longitude) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Preconditions.checkArgument(authUserId.isPresent(), "Service requires logged in user");
			int elevation = GeoHelper.getElevation(latitude, longitude);
			return Response.ok().entity(elevation).build();
		});
	}

	@Operation(summary = "Get frontpage (num media)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageNumMedia.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/num_media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageNumMedia(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			FrontpageNumMedia res = Server.getDao().getFrontpageNumMedia(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Get frontpage (num problems)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageNumProblems.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/num_problems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageNumProblems(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			FrontpageNumProblems res = Server.getDao().getFrontpageNumProblems(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Get frontpage (num ticks)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageNumTicks.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/num_ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageNumTicks(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			FrontpageNumTicks res = Server.getDao().getFrontpageNumTicks(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Get frontpage (random media)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageRandomMedia.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/random_media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageRandomMedia(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			FrontpageRandomMedia res = Server.getDao().getFrontpageRandomMedia(c, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get grade distribution by Area Id or Sector Id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/grade/distribution")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGradeDistribution(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Collection<GradeDistribution> res = Server.getDao().getGradeDistribution(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/graph")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraph(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Collection<GradeDistribution> res = Server.getDao().getContentGraph(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	/**
	 * crc32 is included to ensure correct version downloaded, and not old version from browser cache (e.g. if rotated image)
	 */
	@Operation(summary = "Get media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "image/*", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/images")
	public Response getImages(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Checksum - not used in ws, but necessary to include on client when an image is changed (e.g. rotated) to avoid cached version", required = false) @QueryParam("crc32") int crc32,
			@Parameter(description = "Image size - E.g. minDimention=100 can return an image with the size 100x133px", required = false) @QueryParam("minDimention") int minDimention) {
		return Server.buildResponseWithSql(c -> {
			final Point dimention = minDimention == 0? null : Server.getDao().getMediaDimention(c, id);
			final String acceptHeader = request.getHeader("Accept");
			final boolean webP = dimention == null && acceptHeader != null && acceptHeader.contains("image/webp");
			final String mimeType = webP? "image/webp" : "image/jpeg";
			final java.nio.file.Path p = Server.getDao().getImage(webP, id);
			CacheControl cc = new CacheControl();
			cc.setMaxAge(2678400); // 31 days
			cc.setNoTransform(false);
			if (dimention != null) {
				BufferedImage b = Preconditions.checkNotNull(ImageIO.read(p.toFile()), "Could not read " + p.toString());
				if (b.getWidth() > dimention.getX() && b.getHeight() > dimention.getY()) {
					Mode mode = dimention.getX() < dimention.getY()? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
					b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, mode, minDimention);
				}
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(b, "jpg", baos);
					return Response.ok(baos.toByteArray(), mimeType).cacheControl(cc).build();
				}
			}
			return Response.ok(p.toFile(), mimeType).cacheControl(cc).build();
		});
	}

	@Operation(summary = "Get Media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMedia(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Media res = Server.getDao().getMedia(c, authUserId, idMedia);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get metadata", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meta.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeta(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Meta res = Meta.from(c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get permissions", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<PermissionUser> res = Server.getDao().getPermissions(c, authUserId, setup.getIdRegion());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get problem by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problem")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblem(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Problem res = Server.getDao().getProblem(c, authUserId, setup, id, showHiddenMedia);
			Response response = Response.ok().entity(res).build();
			return response;
		});
	}

	@Operation(summary = "Get problem PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problem/pdf")
	@Produces("application/pdf")
	public Response getProblemPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Access token", required = false) @QueryParam("accessToken") String accessToken,
			@Parameter(description = "Problem id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Problem problem = Server.getDao().getProblem(c, authUserId, setup, id, false);
			final Area area = Server.getDao().getArea(c, setup, authUserId, problem.getAreaId());
			final Sector sector = Server.getDao().getSector(c, authUserId, false, setup, problem.getSectorId());
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeProblem(area, sector, problem);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			String fn = GlobalFunctions.getFilename(problem.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		});
	}

	@Operation(summary = "Get problems", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProblemArea.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblems(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<ProblemArea> res = Server.getDao().getProblemsList(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get problems as Excel (xlsx)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problems/xlsx")
	@Produces(MIME_TYPE_XLSX)
	public Response getProblemsXlsx(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<ProblemArea> res = Server.getDao().getProblemsList(c, authUserId, setup);
			byte[] bytes;
			try (ExcelWorkbook workbook = new ExcelWorkbook()) {
				try (ExcelSheet sheet = workbook.addSheet("TOC")) {
					for (ProblemArea a : res) {
						for (ProblemAreaSector s : a.sectors()) {
							for (ProblemAreaProblem p : s.problems()) {
								sheet.incrementRow();
								sheet.writeHyperlink("URL", p.url());
								sheet.writeString("AREA", a.name());
								sheet.writeString("SECTOR", s.name());
								sheet.writeInt("NR", p.nr());
								sheet.writeString("NAME", p.name());
								sheet.writeString("GRADE", p.grade());
								String type = p.t().type();
								if (p.t().subType() != null) {
									type += " (" + p.t().subType() + ")";			
								}
								sheet.writeString("TYPE", type);
								if (!setup.isBouldering()) {
									sheet.writeInt("PITCHES", p.numPitches() > 0? p.numPitches() : 1);
								}
								sheet.writeString("FA", p.fa());
								sheet.writeDouble("STARS", p.stars());
								sheet.writeString("DESCRIPTION", p.description());
							}
						}
					}
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					workbook.write(os);
					bytes = os.toByteArray();
				}
			}
			String fn = GlobalFunctions.getFilename("ProblemsList", "xlsx");
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + fn + "\"" )
					.build();
		});
	}

	@Operation(summary = "Get profile by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Profile.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfile(@Context HttpServletRequest request,
			@Parameter(description = "User id (will return logged in user without this attribute)", required = true) @QueryParam("id") int reqUserId) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Profile res = Server.getDao().getProfile(c, authUserId, setup, reqUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Media.class)))})})
	@GET
	@Path("/profile/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfilemedia(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<Media> res = Server.getDao().getProfileMediaProblem(c, authUserId, setup, id, captured);
			if (captured) {
				res.addAll(Server.getDao().getProfileMediaCapturedSector(c, authUserId, setup, id));
				res.addAll(Server.getDao().getProfileMediaCapturedArea(c, authUserId, setup, id));
				res.sort(Comparator.comparingInt(Media::id).reversed());
			}
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile statistics by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProfileStatistics.class))})})
	@GET
	@Path("/profile/statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileStatistics(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			ProfileStatistics res = Server.getDao().getProfileStatistics(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile todo", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProfileTodo.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileTodo(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			ProfileTodo res = Server.getDao().getProfileTodo(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get robots.txt", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		final Setup setup = MetaHelper.getMeta().getSetup(request);
		if (setup.isSetRobotsDenyAll()) {
			return Response.ok().entity("User-agent: *\r\nDisallow: /").build(); 
		}
		List<String> lines = Lists.newArrayList(
				"User-agent: *",
				"Disallow: */pdf", // Disallow all pdf-calls
				"Sitemap: " + setup.getUrl("/sitemap.txt"));
		return Response.ok().entity(Joiner.on("\r\n").join(lines)).build(); 
	}

	@Operation(summary = "Get sector by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Sector.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSectors(@Context HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final boolean orderByGrade = setup.isBouldering();
			Sector s = Server.getDao().getSector(c, authUserId, orderByGrade, setup, id);
			Response response = Response.ok().entity(s).build();
			return response;
		});
	}

	@Operation(summary = "Get sector PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Access token", required = false) @QueryParam("accessToken") String accessToken,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Meta meta = Meta.from(c, setup, authUserId);
			final Sector sector = Server.getDao().getSector(c, authUserId, false, setup, id);
			final Collection<GradeDistribution> gradeDistribution = Server.getDao().getGradeDistribution(c, authUserId, setup, 0, id);
			final Area area = Server.getDao().getArea(c, setup, authUserId, sector.getAreaId());
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeArea(meta, area, gradeDistribution, Lists.newArrayList(sector));
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							throw new RuntimeException(e.getMessage(), e);
						}
				}
			};
			String fn = GlobalFunctions.getFilename(sector.getName(), "pdf");
			return Response.ok(stream).header("Content-Disposition", "attachment; filename=\"" + fn + "\"" ).build();
		});
	}

	@Operation(summary = "Get sitemap.txt", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getSitemapTxt(@Context HttpServletRequest request, @QueryParam("base") String base) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			String res = Server.getDao().getSitemapTxt(c, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Ticks.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTicks(@Context HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 0 returns fist page)", required = false) @QueryParam("page") int page
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Ticks res = Server.getDao().getTicks(c, authUserId, setup, page);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Todo.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTodo(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Todo res = Server.getDao().getTodo(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Top.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTop(@Context HttpServletRequest request, 
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Collection<Top> res = Server.getDao().getTop(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Trash.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrash(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<Trash> res = Server.getDao().getTrash(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Search for user", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = User.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			List<User> res = Server.getDao().getUserSearch(c, authUserId, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			byte[] bytes = Server.getDao().getUserTicks(c, authUserId);
			String fn = GlobalFunctions.getFilename("Ticks", "xlsx");
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + fn + "\"" )
					.build();
		});
	}

	@Operation(summary = "Get Frontpage without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJs(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Optional<Integer> authUserId = Optional.empty();
			FrontpageNumProblems frontpageNumProblems = Server.getDao().getFrontpageNumProblems(c, authUserId, setup);
			FrontpageNumMedia frontpageNumMedia = Server.getDao().getFrontpageNumMedia(c, authUserId, setup);
			FrontpageNumTicks frontpageNumTicks = Server.getDao().getFrontpageNumTicks(c, authUserId, setup);
			FrontpageRandomMedia frontpageRandomMedia = Server.getDao().getFrontpageRandomMedia(c, setup);
			String description = String.format("%s - %d %s, %d public ascents, %d images, %d ascents on video",
					setup.getDescription(),
					frontpageNumProblems.numProblems(),
					(setup.isBouldering()? "boulders" : "routes"),
					frontpageNumTicks.numTicks(),
					frontpageNumMedia.numImages(),
					frontpageNumMedia.numMovies());
			String html = getHtml(setup,
					setup.getUrl(),
					setup.getTitle(),
					description,
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.idMedia()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.width()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get area by id without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/area/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsArea(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @PathParam("id") int id) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Optional<Integer> authUserId = Optional.empty();
			Area a = Server.getDao().getArea(c, setup, authUserId, id);
			String description = null;
			String info = a.getTypeNumTicked() == null || a.getTypeNumTicked().isEmpty()? null : a.getTypeNumTicked()
					.stream()
					.map(tnt -> tnt.getNum() + " " + tnt.getType().toLowerCase())
					.collect(Collectors.joining(", "));
			if (setup.isBouldering()) {
				description = String.format("Bouldering in %s (%s)", a.getName(), info);
			}
			else {
				description = String.format("Climbing in %s (%s)", a.getName(), info);
			}
			Media m = a.getMedia() != null && !a.getMedia().isEmpty()? a.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.getUrl("/area/" + a.getId()),
					a.getName(),
					description,
					(m == null? 0 : m.id()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/problem/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblem(@Context HttpServletRequest request, @Parameter(description = "Problem id", required = true) @PathParam("id") int id) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Optional<Integer> authUserId = Optional.empty();
			Problem p = Server.getDao().getProblem(c, authUserId, setup, id, false);
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> x.name().trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			Media m = null;
			if (p.getMedia() != null && !p.getMedia().isEmpty()) {
				Optional<Media> optM = p.getMedia().stream().filter(x -> !x.inherited()).findFirst();
				if (optM.isPresent()) {
					m = optM.get();
				}
				else {
					m = p.getMedia().get(0);
				}
			}
			String html = getHtml(setup,
					setup.getUrl("/problem/" + p.getId()),
					title,
					description,
					(m == null? 0 : m.id()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/sector/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsSector(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @PathParam("id") int id) {
		return Server.buildResponseWithSql(c -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			final Optional<Integer> authUserId = Optional.empty();
			final boolean orderByGrade = false;
			Sector s = Server.getDao().getSector(c, authUserId, orderByGrade, setup, id);
			String title = String.format("%s (%s)", s.getName(), s.getAreaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.isBouldering()? "Bouldering" : "Climbing"),
					s.getAreaName(),
					s.getName(),
					(s.getProblems() != null? s.getProblems().size() : 0),
					(setup.isBouldering()? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.getComment())? " | " + s.getComment() : ""));
			Media m = s.getMedia() != null && !s.getMedia().isEmpty()? s.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.getUrl("/sector/" + s.getId()),
					title,
					description,
					(m == null? 0 : m.id()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Update area (area must be provided as json on field \"json\" in multiPart)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/areas")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAreas(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		Area a = new Gson().fromJson(multiPart.getField("json").getValue(), Area.class);
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			Redirect res = Server.getDao().setArea(c, setup, authUserId, a, multiPart);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update comment (comment must be provided as json on field \"json\" in multiPart)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/comments")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postComments(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		Comment co = new Gson().fromJson(multiPart.getField("json").getValue(), Comment.class);
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Server.getDao().upsertComment(c, authUserId, setup, co, multiPart);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update Media SVG")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/svg")
	public Response postMediaSvg(@Context HttpServletRequest request, Media m) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Server.getDao().upsertMediaSvg(c, authUserId, setup, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update user privilegies")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Server.getDao().upsertPermissionUser(c, setup.getIdRegion(), authUserId, u);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update problem (problem must be provided as json on field \"json\" in multiPart)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblems(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			Redirect res = Server.getDao().setProblem(c, authUserId, setup, p, multiPart);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Add media on problem (problem must be provided as json on field \"json\" in multiPart)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems/media")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblemsMedia(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			FormDataMultiPart multiPart) {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			Server.getDao().addProblemMedia(c, authUserId, p, multiPart);
			Problem res = Server.getDao().getProblem(c, authUserId, setup, p.getId(), false);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update topo line on route/boulder (SVG on sector/problem-image)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@Parameter(description = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Preconditions.checkNotNull(svg, "Invalid svg=" + svg);
			Server.getDao().upsertSvg(c, authUserId, problemId, mediaId, svg);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Search.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			String search = Strings.emptyToNull(Strings.nullToEmpty(sr.value()).trim());
			Preconditions.checkNotNull(search, "Invalid search: " + search);
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			List<Search> res = Server.getDao().getSearch(c, authUserId, setup, search);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update sector (sector smust be provided as json on field \"json\" in multiPart)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/sectors")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSectors(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		Sector s = new Gson().fromJson(multiPart.getField("json").getValue(), Sector.class);
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			final boolean orderByGrade = setup.isBouldering();
			Redirect res = Server.getDao().setSector(c, authUserId, orderByGrade, setup, s, multiPart);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Preconditions.checkArgument(t.idProblem() > 0);
			Server.getDao().setTick(c, authUserId, setup, t);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update todo")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/todo")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postTodo(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("idProblem") int idProblem
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Server.getDao().toggleTodo(c, authUserId, idProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update visible regions")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/user/regions")
	public Response postUserRegions(@Context HttpServletRequest request,
			@Parameter(description = "Region id", required = true) @QueryParam("regionId") int regionId,
			@Parameter(description = "Delete (TRUE=hide, FALSE=show)", required = true) @QueryParam("delete") boolean delete
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Server.getDao().setUserRegion(c, authUserId, regionId, delete);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media location")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media")
	public Response putMedia(@Context HttpServletRequest request,
			@Parameter(description = "Move right", required = true) @QueryParam("id") int id,
			@Parameter(description = "Move left", required = true) @QueryParam("left") boolean left,
			@Parameter(description = "To sector id (will move media to sector if toSectorId>0 and toProblemId=0)", required = true) @QueryParam("toIdSector") int toIdSector,
			@Parameter(description = "To problem id (will move media to problem if toProblemId>0 and toSectorId=0)", required = true) @QueryParam("toIdProblem") int toIdProblem
			) {
		Preconditions.checkArgument((left && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdSector > 0 && toIdProblem == 0) ||
				(!left && toIdSector == 0 && toIdProblem > 0),
				"Invalid arguments");
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Preconditions.checkArgument(id > 0);
			Server.getDao().moveMedia(c, authUserId, id, left, toIdSector, toIdProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media info")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media/info")
	public Response putMediaInfo(@Context HttpServletRequest request, MediaInfo m) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			Server.getDao().updateMediaInfo(c, authUserId, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media rotation (allowed for administrators + user who uploaded specific image)")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media/jpeg/rotate")
	public Response putMediaJpegRotate(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia,
			@Parameter(description = "Degrees (90/180/270)", required = true) @QueryParam("degrees") int degrees
			) {
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Server.getDao().rotateMedia(c, setup.getIdRegion(), authUserId, idMedia, degrees);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)")
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
				(idArea == 0 && idSector == 0 && idProblem == 0),
				"Invalid arguments");
		return Server.buildResponseWithSqlAndAuth(request, (c, authUserId) -> {
			final Setup setup = MetaHelper.getMeta().getSetup(request);
			Server.getDao().trashRecover(c, setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}

	private String getHtml(Setup setup, String url, String title, String description, int mediaId, int mediaWidth, int mediaHeight) {
		String ogImage = "";
		if (mediaId > 0) {
			String image = setup.getUrl("/buldreinfo_media/jpg/" + String.valueOf(mediaId/100*100) + "/" + mediaId + ".jpg");
			ogImage = "<meta property=\"og:image\" content=\"" + image + "\" />" + 
					"<meta property=\"og:image:width\" content=\"" + mediaWidth + "\" />" + 
					"<meta property=\"og:image:height\" content=\"" + mediaHeight + "\" />";
		}
		String html = "<html><head>" +
				"<meta charset=\"UTF-8\">" +
				"<title>" + title + "</title>" + 
				"<meta name=\"description\" content=\"" + description + "\" />" + 
				"<meta property=\"og:type\" content=\"website\" />" + 
				"<meta property=\"og:description\" content=\"" + description + "\" />" + 
				"<meta property=\"og:url\" content=\"" + url + "\" />" + 
				"<meta property=\"og:title\" content=\"" + title + "\" />" + 
				"<meta property=\"fb:app_id\" content=\"275320366630912\" />" +
				ogImage +
				"</head></html>";
		return html;
	}
}
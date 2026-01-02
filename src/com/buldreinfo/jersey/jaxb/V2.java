package com.buldreinfo.jersey.jaxb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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

import com.buldreinfo.jersey.jaxb.beans.GradeSystem;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.CacheHelper;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.buldreinfo.jersey.jaxb.io.ImageSaver;
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
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.TocArea;
import com.buldreinfo.jersey.jaxb.model.TocPitch;
import com.buldreinfo.jersey.jaxb.model.TocProblem;
import com.buldreinfo.jersey.jaxb.model.TocRegion;
import com.buldreinfo.jersey.jaxb.model.TocSector;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.TopRank;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.deleteMedia(c, authUserId, id);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			List<Activity> res = dao.getActivity(c, authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get administrators", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))})})
	@GET
	@Path("/administrators")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdministrators(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			List<Administrator> administrators = dao.getAdministrators(c, setup.idRegion());
			return Response.ok().entity(administrators).build();
		});
	}

	@Operation(summary = "Get areas", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Area.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAreas(@Context HttpServletRequest request,
			@Parameter(description = "Area id", required = false) @QueryParam("id") int id,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Collection<Area> areas = id > 0?
					Collections.singleton(dao.getArea(c, setup, authUserId, id, !dontUpdateHits)) :
						dao.getAreaList(c, authUserId, setup.idRegion());
			return Response.ok().entity(areas).build();
		});
	}

	@Operation(summary = "Get area PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			final boolean dontUpdateHits = true;
			final Meta meta = Meta.from(dao, c, setup, authUserId);
			final Area area = dao.getArea(c, setup, authUserId, id, !dontUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getGradeDistribution(c, authUserId, setup, area.getId(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.AreaSector sector : area.getSectors()) {
				Sector s = dao.getSector(c, authUserId, orderByGrade, setup, sector.getId(), !dontUpdateHits);
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

	@Operation(summary = "Get avatar by user id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "image/*", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@GET
	@Path("/avatar")
	public Response getAvatar(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Avatar CRC32 (cache buster)", required = false) @QueryParam("avatarCrc32") long avatarCrc32,
			@Parameter(description = "Full size", required = false) @QueryParam("fullSize") boolean fullSize) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			java.nio.file.Path p = fullSize ? IOHelper.getPathOriginalUsers(id) : IOHelper.getPathWebUsers(id);
			var builder = Response.ok(p.toFile(), "image/jpeg");
			builder = CacheHelper.applyImmutableLongTermCache(builder);
			return builder.build();
		});
	}

	@Operation(summary = "Get webcams", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Webcam.class)))})})
	@GET
	@Path("/webcams")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCameras() {
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Collection<DangerousArea> res = dao.getDangerous(c, authUserId, setup);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			FrontpageNumMedia res = dao.getFrontpageNumMedia(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get frontpage (num problems)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageNumProblems.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/num_problems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageNumProblems(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			FrontpageNumProblems res = dao.getFrontpageNumProblems(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get frontpage (num ticks)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageNumTicks.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage/num_ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageNumTicks(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			FrontpageNumTicks res = dao.getFrontpageNumTicks(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get frontpage (random media)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = FrontpageRandomMedia.class))})})
	@GET
	@Path("/frontpage/random_media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpageRandomMedia(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			FrontpageRandomMedia res = dao.getFrontpageRandomMedia(c, setup);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Collection<GradeDistribution> res = dao.getGradeDistribution(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/graph")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraph(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Collection<GradeDistribution> res = dao.getContentGraph(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Get media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "image/*", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@GET
	@Path("/images")
	public Response getImages(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Checksum - not used in ws, but necessary to include on client when an image is changed (e.g. rotated) to avoid cached version (cache buster)", required = false) @QueryParam("crc32") int crc32,
			@Parameter(description = "Image region - x", required = false) @QueryParam("x") int x,
			@Parameter(description = "Image region - y", required = false) @QueryParam("y") int y,
			@Parameter(description = "Image region - width", required = false) @QueryParam("width") int width,
			@Parameter(description = "Image region - height", required = false) @QueryParam("height") int height,
			@Parameter(description = "Target Width - The image will be resized to fit this exact width (without upscaling).", required = false) @QueryParam("targetWidth") int targetWidth,
			@Parameter(description = "Minimum Dimension - Ensures the *shortest* edge (min(width, height)) of the returned image is at least this many pixels (without upscaling).", required = false) @QueryParam("minDimension") int minDimension) throws IOException {
		logger.debug("getImages(id={}, crc32={}, x={}, y={}, width={}, height={}, targetWidth={}, minDimention={}) initialized", id, crc32, x, y, width, height, targetWidth, minDimension);
		if (width == 0 && height == 0 && targetWidth == 0 && minDimension == 0) {
			boolean webP = GlobalFunctions.requestAcceptsWebp(request);
			String mimeType = webP ? "image/webp" : "image/jpeg";
			var builder = Response.ok(IOHelper.getPathImage(id, webP).toFile(), mimeType);
			builder = CacheHelper.applyImmutableLongTermCache(builder);
			return builder.build();
		}
		if (width > 0 && height > 0) { // crop
			var p = IOHelper.getPathMediaWebJpgRegion(id, x, y, width, height);
			if (!Files.exists(p)) {
				java.nio.file.Path original = IOHelper.getPathMediaOriginalJpg(id);
				BufferedImage b = Preconditions.checkNotNull(ImageIO.read(original.toFile()), "Could not read " + original.toString());
				b = Scalr.crop(b, x, y, width, height);
				Files.createDirectories(p.getParent());
				ImageIO.write(b, "jpg", p.toFile());
				b.flush();
			}
			var builder = Response.ok(p.toFile(), "image/jpeg");
			builder = CacheHelper.applyImmutableLongTermCache(builder);
			return builder.build();
		}
		boolean useWebImageSource = targetWidth <= ImageSaver.IMAGE_WEB_WIDTH && minDimension <= ImageSaver.IMAGE_WEB_HEIGHT && minDimension <= ImageSaver.IMAGE_WEB_WIDTH;
		boolean webP = false;
		var p = useWebImageSource ? IOHelper.getPathImage(id, webP) : IOHelper.getPathMediaOriginalJpg(id);
		BufferedImage b = Preconditions.checkNotNull(ImageIO.read(p.toFile()), "Could not read " + p.toString());
		if (targetWidth > 0 && targetWidth < b.getWidth()) {
			b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth);
		}
		else if (minDimension > 0 && minDimension < b.getWidth() && minDimension < b.getHeight()) {
			Mode mode = b.getWidth() < b.getHeight()? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
			b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, mode, minDimension);
		}
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(b, "jpg", baos);
			var builder = Response.ok(baos.toByteArray(), "image/jpeg");
			builder = CacheHelper.applyImmutableLongTermCache(builder);
			return builder.build();
		}
	}

	@Operation(summary = "Get Media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMedia(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Media res = dao.getMedia(c, authUserId, idMedia);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get metadata", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meta.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeta(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Meta res = Meta.from(dao, c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get permissions", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			List<PermissionUser> res = dao.getPermissions(c, authUserId, setup.idRegion());
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
			@Parameter(description = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Problem res = dao.getProblem(c, authUserId, setup, id, showHiddenMedia, !dontUpdateHits);
			Response response = Response.ok().entity(res).build();
			return response;
		});
	}

	@Operation(summary = "Get problem PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problem/pdf")
	@Produces("application/pdf")
	public Response getProblemPdf(@Context final HttpServletRequest request, @Parameter(description = "Problem id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			final boolean dontUpdateHits = true;
			final Problem problem = dao.getProblem(c, authUserId, setup, id, false, !dontUpdateHits);
			final Area area = dao.getArea(c, setup, authUserId, problem.getAreaId(), !dontUpdateHits);
			final Sector sector = dao.getSector(c, authUserId, false, setup, problem.getSectorId(), !dontUpdateHits);
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

	@Operation(summary = "Get profile by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Profile.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfile(@Context HttpServletRequest request,
			@Parameter(description = "User id (will return logged in user without this attribute)", required = true) @QueryParam("id") int reqUserId) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Profile res = dao.getProfile(c, authUserId, setup, reqUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile media by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Media.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfilemedia(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			List<Media> res = dao.getProfileMediaProblem(c, authUserId, id, captured);
			if (captured) {
				res.addAll(dao.getProfileMediaCapturedSector(c, authUserId, id));
				res.addAll(dao.getProfileMediaCapturedArea(c, authUserId, id));
				res.sort(Comparator.comparingInt(Media::id).reversed());
			}
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile statistics by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProfileStatistics.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileStatistics(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			ProfileStatistics res = dao.getProfileStatistics(c, authUserId, setup, id);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			ProfileTodo res = dao.getProfileTodo(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get robots.txt", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			List<String> lines = Lists.newArrayList(
					"User-agent: *",
					"Disallow: */pdf", // Disallow all pdf-calls
					"Sitemap: " + setup.url() + "/sitemap.txt");
			return Response.ok().entity(Joiner.on("\r\n").join(lines)).build();
		});
	}

	@Operation(summary = "Get sector by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Sector.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSectors(@Context HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			final boolean orderByGrade = setup.gradeSystem().equals(GradeSystem.BOULDER);
			Sector s = dao.getSector(c, authUserId, orderByGrade, setup, id, !dontUpdateHits);
			Response response = Response.ok().entity(s).build();
			return response;
		});
	}

	@Operation(summary = "Get sector PDF by id", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request, @Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			final boolean dontUpdateHits = true;
			final Meta meta = Meta.from(dao, c, setup, authUserId);
			final Sector sector = dao.getSector(c, authUserId, false, setup, id, !dontUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getGradeDistribution(c, authUserId, setup, 0, id);
			final Area area = dao.getArea(c, setup, authUserId, sector.getAreaId(), !dontUpdateHits);
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
	public Response getSitemapTxt(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			String res = dao.getSitemapTxt(c, setup);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Ticks res = dao.getTicks(c, authUserId, setup, page);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Toc.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getToc(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			return Response.ok().entity(dao.getToc(c, authUserId, setup)).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems) as Excel (xlsx)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc/xlsx")
	@Produces(MIME_TYPE_XLSX)
	public Response getTocXlsx(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Toc toc = dao.getToc(c, authUserId, setup);
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
									String type = p.t().type();
									if (p.t().subType() != null) {
										type += " (" + p.t().subType() + ")";			
									}
									sheet.writeString("TYPE", type);
									if (!setup.gradeSystem().equals(GradeSystem.BOULDER)) {
										sheet.writeInt("PITCHES", p.numPitches() > 0? p.numPitches() : 1);
									}
									sheet.writeString("FA", p.fa());
									sheet.writeDouble("STARS", p.stars());
									sheet.writeString("DESCRIPTION", p.description());
								}
							}
						}
					}
				}
				List<TocPitch> pitches = dao.getTocPitches(c, authUserId, setup);
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
			String fn = GlobalFunctions.getFilename("ProblemsList", "xlsx");
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + fn + "\"" )
					.build();
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Todo res = dao.getTodo(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Top.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTop(@Context HttpServletRequest request, 
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Top res = dao.getTop(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Trash.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrash(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			List<Trash> res = dao.getTrash(c, authUserId, setup);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			List<User> res = dao.getUserSearch(c, authUserId, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			byte[] bytes = dao.getUserTicks(c, authUserId);
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
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			final Optional<Integer> authUserId = Optional.empty();
			FrontpageNumProblems frontpageNumProblems = dao.getFrontpageNumProblems(c, authUserId, setup);
			FrontpageNumMedia frontpageNumMedia = dao.getFrontpageNumMedia(c, authUserId, setup);
			FrontpageNumTicks frontpageNumTicks = dao.getFrontpageNumTicks(c, authUserId, setup);
			FrontpageRandomMedia frontpageRandomMedia = dao.getFrontpageRandomMedia(c, setup);
			String description = String.format("%s - %d %s, %d public ascents, %d images, %d ascents on video",
					setup.description(),
					frontpageNumProblems.numProblems(),
					(setup.gradeSystem().equals(GradeSystem.BOULDER)? "boulders" : "routes"),
					frontpageNumTicks.numTicks(),
					frontpageNumMedia.numImages(),
					frontpageNumMedia.numMovies());
			String html = getHtml(setup,
					setup.url(),
					setup.title(),
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
	public Response getWithoutJsArea(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @PathParam("id") int id,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Area a = dao.getArea(c, setup, authUserId, id, !dontUpdateHits);
			String description = null;
			String info = a.getTypeNumTickedTodo() == null || a.getTypeNumTickedTodo().isEmpty()? null : a.getTypeNumTickedTodo()
					.stream()
					.map(tnt -> tnt.getNum() + " " + tnt.getType().toLowerCase())
					.collect(Collectors.joining(", "));
			if (setup.gradeSystem().equals(GradeSystem.BOULDER)) {
				description = String.format("Bouldering in %s (%s)", a.getName(), info);
			}
			else {
				description = String.format("Climbing in %s (%s)", a.getName(), info);
			}
			Media m = a.getMedia() != null && !a.getMedia().isEmpty()? a.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.url() + "/area/" + a.getId(),
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
	public Response getWithoutJsProblem(@Context HttpServletRequest request, @Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return getWithoutJsProblemMedia(request, id, 0, dontUpdateHits);
	}

	@Operation(summary = "Get problem by id and idMedia without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/problem/{id}/{mediaId}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMedia(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Problem p = dao.getProblem(c, authUserId, setup, id, false, !dontUpdateHits);
			String title = String.format("%s [%s] (%s / %s)", p.getName(), p.getGrade(), p.getAreaName(), p.getSectorName());
			String description = p.getComment();
			if (p.getFa() != null && !p.getFa().isEmpty()) {
				String fa = Joiner.on(", ").join(p.getFa().stream().map(x -> x.name().trim()).collect(Collectors.toList()));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.getFaDateHr())? " (" + p.getFaDate() + ")" : "");
			}
			Media m = null;
			if (p.getMedia() != null && !p.getMedia().isEmpty()) {
				Optional<Media> optM = p.getMedia()
						.stream()
						.filter(x -> !x.inherited() && (mediaId == 0 || x.id() == mediaId))
						.findFirst();
				if (optM.isPresent()) {
					m = optM.get();
				}
				else {
					m = p.getMedia().get(0);
				}
			}
			String html = getHtml(setup,
					setup.url() + "/problem/" + p.getId(),
					title,
					description,
					(m == null? 0 : m.id()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id, idMedia and pitch without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/problem/{id}/{mediaId}/{pitch}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMediaPitch(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId,
			@Parameter(description = "Pitch", required = true) @PathParam("pitch") int pitch,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return getWithoutJsProblemMedia(request, id, mediaId, dontUpdateHits);
	}

	@Operation(summary = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))})})
	@GET
	@Path("/without-js/sector/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsSector(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @PathParam("id") int id,
			@Parameter(description = "Dont update hits", required = false) @QueryParam("dontUpdateHits") boolean dontUpdateHits) {
		return Server.buildResponseWithSql(request, (dao, c, setup) -> {
			final Optional<Integer> authUserId = Optional.empty();
			final boolean orderByGrade = false;
			Sector s = dao.getSector(c, authUserId, orderByGrade, setup, id, !dontUpdateHits);
			String title = String.format("%s (%s)", s.getName(), s.getAreaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.gradeSystem().equals(GradeSystem.BOULDER)? "Bouldering" : "Climbing"),
					s.getAreaName(),
					s.getName(),
					(s.getProblems() != null? s.getProblems().size() : 0),
					(setup.gradeSystem().equals(GradeSystem.BOULDER)? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.getComment())? " | " + s.getComment() : ""));
			Media m = s.getMedia() != null && !s.getMedia().isEmpty()? s.getMedia().get(0) : null;
			String html = getHtml(setup,
					setup.url() + "/sector/" + s.getId(),
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			Redirect res = dao.setArea(c, setup, authUserId, a, multiPart);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.upsertComment(c, authUserId, setup, co, multiPart);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update Media SVG")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/svg")
	public Response postMediaSvg(@Context HttpServletRequest request, Media m) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.upsertMediaSvg(c, authUserId, setup, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update user privilegies")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.upsertPermissionUser(c, setup.idRegion(), authUserId, u);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			Redirect res = dao.setProblem(c, authUserId, setup, p, multiPart);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Add media on problem (problem must be provided as json on field \"json\" in multiPart)", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems/media")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblemsMedia(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		final boolean dontUpdateHits = true;
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			dao.addProblemMedia(c, authUserId, p, multiPart);
			Problem res = dao.getProblem(c, authUserId, setup, p.getId(), false, !dontUpdateHits);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update topo line on route/boulder (SVG on sector/problem-image)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@Parameter(description = "Problem section id", required = true) @QueryParam("pitch") int pitch,
			@Parameter(description = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Preconditions.checkNotNull(svg, "Invalid svg=" + svg);
			dao.upsertSvg(c, authUserId, problemId, pitch, mediaId, svg);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update profile (profile must be provided as json on field \"json\" in multiPart, \"avatar\" is optional)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/profile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postProfile(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		Profile profile = new Gson().fromJson(multiPart.getField("json").getValue(), Profile.class);
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.setProfile(c, authUserId, setup, profile, multiPart);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Search.class)))})})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			String search = Strings.emptyToNull(Strings.nullToEmpty(sr.value()).trim());
			Preconditions.checkNotNull(search, "Invalid search: " + search);
			List<Search> res = dao.getSearch(c, authUserId, setup, search);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			Redirect res = dao.setSector(c, authUserId, setup, s, multiPart);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)")
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(t.idProblem() > 0);
			dao.setTick(c, authUserId, setup, t);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.toggleTodo(c, authUserId, idProblem);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.setUserRegion(c, authUserId, regionId, delete);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media location")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media")
	public Response putMedia(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Move left", required = true) @QueryParam("left") boolean left,
			@Parameter(description = "To sector id (will move media to area if toIdArea>0, toIdSector=0 and toIdProblem=0)", required = true) @QueryParam("toIdArea") int toIdArea,
			@Parameter(description = "To sector id (will move media to sector if toSectorId>0, toIdArea=0 and toIdProblem=0)", required = true) @QueryParam("toIdSector") int toIdSector,
			@Parameter(description = "To problem id (will move media to problem if toProblemId>0, toIdArea=0 and toSectorId=0)", required = true) @QueryParam("toIdProblem") int toIdProblem
			) {
		Preconditions.checkArgument((left && toIdArea == 0 && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdArea == 0 && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdArea > 0 && toIdSector == 0 && toIdProblem == 0) ||
				(!left && toIdArea == 0 && toIdSector > 0 && toIdProblem == 0) ||
				(!left && toIdArea == 0 && toIdSector == 0 && toIdProblem > 0),
				"Invalid arguments");
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(id > 0);
			dao.moveMedia(c, authUserId, id, left, toIdArea, toIdSector, toIdProblem);
			return Response.ok().build();
		});
	}
	
	@Operation(summary = "Set media as avatar")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media/avatar")
	public Response putMediaAvatar(@Context HttpServletRequest request, @Parameter(description = "Media id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			Preconditions.checkArgument(id > 0);
			try (InputStream is = Files.newInputStream(IOHelper.getPathMediaOriginalJpg(id))) {
				dao.saveAvatar(c, authUserId, is);
			}
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media info")
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media/info")
	public Response putMediaInfo(@Context HttpServletRequest request, MediaInfo m) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.updateMediaInfo(c, authUserId, m);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.rotateMedia(c, setup.idRegion(), authUserId, idMedia, degrees);
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId) -> {
			dao.trashRecover(c, setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}

	private String getHtml(Setup setup, String url, String title, String description, int mediaId, int mediaWidth, int mediaHeight) {
		String ogImage = "";
		if (mediaId > 0) {
			String image = setup.url() + "/buldreinfo_media/jpg/" + String.valueOf(mediaId/100*100) + "/" + mediaId + ".jpg";
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
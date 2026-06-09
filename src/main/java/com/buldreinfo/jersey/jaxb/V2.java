package com.buldreinfo.jersey.jaxb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.ApifyInstagramResolver;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.ImageSaver;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.io.VideoHelper;
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
import com.buldreinfo.jersey.jaxb.model.ProblemSearchResult;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileIdentity;
import com.buldreinfo.jersey.jaxb.model.ProfileAscent;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
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
import com.buldreinfo.jersey.jaxb.model.VideoInitPayload;
import com.buldreinfo.jersey.jaxb.model.VideoInitResponse;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.buldreinfo.jersey.jaxb.xml.Webcam;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
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

@Tag(name = "/v2/")
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "jwt", scheme = "bearer")
@Path("/v2/")
public class V2 {
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static Logger logger = LogManager.getLogger();

	public V2() {
	}

	@Operation(summary = "Move media to trash", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@DELETE
	@Path("/media")
	public Response deleteMedia(@Context HttpServletRequest request, @Parameter(description = "Media id", required = true) @QueryParam("id") int id) {
		Preconditions.checkArgument(id > 0, "Invalid id=" + id);
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.deleteMedia(c, authUserId, id);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Get activity feed", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Activity.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
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
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<Activity> res = dao.getActivity(c, authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get administrators", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Administrator.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/administrators")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdministrators(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup, _) -> {
			List<Administrator> administrators = dao.getAdministrators(c, setup);
			return Response.ok().entity(administrators).build();
		});
	}

	@Operation(summary = "Get areas", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Area.class)))}),
			@ApiResponse(responseCode = "404", description = "Area not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAreas(@Context HttpServletRequest request,
			@Parameter(description = "Area id", required = false) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			Collection<Area> areas = id > 0?
					Collections.singleton(dao.getArea(c, setup, authUserId, id, shouldUpdateHits)) :
						dao.getAreaList(c, authUserId, setup.idRegion());
			return Response.ok().entity(areas).build();
		});
	}

	@Operation(summary = "Get area PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Area not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Area area = dao.getArea(c, setup, authUserId, id, shouldUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getGradeDistribution(c, authUserId, area.id(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.AreaSector sector : area.sectors()) {
				Sector s = dao.getSector(c, authUserId, orderByGrade, setup, sector.id(), shouldUpdateHits);
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
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

	@Operation(summary = "Get boulders/routes marked as dangerous", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/dangerous")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDangerous(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<DangerousArea> res = dao.getDangerous(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get elevation by latitude and longitude", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/plain", schema = @Schema(implementation = Integer.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/elevation")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getElevation(@Context HttpServletRequest request,
			@Parameter(description = "latitude", required = true) @QueryParam("latitude") double latitude,
			@Parameter(description = "longitude", required = true) @QueryParam("longitude") double longitude) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (_, _, _, _, _) -> {
			int elevation = GeoHelper.getElevation(latitude, longitude);
			return Response.ok().entity(elevation).build();
		});
	}

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Frontpage.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFrontpage(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (_, _, setup, authUserId, _) -> {
			var stats = Server.submitDaoTask((dao, c) -> dao.getFrontpageStats(c, authUserId, setup));
			var randomMedia = Server.submitDaoTask((dao, c) -> dao.getFrontpageRandomMedia(c, setup));
			var firstAscents = Server.submitDaoTask((dao, c) -> dao.getFrontpageFirstAscents(c, authUserId, setup));
			var newestComments = Server.submitDaoTask((dao, c) -> dao.getFrontpageNewestAscents(c, authUserId, setup));
			var newestMedia = Server.submitDaoTask((dao, c) -> dao.getFrontpageNewestMedia(c, authUserId, setup));
			var lastComments = Server.submitDaoTask((dao, c) -> dao.getFrontpageLastComments(c, authUserId, setup));
			Frontpage res = new Frontpage(stats.get(), randomMedia.get(), firstAscents.join(), newestComments.join(), newestMedia.join(), lastComments.join());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get grade distribution by Area Id or Sector Id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/grade/distribution")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGradeDistribution(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Collection<GradeDistribution> res = dao.getGradeDistribution(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/graph")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGraph(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<GradeDistribution> res = dao.getContentGraph(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get Media by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = "404", description = "Media not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMedia(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Media res = dao.getMedia(c, authUserId, idMedia);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get media file by id", responses = {
			@ApiResponse(responseCode = "302", description = "Redirects to the public object storage URL"),
			@ApiResponse(responseCode = "404", description = "Media file not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/media/file")
	public Response getMediaFile(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Is movie (true) or image (false)", required = true) @QueryParam("isMovie") boolean isMovie,
			@Parameter(description = "Version stamp (cache buster)", required = false) @QueryParam("versionStamp") int versionStamp,
			@Parameter(description = "Download original source", required = false) @QueryParam("original") boolean original,
			@Parameter(description = "Target Width", required = false) @QueryParam("targetWidth") int targetWidth,
			@Parameter(description = "Minimum Dimension", required = false) @QueryParam("minDimension") int minDimension,
			@Parameter(description = "Region X", required = false) @QueryParam("x") int x,
			@Parameter(description = "Region Y", required = false) @QueryParam("y") int y,
			@Parameter(description = "Region Width", required = false) @QueryParam("width") int width,
			@Parameter(description = "Region Height", required = false) @QueryParam("height") int height) {
		StorageManager storage = StorageManager.getInstance();
		// Movie
		if (isMovie) {
			String finalObjectKey = GlobalFunctions.requestAcceptsWebm(request) ? S3KeyGenerator.getWebWebm(id) : S3KeyGenerator.getWebMp4(id);
			if (!storage.exists(finalObjectKey)) {
				return createNotFoundResponse("Media file not found");
			}
			return createRedirect(finalObjectKey, versionStamp);
		}
		// Image
		String finalObjectKey;
		boolean webP = GlobalFunctions.requestAcceptsWebp(request);
		if (original) {
			finalObjectKey = S3KeyGenerator.getOriginalJpg(id);
			if (!storage.exists(finalObjectKey)) {
				return createNotFoundResponse("Media file not found");
			}
		} 
		else if (targetWidth > 0 || minDimension > 0) {
			finalObjectKey = webP ? S3KeyGenerator.getWebWebpResized(id, targetWidth, minDimension) : S3KeyGenerator.getWebJpgResized(id, targetWidth, minDimension);
			if (!storage.exists(finalObjectKey)) {
				return Server.buildResponse(() -> {
					boolean useWebSource = (targetWidth <= 0 || targetWidth <= ImageSaver.IMAGE_WEB_WIDTH) && (minDimension <= 0 || minDimension <= ImageSaver.IMAGE_WEB_WIDTH);
					String sourceKey = useWebSource ? S3KeyGenerator.getWebJpg(id) : S3KeyGenerator.getOriginalJpg(id);
					if (useWebSource && !storage.exists(sourceKey)) {
						sourceKey = S3KeyGenerator.getOriginalJpg(id);
					}
					if (!storage.exists(sourceKey)) {
						return createNotFoundResponse("Media file not found");
					}
					BufferedImage b = storage.downloadImage(sourceKey);
					if (b == null) {
						return createNotFoundResponse("Media file not found");
					}
					if (targetWidth > 0 && targetWidth < b.getWidth()) {
						b = Scalr.resize(b, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth);
					}
					else if (minDimension > 0) {
						Scalr.Mode mode = b.getWidth() < b.getHeight() ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
						b = Scalr.resize(b, Scalr.Method.QUALITY, mode, minDimension);
					}
					storage.uploadImage(finalObjectKey, b, webP ? StorageType.WEBP : StorageType.JPG);
					b.flush();
					if (!storage.exists(finalObjectKey)) {
						return createNotFoundResponse("Media file not found");
					}
					return createRedirect(finalObjectKey, versionStamp);
				});
			}
		} 
		else if (width > 0 && height > 0) {
			finalObjectKey = webP ? S3KeyGenerator.getWebWebpRegion(id, x, y, width, height) : S3KeyGenerator.getWebJpgRegion(id, x, y, width, height);
			if (!storage.exists(finalObjectKey)) {
				return Server.buildResponse(() -> {
					String sourceKey = S3KeyGenerator.getOriginalJpg(id);
					if (!storage.exists(sourceKey)) {
						return createNotFoundResponse("Media file not found");
					}
					BufferedImage b = storage.downloadImage(sourceKey);
					if (b == null) {
						return createNotFoundResponse("Media file not found");
					}
					if (x >= 0 && y >= 0 && width > 0 && height > 0 && x + width <= b.getWidth() && y + height <= b.getHeight()) {
						b = Scalr.crop(b, x, y, width, height);
					}
					storage.uploadImage(finalObjectKey, b, webP ? StorageType.WEBP : StorageType.JPG);
					b.flush();
					if (!storage.exists(finalObjectKey)) {
						return createNotFoundResponse("Media file not found");
					}
					return createRedirect(finalObjectKey, versionStamp);
				});
			}
		}
		else {
			finalObjectKey = webP ? S3KeyGenerator.getWebWebp(id) : S3KeyGenerator.getWebJpg(id);
			if (!storage.exists(finalObjectKey)) {
				return Server.buildResponse(() -> {
					String sourceKey = S3KeyGenerator.getWebJpg(id);
					if (!storage.exists(sourceKey)) {
						sourceKey = S3KeyGenerator.getOriginalJpg(id);
					}
					if (!storage.exists(sourceKey)) {
						return createNotFoundResponse("Media file not found");
					}
					BufferedImage b = storage.downloadImage(sourceKey);
					if (b == null) {
						return createNotFoundResponse("Media file not found");
					}
					if (b.getWidth() > ImageSaver.IMAGE_WEB_WIDTH || b.getHeight() > ImageSaver.IMAGE_WEB_HEIGHT) {
						b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, ImageSaver.IMAGE_WEB_WIDTH, ImageSaver.IMAGE_WEB_HEIGHT, Scalr.OP_ANTIALIAS);
					}
					storage.uploadImage(finalObjectKey, b, webP ? StorageType.WEBP : StorageType.JPG);
					b.flush();
					if (!storage.exists(finalObjectKey)) {
						return createNotFoundResponse("Media file not found");
					}
					return createRedirect(finalObjectKey, versionStamp);
				});
			}
		}
		if (!storage.exists(finalObjectKey)) {
			return createNotFoundResponse("Media file not found");
		}
		return createRedirect(finalObjectKey, versionStamp);
	}

	@Operation(summary = "Get metadata", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Meta.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeta(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Meta res = Meta.from(dao, c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get permissions", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<PermissionUser> res = dao.getPermissions(c, setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get problem by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Problem.class))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problem")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblem(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Include hidden media (example: if a sector has multiple topo-images, the topo-images without this route will be hidden)", required = false) @QueryParam("showHiddenMedia") boolean showHiddenMedia) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			Problem res = dao.getProblem(c, authUserId, setup, id, showHiddenMedia, shouldUpdateHits);
			Response response = Response.ok().entity(res).build();
			return response;
		});
	}

	@Operation(summary = "Get problem PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Problem not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problem/pdf")
	@Produces("application/pdf")
	public Response getProblemPdf(@Context final HttpServletRequest request, @Parameter(description = "Problem id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Problem problem = dao.getProblem(c, authUserId, setup, id, false, shouldUpdateHits);
			final Area area = dao.getArea(c, setup, authUserId, problem.areaId(), shouldUpdateHits);
			final Sector sector = dao.getSector(c, authUserId, false, setup, problem.sectorId(), shouldUpdateHits);
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
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/problems/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProblemsSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<ProblemSearchResult> res = dao.getProblemsSearch(c, authUserId, setup, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Profile.class))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfile(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int reqUserId) {
		return Server.buildResponseWithSqlAndAuth(request, (dao1, c1, setup, _, _) -> {
			if (reqUserId > 0) {
				dao1.ensureUserExists(c1, reqUserId);
			}
			var identity = Server.submitDaoTask((dao, c) -> dao.getProfileIdentity(c, setup, reqUserId));
			var kpis = Server.submitDaoTask((dao, c) -> dao.getProfileKpis(c, reqUserId));
			var disciplines = Server.submitDaoTask((dao, c) -> dao.getProfileDisciplines(c, setup, reqUserId));
			Profile res = new Profile(identity.get(), kpis.get(), disciplines.get());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile ascents", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProfileAscent.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/ascents")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileAscents(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.ensureUserExists(c, id);
			List<ProfileAscent> res = dao.getProfileAscents(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile media by user id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Media.class)))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileMedia(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			dao.ensureUserExists(c, id);
			List<Media> res = dao.getProfileMedia(c, authUserId, id, captured);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile todo", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ProfileTodo.class))}),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/profile/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfileTodo(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.ensureUserExists(c, id);
			ProfileTodo res = dao.getProfileTodo(c, authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/restrictions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRestrictions(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Collection<RestrictionsRegion> res = dao.getRestrictions(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get robots.txt", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRobotsTxt(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (_, _, setup, _) -> {
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
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSectors(@Context HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final boolean orderByGrade = setup.isBouldering();
			Sector s = dao.getSector(c, authUserId, orderByGrade, setup, id, shouldUpdateHits);
			Response response = Response.ok().entity(s).build();
			return response;
		});
	}

	@Operation(summary = "Get sector PDF by id", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/pdf", array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = "404", description = "Sector not found"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request, @Parameter(description = "Sector id", required = true) @QueryParam("id") int id) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, shouldUpdateHits) -> {
			final Sector sector = dao.getSector(c, authUserId, false, setup, id, shouldUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = dao.getGradeDistribution(c, authUserId, 0, id);
			final Area area = dao.getArea(c, setup, authUserId, sector.areaId(), shouldUpdateHits);
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getSitemapTxt(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup, _) -> {
			String res = dao.getSitemapTxt(c, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Ticks.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTicks(@Context HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 0 returns first page)", required = false) @QueryParam("page") int page
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Ticks res = dao.getTicks(c, authUserId, setup, page);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Toc.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getToc(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			return Response.ok().entity(dao.getToc(c, authUserId, setup)).build();
		});
	}

	@Operation(summary = "Get table of contents (all problems) as Excel (xlsx)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/toc/xlsx")
	@Produces(MIME_TYPE_XLSX)
	public Response getTocXlsx(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
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
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("TOC", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Todo.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTodo(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			Todo res = dao.getTodo(c, authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Top.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTop(@Context HttpServletRequest request, 
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			Top res = dao.getTop(c, authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrash(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			List<Trash> res = dao.getTrash(c, authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Search for problem", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = User.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersSearch(@Context HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @QueryParam("value") String value
			) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			List<User> res = dao.getUserSearch(c, authUserId, value);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = MIME_TYPE_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, _, authUserId, _) -> {
			byte[] bytes = dao.getUserTicks(c, authUserId);
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Length", bytes.length)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("UserTicks", "xlsx")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Get Frontpage without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "text/html", schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJs(@Context HttpServletRequest request) {
		return Server.buildResponseWithSql(request, (dao, c, setup, _) -> {
			final Optional<Integer> authUserId = Optional.empty();
			var meta = Meta.from(dao, c, setup, authUserId);
			var stats = dao.getFrontpageStats(c, authUserId, setup);
			FrontpageRandomMedia frontpageRandomMedia = dao.getFrontpageRandomMedia(c, setup).stream()
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/area/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsArea(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @PathParam("id") int id) {
		return Server.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Area a = dao.getArea(c, setup, authUserId, id, shouldUpdateHits);
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/problem/{id}/{mediaId}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMedia(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId) {
		return Server.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Problem p = dao.getProblem(c, authUserId, setup, id, false, shouldUpdateHits);
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
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
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js/sector/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsSector(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @PathParam("id") int id) {
		return Server.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			final boolean orderByGrade = false;
			Sector s = dao.getSector(c, authUserId, orderByGrade, setup, id, shouldUpdateHits);
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

	@Operation(summary = "Reorder media", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PATCH
	@Path("/media/order")
	public Response patchMediaOrder(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Move left", required = false) @QueryParam("left") boolean left,
			@Parameter(description = "Move right", required = false) @QueryParam("right") boolean right
			) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(id > 0);
			Preconditions.checkArgument(left ^ right, "You must specify either 'left' or 'right', but not both.");
			dao.shiftMediaPosition(c, authUserId, id, left, right);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update area", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAreas(@Context HttpServletRequest request, Area a) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Objects.requireNonNull(Strings.emptyToNull(a.name()));
			Redirect res = dao.setArea(c, setup, authUserId, a);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/comments")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postComments(@Context HttpServletRequest request, Comment co) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			int idGuestbook = dao.upsertComment(c, authUserId, setup, co);
			return Response.ok(idGuestbook).build();
		});
	}

	@Operation(summary = "Add single image media item", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/image")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaImage(@Context HttpServletRequest request, FormDataMultiPart multiPart) {
		FormDataBodyPart jsonPart = multiPart.getField("json");
		Preconditions.checkArgument(jsonPart != null);
		Media m = new Gson().fromJson(jsonPart.getValue(), Media.class);
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(m != null);
			FormDataBodyPart filePart = multiPart.getField("file");
			Preconditions.checkNotNull(filePart, "File part is required");
			String fileName = filePart.getContentDisposition().getFileName();
			StorageType storageType = StorageType.fromFilename(fileName)
					.orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + fileName));
			int newMediaId = dao.addMediaImage(c, authUserId, m, storageType, () -> filePart.getValueAs(InputStream.class));
			Media res = dao.getMedia(c, authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Commit verified Instagram media to application storage", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/instagram-save")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaInstagramSave(
			@Context HttpServletRequest request, 
			@HeaderParam("X-Selected-Cdn-Url") String selectedCdnUrl,
			@HeaderParam("X-Selected-Is-Video") boolean isVideo,
			@HeaderParam("X-Selected-Media-Index") int mediaIndex,
			Media mediaPayload) {
		Preconditions.checkArgument(mediaPayload != null, "Media payload is missing");
		Preconditions.checkArgument(selectedCdnUrl != null && !selectedCdnUrl.isBlank(), "Selected slide CDN URL is required");
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.ensureSuperadminWriteRegion(c, setup, authUserId);
			mediaPayload.ensureCorrectMediaAssociations(authUserId);
			if (isVideo) {
				int newMediaId = dao.addMediaVideoPlaceholder(c, authUserId, mediaPayload, StorageType.MP4);
				Server.runAsync(() -> {
					try {
						byte[] videoData;
						try (InputStream is = URI.create(selectedCdnUrl).toURL().openStream()) {
							videoData = StorageManager.getInstance().readBoundedStream(is);
						} catch (IOException e) {
							logger.warn("Initial instagram video link expired, attempting fallback re-scrape for id=" + newMediaId, e);
							List<ApifyInstagramResolver.InstagramMedia> freshMedia = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
							ApifyInstagramResolver.InstagramMedia target = freshMedia.stream()
									.filter(m -> m.mediaIndex() == mediaIndex)
									.findFirst()
									.orElse(freshMedia.get(0));
							try (InputStream is = URI.create(target.cdnUrl()).toURL().openStream()) {
								videoData = StorageManager.getInstance().readBoundedStream(is);
							}
						}
						StorageManager.getInstance().uploadBytes(S3KeyGenerator.getOriginalMp4(newMediaId), videoData, StorageType.MP4);
						Server.runSql((backgroundDao, backgroundConn) -> {
							VideoHelper.processVideo(backgroundConn, backgroundDao, newMediaId, mediaPayload.thumbnailSeconds());
						});
					} catch (Exception e) {
						logger.error("Failed async instagram video save for id=" + newMediaId, e);
					}
				});
				Media res = dao.getMedia(c, authUserId, newMediaId);
				return Response.ok().entity(res).build();
			}
			byte[] imageData;
			try (InputStream is = URI.create(selectedCdnUrl).toURL().openStream()) {
				imageData = StorageManager.getInstance().readBoundedStream(is);
			} catch (IOException e) {
				logger.warn("Initial instagram image link expired, attempting fallback re-scrape", e);
				List<ApifyInstagramResolver.InstagramMedia> freshMedia = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
				ApifyInstagramResolver.InstagramMedia target = freshMedia.stream()
						.filter(m -> m.mediaIndex() == mediaIndex)
						.findFirst()
						.orElse(freshMedia.get(0));
				try (InputStream is = URI.create(target.cdnUrl()).toURL().openStream()) {
					imageData = StorageManager.getInstance().readBoundedStream(is);
				}
			}
			final byte[] finalImageData = imageData;
			int newMediaId = dao.addMediaImage(c, authUserId, mediaPayload, StorageType.JPG, () -> new ByteArrayInputStream(finalImageData));
			Media res = dao.getMedia(c, authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}
	
	@Operation(summary = "Scrape Instagram URL metadata for frontend preview box", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ApifyInstagramResolver.InstagramMedia.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/instagram-scrape")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaInstagramScrape(@Context HttpServletRequest request, @QueryParam("url") String url) {
		Preconditions.checkArgument(url != null && !url.isBlank(), "Instagram URL is required");
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.ensureSuperadminWriteRegion(c, setup, authUserId);
			List<ApifyInstagramResolver.InstagramMedia> scrapedList = ApifyInstagramResolver.resolveMedia(url);
			return Response.ok().entity(scrapedList).build();
		});
	}

	@Operation(summary = "Update Media SVG", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/svg")
	public Response postMediaSvg(@Context HttpServletRequest request, Media m) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.upsertMediaSvg(c, authUserId, setup, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Signal direct video upload completion and trigger async background processing", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/video/{id}/complete")
	public Response postMediaVideoComplete(@Context HttpServletRequest request, @PathParam("id") int mediaId) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
			Media media = dao.getMedia(c, authUserId, mediaId);
			Preconditions.checkArgument(media.isMovie(), "Target media is an image, not a video.");
			Preconditions.checkArgument(media.uploadedByMe(), "You do not have permission to modify this media item.");
			Server.runAsync(() -> {
				try {
					Server.runSql((backgroundDao, backgroundConn) -> {
						VideoHelper.processVideo(backgroundConn, backgroundDao, mediaId, media.thumbnailSeconds());
					});
				} catch (Exception e) {
					logger.error("Failed async video processing for id=" + mediaId, e);
				}
			});
			return Response.ok().build();
		});
	}

	@Operation(summary = "Add embedded external video (YouTube/Vimeo)", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/video/embed")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaVideoEmbed(@Context HttpServletRequest request, Media media) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(media != null, "Media payload is missing");
			Preconditions.checkArgument(media.embedUrl() != null && !media.embedUrl().isBlank(), "External video URL is required");
			String lowerUrl = media.embedUrl().toLowerCase();
			boolean isValidProvider = lowerUrl.contains("youtube.com") 
					|| lowerUrl.contains("youtu.be") 
					|| lowerUrl.contains("vimeo.com");
			Preconditions.checkArgument(isValidProvider, "Unsupported video provider. Only YouTube and Vimeo links are allowed.");
			try {
				URI.create(media.embedUrl()).toURL();
			} catch (Exception e) {
				throw new IllegalArgumentException("The provided embed URL is malformed.", e);
			}
			int newMediaId = dao.addMediaVideoEmbed(c, authUserId, media, StorageType.MP4);
			Server.runAsync(() -> {
				try {
					Server.runSql((backgroundDao, backgroundConn) -> {
						ImageHelper.saveImageFromEmbedVideo(backgroundDao, backgroundConn, newMediaId, media.embedUrl());
					});
				} catch (Exception e) {
					logger.error("Failed async embed thumbnail processing for id=" + newMediaId, e);
				}
			});
			Media res = dao.getMedia(c, authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Initiate video upload to get a presigned storage URL", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = VideoInitResponse.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/media/video/initiate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaVideoInitiate(@Context HttpServletRequest request, VideoInitPayload payload) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(payload != null && payload.media() != null);
			Preconditions.checkArgument(payload.fileSize() <= StorageManager.MAX_VIDEO_UPLOAD_BYTES, "Video exceeds maximum allowed size (max " + StorageManager.MAX_VIDEO_UPLOAD_BYTES + " bytes)");
			StorageType storageType = StorageType.fromMimeType(payload.contentType())
					.orElseThrow(() -> new IllegalArgumentException("Unsupported video content type: " + payload.contentType()));
			Preconditions.checkArgument(storageType.isMovie(), "Provided format is not a video type.");
			int newMediaId = dao.addMediaVideoPlaceholder(c, authUserId, payload.media(), storageType);
			String presignedUrl = StorageManager.getInstance().generatePresignedPutUrl(
					S3KeyGenerator.getOriginalMp4(newMediaId), 
					storageType.getMimeType(),
					payload.fileSize()
					);
			return Response.ok().entity(new VideoInitResponse(newMediaId, presignedUrl)).build();
		});
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) {
		Preconditions.checkArgument(u.userId() > 0, "Invalid userId");
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.upsertPermissionUser(c, setup, authUserId, u);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update problem", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postProblems(@Context HttpServletRequest request, Problem p) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.sectorId() > 1);
			Objects.requireNonNull(Strings.emptyToNull(p.name()));
			Redirect res = dao.setProblem(c, authUserId, setup, p);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update topo line on route/boulder (SVG on sector/problem-image)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("problemId") int problemId,
			@Parameter(description = "Problem section id", required = true) @QueryParam("pitch") int pitch,
			@Parameter(description = "Media id", required = true) @QueryParam("mediaId") int mediaId,
			Svg svg
			) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Objects.requireNonNull(svg, "Invalid svg=" + svg);
			dao.upsertSvg(c, authUserId, problemId, pitch, mediaId, svg);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update profile identity", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/profile/identity")
	public Response postProfileIdentity(@Context HttpServletRequest request, ProfileIdentity profile) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.setProfile(c, authUserId, profile);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update theme preference (light/dark)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/profile/theme")
	public Response postProfileTheme(@Context HttpServletRequest request,
			@Parameter(description = "Theme preference (light or dark)", required = true) @QueryParam("themePreference") String themePreference) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.setThemePreference(c, authUserId, themePreference);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) {
		return Server.buildResponseWithSqlAndAuth(request, (dao, c, setup, authUserId, _) -> {
			String search = Strings.emptyToNull(Strings.nullToEmpty(sr.value()).trim());
			Objects.requireNonNull(search, "Invalid search: " + search);
			List<Search> res = dao.getSearch(c, authUserId, setup, search);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update sector", responses = {
			@ApiResponse(responseCode = "200", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSectors(@Context HttpServletRequest request, Sector s) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Preconditions.checkArgument(s.areaId() > 1);
			Objects.requireNonNull(Strings.emptyToNull(s.name()));
			Redirect res = dao.setSector(c, authUserId, setup, s);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			Preconditions.checkArgument(t.idProblem() > 0);
			dao.setTick(c, authUserId, setup, t);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/todo")
	public Response postTodo(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("idProblem") int idProblem
			) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.toggleTodo(c, authUserId, idProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/trails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTrails(@Context HttpServletRequest request, List<Trail> trails) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, connection, _, authUserId, _) -> {
			dao.upsertTrails(connection, authUserId, trails);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update visible regions", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/user/regions")
	public Response postUserRegions(@Context HttpServletRequest request,
			@Parameter(description = "Region id", required = true) @QueryParam("regionId") int regionId,
			@Parameter(description = "Delete (TRUE=hide, FALSE=show)", required = true) @QueryParam("delete") boolean delete
			) {
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.setUserRegion(c, authUserId, regionId, delete);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media")
	public Response putMedia(@Context HttpServletRequest request, Media m) {
		Preconditions.checkArgument(m.identity().id() > 0, "Invalid mediaId");
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.updateMedia(c, authUserId, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media rotation (allowed for administrators + user who uploaded specific image)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/media/jpeg/rotate")
	public Response putMediaJpegRotate(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia,
			@Parameter(description = "Degrees (90/180/270)", required = true) @QueryParam("degrees") int degrees
			) {
		Preconditions.checkArgument(idMedia > 0, "Invalid idMedia");
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, _, authUserId, _) -> {
			dao.rotateMedia(c, authUserId, idMedia, degrees);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = "200"),
			@ApiResponse(responseCode = OpenApiResponseRefs.BAD_REQUEST_CODE, description = OpenApiResponseRefs.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.UNAUTHORIZED_CODE, description = OpenApiResponseRefs.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.FORBIDDEN_CODE, description = OpenApiResponseRefs.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_CODE, description = OpenApiResponseRefs.INTERNAL_SERVER_ERROR_DESCRIPTION)
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
		return Server.buildResponseWithSqlAndRequiredAuth(request, (dao, c, setup, authUserId, _) -> {
			dao.trashRecover(c, setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}

	private Response createNotFoundResponse(String message) {
		return Response.status(Response.Status.NOT_FOUND).entity(message).build();
	}

	private Response createRedirect(String key, int version) {
		String localProxyPath = StorageManager.getPublicUrl(key, version);
		CacheControl cc = new CacheControl();
		cc.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));
		cc.setNoTransform(true);
		return Response.status(Response.Status.FOUND)
				.header("Location", localProxyPath)
				.cacheControl(cc).build();
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
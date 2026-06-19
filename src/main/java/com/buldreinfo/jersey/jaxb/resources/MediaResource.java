package com.buldreinfo.jersey.jaxb.resources;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.dao.MediaRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.helpers.ApifyInstagramResolver;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.ImageSaver;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.io.VideoHelper;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.VideoInitPayload;
import com.buldreinfo.jersey.jaxb.model.VideoInitResponse;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Media")
@Path("/media")
public class MediaResource extends BaseResource {
	@FunctionalInterface
	private interface ImageTask {
		void execute(StorageManager storage) throws Exception;
	}
	private static Logger logger = LogManager.getLogger();
	private final MediaRepository mediaRepo;

	private final TransactionManager txManager;

	@Inject
	public MediaResource(TransactionManager txManager, MediaRepository mediaRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.txManager = txManager;
		this.mediaRepo = mediaRepo;
	}

	@Operation(summary = "Move media to trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@DELETE
	public Response deleteMedia(@Context HttpServletRequest request, @Parameter(description = "Media id", required = true) @QueryParam("id") int id) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			mediaRepo.deleteMedia(authUserId, id);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Get Media by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMedia(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia) throws Exception {
		if (idMedia <= 0) {
			return createBadRequestResponse("Invalid idMedia=" + idMedia);
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			Media res = mediaRepo.getMedia(authUserId, idMedia);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get media file by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.FOUND_CODE, description = OpenApiConstants.FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/file")
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
			@Parameter(description = "Region Height", required = false) @QueryParam("height") int height) throws Exception {
		StorageManager storage = StorageManager.getInstance();
		if (isMovie) {
			String finalObjectKey = GlobalFunctions.requestAcceptsWebm(request) ? S3KeyGenerator.getWebWebm(id) : S3KeyGenerator.getWebMp4(id);
			if (!storage.exists(finalObjectKey)) {
				return createNotFoundResponse();
			}
			return createRedirect(finalObjectKey, versionStamp);
		}

		boolean webP = GlobalFunctions.requestAcceptsWebp(request);
		StorageType outputType = webP ? StorageType.WEBP : StorageType.JPG;
		String finalObjectKey;

		if (original) {
			finalObjectKey = S3KeyGenerator.getOriginalJpg(id);
			if (!storage.exists(finalObjectKey)) {
				return createNotFoundResponse();
			}
			return createRedirect(finalObjectKey, versionStamp);
		} 

		if (targetWidth > 0 || minDimension > 0) {
			finalObjectKey = webP ? S3KeyGenerator.getWebWebpResized(id, targetWidth, minDimension) : S3KeyGenerator.getWebJpgResized(id, targetWidth, minDimension);
			if (storage.exists(finalObjectKey)) {
				return createRedirect(finalObjectKey, versionStamp);
			}
			return executeGenerationPipeline(storage, finalObjectKey, versionStamp, s -> processResize(s, id, targetWidth, minDimension, finalObjectKey, outputType));
		} 

		if (width > 0 && height > 0) {
			finalObjectKey = webP ? S3KeyGenerator.getWebWebpRegion(id, x, y, width, height) : S3KeyGenerator.getWebJpgRegion(id, x, y, width, height);
			if (storage.exists(finalObjectKey)) {
				return createRedirect(finalObjectKey, versionStamp);
			}
			return executeGenerationPipeline(storage, finalObjectKey, versionStamp, s -> processCrop(s, id, x, y, width, height, finalObjectKey, outputType));
		}

		finalObjectKey = webP ? S3KeyGenerator.getWebWebp(id) : S3KeyGenerator.getWebJpg(id);
		if (storage.exists(finalObjectKey)) {
			return createRedirect(finalObjectKey, versionStamp);
		}
		return executeGenerationPipeline(storage, finalObjectKey, versionStamp, s -> processStandard(s, id, finalObjectKey, outputType));
	}

	@Operation(summary = "Reorder media", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PATCH
	@Path("/order")
	public Response patchMediaOrder(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("id") int id,
			@Parameter(description = "Move left", required = false) @QueryParam("left") boolean left,
			@Parameter(description = "Move right", required = false) @QueryParam("right") boolean right
			) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		if (!(left ^ right)) {
			return createBadRequestResponse("You must specify either 'left' or 'right', but not both.");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			mediaRepo.shiftMediaPosition(authUserId, id, left, right);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Add single image media item", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/image")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaImage(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws Exception {
		FormDataBodyPart jsonPart = multiPart.getField("json");
		Preconditions.checkArgument(jsonPart != null);
		Media m = new Gson().fromJson(jsonPart.getValue(), Media.class);
		Preconditions.checkArgument(m != null, "Media payload is required");
		FormDataBodyPart filePart = multiPart.getField("file");
		Preconditions.checkNotNull(filePart, "File part is required");
		String fileName = filePart.getContentDisposition().getFileName();
		StorageType storageType = StorageType.fromFilename(fileName)
				.orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + fileName));
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			int newMediaId = mediaRepo.addMediaImage(authUserId, m, storageType, () -> filePart.getValueAs(InputStream.class));
			Media res = mediaRepo.getMedia(authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Commit verified Instagram media to application storage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/instagram-save")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaInstagramSave(
			@Context HttpServletRequest request, 
			@HeaderParam("X-Selected-Cdn-Url") String selectedCdnUrl,
			@HeaderParam("X-Selected-Is-Video") boolean isVideo,
			@HeaderParam("X-Selected-Media-Index") int mediaIndex,
			Media mediaPayload) throws Exception {
		Preconditions.checkArgument(mediaPayload != null, "Media payload is missing");
		Preconditions.checkArgument(selectedCdnUrl != null && !selectedCdnUrl.isBlank(), "Selected slide CDN URL is required");
		URI validatedInitialUri = ApifyInstagramResolver.validateInstagramCdnUrl(selectedCdnUrl);
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50) {
				throw new WebApplicationException(
						Response.status(Response.Status.TOO_MANY_REQUESTS)
						.entity("Daily Instagram import limit reached (max 50 per day)")
						.build()
						);
			}
			mediaPayload.ensureCorrectMediaAssociations(authUserId);
			if (isVideo) {
				int newMediaId = mediaRepo.addMediaVideoPlaceholder(authUserId, mediaPayload, StorageType.MP4);
				supplyAsync(() -> {
					try {
						byte[] videoData;
						try (InputStream is = validatedInitialUri.toURL().openStream()) {
							videoData = StorageManager.getInstance().readBoundedStream(is);
						} catch (IOException e) {
							logger.warn("Initial instagram video link expired, attempting fallback re-scrape for id=" + newMediaId, e);
							List<ApifyInstagramResolver.InstagramMedia> freshMedia = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
							ApifyInstagramResolver.InstagramMedia target = freshMedia.stream()
									.filter(m -> m.mediaIndex() == mediaIndex)
									.findFirst()
									.orElse(freshMedia.get(0));
							URI validatedFallbackUri = ApifyInstagramResolver.validateInstagramCdnUrl(target.cdnUrl());
							txManager.executeInTransaction(() -> {
								mediaRepo.logInstagramScrape(authUserId, mediaPayload.embedUrl(), freshMedia.size());
								return null;
							});
							try (InputStream is = validatedFallbackUri.toURL().openStream()) {
								videoData = StorageManager.getInstance().readBoundedStream(is);
							}
						}
						StorageManager.getInstance().uploadBytes(S3KeyGenerator.getOriginalMp4(newMediaId), videoData, StorageType.MP4);
						VideoHelper.processVideo(txManager, mediaRepo, newMediaId, mediaPayload.thumbnailSeconds());
						return null;
					} catch (Exception e) {
						throw new RuntimeException("Async instagram video save failed for id=" + newMediaId, e);
					}
				}).exceptionally(ex -> {
					logger.error("Failed async instagram video save for id=" + newMediaId, ex);
					return null;
				});
				Media res = mediaRepo.getMedia(authUserId, newMediaId);
				return Response.ok().entity(res).build();
			}
			byte[] imageData;
			try (InputStream is = validatedInitialUri.toURL().openStream()) {
				imageData = StorageManager.getInstance().readBoundedStream(is);
			} catch (IOException e) {
				logger.warn("Initial instagram image link expired, attempting fallback re-scrape", e);
				List<ApifyInstagramResolver.InstagramMedia> freshMedia = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
				ApifyInstagramResolver.InstagramMedia target = freshMedia.stream()
						.filter(m -> m.mediaIndex() == mediaIndex)
						.findFirst()
						.orElse(freshMedia.get(0));
				URI validatedFallbackUri = ApifyInstagramResolver.validateInstagramCdnUrl(target.cdnUrl());
				mediaRepo.logInstagramScrape(authUserId, mediaPayload.embedUrl(), freshMedia.size());
				try (InputStream is = validatedFallbackUri.toURL().openStream()) {
					imageData = StorageManager.getInstance().readBoundedStream(is);
				}
			}
			final byte[] finalImageData = imageData;
			int newMediaId = mediaRepo.addMediaImage(authUserId, mediaPayload, StorageType.JPG, () -> new ByteArrayInputStream(finalImageData));
			Media res = mediaRepo.getMedia(authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Scrape Instagram URL metadata for frontend preview box", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ApifyInstagramResolver.InstagramMedia.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/instagram-scrape")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaInstagramScrape(@Context HttpServletRequest request, @QueryParam("url") String url) throws Exception {
		if (url == null || url.isBlank()) {
			return createBadRequestResponse("Instagram URL is required");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50) {
				throw new WebApplicationException(
						Response.status(Response.Status.TOO_MANY_REQUESTS)
						.entity("Daily Instagram import limit reached (max 50 per day)")
						.build()
						);
			}
			List<ApifyInstagramResolver.InstagramMedia> scrapedList = ApifyInstagramResolver.resolveMedia(url);
			mediaRepo.logInstagramScrape(authUserId, url, scrapedList.size());
			return Response.ok().entity(scrapedList).build();
		});
	}

	@Operation(summary = "Update Media SVG", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/svg")
	public Response postMediaSvg(@Context HttpServletRequest request, Media m) throws Exception {
		if (m == null || m.identity() == null || m.identity().id() <= 0) {
			return createBadRequestResponse("Invalid media payload");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			mediaRepo.upsertMediaSvg(setup, authUserId, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Signal direct video upload completion and trigger async background processing", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/video/{id}/complete")
	public Response postMediaVideoComplete(@Context HttpServletRequest request, @PathParam("id") int mediaId) throws Exception {
		if (mediaId <= 0) {
			return createBadRequestResponse("Invalid mediaId=" + mediaId);
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			Media media = mediaRepo.getMedia(authUserId, mediaId);
			Preconditions.checkArgument(media.isMovie(), "Target media is an image, not a video.");
			Preconditions.checkArgument(media.uploadedByMe(), "You do not have permission to modify this media item.");
			supplyAsync(() -> {
				VideoHelper.processVideo(txManager, mediaRepo, mediaId, media.thumbnailSeconds());
				return null;
			}).exceptionally(ex -> {
				logger.error("Failed async video processing for id=" + mediaId, ex);
				return null;
			});
			return Response.ok().build();
		});
	}

	@Operation(summary = "Add embedded external video (YouTube/Vimeo)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/video/embed")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaVideoEmbed(@Context HttpServletRequest request, Media media) throws Exception {
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
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			int newMediaId = mediaRepo.addMediaVideoEmbed(authUserId, media, StorageType.MP4);
			supplyAsync(() -> {
				ImageHelper.saveImageFromEmbedVideo(txManager, mediaRepo, newMediaId, media.embedUrl());
				return null;
			}).exceptionally(ex -> {
				logger.error("Failed async embed thumbnail processing for id=" + newMediaId, ex);
				return null;
			});
			Media res = mediaRepo.getMedia(authUserId, newMediaId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Initiate video upload to get a presigned storage URL", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VideoInitResponse.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/video/initiate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMediaVideoInitiate(@Context HttpServletRequest request, VideoInitPayload payload) throws Exception {
		Preconditions.checkArgument(payload != null && payload.media() != null, "Video payload or media is missing");
		Preconditions.checkArgument(payload.fileSize() <= StorageManager.MAX_VIDEO_UPLOAD_BYTES, "Video exceeds maximum allowed size (max " + StorageManager.MAX_VIDEO_UPLOAD_BYTES + " bytes)");
		StorageType storageType = StorageType.fromMimeType(payload.contentType())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported video content type: " + payload.contentType()));
		Preconditions.checkArgument(storageType.isMovie(), "Provided format is not a video type.");
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			int newMediaId = mediaRepo.addMediaVideoPlaceholder(authUserId, payload.media(), storageType);
			String presignedUrl = StorageManager.getInstance().generatePresignedPutUrl(
					S3KeyGenerator.getOriginalMp4(newMediaId), 
					storageType.getMimeType(),
					payload.fileSize()
					);
			return Response.ok().entity(new VideoInitResponse(newMediaId, presignedUrl)).build();
		});
	}

	@Operation(summary = "Update media", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("")
	public Response putMedia(@Context HttpServletRequest request, Media m) throws Exception {
		if (m == null || m.identity() == null || m.identity().id() <= 0) {
			return createBadRequestResponse("Invalid mediaId");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			mediaRepo.updateMedia(authUserId, m);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update media rotation (allowed for administrators + user who uploaded specific image)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/jpeg/rotate")
	public Response putMediaJpegRotate(@Context HttpServletRequest request,
			@Parameter(description = "Media id", required = true) @QueryParam("idMedia") int idMedia,
			@Parameter(description = "Degrees (90/180/270)", required = true) @QueryParam("degrees") int degrees
			) throws Exception {
		if (idMedia <= 0) {
			return createBadRequestResponse("Invalid idMedia");
		}
		if (degrees != 90 && degrees != 180 && degrees != 270) {
			return createBadRequestResponse("Invalid rotation degrees. Must be 90, 180, or 270.");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			mediaRepo.rotateMedia(authUserId, idMedia, degrees);
			return Response.ok().build();
		});
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

	private Response executeGenerationPipeline(StorageManager storage, String key, int version, ImageTask task) throws Exception {
		task.execute(storage);
		if (!storage.exists(key)) {
			return createNotFoundResponse();
		}
		return createRedirect(key, version);
	}

	private void processCrop(StorageManager storage, int id, int x, int y, int width, int height, String key, StorageType type) throws IOException {
		String sourceKey = S3KeyGenerator.getOriginalJpg(id);
		if (!storage.exists(sourceKey)) {
			return;
		}
		BufferedImage b = storage.downloadImage(sourceKey);
		if (b == null) {
			return;
		}
		try {
			if (x >= 0 && y >= 0 && width > 0 && height > 0 && x + width <= b.getWidth() && y + height <= b.getHeight()) {
				b = Scalr.crop(b, x, y, width, height);
			}
			storage.uploadImage(key, b, type);
		} finally {
			b.flush();
		}
	}

	private void processResize(StorageManager storage, int id, int targetWidth, int minDimension, String key, StorageType type) throws IOException {
		boolean useWebSource = (targetWidth <= 0 || targetWidth <= ImageSaver.IMAGE_WEB_WIDTH) && (minDimension <= 0 || minDimension <= ImageSaver.IMAGE_WEB_WIDTH);
		String sourceKey = useWebSource ? S3KeyGenerator.getWebJpg(id) : S3KeyGenerator.getOriginalJpg(id);
		if (useWebSource && !storage.exists(sourceKey)) {
			sourceKey = S3KeyGenerator.getOriginalJpg(id);
		}
		if (!storage.exists(sourceKey)) {
			return;
		}
		BufferedImage b = storage.downloadImage(sourceKey);
		if (b == null) {
			return;
		}
		try {
			if (targetWidth > 0 && targetWidth < b.getWidth()) {
				b = Scalr.resize(b, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth);
			} else if (minDimension > 0) {
				Scalr.Mode mode = b.getWidth() < b.getHeight() ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
				b = Scalr.resize(b, Scalr.Method.QUALITY, mode, minDimension);
			}
			storage.uploadImage(key, b, type);
		} finally {
			b.flush();
		}
	}

	private void processStandard(StorageManager storage, int id, String key, StorageType type) throws IOException {
		String sourceKey = S3KeyGenerator.getWebJpg(id);
		if (!storage.exists(sourceKey)) {
			sourceKey = S3KeyGenerator.getOriginalJpg(id);
		}
		if (!storage.exists(sourceKey)) {
			return;
		}
		BufferedImage b = storage.downloadImage(sourceKey);
		if (b == null) {
			return;
		}
		try {
			if (b.getWidth() > ImageSaver.IMAGE_WEB_WIDTH || b.getHeight() > ImageSaver.IMAGE_WEB_HEIGHT) {
				b = Scalr.resize(b, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, ImageSaver.IMAGE_WEB_WIDTH, ImageSaver.IMAGE_WEB_HEIGHT, Scalr.OP_ANTIALIAS);
			}
			storage.uploadImage(key, b, type);
		} finally {
			b.flush();
		}
	}
}
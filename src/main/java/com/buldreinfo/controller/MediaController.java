package com.buldreinfo.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.helpers.ApifyInstagramResolver;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.io.ImageHelper;
import com.buldreinfo.io.ImageSaver;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.io.VideoHelper;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.VideoInitPayload;
import com.buldreinfo.model.VideoInitResponse;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Media")
@RestController
@RequestMapping("/media")
public class MediaController extends BaseController {
	@FunctionalInterface
	private interface ImageTask {
		void execute(StorageManager storage) throws Exception;
	}

	private static final Logger logger = LogManager.getLogger();
	private final StorageManager storage;
	private final MediaRepository mediaRepo;
	private final RegionRepository regionRepo;
	private final ClimbingTransactionManager txManager;

	public MediaController(StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo,  RegionRepository regionRepo) {
		super(txManager, regionRepo);
		this.storage = storage;
		this.txManager = txManager;
		this.mediaRepo = mediaRepo;
		this.regionRepo = regionRepo;
	}

	@Operation(summary = "Move media to trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@DeleteMapping
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> deleteMedia(HttpServletRequest request, @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		executeContextualTask(request, ctx -> {
			mediaRepo.deleteMedia(ctx.authUserId(), id);
			return null;
		});
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get Media by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Media> getMedia(HttpServletRequest request, @RequestParam(name = "idMedia") int idMedia) throws Exception {
		if (idMedia <= 0) throw new ValidationFailedException("Invalid idMedia=" + idMedia);
		return ResponseEntity.ok(executeContextualTask(request, ctx -> mediaRepo.getMedia(ctx.authUserId(), idMedia)));
	}

	@Operation(summary = "Get media file by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.FOUND_CODE, description = OpenApiConstants.FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GetMapping("/file")
	public ResponseEntity<Void> getMediaFile(HttpServletRequest request,
			@RequestParam(name = "id") int id,
			@RequestParam(name = "isMovie") boolean isMovie,
			@RequestParam(name = "versionStamp", defaultValue = "0") int versionStamp,
			@RequestParam(name = "original", defaultValue = "false") boolean original,
			@RequestParam(name = "targetWidth", defaultValue = "0") int targetWidth,
			@RequestParam(name = "minDimension", defaultValue = "0") int minDimension,
			@RequestParam(name = "x", defaultValue = "0") int x,
			@RequestParam(name = "y", defaultValue = "0") int y,
			@RequestParam(name = "width", defaultValue = "0") int width,
			@RequestParam(name = "height", defaultValue = "0") int height) throws Exception {

		if (isMovie) {
			String key = GlobalFunctions.requestAcceptsWebm(request) ? S3KeyGenerator.getWebWebm(id) : S3KeyGenerator.getWebMp4(id);
			if (!storage.exists(key)) {
		        throw new NoSuchElementException("Movie resource not found: " + key);
		    }
		    return createRedirect(key, versionStamp);
		}

		boolean webP = GlobalFunctions.requestAcceptsWebp(request);
		StorageType outputType = webP ? StorageType.WEBP : StorageType.JPG;
		String key;

		if (original) {
		    key = S3KeyGenerator.getOriginalJpg(id);
		    if (!storage.exists(key)) {
		        throw new NoSuchElementException("Original JPG not found for id: " + id);
		    }
		    return createRedirect(key, versionStamp);
		}

		if (targetWidth > 0 || minDimension > 0) {
			key = webP ? S3KeyGenerator.getWebWebpResized(id, targetWidth, minDimension) : S3KeyGenerator.getWebJpgResized(id, targetWidth, minDimension);
			if (storage.exists(key)) return createRedirect(key, versionStamp);
			return executeGenerationPipeline(storage, key, versionStamp, s -> processResize(s, id, targetWidth, minDimension, key, outputType));
		}

		if (width > 0 && height > 0) {
			key = webP ? S3KeyGenerator.getWebWebpRegion(id, x, y, width, height) : S3KeyGenerator.getWebJpgRegion(id, x, y, width, height);
			if (storage.exists(key)) return createRedirect(key, versionStamp);
			return executeGenerationPipeline(storage, key, versionStamp, s -> processCrop(s, id, x, y, width, height, key, outputType));
		}

		key = webP ? S3KeyGenerator.getWebWebp(id) : S3KeyGenerator.getWebJpg(id);
		if (storage.exists(key)) return createRedirect(key, versionStamp);
		return executeGenerationPipeline(storage, key, versionStamp, s -> processStandard(s, id, key, outputType));
	}

	@Operation(summary = "Reorder media", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PatchMapping("/order")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> patchMediaOrder(HttpServletRequest request,
			@RequestParam(name = "id") int id,
			@RequestParam(name = "left", defaultValue = "false") boolean left,
			@RequestParam(name = "right", defaultValue = "false") boolean right) throws Exception {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		if (!(left ^ right)) throw new ValidationFailedException("Specify either 'left' or 'right', not both.");
		executeContextualTask(request, ctx -> {
			mediaRepo.shiftMediaPosition(ctx.authUserId(), id, left, right);
			return null;
		});
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Add single image media item", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Media> postMediaImage(HttpServletRequest request, 
			@RequestPart("json") String json, 
			@RequestPart("file") MultipartFile file) throws Exception {
		Media m = new Gson().fromJson(json, Media.class);
		Preconditions.checkArgument(m != null, "Media payload is required");
		String originalFilename = file.getOriginalFilename();
		StorageType storageType = StorageType.fromFilename(originalFilename)
				.orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + originalFilename));
		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			int newMediaId = mediaRepo.addMediaImage(ctx.authUserId(), m, storageType, () -> {
				try {
					return file.getInputStream();
				} catch (IOException e) {
					throw new RuntimeException("Failed to get InputStream from multipart file", e);
				}
			});
			return mediaRepo.getMedia(ctx.authUserId(), newMediaId);
		}));
	}

	@Operation(summary = "Commit verified Instagram media to application storage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/instagram-save")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Media> postMediaInstagramSave(HttpServletRequest request,
			@RequestHeader("X-Selected-Cdn-Url") String selectedCdnUrl,
			@RequestHeader("X-Selected-Is-Video") boolean isVideo,
			@RequestHeader("X-Selected-Media-Index") int mediaIndex,
			@RequestBody Media mediaPayload) throws Exception {
		Preconditions.checkArgument(mediaPayload != null, "Media payload missing");
		URI validatedUri = ApifyInstagramResolver.validateInstagramCdnUrl(selectedCdnUrl);
		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			if (mediaRepo.getDailyInstagramScrapeCount(ctx.authUserId()) > 50)
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily limit reached");

			mediaPayload.ensureCorrectMediaAssociations(ctx.authUserId());
			if (isVideo) {
				int id = mediaRepo.addMediaVideoPlaceholder(ctx.authUserId(), mediaPayload, StorageType.MP4);
				supplyAsync(() -> {
					try {
						byte[] videoData;
						try (InputStream is = validatedUri.toURL().openStream()) {
							videoData = storage.readBoundedStream(is);
						} catch (IOException e) {
							logger.warn("Initial instagram video link expired, attempting fallback re-scrape for id=" + id, e);
							List<ApifyInstagramResolver.InstagramMedia> fresh = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
							ApifyInstagramResolver.InstagramMedia target = fresh.stream().filter(m -> m.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
							txManager.executeInTransaction(() -> { mediaRepo.logInstagramScrape(ctx.authUserId(), mediaPayload.embedUrl(), fresh.size()); return null; });
							try (InputStream is = ApifyInstagramResolver.validateInstagramCdnUrl(target.cdnUrl()).toURL().openStream()) {
								videoData = storage.readBoundedStream(is);
							}
						}
						storage.uploadBytes(S3KeyGenerator.getOriginalMp4(id), videoData, StorageType.MP4);
						VideoHelper.processVideo(storage, txManager, mediaRepo, id, mediaPayload.thumbnailSeconds());
					} catch (Exception e) { throw new RuntimeException(e); }
					return null;
				}).exceptionally(ex -> { logger.error("Async video save failed", ex); return null; });
				return mediaRepo.getMedia(ctx.authUserId(), id);
			}

			byte[] imageData;
			try (InputStream is = validatedUri.toURL().openStream()) {
				imageData = storage.readBoundedStream(is);
			} catch (IOException e) {
				logger.warn("Initial instagram image link expired, attempting fallback re-scrape", e);
				List<ApifyInstagramResolver.InstagramMedia> fresh = ApifyInstagramResolver.resolveMedia(mediaPayload.embedUrl());
				ApifyInstagramResolver.InstagramMedia target = fresh.stream().filter(m -> m.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
				mediaRepo.logInstagramScrape(ctx.authUserId(), mediaPayload.embedUrl(), fresh.size());
				try (InputStream is = ApifyInstagramResolver.validateInstagramCdnUrl(target.cdnUrl()).toURL().openStream()) {
					imageData = storage.readBoundedStream(is);
				}
			}
			final byte[] finalData = imageData;
			int newId = mediaRepo.addMediaImage(ctx.authUserId(), mediaPayload, StorageType.JPG, () -> new ByteArrayInputStream(finalData));
			return mediaRepo.getMedia(ctx.authUserId(), newId);
		}));
	}

	@Operation(summary = "Scrape Instagram URL metadata for frontend preview box", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ApifyInstagramResolver.InstagramMedia.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/instagram-scrape")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<List<ApifyInstagramResolver.InstagramMedia>> postMediaInstagramScrape(HttpServletRequest request, @RequestParam(name = "url") String url) throws Exception {
		if (url == null || url.isBlank()) throw new ValidationFailedException("Instagram URL is required");
		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			if (mediaRepo.getDailyInstagramScrapeCount(ctx.authUserId()) > 50)
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily limit reached");
			List<ApifyInstagramResolver.InstagramMedia> list = ApifyInstagramResolver.resolveMedia(url);
			mediaRepo.logInstagramScrape(ctx.authUserId(), url, list.size());
			return list;
		}));
	}

	@Operation(summary = "Update Media SVG", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/svg")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> postMediaSvg(HttpServletRequest request, @RequestBody Media m) throws Exception {
		if (m == null || m.identity() == null || m.identity().id() <= 0) throw new ValidationFailedException("Invalid media payload");
		executeContextualTask(request, ctx -> {
			regionRepo.ensureAdminWriteRegion(ctx.setup(), ctx.authUserId());
			mediaRepo.upsertMediaSvg(m);
			return null;
		});
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Signal direct video upload completion and trigger async background processing", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/video/{id}/complete")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> postMediaVideoComplete(HttpServletRequest request, @PathVariable(name = "id") int id) throws Exception {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		executeContextualTask(request, ctx -> {
			Media m = mediaRepo.getMedia(ctx.authUserId(), id);
			Preconditions.checkArgument(m.isMovie(), "Target is not a video");
			Preconditions.checkArgument(m.uploadedByMe(), "Permission denied");
			supplyAsync(() -> { VideoHelper.processVideo(storage, txManager, mediaRepo, id, m.thumbnailSeconds()); return null; })
			.exceptionally(ex -> { logger.error("Async video error for id=" + id, ex); return null; });
			return null;
		});
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Add embedded external video (YouTube/Vimeo)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Media.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/video/embed")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Media> postMediaVideoEmbed(HttpServletRequest request, @RequestBody Media media) throws Exception {
		Preconditions.checkArgument(media != null && media.embedUrl() != null && !media.embedUrl().isBlank());
		String url = media.embedUrl().toLowerCase();
		Preconditions.checkArgument(url.contains("youtube.com") || url.contains("youtu.be") || url.contains("vimeo.com"), "Unsupported provider");
		try { URI.create(media.embedUrl()).toURL(); } catch (Exception e) { throw new IllegalArgumentException("Malformed URL", e); }

		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			int newId = mediaRepo.addMediaVideoEmbed(ctx.authUserId(), media, StorageType.MP4);
			supplyAsync(() -> { ImageHelper.saveImageFromEmbedVideo(storage, txManager, mediaRepo, newId, media.embedUrl()); return null; })
			.exceptionally(ex -> { logger.error("Async embed thumbnail failed for id=" + newId, ex); return null; });
			return mediaRepo.getMedia(ctx.authUserId(), newId);
		}));
	}

	@Operation(summary = "Initiate video upload to get a presigned storage URL", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = VideoInitResponse.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/video/initiate")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<VideoInitResponse> postMediaVideoInitiate(HttpServletRequest request, @RequestBody VideoInitPayload payload) throws Exception {
		Preconditions.checkArgument(payload != null && payload.media() != null, "Video payload or media is missing");
		Preconditions.checkArgument(payload.fileSize() <= StorageManager.MAX_VIDEO_UPLOAD_BYTES, "Video exceeds maximum allowed size");
		StorageType storageType = StorageType.fromMimeType(payload.contentType())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported video content type: " + payload.contentType()));
		Preconditions.checkArgument(storageType.isMovie(), "Provided format is not a video type.");

		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			int newMediaId = mediaRepo.addMediaVideoPlaceholder(ctx.authUserId(), payload.media(), storageType);
			String presignedUrl = storage.generatePresignedPutUrl(
					S3KeyGenerator.getOriginalMp4(newMediaId),
					storageType.getMimeType(),
					payload.fileSize()
					);
			return new VideoInitResponse(newMediaId, presignedUrl);
		}));
	}

	@Operation(summary = "Update media", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PutMapping
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> putMedia(HttpServletRequest request, @RequestBody Media m) throws Exception {
		if (m == null || m.identity() == null || m.identity().id() <= 0) {
			throw new ValidationFailedException("Invalid mediaId");
		}
		executeContextualTask(request, ctx -> {
			mediaRepo.updateMedia(ctx.authUserId(), m);
			return null;
		});
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update media rotation (allowed for administrators + user who uploaded specific image)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PutMapping("/jpeg/rotate")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<Void> putMediaJpegRotate(HttpServletRequest request,
			@RequestParam(name = "idMedia") int idMedia,
			@RequestParam(name = "degrees") int degrees) throws Exception {
		if (idMedia <= 0) {
			throw new ValidationFailedException("Invalid idMedia");
		}
		if (degrees != 90 && degrees != 180 && degrees != 270) {
			throw new ValidationFailedException("Invalid rotation degrees. Must be 90, 180, or 270.");
		}
		executeContextualTask(request, ctx -> {
			mediaRepo.rotateMedia(ctx.authUserId(), idMedia, degrees);
			return null;
		});
		return ResponseEntity.ok().build();
	}

	private ResponseEntity<Void> createRedirect(String key, int version) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, StorageManager.getPublicUrl(key, version))
				.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).mustRevalidate())
				.build();
	}

	private ResponseEntity<Void> executeGenerationPipeline(StorageManager storage, String key, int version, ImageTask task) throws Exception {
	    task.execute(storage);
	    if (!storage.exists(key)) {
	        throw new NoSuchElementException("Generated resource not found at key: " + key);
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
				storage.uploadImage(key, Scalr.crop(b, x, y, width, height), type);
			}
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
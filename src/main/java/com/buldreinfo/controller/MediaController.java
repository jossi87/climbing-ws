package com.buldreinfo.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
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
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.io.ImageSaver;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.VideoInitPayload;
import com.buldreinfo.model.VideoInitResponse;
import com.buldreinfo.service.ImageService;
import com.buldreinfo.service.InstagramService;
import com.buldreinfo.service.VideoService;

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
public class MediaController {
	@FunctionalInterface
	private interface ImageTask {
		void execute(StorageManager storage);
	}
	private static final Logger logger = LogManager.getLogger();
	private final RequestContext requestContext;
	private final StorageManager storage;
	private final ImageService imageService;
	private final VideoService videoService;
	private final MediaRepository mediaRepo;
	private final RegionRepository regionRepo;
	private final InstagramService instagramService;

	public MediaController(RequestContext requestContext, StorageManager storage, ImageService imageService, VideoService videoService, MediaRepository mediaRepo, RegionRepository regionRepo, InstagramService instagramService) {
		this.requestContext = requestContext;
		this.storage = storage;
		this.imageService = imageService;
		this.videoService = videoService;
		this.mediaRepo = mediaRepo;
		this.regionRepo = regionRepo;
		this.instagramService = instagramService;
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
	public ResponseEntity<Void> deleteMedia(@RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.deleteMedia(authUserId, id);
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
	public ResponseEntity<Media> getMedia(@RequestParam(name = "idMedia") int idMedia) {
		if (idMedia <= 0) throw new ValidationFailedException("Invalid idMedia=" + idMedia);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, idMedia));
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
			@RequestParam(name = "height", defaultValue = "0") int height) {

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
	public ResponseEntity<Void> patchMediaOrder(@RequestParam(name = "id") int id,
			@RequestParam(name = "left", defaultValue = "false") boolean left,
			@RequestParam(name = "right", defaultValue = "false") boolean right) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		if (!(left ^ right)) throw new ValidationFailedException("Specify either 'left' or 'right', not both.");
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.shiftMediaPosition(authUserId, id, left, right);
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
	public ResponseEntity<Media> postMediaImage(@RequestPart("json") Media m, @RequestPart("file") MultipartFile file) {
		if (m == null) throw new IllegalArgumentException("Media payload is required");
		String originalFilename = file.getOriginalFilename();
		StorageType storageType = StorageType.fromFilename(originalFilename)
				.orElseThrow(() -> new IllegalArgumentException("Unsupported file extension: " + originalFilename));
		var authUserId = requestContext.getAuthenticatedUserId();
		int newMediaId = mediaRepo.addMediaImage(authUserId, m, storageType, () -> {
			try {
				return file.getInputStream();
			} catch (IOException e) {
				throw new RuntimeException("Failed to get InputStream from multipart file", e);
			}
		});
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, newMediaId));
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
	public ResponseEntity<Media> postMediaInstagramSave(@RequestHeader("X-Selected-Cdn-Url") String selectedCdnUrl,
			@RequestHeader("X-Selected-Is-Video") boolean isVideo,
			@RequestHeader("X-Selected-Media-Index") int mediaIndex,
			@RequestBody Media mediaPayload) {
		if (mediaPayload == null) throw new IllegalArgumentException("Media payload missing");
		URI validatedUri = InstagramService.validateInstagramCdnUrl(selectedCdnUrl);
		var authUserId = requestContext.getAuthenticatedUserId();
		if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50)
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily limit reached");

		mediaPayload.ensureCorrectMediaAssociations(authUserId);
		if (isVideo) {
			int id = mediaRepo.addMediaVideoPlaceholder(authUserId, mediaPayload, StorageType.MP4);
			CompletableFuture.runAsync(() -> {
				try {
					byte[] videoData;
					try (InputStream is = validatedUri.toURL().openStream()) {
						videoData = storage.readBoundedStream(is);
					} catch (IOException e) {
						logger.warn("Initial instagram video link expired, attempting fallback re-scrape for id=" + id, e);
						List<InstagramService.InstagramMedia> fresh = instagramService.resolveMedia(mediaPayload.embedUrl());
						InstagramService.InstagramMedia target = fresh.stream().filter(m -> m.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
						mediaRepo.logInstagramScrape(authUserId, mediaPayload.embedUrl(), fresh.size());
						try (InputStream is = InstagramService.validateInstagramCdnUrl(target.cdnUrl()).toURL().openStream()) {
							videoData = storage.readBoundedStream(is);
						}
					}
					storage.uploadBytes(S3KeyGenerator.getOriginalMp4(id), videoData, StorageType.MP4);
					videoService.processVideo(id, mediaPayload.thumbnailSeconds());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.exceptionally(ex -> { 
				logger.error("Async video save failed", ex);
				return null; 
			});
			return ResponseEntity.ok(mediaRepo.getMedia(authUserId, id));
		}

		byte[] imageData;
		try (InputStream is = validatedUri.toURL().openStream()) {
			imageData = storage.readBoundedStream(is);
		} catch (IOException e) {
			logger.warn("Initial instagram image link expired, attempting fallback re-scrape", e);
			List<InstagramService.InstagramMedia> fresh = instagramService.resolveMedia(mediaPayload.embedUrl());
			InstagramService.InstagramMedia target = fresh.stream().filter(m -> m.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
			mediaRepo.logInstagramScrape(authUserId, mediaPayload.embedUrl(), fresh.size());
			try (InputStream is = InstagramService.validateInstagramCdnUrl(target.cdnUrl()).toURL().openStream()) {
				imageData = storage.readBoundedStream(is);
			} catch (IOException ex) {
				throw new UncheckedIOException(ex.getMessage(), ex);
			}
		}
		final byte[] finalData = imageData;
		int newId = mediaRepo.addMediaImage(authUserId, mediaPayload, StorageType.JPG, () -> new ByteArrayInputStream(finalData));
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, newId));
	}

	@Operation(summary = "Scrape Instagram URL metadata for frontend preview box", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = InstagramService.InstagramMedia.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@PostMapping("/instagram-scrape")
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	public ResponseEntity<List<InstagramService.InstagramMedia>> postMediaInstagramScrape(@RequestParam(name = "url") String url) {
		if (url == null || url.isBlank()) throw new ValidationFailedException("Instagram URL is required");
		var authUserId = requestContext.getAuthenticatedUserId();
		if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50)
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily limit reached");
		List<InstagramService.InstagramMedia> list = instagramService.resolveMedia(url);
		mediaRepo.logInstagramScrape(authUserId, url, list.size());
		return ResponseEntity.ok(list);
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
	public ResponseEntity<Void> postMediaSvg(HttpServletRequest request, @RequestBody Media m) {
		if (m == null || m.identity() == null || m.identity().id() <= 0) throw new ValidationFailedException("Invalid media payload");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		mediaRepo.upsertMediaSvg(m);
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
	public ResponseEntity<Void> postMediaVideoComplete(@PathVariable(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var authUserId = requestContext.getAuthenticatedUserId();
		Media m = mediaRepo.getMedia(authUserId, id);
		if (!m.isMovie()) throw new IllegalArgumentException("Target is not a video");
		if (!m.uploadedByMe()) throw new IllegalArgumentException("Permission denied");
		CompletableFuture.runAsync(() -> {
			try {
				videoService.processVideo(id, m.thumbnailSeconds());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		})
		.exceptionally(ex -> { 
			logger.error("Async video error for id=" + id, ex);
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
	public ResponseEntity<Media> postMediaVideoEmbed(@RequestBody Media media) {
		if (media == null || media.embedUrl() == null || media.embedUrl().isBlank()) throw new IllegalArgumentException();
		String url = media.embedUrl().toLowerCase();
		if (!url.contains("youtube.com") && !url.contains("youtu.be") && !url.contains("vimeo.com")) throw new IllegalArgumentException("Unsupported provider");
		try { URI.create(media.embedUrl()).toURL(); } catch (Exception e) { throw new IllegalArgumentException("Malformed URL", e); }
		var authUserId = requestContext.getAuthenticatedUserId();
		int newId = mediaRepo.addMediaVideoEmbed(authUserId, media, StorageType.MP4);
		CompletableFuture.runAsync(() -> {
			try {
				imageService.saveImageFromEmbedVideo(newId, media.embedUrl());
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		})
		.exceptionally(ex -> { 
			logger.error("Async embed thumbnail failed for id=" + newId, ex);
			return null; 
		});
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, newId));
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
	public ResponseEntity<VideoInitResponse> postMediaVideoInitiate(@RequestBody VideoInitPayload payload) {
		if (payload == null || payload.media() == null) throw new IllegalArgumentException("Video payload or media is missing");
		if (payload.fileSize() > StorageManager.MAX_VIDEO_UPLOAD_BYTES) throw new IllegalArgumentException("Video exceeds maximum allowed size");
		StorageType storageType = StorageType.fromMimeType(payload.contentType())
				.orElseThrow(() -> new IllegalArgumentException("Unsupported video content type: " + payload.contentType()));
		if (!storageType.isMovie()) throw new IllegalArgumentException("Provided format is not a video type.");
		var authUserId = requestContext.getAuthenticatedUserId();
		int newMediaId = mediaRepo.addMediaVideoPlaceholder(authUserId, payload.media(), storageType);
		String presignedUrl = storage.generatePresignedPutUrl(
				S3KeyGenerator.getOriginalMp4(newMediaId),
				storageType.getMimeType(),
				payload.fileSize()
				);
		return ResponseEntity.ok(new VideoInitResponse(newMediaId, presignedUrl));
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
	public ResponseEntity<Void> putMedia(@RequestBody Media m) {
		if (m == null || m.identity() == null || m.identity().id() <= 0) {
			throw new ValidationFailedException("Invalid mediaId");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.updateMedia(authUserId, m);
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
	public ResponseEntity<Void> putMediaJpegRotate(@RequestParam(name = "idMedia") int idMedia, @RequestParam(name = "degrees") int degrees) {
		if (idMedia <= 0) {
			throw new ValidationFailedException("Invalid idMedia");
		}
		if (degrees != 90 && degrees != 180 && degrees != 270) {
			throw new ValidationFailedException("Invalid rotation degrees. Must be 90, 180, or 270.");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.rotateMedia(authUserId, idMedia, degrees);
		return ResponseEntity.ok().build();
	}

	private ResponseEntity<Void> createRedirect(String key, int version) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, StorageManager.getPublicUrl(key, version))
				.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).mustRevalidate())
				.build();
	}

	private ResponseEntity<Void> executeGenerationPipeline(StorageManager storage, String key, int version, ImageTask task) {
		task.execute(storage);
		if (!storage.exists(key)) {
			throw new NoSuchElementException("Generated resource not found at key: " + key);
		}
		return createRedirect(key, version);
	}

	private void processCrop(StorageManager storage, int id, int x, int y, int width, int height, String key, StorageType type) {
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

	private void processResize(StorageManager storage, int id, int targetWidth, int minDimension, String key, StorageType type) {
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

	private void processStandard(StorageManager storage, int id, String key, StorageType type) {
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
package com.buldreinfo.controller;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
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

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.AsyncConfig;
import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.TooManyRequestsException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.VideoInitPayload;
import com.buldreinfo.model.VideoInitResponse;
import com.buldreinfo.service.ImageService;
import com.buldreinfo.service.InstagramService;
import com.buldreinfo.service.VideoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Media")
@RestController
@RequestMapping("/media")
public class MediaController {

	private static final Logger logger = LogManager.getLogger();

	private final ImageService imageService;
	private final InstagramService instagramService;
	private final MediaRepository mediaRepo;
	private final RegionRepository regionRepo;
	private final RequestContext requestContext;
	private final StorageManager storage;
	private final TaskExecutor videoProcessingExecutor;
	private final VideoService videoService;

	public MediaController(
			RequestContext requestContext, 
			StorageManager storage, 
			ImageService imageService, 
			VideoService videoService, 
			MediaRepository mediaRepo, 
			RegionRepository regionRepo, 
			InstagramService instagramService,
			@Qualifier(AsyncConfig.VIDEO_EXECUTOR_BEAN_NAME) TaskExecutor videoProcessingExecutor) {
		this.requestContext = requestContext;
		this.storage = storage;
		this.imageService = imageService;
		this.videoService = videoService;
		this.mediaRepo = mediaRepo;
		this.regionRepo = regionRepo;
		this.instagramService = instagramService;
		this.videoProcessingExecutor = videoProcessingExecutor;
	}

	@Operation(summary = "Move media to trash")
	@DeleteMapping
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Void> deleteMedia(@RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.deleteMedia(authUserId, id);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get Media by id")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Media> getMedia(@RequestParam(name = "idMedia") int idMedia) {
		if (idMedia <= 0) throw new ValidationFailedException("Invalid idMedia=" + idMedia);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, idMedia));
	}

	@Operation(summary = "Get media file by id")
	@ApiResponse(responseCode = "302", description = "Redirects to the generated or cached static asset URL hosted on S3 Storage")
	@GetMapping("/file")
	public ResponseEntity<Void> getMediaFile(HttpServletRequest request,
			@RequestParam(name = "id") int id,
			@RequestParam(name = "isMovie") boolean isMovie,
			@RequestParam(name = "versionStamp", defaultValue = "0") long versionStamp,
			@RequestParam(name = "original", defaultValue = "false") boolean original,
			@RequestParam(name = "targetWidth", defaultValue = "0") int targetWidth,
			@RequestParam(name = "minDimension", defaultValue = "0") int minDimension,
			@RequestParam(name = "x", defaultValue = "0") int x,
			@RequestParam(name = "y", defaultValue = "0") int y,
			@RequestParam(name = "width", defaultValue = "0") int width,
			@RequestParam(name = "height", defaultValue = "0") int height) {

		if (isMovie) {
			String key = requestContext.acceptsWebm(request) ? S3KeyGenerator.getWebWebm(id) : S3KeyGenerator.getWebMp4(id);
			if (!storage.exists(key)) {
				throw new NoSuchElementException("Movie resource not found: " + key);
			}
			return createRedirect(key, versionStamp);
		}

		boolean webP = requestContext.acceptsWebp(request);
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
			return executeGenerationPipeline(key, versionStamp, () -> imageService.processResize(id, targetWidth, minDimension, key, outputType));
		}

		if (width > 0 && height > 0) {
			key = webP ? S3KeyGenerator.getWebWebpRegion(id, x, y, width, height) : S3KeyGenerator.getWebJpgRegion(id, x, y, width, height);
			if (storage.exists(key)) return createRedirect(key, versionStamp);
			return executeGenerationPipeline(key, versionStamp, () -> imageService.processCrop(id, x, y, width, height, key, outputType));
		}

		key = webP ? S3KeyGenerator.getWebWebp(id) : S3KeyGenerator.getWebJpg(id);
		if (storage.exists(key)) return createRedirect(key, versionStamp);
		return executeGenerationPipeline(key, versionStamp, () -> imageService.processStandard(id, key, outputType));
	}

	@Operation(summary = "Reorder media")
	@PatchMapping("/order")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Void> patchMediaOrder(@RequestParam(name = "id") int id,
			@RequestParam(name = "left", defaultValue = "false") boolean left,
			@RequestParam(name = "right", defaultValue = "false") boolean right) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		if (!(left ^ right)) throw new ValidationFailedException("Specify either 'left' or 'right', not both.");
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.shiftMediaPosition(authUserId, id, left, right);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Add single image media item")
	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Media> postMediaImage(@RequestPart("json") Media m, @RequestPart("file") MultipartFile file) {
		if (m == null) throw new ValidationFailedException("Media payload is required");
		String originalFilename = file.getOriginalFilename();
		StorageType storageType = StorageType.fromFilename(originalFilename)
				.orElseThrow(() -> new ValidationFailedException("Unsupported file extension: " + originalFilename));
		var authUserId = requestContext.getAuthenticatedUserId();
		int newMediaId = mediaRepo.addMediaImage(authUserId, m, storageType, () -> {
			try {
				return file.getInputStream();
			} catch (IOException e) {
				throw new InternalServerErrorException("Failed to read uploaded file", e);
			}
		});
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, newMediaId));
	}

	@Operation(summary = "Commit verified Instagram media to application storage")
	@PostMapping("/instagram-save")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Media> postMediaInstagramSave(@RequestHeader("X-Selected-Cdn-Url") String selectedCdnUrl,
			@RequestHeader("X-Selected-Is-Video") boolean isVideo,
			@RequestHeader("X-Selected-Media-Index") int mediaIndex,
			@RequestBody Media mediaPayload) {
		if (mediaPayload == null) throw new ValidationFailedException("Media payload missing");
		URI validatedUri = InstagramService.validateInstagramCdnUrl(selectedCdnUrl);
		var authUserId = requestContext.getAuthenticatedUserId();
		if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50)
			throw new TooManyRequestsException("Daily limit reached");

		mediaPayload.ensureCorrectMediaAssociations(authUserId);
		if (isVideo) {
			var storageType = StorageType.MP4;
			int id = mediaRepo.addMediaVideoPlaceholder(authUserId, mediaPayload, storageType);
			CompletableFuture.runAsync(() -> {
				try {
					byte[] videoData;
					try (InputStream is = validatedUri.toURL().openStream()) {
						videoData = storage.readBoundedStream(is);
					} catch (IOException e) {
						logger.warn("Initial instagram video link expired, attempting fallback re-scrape for id=" + id, e);
						List<InstagramService.InstagramMedia> fresh = instagramService.resolveMedia(mediaPayload.embedUrl());
						InstagramService.InstagramMedia target = fresh.stream().filter(md -> md.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
						mediaRepo.logInstagramScrape(authUserId, mediaPayload.embedUrl(), fresh.size());
						try (InputStream is = InstagramService.validateInstagramCdnUrl(target.cdnUrl()).toURL().openStream()) {
							videoData = storage.readBoundedStream(is);
						}
					}
					storage.uploadBytes(S3KeyGenerator.getOriginalMp4(id, storageType), videoData, storageType);
					videoService.processVideo(id, storageType, mediaPayload.thumbnailSeconds());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, videoProcessingExecutor)
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
			InstagramService.InstagramMedia target = fresh.stream().filter(md -> md.mediaIndex() == mediaIndex).findFirst().orElse(fresh.get(0));
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

	@Operation(summary = "Scrape Instagram URL metadata for frontend preview box")
	@PostMapping("/instagram-scrape")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<List<InstagramService.InstagramMedia>> postMediaInstagramScrape(@RequestParam(name = "url") String url) {
		if (url == null || url.isBlank()) throw new ValidationFailedException("Instagram URL is required");
		var authUserId = requestContext.getAuthenticatedUserId();
		if (mediaRepo.getDailyInstagramScrapeCount(authUserId) > 50)
			throw new TooManyRequestsException("Daily limit reached");
		List<InstagramService.InstagramMedia> list = instagramService.resolveMedia(url);
		mediaRepo.logInstagramScrape(authUserId, url, list.size());
		return ResponseEntity.ok(list);
	}

	@Operation(summary = "Update Media SVG")
	@PostMapping("/svg")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Void> postMediaSvg(HttpServletRequest request, @RequestBody Media m) {
		if (m == null || m.identity() == null || m.identity().id() <= 0) throw new ValidationFailedException("Invalid media payload");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		mediaRepo.upsertMediaSvg(m);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Signal direct video upload completion and trigger async background processing")
	@PostMapping("/video/{id}/complete")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Void> postMediaVideoComplete(@PathVariable(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var authUserId = requestContext.getAuthenticatedUserId();
		Media m = mediaRepo.getMedia(authUserId, id);
		if (!m.isMovie()) throw new ValidationFailedException("Target is not a video");
		if (!m.uploadedByMe()) throw new ForbiddenException("Permission denied");

		CompletableFuture.runAsync(() -> {
			try {
				var storageType = StorageType.fromExtension(m.suffix()).orElseThrow();
				videoService.processVideo(id, storageType, m.thumbnailSeconds());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, videoProcessingExecutor)
		.exceptionally(ex -> {
			logger.error("Async video error for id=" + id, ex);
			return null;
		});

		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Add embedded external video (YouTube/Vimeo)")
	@PostMapping("/video/embed")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Media> postMediaVideoEmbed(@RequestBody Media media) {
		if (media == null || media.embedUrl() == null || media.embedUrl().isBlank()) throw new ValidationFailedException("Embed URL is required");
		String url = media.embedUrl().toLowerCase();
		if (!url.contains("youtube.com") && !url.contains("youtu.be") && !url.contains("vimeo.com")) throw new ValidationFailedException("Unsupported provider");
		try {
			URI.create(media.embedUrl()).toURL();
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			throw new ValidationFailedException("Malformed URL");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		int newId = mediaRepo.addMediaVideoEmbed(authUserId, media, StorageType.MP4);
		CompletableFuture.runAsync(() -> {
			try {
				var thumb = imageService.readFromEmbedUrl(media.embedUrl());
				try {
					imageService.saveImage(newId, thumb);
				} finally {
					thumb.flush();
				}
			} catch (IOException | InterruptedException e) {
				throw new UncheckedIOException(new IOException("Failed to fetch embed thumbnail", e));
			}
		})
		.exceptionally(ex -> {
			logger.error("Async embed thumbnail failed for id=" + newId, ex);
			return null;
		});
		return ResponseEntity.ok(mediaRepo.getMedia(authUserId, newId));
	}

	@Operation(summary = "Initiate video upload to get a presigned storage URL")
	@PostMapping("/video/initiate")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<VideoInitResponse> postMediaVideoInitiate(@RequestBody VideoInitPayload payload) {
		if (payload == null || payload.media() == null) throw new ValidationFailedException("Video payload or media is missing");
		if (payload.fileSize() > StorageManager.MAX_VIDEO_UPLOAD_BYTES) throw new ValidationFailedException("Video exceeds maximum allowed size");
		StorageType storageType = StorageType.fromMimeType(payload.contentType())
				.orElseThrow(() -> new ValidationFailedException("Unsupported video content type: " + payload.contentType()));
		if (!storageType.isMovie()) throw new ValidationFailedException("Provided format is not a video type.");
		var authUserId = requestContext.getAuthenticatedUserId();
		int newMediaId = mediaRepo.addMediaVideoPlaceholder(authUserId, payload.media(), storageType);
		String presignedUrl = storage.generatePresignedPutUrl(
				S3KeyGenerator.getOriginalMp4(newMediaId, storageType),
				storageType.getMimeType(),
				payload.fileSize()
				);
		return ResponseEntity.ok(new VideoInitResponse(newMediaId, presignedUrl));
	}

	@Operation(summary = "Update media")
	@PutMapping
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	public ResponseEntity<Void> putMedia(@RequestBody Media m) {
		if (m == null || m.identity() == null || m.identity().id() <= 0) {
			throw new ValidationFailedException("Invalid mediaId");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.updateMedia(authUserId, m);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update media rotation (allowed for administrators + user who uploaded specific image)")
	@PutMapping("/jpeg/rotate")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
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

	private ResponseEntity<Void> createRedirect(String key, long version) {
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, StorageManager.getPublicUrl(key, version))
				.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).mustRevalidate())
				.build();
	}

	private ResponseEntity<Void> executeGenerationPipeline(String key, long version, Runnable task) {
		task.run();
		if (!storage.exists(key)) {
			throw new NoSuchElementException("Generated resource not found at key: " + key);
		}
		return createRedirect(key, version);
	}
}
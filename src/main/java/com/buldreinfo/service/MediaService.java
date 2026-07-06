package com.buldreinfo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.MediaRepository.MediaAssociation.TargetType;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.io.ExifReader.ImageRotation;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Media.Association;
import com.buldreinfo.model.Media.MediaProblem;
import com.buldreinfo.model.Svg;

@Service
public class MediaService {
	private final ActivityRepository activityRepo;
	private final ImageService imageService;
	private final MediaRepository mediaRepo;
	private final ProblemRepository problemRepo;
	private final StorageManager storage;
	private final UserRepository userRepo;
	private final VideoService videoService;

	public MediaService(
			MediaRepository mediaRepo,
			ActivityRepository activityRepo,
			ImageService imageService,
			ProblemRepository problemRepo,
			StorageManager storage,
			UserRepository userRepo,
			VideoService videoService) {
		this.mediaRepo = mediaRepo;
		this.activityRepo = activityRepo;
		this.imageService = imageService;
		this.problemRepo = problemRepo;
		this.storage = storage;
		this.userRepo = userRepo;
		this.videoService = videoService;
	}

	@Transactional
	public int addMediaImage(Optional<Integer> authUserId, Media m, StorageType storageType, Supplier<InputStream> inputStreamSupplier) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("Not logged in");
		if (storageType == null) throw new NullPointerException("StorageType is required");
		if (inputStreamSupplier == null) throw new NullPointerException("InputStreamSupplier is required");
		if (storageType.isMovie()) throw new IllegalArgumentException("Use the video endpoints for video uploads");

		var associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(authUserId.get(), m, storageType);
		saveMediaContext(idMedia, associations, m, false);

		if (associations == Association.PROBLEMS) {
			m.problems().forEach(p -> activityRepo.fillActivity(p.problemId()));
		}

		try (var is = inputStreamSupplier.get()) {
			imageService.saveImage(idMedia, storage.readBoundedStream(is));
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return idMedia;
	}

	@Transactional
	public int addMediaVideoEmbed(Optional<Integer> authUserId, Media m, StorageType storageType) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("Not logged in");
		var associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(authUserId.get(), m, storageType);
		saveMediaContext(idMedia, associations, m, false);
		if (associations == Association.PROBLEMS) {
			for (var problem : m.problems()) {
				activityRepo.fillActivity(problem.problemId());
			}
		}
		return idMedia;
	}

	@Transactional
	public int addMediaVideoPlaceholder(Optional<Integer> authUserId, Media m, StorageType storageType) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("Not logged in");
		var associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(authUserId.get(), m, storageType);
		saveMediaContext(idMedia, associations, m, false);
		if (associations == Association.PROBLEMS) {
			for (var problem : m.problems()) {
				activityRepo.fillActivity(problem.problemId());
			}
		}
		return idMedia;
	}

	@Transactional
	public void deleteMedia(Optional<Integer> authUserId, int idMedia) {
		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(authUserId, idMedia);

		var idProblems = mediaRepo.getProblemIdsForMedia(idMedia);
		mediaRepo.markMediaDeleted(authUserId.orElseThrow(), idMedia);

		idProblems.forEach(activityRepo::fillActivity);
	}

	public void deleteMediaAnalysis(int idMedia) {
		mediaRepo.deleteMediaAnalysis(idMedia);
	}

	public int getDailyInstagramScrapeCount(Optional<Integer> authUserId) {
		return mediaRepo.getDailyInstagramScrapeCount(authUserId);
	}

	public List<MediaRepository.EmbeddedVideo> getEmbeddedVideos() {
		return mediaRepo.getEmbeddedVideos();
	}

	public Media getMedia(Optional<Integer> authUserId, int id) {
		return mediaRepo.getMedia(authUserId, id);
	}

	public List<Media> getMediaArea(Optional<Integer> authUserId, int id, boolean inherited) {
		return mediaRepo.getMediaArea(authUserId, id, inherited);
	}

	public List<Media> getMediaGuestbook(Optional<Integer> authUserId, int guestbookId) {
		return mediaRepo.getMediaGuestbook(authUserId, guestbookId);
	}

	public List<MediaRepository.MediaPendingAnalysis> getMediaPendingAnalysis() {
		return mediaRepo.getMediaPendingAnalysis();
	}

	public List<Media> getMediaProblem(Setup s, Optional<Integer> authUserId, int sectorId, int problemId, boolean showHiddenMedia) {
		List<Media> media = mediaRepo.getMediaProblemCombined(authUserId, sectorId, problemId);
		if (media != null && !media.isEmpty()) {
			var mediaWithRequestedTopoLine = new HashSet<Media>();
			for (var m : media) {
				if (m.svgs() != null && m.svgs().stream().anyMatch(svg -> svg.problemId() == problemId)) {
					mediaWithRequestedTopoLine.add(m);
				}
			}
			if (!showHiddenMedia && !mediaWithRequestedTopoLine.isEmpty()) {
				media = media.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty() || mediaWithRequestedTopoLine.contains(m)).toList();
			} else if (!showHiddenMedia && s.isBouldering() && problemId != 0) {
				media = media.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty()).toList();
			}
		}
		return media;
	}

	public List<Media> getMediaSectorThumbnails(Optional<Integer> authUserId, int idSector) {
		return mediaRepo.getMediaSectorThumbnails(authUserId, idSector);
	}

	public Map<Integer, List<Media>> getMediaTrails(Optional<Integer> authUserId, Collection<Integer> trailIds) {
		return mediaRepo.getMediaTrails(authUserId, trailIds);
	}

	public List<Media> getProfileMedia(Optional<Integer> authUserId, int reqId, boolean captured) {
		return mediaRepo.getProfileMedia(authUserId, reqId, captured);
	}

	public void logInstagramScrape(Optional<Integer> authUserId, String originalUrl, int slideCount) {
		mediaRepo.logInstagramScrape(authUserId, InstagramService.extractInstagramShortcode(originalUrl), originalUrl, slideCount);
	}

	@Transactional
	public void rotateMedia(Optional<Integer> authUserId, int idMedia, int degrees) {
		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(authUserId, idMedia);
		var r = switch (degrees) {
		case 90 -> ImageRotation.CW_90;
		case 180 -> ImageRotation.CW_180;
		case 270 -> ImageRotation.CW_270;
		default -> throw new IllegalArgumentException("Cannot rotate image " + degrees + " degrees (legal degrees = 90, 180, 270)");
		};
		imageService.rotateImage(idMedia, r);
	}

	public void saveMediaAnalysis(int mediaId, int imageWidth, int imageHeight, String hexColor, List<String> labels, List<ImageClassifierService.MediaObject> objects, boolean failed) {
		mediaRepo.saveMediaAnalysis(mediaId, imageWidth, imageHeight, hexColor, labels, objects, failed);
	}

	public void setMediaMetadata(int idMedia, int width, int height, LocalDateTime dateTaken, boolean is360) {
		mediaRepo.setMediaMetadata(idMedia, width, height, dateTaken, is360);
	}

	@Transactional
	public void shiftMediaPosition(Optional<Integer> authUserId, int id, boolean left, boolean right) {
		var authId = authUserId.orElseThrow();
		var result = mediaRepo.getMediaAssociation(authId, id);
		var idMediaList = mediaRepo.getMediaIdsForSorting(result);

		final int ixToMove = idMediaList.indexOf(id);
		if (ixToMove < 0) throw new IllegalArgumentException("Could not find " + id + " in list");

		idMediaList.remove(ixToMove);
		if (left) {
			idMediaList.add(Math.max(0, ixToMove - 1), id);
		} else if (right) {
			idMediaList.add(Math.min(idMediaList.size(), ixToMove + 1), id);
		} else {
			throw new UnsupportedOperationException("left=false and right=false");
		}

		mediaRepo.batchUpdateSorting(result, idMediaList);

		if (result.type() == TargetType.PROBLEM) {
			activityRepo.fillActivity(result.columnId());
		}
	}

	@Transactional
	public void updateMedia(Optional<Integer> authUserId, Media m) {
		if (m.identity() == null || m.identity().id() == 0) throw new IllegalArgumentException("Media id required.");
		if (m.photographer() == null || m.photographer().name() == null || m.photographer().name().isBlank()) throw new IllegalArgumentException("A valid photographer must be specified to update media context.");

		var associations = m.ensureCorrectMediaAssociations(authUserId);
		final var mediaId = m.identity().id();

		final var storageType = StorageType.fromExtension(m.suffix()).orElseThrow();

		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(authUserId, mediaId);

		var originalMedia = getMedia(authUserId, mediaId);
		var thumbnailChanged = originalMedia.thumbnailSeconds() != m.thumbnailSeconds();
		int photographerId = m.photographer().id() > 0 ? m.photographer().id() : userRepo.getExistingOrInsertUser(m.photographer().name());

		mediaRepo.updateMediaMetadata(m.description(), photographerId, m.thumbnailSeconds(), mediaId, thumbnailChanged);

		if (originalMedia.isMovie() && thumbnailChanged) {
			try {
				var originalMp4Key = S3KeyGenerator.getOriginalMp4(mediaId, storageType);
				var tempOriginal = Files.createTempFile("original-re-thumb-" + mediaId, ".mp4");
				try {
					storage.downloadFile(originalMp4Key, tempOriginal);
					videoService.extractThumbnail(mediaId, tempOriginal, m.thumbnailSeconds());
					S3KeyGenerator.getGeneratedMediaPrefixes(mediaId).forEach(storage::invalidateCache);
				} finally {
					Files.deleteIfExists(tempOriginal);
				}
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		saveMediaContext(mediaId, associations, m, true);

		Stream.of(originalMedia.problems(), m.problems())
		.filter(Objects::nonNull)
		.flatMap(List::stream)
		.map(MediaProblem::problemId)
		.distinct()
		.forEach(activityRepo::fillActivity);
	}


	public void updateMediaFocusAndActionStatus() {
		mediaRepo.updateMediaFocusAndActionStatus();
	}

	public void upsertMediaSvg(Media m) {
		mediaRepo.upsertMediaSvg(m);
	}

	@Transactional
	public void upsertSvg(Optional<Integer> authUserId, int problemId, int pitch, int mediaId, Svg svg) {
		problemRepo.ensureAdminWriteProblem(authUserId, problemId);
		mediaRepo.upsertSvgDb(problemId, pitch, mediaId, svg);
	}

	private void ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(Optional<Integer> authUserId, int idMedia) {
		var m = mediaRepo.getMedia(authUserId, idMedia);
		if (m.uploadedByMe()) return;

		if (!mediaRepo.isMediaAuthorizedForUser(authUserId.orElseThrow(), idMedia)) {
			throw new ForbiddenException("Insufficient permissions");
		}
	}

	private int insertMediaMetadata(int uploaderId, Media m, StorageType storageType) {
		var photographerName = (m.photographer() != null) ? m.photographer().name() : null;
		int photographerId = (m.photographer() != null && m.photographer().id() > 0) 
				? m.photographer().id() 
						: userRepo.getExistingOrInsertUser(photographerName);
		return mediaRepo.insertMediaMetadata(uploaderId, m, storageType, photographerId);
	}

	private void saveMediaContext(int mediaId, Association associations, Media m, boolean isUpdate) {
		List<Integer> resolvedTaggedUserIds = null;
		if (m.tagged() != null && !m.tagged().isEmpty()) {
			resolvedTaggedUserIds = new ArrayList<>();
			for (var u : m.tagged()) {
				if (isUpdate && (u.name() == null || u.name().isBlank())) throw new IllegalArgumentException("Invalid tagged user: " + u);
				resolvedTaggedUserIds.add(u.id() > 0 ? u.id() : userRepo.getExistingOrInsertUser(u.name()));
			}
		}
		mediaRepo.saveMediaContext(mediaId, associations, m, isUpdate, resolvedTaggedUserIds);
	}
}
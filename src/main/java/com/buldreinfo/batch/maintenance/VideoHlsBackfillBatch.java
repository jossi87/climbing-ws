package com.buldreinfo.batch.maintenance;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.service.VideoService;

/**
 * Ensures the adaptive HLS ladder exists for every movie that is actually served from HLS. The ids come straight
 * from the database (uploaded movies plus Instagram movies) — never from the contents of the local mirror — so
 * orphaned originals left behind by failed uploads or deleted media are ignored. For each id the original is read
 * from {@code original/mp4/} and the renditions are written under {@code web/hls/}; a later
 * {@link S3BucketUploadBatch} run pushes them to the cloud.
 * <p>
 * Nothing is touched unless it needs to be: a movie is only generated when its ladder is missing or incomplete —
 * a vanished master playlist, a dropped rung, or a truncated segment. Encoding happens in a scratch directory next
 * to the target and is only published once verified, so a failed run never leaves a half-written ladder behind for
 * the uploader. Encoding is CPU heavy, so only a handful of ffmpeg processes run at once.
 */
public class VideoHlsBackfillBatch {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String MASTER_PLAYLIST = "master.m3u8";
	private static final String SCRATCH_SUFFIX = ".tmp";
	private static final List<String> ORIGINAL_EXTENSIONS = StorageType.MOVIE_SOURCE_TYPES.stream().map(StorageType::getExtension).toList();
	private final Path localMediaRoot;
	private final VideoService videoService;
	private final List<Integer> hlsMovieIds;
	private final ExecutorService executor = Executors.newFixedThreadPool(4);
	private final AtomicInteger generatedCount = new AtomicInteger(0);
	private final AtomicInteger skippedCount = new AtomicInteger(0);
	private final AtomicInteger noOriginalCount = new AtomicInteger(0);
	private final AtomicInteger failedCount = new AtomicInteger(0);

	protected VideoHlsBackfillBatch(Path localMediaRoot, VideoService videoService, List<Integer> hlsMovieIds) {
		this.localMediaRoot = localMediaRoot;
		this.videoService = videoService;
		this.hlsMovieIds = hlsMovieIds;
	}

	/** Locates the downloaded original for {@code idMedia}, trying each supported container extension. */
	private Path findOriginal(int idMedia) {
		Path dir = localMediaRoot.resolve(S3KeyGenerator.getOriginalMp4Prefix(idMedia));
		for (String extension : ORIGINAL_EXTENSIONS) {
			Path candidate = dir.resolve(idMedia + "." + extension);
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private void process(int idMedia) {
		Path outDir = localMediaRoot.resolve(S3KeyGenerator.getWebHlsPrefix(idMedia));
		if (isHlsComplete(outDir)) {
			skippedCount.incrementAndGet();
			return;
		}
		Path originalFile = findOriginal(idMedia);
		if (originalFile == null) {
			logger.warn("No original found for movie id={}; cannot generate HLS", idMedia);
			noOriginalCount.incrementAndGet();
			return;
		}
		Path scratchDir = outDir.resolveSibling(outDir.getFileName().toString() + SCRATCH_SUFFIX);
		deleteRecursively(scratchDir);
		try {
			videoService.generateHls(originalFile, scratchDir);
			if (!isHlsComplete(scratchDir)) {
				throw new IOException("HLS ladder is incomplete after generation");
			}
			// Publish only a verified ladder, replacing whatever partial tree was there before.
			deleteRecursively(outDir);
			Files.move(scratchDir, outDir);
			logger.info("Generated HLS for id={} into {}", idMedia, outDir);
			generatedCount.incrementAndGet();
		} catch (Exception e) {
			logger.error("Failed to generate HLS for id={} ({}): {}", idMedia, originalFile, e.getMessage());
			deleteRecursively(scratchDir);
			failedCount.incrementAndGet();
		}
	}

	private static List<String> referencedUris(Path playlist) throws IOException {
		List<String> uris = new ArrayList<>();
		for (String line : Files.readAllLines(playlist)) {
			String uri = line.strip();
			if (!uri.isEmpty() && !uri.startsWith("#")) {
				uris.add(uri);
			}
		}
		return uris;
	}

	private static boolean isNonEmptyFile(Path path) {
		try {
			return Files.isRegularFile(path) && Files.size(path) > 0;
		} catch (IOException _) {
			return false;
		}
	}

	/**
	 * True when {@code outDir} holds a complete ladder: a master playlist whose every referenced variant
	 * playlist exists, and within each variant every referenced segment exists. A single missing rung or
	 * segment makes the whole tree incomplete, which is what forces a regeneration.
	 */
	private static boolean isHlsComplete(Path outDir) {
		Path master = outDir.resolve(MASTER_PLAYLIST);
		if (!isNonEmptyFile(master)) {
			return false;
		}
		try {
			List<String> variants = referencedUris(master);
			if (variants.isEmpty()) {
				return false;
			}
			for (String variant : variants) {
				Path variantPlaylist = outDir.resolve(variant);
				if (!isNonEmptyFile(variantPlaylist)) {
					return false;
				}
				List<String> segments = referencedUris(variantPlaylist);
				if (segments.isEmpty()) {
					return false;
				}
				for (String segment : segments) {
					if (!isNonEmptyFile(outDir.resolve(segment))) {
						return false;
					}
				}
			}
			return true;
		} catch (IOException e) {
			logger.warn("Unable to read HLS playlists under {}: {}", outDir, e.getMessage());
			return false;
		}
	}

	private static void deleteRecursively(Path dir) {
		if (!Files.exists(dir)) {
			return;
		}
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					logger.warn("Failed to delete {}: {}", path, e.getMessage());
				}
			});
		} catch (IOException e) {
			logger.warn("Failed to clean up directory {}: {}", dir, e.getMessage());
		}
	}

	private void shutdownExecutor() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(12, TimeUnit.HOURS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException _) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected void run() {
		logger.info("Starting HLS backfill for {} database movie(s) into {}", hlsMovieIds.size(), localMediaRoot.resolve("web/hls"));
		for (int idMedia : hlsMovieIds) {
			executor.submit(() -> process(idMedia));
		}
		shutdownExecutor();
		logger.info("HLS backfill complete! Generated: {} [Skipped: {}] [NoOriginal: {}] [Failed: {}]", generatedCount.get(), skippedCount.get(), noOriginalCount.get(), failedCount.get());
	}
}

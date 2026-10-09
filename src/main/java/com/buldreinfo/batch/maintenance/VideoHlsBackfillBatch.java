package com.buldreinfo.batch.maintenance;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
 * a vanished master playlist, a dropped rung, or a truncated segment. This is a file existence scan and nothing
 * more, which is what keeps it cheap enough to run over every movie on every sync; a ladder whose playlists cannot
 * be read at all is logged and left alone rather than rebuilt on a guess.
 * <p>
 * The audio inside a ladder is deliberately not decoded here. It was, until every rendition that had copied a
 * malformed AAC frame out of its original had been rebuilt (the last of those in October 2026), but such a defect
 * can no longer get into a ladder in the first place: {@link VideoService#generateHls} re-encodes the audio unless
 * the original's own audio passes that very check, so a scan would only ever find damage that both checks are blind
 * to anyway — they read the first second, and a defect past it is copied by one and missed by the other. Verifying
 * one ladder by hand remains a single command, where silence means the player will decode what it is served:
 * {@code ffmpeg -v error -t 1 -i web/hls/<id>/v360p.m3u8 -vn -f null -}
 * <p>
 * Encoding happens in a scratch directory next to the target and is only published once verified, so a failed run
 * never leaves a half-written ladder behind for the uploader. Encoding is CPU heavy, so only a handful of ffmpeg
 * processes run at once.
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
		try {
			if (isHlsComplete(outDir)) {
				skippedCount.incrementAndGet();
				return;
			}
		} catch (IOException e) {
			// A ladder we cannot read is not a ladder we know to be broken, and re-encoding a movie costs about half
			// a minute, so a hiccup on a network-backed mirror is no reason to rebuild on a guess.
			logger.warn("Unable to read the HLS ladder of id={} ({}); leaving it alone", idMedia, e.getMessage());
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

	/** A file that exists and holds something; a file that is not there is simply false, not an error. */
	private static boolean isNonEmptyFile(Path path) throws IOException {
		return Files.isRegularFile(path) && Files.size(path) > 0;
	}

	/**
	 * True when {@code outDir} holds a complete ladder: a master playlist whose every referenced variant
	 * playlist exists, and within each variant every referenced segment exists. A single missing rung or
	 * segment makes the whole tree incomplete, which is what forces a regeneration.
	 *
	 * @throws IOException when the playlists cannot be read at all. That distinction is left to the callers, which
	 *         want opposite things from it: the check before generating leaves a movie alone on an unreadable
	 *         ladder, while the verification of a freshly generated directory reports it as a failure.
	 */
	private static boolean isHlsComplete(Path outDir) throws IOException {
		Path master = outDir.resolve(MASTER_PLAYLIST);
		if (!isNonEmptyFile(master)) {
			return false;
		}
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
			// single_file packs every fragment of a rung into one byte-range addressed m4s, so the playlist names
			// that same segment once per fragment; without deduplicating, one file would be stat'ed dozens of
			// times on a network-backed mirror.
			for (String segment : new LinkedHashSet<>(segments)) {
				if (!isNonEmptyFile(outDir.resolve(segment))) {
					return false;
				}
			}
		}
		return true;
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

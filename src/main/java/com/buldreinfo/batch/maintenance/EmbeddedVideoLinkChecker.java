package com.buldreinfo.batch.maintenance;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.MediaRepository.EmbeddedVideo;
import com.buldreinfo.service.MediaService;

/**
 * Asks YouTube and Vimeo whether the videos we embed still exist, and logs, for each one that does not, the
 * statement that would move that media over to the local copy.
 * <p>
 * Nothing is changed here: no database write, no file, no report. A dead link is printed as one line: the statement
 * that would move that media to the local copy, then a {@code --} comment naming the provider, the url and whether the
 * local mirror still holds the original the conversion reads. Whether to run it is decided by hand.
 * <p>
 * The question is put to the providers' own oEmbed endpoint, the check a browser makes before it plays the iframe,
 * and not to yt-dlp: there is nothing to download, and oEmbed answers "does this url still resolve to a video" in a
 * few hundred milliseconds where a download attempt takes seconds and can fail for reasons that say nothing about
 * the video. The stored {@code /embed/} url is not what is asked about, because YouTube answers 404 for its own embed
 * form even for a video that plays fine; the canonical {@code watch?v=} / {@code vimeo.com/<id>} form is used.
 * <p>
 * A provider that will not answer is not a dead video. Only the statuses that mean "this video is gone or will not be
 * served" (YouTube 400, 401, 403, 404, 410; Vimeo 401, 403, 404, 410) are called dead. A 429 or a 5xx is retried and
 * then logged as could not be checked, because both providers have outages, and a false death certificate costs a
 * video that still plays. Instagram posts and any other host are not YouTube/Vimeo videos at all and are skipped
 * without a request.
 * <p>
 * Clearing {@code embed_url} is what moves the video to the local copy: the row then counts as an uploaded movie, so
 * the next {@link VideoHlsBackfillBatch} run builds the HLS ladder from {@code original/mp4/} and
 * {@link S3BucketUploadBatch} publishes it. That is why the comment on the statement also reports whether the
 * original is in the local mirror: without it the backfill has nothing to build from, and the row should wait.
 */
public class EmbeddedVideoLinkChecker {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
	/**
	 * The same two patterns {@link com.buldreinfo.service.ImageService} reads a thumbnail url out of, so the id asked
	 * about is always the id the stored thumbnail was fetched for.
	 */
	private static final Pattern YOUTUBE_PATTERN = Pattern.compile("(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIMEO_PATTERN = Pattern.compile("vimeo\\.com/(?:video/)?([0-9]+)", Pattern.CASE_INSENSITIVE);
	/**
	 * How many embeds are asked about at once. The endpoints answer a burst without complaint, but a few thousand
	 * rows from one address is still a guest's share of their capacity, so the pool stays small.
	 */
	private static final int CHECK_THREADS = 8;
	/** A request is retried only when the answer was "ask again later" (429 or 5xx), and never more than this. */
	private static final int MAX_ATTEMPTS = 3;
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
	private static final Duration RETRY_BACKOFF = Duration.ofSeconds(2);
	private final HttpClient httpClient;
	private final MediaService mediaService;
	private final Path localMediaRoot;
	private final ExecutorService executor = Executors.newFixedThreadPool(CHECK_THREADS);

	protected EmbeddedVideoLinkChecker(MediaService mediaService, HttpClient httpClient, Path localMediaRoot) {
		this.mediaService = mediaService;
		this.httpClient = httpClient;
		this.localMediaRoot = localMediaRoot;
	}

	private enum Provider {
		YOUTUBE, VIMEO;

		/**
		 * The oEmbed endpoint that answers whether {@code videoId} still exists, with the url form that endpoint
		 * accepts. YouTube only resolves the canonical watch url, never its own {@code /embed/<id>} form.
		 */
		String oEmbedUrl(String videoId) {
			String canonicalUrl = switch (this) {
			case YOUTUBE -> "https://www.youtube.com/watch?v=" + videoId;
			case VIMEO -> "https://vimeo.com/" + videoId;
			};
			String endpoint = switch (this) {
			case YOUTUBE -> "https://www.youtube.com/oembed?format=json&url=";
			case VIMEO -> "https://vimeo.com/api/oembed.json?url=";
			};
			return endpoint + URLEncoder.encode(canonicalUrl, StandardCharsets.UTF_8);
		}

		/**
		 * The statuses that mean the provider will not serve the video any more. Measured against the live endpoints
		 * in November 2026: YouTube answers 400 for an id that does not exist, 401 for a video that is private or
		 * cannot be embedded, and 404 (or 410) once the uploader has deleted it; Vimeo answers 404 for an id that does
		 * not exist and 403 for a video its owner has made private. Everything else is left undecided.
		 */
		boolean meansGone(int status) {
			return switch (this) {
			case YOUTUBE -> status == 400 || status == 401 || status == 403 || status == 404 || status == 410;
			case VIMEO -> status == 401 || status == 403 || status == 404 || status == 410;
			};
		}
	}

	/** The provider an embed points at, or {@code null} for anything that is not a YouTube or Vimeo video. */
	private static Provider providerOf(String embedUrl) {
		if (embedUrl == null) {
			return null;
		}
		String url = embedUrl.toLowerCase(Locale.ROOT);
		if (url.contains("youtube.com") || url.contains("youtu.be")) {
			return Provider.YOUTUBE;
		}
		if (url.contains("vimeo.com")) {
			return Provider.VIMEO;
		}
		return null;
	}

	private static String videoIdOf(Provider provider, String embedUrl) {
		Matcher matcher = (provider == Provider.YOUTUBE ? YOUTUBE_PATTERN : VIMEO_PATTERN).matcher(embedUrl);
		return matcher.find() ? matcher.group(1) : null;
	}

	/**
	 * Whether the local mirror holds the untouched upload the conversion needs, probed exactly the way
	 * {@link VideoHlsBackfillBatch} looks for it: the same three containers under {@code original/mp4/}, because
	 * which one a movie was uploaded in is recorded nowhere. Clearing {@code embed_url} without an original here
	 * leaves a movie the backfill cannot build a ladder for.
	 */
	private boolean hasLocalBackup(int id) {
		Path dir = localMediaRoot.resolve(S3KeyGenerator.getOriginalMp4Prefix(id));
		for (StorageType type : StorageType.MOVIE_SOURCE_TYPES) {
			if (Files.isRegularFile(dir.resolve(id + "." + type.getExtension()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Asks the provider about {@code videoId}. A 429 or 5xx is retried after a short pause, because the provider is
	 * telling us to come back later, which is not an answer about the video. Returns {@code null} when every attempt
	 * failed at the transport level.
	 */
	private HttpResponse<String> ask(Provider provider, String videoId) throws InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(provider.oEmbedUrl(videoId)))
				.header("User-Agent", USER_AGENT)
				.timeout(REQUEST_TIMEOUT)
				.GET()
				.build();
		HttpResponse<String> last = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				last = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			} catch (IOException e) {
				logger.debug("oEmbed request for {} {} failed on attempt {}: {}", provider, videoId, attempt, e.getMessage());
				last = null;
			}
			if (last != null && last.statusCode() != 429 && last.statusCode() < 500) {
				return last;
			}
			if (attempt < MAX_ATTEMPTS) {
				Thread.sleep(RETRY_BACKOFF.multipliedBy(attempt));
			}
		}
		return last;
	}

	/**
	 * One embed: asks the provider and, when the answer is a dead video, logs the single line that would move it to
	 * the local copy. The statement comes first so it can be copied as it stands; everything after {@code --} is for
	 * the person reading the log and is thrown away by MySQL.
	 */
	private void check(int id, String embedUrl, Provider provider, String videoId,
			AtomicInteger live, AtomicInteger dead, AtomicInteger unknown) throws InterruptedException {
		HttpResponse<String> response = ask(provider, videoId);
		if (response == null) {
			unknown.incrementAndGet();
			logger.warn("Could not check embedded video id={} ({} {}): the provider did not answer", id, provider, embedUrl);
			return;
		}
		int status = response.statusCode();
		if (status == 200) {
			live.incrementAndGet();
		} else if (provider.meansGone(status)) {
			dead.incrementAndGet();
			logger.warn("UPDATE media SET embed_url=NULL WHERE id={}; -- {} {} (local backup {})",
					id, provider, embedUrl, hasLocalBackup(id) ? "exists" : "does not exist");
		} else {
			unknown.incrementAndGet();
			logger.warn("Could not check embedded video id={} ({} {}): status {}", id, provider, embedUrl, status);
		}
	}

	private void shutdownExecutor() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(4, TimeUnit.HOURS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException _) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected void run() {
		List<EmbeddedVideo> links = mediaService.getEmbeddedVideos();
		AtomicInteger live = new AtomicInteger();
		AtomicInteger dead = new AtomicInteger();
		AtomicInteger unknown = new AtomicInteger();
		int submitted = 0;
		for (EmbeddedVideo link : links) {
			Provider provider = providerOf(link.embedUrl());
			if (provider == null) {
				continue;
			}
			String videoId = videoIdOf(provider, link.embedUrl());
			if (videoId == null) {
				logger.warn("No video id in embedded url, skipping: id={} url={}", link.id(), link.embedUrl());
				continue;
			}
			submitted++;
			executor.submit(() -> {
				try {
					check(link.id(), link.embedUrl(), provider, videoId, live, dead, unknown);
				} catch (InterruptedException _) {
					Thread.currentThread().interrupt();
				} catch (RuntimeException e) {
					unknown.incrementAndGet();
					logger.warn("Could not check embedded video id={} ({}): {}", link.id(), link.embedUrl(), e.getMessage());
				}
			});
		}
		shutdownExecutor();
		logger.info("Embedded video link check: {} checked, {} live, {} dead, {} could not be checked",
				submitted, live.get(), dead.get(), unknown.get());
	}
}

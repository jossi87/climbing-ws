package com.buldreinfo.batch.maintenance;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.buldreinfo.Application;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.service.ImageClassifierService;
import com.buldreinfo.service.ImageService;
import com.buldreinfo.service.MediaService;
import com.buldreinfo.service.VideoService;

public class VardenMaintenanceOrchestrator {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String SSH_HOST = "172.232.129.122";
	private static final String SSH_USER = "root";
	private static final String SSH_KEY_PATH = System.getProperty("user.home") + "/.ssh/id_rsa";
	private static final Path LOCAL_DB_BASE_PATH = Path.of("G:/My Drive/web/climbing-web/database");
	private static final Path LOCAL_INFRA_PATH = Path.of("G:/My Drive/web/varden-infra");
	private static final Path LOCAL_MEDIA_ROOT = Path.of("G:/My Drive/web/climbing-web/s3_bucket_climbing_web");
	private static final Path LOCAL_FFMPEG_BIN_PATH = Path.of("G:/My Drive/web/climbing-web/sw/ffmpeg-9.0.2-essentials_build/bin");
	private static final Path LOCAL_FFMPEG_PATH = LOCAL_FFMPEG_BIN_PATH.resolve("ffmpeg.exe");
	private static final Path LOCAL_FFPROBE_PATH = LOCAL_FFMPEG_BIN_PATH.resolve("ffprobe.exe");
	private static final Path LOCAL_YT_DLP_PATH = Path.of("G:/My Drive/web/climbing-web/sw/yt-dlp/yt-dlp.exe");
	private static final String REMOTE_BACKUP_DIR = "/opt/varden-infra/backups";
	private static final List<Integer> privateEmbeddedVideosToIgnore = List.of(36370, 36374, 36379, 36380, 36381, 36383, 36388);

	public static void main(String[] args) {
		long startedAt = System.nanoTime();
		var context = new SpringApplicationBuilder(Application.class)
				.web(WebApplicationType.NONE)
				.properties("ffmpeg.path=" + LOCAL_FFMPEG_PATH, "ffprobe.path=" + LOCAL_FFPROBE_PATH)
				.run(args);
		var imageService = context.getBean(ImageService.class);
		var imageClassifierService = context.getBean(ImageClassifierService.class);
		var storage = context.getBean(StorageManager.class);
		var mediaService = context.getBean(MediaService.class);
		var videoService = context.getBean(VideoService.class);
		List<Integer> hlsMovieIds = mediaService.getHlsMovieIds();
		for (Path p : List.of(LOCAL_DB_BASE_PATH, LOCAL_INFRA_PATH, LOCAL_MEDIA_ROOT, LOCAL_YT_DLP_PATH)) {
			if (!Files.exists(p)) {
				throw new RuntimeException(p.toString() + " not found");
			}
		}
		runStep("DataSftpDownloadTask", () -> new DataSftpDownloadTask(SSH_HOST, SSH_USER, SSH_KEY_PATH, LOCAL_DB_BASE_PATH, LOCAL_INFRA_PATH, REMOTE_BACKUP_DIR).run());

		runStep("S3BucketDownloadBatch", () -> new S3BucketDownloadBatch(LOCAL_MEDIA_ROOT, storage).run());

		runStep("EmbeddedVideoDownloader", () -> new EmbeddedVideoDownloader(mediaService, imageService, LOCAL_MEDIA_ROOT, LOCAL_FFMPEG_PATH, LOCAL_YT_DLP_PATH, privateEmbeddedVideosToIgnore).run());

		runStep("FixMediaAnalyze", () -> new FixMediaAnalyze(LOCAL_MEDIA_ROOT, imageClassifierService, mediaService).run());

		runStep("VideoHlsBackfillBatch", () -> new VideoHlsBackfillBatch(LOCAL_MEDIA_ROOT, videoService, hlsMovieIds).run());

		runStep("S3BucketUploadBatch", () -> new S3BucketUploadBatch(LOCAL_MEDIA_ROOT, storage).run());

		boolean runS3BucketDeleteResized = false;
		if (runS3BucketDeleteResized) {
			runStep("S3BucketDeleteResized", () -> new S3BucketDeleteResized().run(storage));
		}
		else {
			logger.debug("S3BucketDeleteResized skipped");
		}
		logger.info("VardenMaintenanceOrchestrator finished successfully in {}.", elapsed(startedAt));
	}

	/**
	 * Runs one maintenance step and reports how long it took. The steps range from a few seconds to a couple of hours,
	 * and these are the only numbers that say where a run actually spent its time: each batch reports what it did, not
	 * how long it was at it.
	 */
	private static void runStep(String name, Runnable step) {
		long startedAt = System.nanoTime();
		logger.debug("{} started", name);
		step.run();
		logger.info("{} finished in {}", name, elapsed(startedAt));
	}

	/** Wall-clock time since {@code startNanos}: "12m 31s", or "1h 47m 22s" once a run passes the hour. */
	private static String elapsed(long startNanos) {
		Duration took = Duration.ofNanos(System.nanoTime() - startNanos);
		long hours = took.toHours();
		return hours > 0
				? "%dh %02dm %02ds".formatted(hours, took.toMinutesPart(), took.toSecondsPart())
				: "%dm %02ds".formatted(took.toMinutes(), took.toSecondsPart());
	}
}
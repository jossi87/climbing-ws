package com.buldreinfo.batch.maintenance;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
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
		logger.debug("DataSftpDownloadTask started");
		new DataSftpDownloadTask(SSH_HOST, SSH_USER, SSH_KEY_PATH, LOCAL_DB_BASE_PATH, LOCAL_INFRA_PATH, REMOTE_BACKUP_DIR).run();

		logger.debug("S3BucketDownloadBatch started");
		new S3BucketDownloadBatch(LOCAL_MEDIA_ROOT, storage).run();

		logger.debug("Starting EmbeddedVideoDownloader background embedding sync task.");
		new EmbeddedVideoDownloader(mediaService, imageService, LOCAL_MEDIA_ROOT, LOCAL_FFMPEG_PATH, LOCAL_YT_DLP_PATH, privateEmbeddedVideosToIgnore).run();

		logger.debug("FixMediaAnalyze started");
		new FixMediaAnalyze(LOCAL_MEDIA_ROOT, imageClassifierService, mediaService).run();

		logger.debug("VideoHlsBackfillBatch started");
		new VideoHlsBackfillBatch(LOCAL_MEDIA_ROOT, videoService, hlsMovieIds).run();

		logger.debug("S3BucketUploadBatch started");
		new S3BucketUploadBatch(LOCAL_MEDIA_ROOT, storage).run();

		boolean runS3BucketDeleteResized = false;
		if (runS3BucketDeleteResized) {
			logger.debug("S3BucketDeleteResized started");
			new S3BucketDeleteResized().run(storage);
		}
		else {
			logger.debug("S3BucketDeleteResized skipped");
		}
		logger.info("VardenMaintenanceOrchestrator finished successfully.");
	}
}
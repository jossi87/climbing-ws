package com.buldreinfo.batch.maintenance;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.MediaRepository.EmbeddedVideo;
import com.buldreinfo.service.ImageService;

public class EmbeddedVideoDownloader {
	private static final Logger logger = LogManager.getLogger();
	private final ExecutorService executor = Executors.newFixedThreadPool(12);
	private final Path ffmpegPath;
	private final ImageService imageService;
	private final Path localBucketRoot;
	private final MediaRepository mediaRepo;
	private final List<Integer> privateEmbeddedVideosToIgnore;
	private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());
	private final Path ytDlpPath;

	protected EmbeddedVideoDownloader(MediaRepository mediaRepo, ImageService imageService, Path localBucketRoot, Path ffmpegPath, Path ytDlpPath, List<Integer> privateEmbeddedVideosToIgnore) {
		this.mediaRepo = mediaRepo;
		this.imageService = imageService;
		this.localBucketRoot = localBucketRoot;
		this.ffmpegPath = ffmpegPath;
		this.ytDlpPath = ytDlpPath;
		this.privateEmbeddedVideosToIgnore = privateEmbeddedVideosToIgnore;
	}

	private Path getLocalPath(String s3Key) {
		return localBucketRoot.resolve(s3Key);
	}

	private void processTask(EmbeddedVideo task) throws InterruptedException, IOException {
		int id = task.id();
		StorageType storageType = StorageType.fromExtension(task.suffix()).orElseThrow();
		String embedUrl = task.embedUrl();
		Path originalMp4 = getLocalPath(S3KeyGenerator.getOriginalMp4(id, storageType));
		Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(id));

		if (!privateEmbeddedVideosToIgnore.contains(id)) {
			if (!Files.exists(originalMp4)) {
				logger.info("Downloading embed video with id={} to {}", id, originalMp4);
				Files.createDirectories(originalMp4.getParent());
				String[] commands = {
						ytDlpPath.toString(), 
						"--ffmpeg-location", ffmpegPath.toString(), 
						"--js-runtimes", "node",
						embedUrl, 
						"-S", "ext:mp4:m4a", 
						"--merge-output-format", "mp4", 
						"-o", originalMp4.toString()
				};
				int exitCode = new ProcessBuilder()
						.command(commands)
						.redirectErrorStream(true)
						.redirectOutput(ProcessBuilder.Redirect.INHERIT)
						.start()
						.waitFor();
				if (exitCode != 0 || !Files.exists(originalMp4)) {
					warnings.add("Failed to download embedded video with id=" + id + " (exit code: " + exitCode + ") to originalMp4=" + originalMp4 + " from " + embedUrl);
				}
			}
		}

		if (!Files.exists(originalJpg)) {
			try {
				BufferedImage thumb = imageService.readFromEmbedUrl(embedUrl);
				try {
					imageService.saveImage(id, thumb);
				} finally {
					thumb.flush();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (!Files.exists(originalJpg) && !privateEmbeddedVideosToIgnore.contains(id)) {
			warnings.add("Failed to download embedded video thumbnail with id=" + id + " to originalJpg=" + originalJpg);
		}
	}

	protected void run() {
		for (var task : mediaRepo.getEmbeddedVideos()) {
			executor.submit(() -> {
				try {
					processTask(task);
				} catch (Exception e) {
					warnings.add("Error processing id=" + task.id() + ": " + e.getMessage());
				}
			});
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			logger.error("Executor interrupted", e);
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		Collections.sort(warnings);
		for (String w : warnings) {
			logger.warn(w);
		}
		logger.debug("FixMedia processing completed.");
	}
}
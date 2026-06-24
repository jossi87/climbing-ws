package com.buldreinfo.batch.maintenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.service.ImageService;

public class FixMedia {
	private record MediaTask(int id, String embedUrl) {}
	private static final Logger logger = LogManager.getLogger();
	private final ImageService imageService;
	private final ClimbingTransactionManager txManager;
	private final Path localBucketRoot;
	private final Path ffmpegPath;
	private final Path ytDlpPath;
	private final List<Integer> privateEmbeddedVideosToIgnore;
	private final ExecutorService executor = Executors.newFixedThreadPool(12);
	private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

	protected FixMedia(ImageService imageService, ClimbingTransactionManager txManager, Path localBucketRoot, Path ffmpegPath, Path ytDlpPath, List<Integer> privateEmbeddedVideosToIgnore) {
		this.imageService = imageService;
		this.txManager = txManager;
		this.localBucketRoot = localBucketRoot;
		this.ffmpegPath = ffmpegPath;
		this.ytDlpPath = ytDlpPath;
		this.privateEmbeddedVideosToIgnore = privateEmbeddedVideosToIgnore;
	}

	private Path getLocalPath(String s3Key) {
		return localBucketRoot.resolve(s3Key);
	}

	private void processTask(MediaTask task) throws Exception {
		int id = task.id();
		String embedUrl = task.embedUrl();
		Path originalMp4 = getLocalPath(S3KeyGenerator.getOriginalMp4(id));
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
			txManager.executeInTransaction(() -> {
                try {
                    imageService.saveImageFromEmbedVideo(id, embedUrl);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
		}

		if (!Files.exists(originalJpg) && !privateEmbeddedVideosToIgnore.contains(id)) {
			warnings.add("Failed to download embedded video thumbnail with id=" + id + " to originalJpg=" + originalJpg);
		}
	}

	protected void run() throws Exception {
		List<MediaTask> tasks = new ArrayList<>();
		txManager.executeInTransaction(() -> {
			String sqlStr = "SELECT id, embed_url FROM media WHERE is_movie=1 AND embed_url IS NOT NULL";
			var c = txManager.getConnection();
			try (PreparedStatement ps = c.prepareStatement(sqlStr);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					tasks.add(new MediaTask(rst.getInt("id"), rst.getString("embed_url")));
				}
			} catch (SQLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		});
		for (MediaTask task : tasks) {
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
package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.google.common.base.Preconditions;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3BucketDownloadBatch {
	private static final Logger logger = LogManager.getLogger();
	private static final Path LOCAL_MEDIA_ROOT = Path.of("G:/My Drive/web/climbing-web/s3_bucket_climbing_web");
	public static void main(String[] args) {
		new S3BucketDownloadBatch().run();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicInteger skipCount = new AtomicInteger(0);
	private final AtomicInteger downloadCount = new AtomicInteger(0);
	private final AtomicLong totalBytesDownloaded = new AtomicLong(0);

	private void run() {
		Preconditions.checkArgument(Files.exists(LOCAL_MEDIA_ROOT), LOCAL_MEDIA_ROOT.toString() + " does not exist");
		StorageManager storage = StorageManager.getInstance();
		logger.info("Starting downloading from bucket [{}] to local directory {}", StorageManager.BUCKET_NAME, LOCAL_MEDIA_ROOT);
		try {
			ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(StorageManager.BUCKET_NAME).build();
			for (ListObjectsV2Response page : storage.getS3Client().listObjectsV2Paginator(listRequest)) {
				for (S3Object s3Object : page.contents()) {
					String key = s3Object.key();
					if (key.startsWith("web/jpg_resized/") || key.startsWith("web/webp_resized/")) {
						continue; 
					}
					executor.submit(() -> syncFile(storage, s3Object));
				}
			}
		} catch (Exception e) {
			logger.error("Error while listing bucket contents: " + e.getMessage(), e);
		} finally {
			shutdownExecutor();
		}
		logger.info("Download complete! Status: [Downloaded: {} files ({} GB)] [Skipped: {} files]", downloadCount.get(), String.format("%.2f", totalBytesDownloaded.get() / (1024.0 * 1024.0 * 1024.0)), skipCount.get());
	}

	private void shutdownExecutor() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException _) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private void syncFile(StorageManager storage, S3Object s3Object) {
		try {
			String key = s3Object.key();
			Path localPath = LOCAL_MEDIA_ROOT.resolve(key);
			long s3Size = s3Object.size();
			if (Files.exists(localPath)) {
				long localSize = Files.size(localPath);
				if (s3Size == 0) {
					logger.warn("SAFETY TRIGGERED: Cloud file is 0 bytes, but local file is {} bytes. Skipping download to prevent data loss: {}", localSize, key);
					skipCount.incrementAndGet();
					return;
				}
				if (localSize == s3Size) {
					int currentSkips = skipCount.incrementAndGet();
					if (currentSkips % 10_000 == 0) {
						logger.info("Sync Progress: {} files verified and skipped", currentSkips);
					}
					return; 
				}
				logger.info("Size mismatch detected (S3: {} vs Local: {}) for: {}", s3Size, localSize, key);
			}
			else {
				Files.createDirectories(localPath.getParent());
			}
			storage.downloadFile(key, localPath);
			totalBytesDownloaded.addAndGet(s3Size);
			int currentDownloads = downloadCount.incrementAndGet();
			logger.info("Download Progress: {} files. Current: {} ({} MB)", currentDownloads, key, s3Size / (1024 * 1024));
		} catch (Exception e) {
			logger.error("Failed to sync {}: {}", s3Object.key(), e.getMessage());
		}
	}
}
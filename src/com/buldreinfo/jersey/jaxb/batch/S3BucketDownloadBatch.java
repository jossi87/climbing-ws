package com.buldreinfo.jersey.jaxb.batch;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.io.StorageManager;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3BucketDownloadBatch {
	private static final Logger logger = LogManager.getLogger();
	private static final String LOCAL_MEDIA_ROOT = "G:/My Drive/web/buldreinfo/s3_bucket_climbing_web";
	public static void main(String[] args) {
		new S3BucketDownloadBatch().run();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicInteger skipCount = new AtomicInteger(0);
	private final AtomicInteger downloadCount = new AtomicInteger(0);
	private final AtomicLong totalBytesDownloaded = new AtomicLong(0);

	private void run() {
		StorageManager storage = StorageManager.getInstance();
		logger.info("Starting downloading from bucket [{}] to local directory {}", StorageManager.BUCKET_NAME, LOCAL_MEDIA_ROOT);
		try {
			ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(StorageManager.BUCKET_NAME).build();
			for (ListObjectsV2Response page : storage.getS3Client().listObjectsV2Paginator(listRequest)) {
				for (S3Object s3Object : page.contents()) {
					String key = s3Object.key();
					if (key.startsWith("web/jpg_resized/")) {
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
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

	private void syncFile(StorageManager storage, S3Object s3Object) {
		try {
			String key = s3Object.key();
			Path localPath = Paths.get(LOCAL_MEDIA_ROOT, key);
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
						logger.info("Sync Progress: {} files already verified and skipped", currentSkips);
					}
					return; 
				}
				logger.info("Size mismatch detected (S3: {} vs Local: {}) for: {}", s3Size, localSize, key);
			}
			else {
				Files.createDirectories(localPath.getParent());
				logger.info("New file found in cloud: {}", key);
			}
			try (InputStream in = storage.getInputStream(key)) {
				Files.copy(in, localPath, StandardCopyOption.REPLACE_EXISTING);
				totalBytesDownloaded.addAndGet(s3Size);
				int currentDownloads = downloadCount.incrementAndGet();
				if (currentDownloads % 100 == 0 || s3Size > 50 * 1024 * 1024) {
					logger.info("Download Progress: {} files. Current: {} ({} MB)", 
							currentDownloads, key, s3Size / (1024 * 1024));
				}
			}
		} catch (Exception e) {
			logger.error("Failed to sync {}: {}", s3Object.key(), e.getMessage());
		}
	}
}
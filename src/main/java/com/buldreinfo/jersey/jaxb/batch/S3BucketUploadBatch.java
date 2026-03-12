package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.google.common.base.Preconditions;

import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class S3BucketUploadBatch {
	private static final Logger logger = LogManager.getLogger();
	private static final Path LOCAL_MEDIA_ROOT = Path.of("G:/My Drive/web/climbing-web/s3_bucket_climbing_web");
	public static void main(String[] args) {
		new S3BucketUploadBatch().run();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicInteger uploadCount = new AtomicInteger(0);
	private final AtomicInteger skipCount = new AtomicInteger(0);
	private final AtomicLong totalBytesUploaded = new AtomicLong(0);

	public void run() {
		Preconditions.checkArgument(Files.exists(LOCAL_MEDIA_ROOT), LOCAL_MEDIA_ROOT.toString() + " does not exist");
		StorageManager storage = StorageManager.getInstance();
		logger.info("Starting uploading from local directory {} to bucket [{}]", LOCAL_MEDIA_ROOT, StorageManager.BUCKET_NAME);
		try {
			try (var stream = Files.walk(LOCAL_MEDIA_ROOT)) {
				stream.filter(Files::isRegularFile)
				.filter(this::isNotSystemFile)
				.forEach(path -> {
					executor.submit(() -> uploadFile(storage, LOCAL_MEDIA_ROOT, path));
				});
			}
		} catch (IOException e) {
			logger.error("Failed to walk directory: " + e.getMessage(), e);
		} finally {
			shutdownExecutor();
		}
		logger.info("Upload complete! Status: [Uploaded: {} files ({} GB)] [Skipped: {} files]", uploadCount.get(), String.format("%.2f", totalBytesUploaded.get() / (1024.0 * 1024.0 * 1024.0)), skipCount.get());
	}

	private boolean isNotSystemFile(Path path) {
		String name = path.getFileName().toString();
		return !name.equalsIgnoreCase("desktop.ini") && !name.equals(".DS_Store") && !name.startsWith("~$");
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

	private void uploadFile(StorageManager storage, Path root, Path file) {
		try {
			String relativePath = root.relativize(file).toString().replace("\\", "/");
			long localSize = Files.size(file);
			if (localSize == 0) {
				logger.warn("SAFETY TRIGGERED: Local file is 0 bytes. Skipping upload to prevent cloud corruption: {}", relativePath);
				skipCount.incrementAndGet();
				return;
			}
			StorageType type = StorageType.fromFilename(relativePath).orElseThrow(() -> new IllegalArgumentException("Invalid fileName: " + relativePath));
			boolean shouldUpload = true;
			try {
				HeadObjectResponse remoteMeta = storage.getS3Client().headObject(HeadObjectRequest.builder()
						.bucket(StorageManager.BUCKET_NAME)
						.key(relativePath)
						.build());
				boolean sameSize = remoteMeta.contentLength() == localSize;
				boolean sameMime = remoteMeta.contentType().equalsIgnoreCase(type.getMimeType());
				if (sameSize && sameMime) {
					shouldUpload = false;
					int currentSkips = skipCount.incrementAndGet();
					if (currentSkips % 10_000 == 0) {
						logger.info("Sync Progress: {} files verified and skipped", currentSkips);
					}
				} else {
					if (!sameMime) {
						logger.warn("Fixing MimeType mismatch for {}: Cloud={}, Correct={}", relativePath, remoteMeta.contentType(), type.getMimeType());
					} else {
						logger.info("Updating {} due to size change.", relativePath);
					}
				}
			} catch (NoSuchKeyException _) {
				// Not in bucket yet
			}
			if (!shouldUpload) {
				return;
			}
			storage.uploadFile(relativePath, file, type);
			totalBytesUploaded.addAndGet(localSize);
			int currentUploads = uploadCount.incrementAndGet();
			logger.info("Upload Progress: {} files. Current: {} ({} MB) Type: {}", currentUploads, relativePath, localSize / (1024 * 1024), type.getMimeType());
		} catch (Exception e) {
			logger.error("Failed to process {}: {}", file, e.getMessage());
		}
	}
}
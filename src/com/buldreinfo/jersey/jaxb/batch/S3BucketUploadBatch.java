package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.io.StorageManager;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3BucketUploadBatch {
	private static final Logger logger = LogManager.getLogger();
	private static final String LOCAL_MEDIA_ROOT = "G:/My Drive/web/buldreinfo/s3_bucket_climbing_web";
	public static void main(String[] args) {
		new S3BucketUploadBatch().run();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicInteger uploadCount = new AtomicInteger(0);
	private final AtomicInteger skipCount = new AtomicInteger(0);
	private final AtomicLong totalBytesUploaded = new AtomicLong(0);

	public void run() {
		Path root = Paths.get(LOCAL_MEDIA_ROOT);
		StorageManager storage = StorageManager.getInstance();
		logger.info("Starting uploading from local directory {} to bucket [{}]", LOCAL_MEDIA_ROOT, StorageManager.BUCKET_NAME);
		try {
			try (var stream = Files.walk(root)) {
				stream.filter(Files::isRegularFile)
				.filter(this::isNotSystemFile)
				.forEach(path -> {
					executor.submit(() -> uploadFile(storage, root, path));
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
		} catch (InterruptedException e) {
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
			String ext = com.google.common.io.Files.getFileExtension(relativePath).toLowerCase();
			String mimeType = StorageType.fromExtension(ext)
					.map(StorageType::getMimeType)
					.orElseGet(() -> {
						try {
							String probed = Files.probeContentType(file);
							return (probed != null) ? probed : "application/octet-stream";
						} catch (IOException e) {
							return "application/octet-stream";
						}
					});
			boolean shouldUpload = true;
			try {
				HeadObjectResponse remoteMeta = storage.getS3Client().headObject(HeadObjectRequest.builder()
						.bucket(StorageManager.BUCKET_NAME)
						.key(relativePath)
						.build());
				boolean sameSize = remoteMeta.contentLength() == localSize;
				boolean sameMime = remoteMeta.contentType().equalsIgnoreCase(mimeType);
				if (sameSize && sameMime) {
					shouldUpload = false;
					int currentSkips = skipCount.incrementAndGet();
					if (currentSkips % 10_000 == 0) {
						logger.info("Sync Progress: {} files verified and skipped", currentSkips);
					}
				} else {
					if (!sameMime) {
						logger.warn("Fixing MimeType mismatch for {}: Cloud={}, Correct={}", relativePath, remoteMeta.contentType(), mimeType);
					} else {
						logger.info("Updating {} due to size change.", relativePath);
					}
				}
			} catch (NoSuchKeyException e) {
				// Not in bucket yet
			}
			if (!shouldUpload) {
				return;
			}
			PutObjectRequest putRequest = PutObjectRequest.builder()
					.bucket(StorageManager.BUCKET_NAME)
					.key(relativePath)
					.contentType(mimeType)
					.acl(ObjectCannedACL.PUBLIC_READ)
					.build();
			storage.getS3Client().putObject(putRequest, RequestBody.fromFile(file));
			totalBytesUploaded.addAndGet(localSize);
			int currentUploads = uploadCount.incrementAndGet();
			logger.info("Upload Progress: {} files. Current: {} ({} MB) Type: {}", currentUploads, relativePath, localSize / (1024 * 1024), mimeType);
		} catch (Exception e) {
			logger.error("Failed to process {}: {}", file, e.getMessage());
		}
	}
}
package com.buldreinfo.batch.maintenance;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buldreinfo.io.StorageManager;

import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Removes the legacy video derivatives the old pipeline produced: the WebM transcodes ({@code web/webm/}) and the
 * single 1080p MP4 transcodes ({@code web/mp4/}). Both were replaced by the adaptive HLS ladder ({@code web/hls/}).
 * The originals ({@code original/}) are intentionally left untouched. Intended to run after the HLS backfill has
 * been generated and uploaded, so it is gated by a flag in the orchestrator.
 */
public class S3BucketDeleteLegacyVideoBatch {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final List<String> LEGACY_PREFIXES = List.of("web/webm/", "web/mp4/");
    private final AtomicInteger deleteCount = new AtomicInteger(0);

    private void deleteBatch(StorageManager storage, List<ObjectIdentifier> objects) {
        try {
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(StorageManager.BUCKET_NAME)
                    .delete(Delete.builder().objects(objects).build())
                    .build();
            storage.getS3Client().deleteObjects(deleteRequest);
            int currentTotal = deleteCount.addAndGet(objects.size());
            logger.info("Deleted batch of {} files. Total so far: {}", objects.size(), currentTotal);
        } catch (Exception e) {
            logger.error("Failed to delete batch: {}", e.getMessage());
        }
    }

    protected void run(StorageManager storage) {
        logger.info("Starting cleanup of legacy video derivatives in bucket [{}]", StorageManager.BUCKET_NAME);
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(StorageManager.BUCKET_NAME)
                    .build();
            List<ObjectIdentifier> batch = new ArrayList<>();
            for (ListObjectsV2Response page : storage.getS3Client().listObjectsV2Paginator(listRequest)) {
                for (S3Object s3Object : page.contents()) {
                    String key = s3Object.key();
                    if (LEGACY_PREFIXES.stream().anyMatch(key::startsWith)) {
                        batch.add(ObjectIdentifier.builder().key(key).build());
                        if (batch.size() >= 1000) {
                            deleteBatch(storage, batch);
                            batch.clear();
                        }
                    }
                }
            }
            if (!batch.isEmpty()) {
                deleteBatch(storage, batch);
            }
            logger.info("Cleanup complete! Total legacy files deleted: {}", deleteCount.get());
        } catch (Exception e) {
            logger.error("Error while cleaning up bucket: " + e.getMessage(), e);
        }
    }
}

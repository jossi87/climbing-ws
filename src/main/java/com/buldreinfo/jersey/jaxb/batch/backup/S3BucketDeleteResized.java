package com.buldreinfo.jersey.jaxb.batch.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.io.StorageManager;

import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3BucketDeleteResized {
    private static final Logger logger = LogManager.getLogger();
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

    protected void run() {
        StorageManager storage = StorageManager.getInstance();
        logger.info("Starting cleanup of resized images in bucket [{}]", StorageManager.BUCKET_NAME);
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(StorageManager.BUCKET_NAME)
                    .build();
            List<ObjectIdentifier> batch = new ArrayList<>();
            for (ListObjectsV2Response page : storage.getS3Client().listObjectsV2Paginator(listRequest)) {
                for (S3Object s3Object : page.contents()) {
                    String key = s3Object.key();
                    if (key.startsWith("web/jpg_resized/") || key.startsWith("web/webp_resized/")) {
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
            logger.info("Cleanup complete! Total files deleted: {}", deleteCount.get());
        } catch (Exception e) {
            logger.error("Error while cleaning up bucket: " + e.getMessage(), e);
        }
    }
}
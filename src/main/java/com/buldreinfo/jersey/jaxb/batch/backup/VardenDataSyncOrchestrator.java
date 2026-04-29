package com.buldreinfo.jersey.jaxb.batch.backup;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VardenDataSyncOrchestrator {
    private static final Logger logger = LogManager.getLogger();
    private static final String SSH_HOST = "172.232.129.122";
    private static final String SSH_USER = "root";
    private static final String SSH_KEY_PATH = System.getProperty("user.home") + "/.ssh/id_rsa";
    private static final Path DB_BASE_PATH = Path.of("G:/My Drive/web/climbing-web/database");
    private static final Path INFRA_PATH = Path.of("G:/My Drive/web/varden-infra");
    private static final String REMOTE_BACKUP_DIR = "/opt/varden-infra/backups";
    private static final Path LOCAL_MEDIA_ROOT = Path.of("G:/My Drive/web/climbing-web/s3_bucket_climbing_web");

    public static void main(String[] args) {
    	if (!Files.exists(DB_BASE_PATH)) {
    	    throw new RuntimeException(DB_BASE_PATH.toString() + " not found");
    	}
    	if (!Files.exists(INFRA_PATH)) {
    	    throw new RuntimeException(INFRA_PATH.toString() + " not found");
    	}
    	if (!Files.exists(LOCAL_MEDIA_ROOT)) {
    	    throw new RuntimeException(LOCAL_MEDIA_ROOT.toString() + " not found");
    	}
    	if (!Files.exists(Path.of(SSH_KEY_PATH))) {
    	    throw new RuntimeException(SSH_KEY_PATH.toString() + " not found");
    	}
        logger.debug("DataSftpDownloadTask started");
        new DataSftpDownloadTask(SSH_HOST, SSH_USER, SSH_KEY_PATH, DB_BASE_PATH, INFRA_PATH, REMOTE_BACKUP_DIR).run();
        logger.debug("S3BucketDownloadBatch started");
        new S3BucketDownloadBatch(LOCAL_MEDIA_ROOT).run();
        logger.debug("S3BucketUploadBatch started");
        new S3BucketUploadBatch(LOCAL_MEDIA_ROOT).run();
        boolean runS3BucketDeleteResized = false;
        if (runS3BucketDeleteResized) {
            logger.debug("S3BucketDeleteResized started");
            new S3BucketDeleteResized().run();
        }
        logger.info("VardenDataSyncOrchestrator finished successfully.");
    }
}
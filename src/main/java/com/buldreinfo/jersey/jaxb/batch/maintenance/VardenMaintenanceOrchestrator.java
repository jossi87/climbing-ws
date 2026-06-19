package com.buldreinfo.jersey.jaxb.batch.maintenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.batch.BatchBootstrapper;
import com.buldreinfo.jersey.jaxb.dao.MediaRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;

public class VardenMaintenanceOrchestrator {
    private static final Logger logger = LogManager.getLogger();
    private static final String SSH_HOST = "172.232.129.122";
    private static final String SSH_USER = "root";
    private static final String SSH_KEY_PATH = System.getProperty("user.home") + "/.ssh/id_rsa";
    private static final Path LOCAL_DB_BASE_PATH = Path.of("G:/My Drive/web/climbing-web/database");
    private static final Path LOCAL_INFRA_PATH = Path.of("G:/My Drive/web/varden-infra");
    private static final Path LOCAL_MEDIA_ROOT = Path.of("G:/My Drive/web/climbing-web/s3_bucket_climbing_web");
    private static final Path LOCAL_FFMPEG_PATH = Path.of("G:/My Drive/web/climbing-web/sw/ffmpeg-master-latest-win64-gpl-shared/bin/ffmpeg.exe");
    private static final Path LOCAL_YT_DLP_PATH = Path.of("G:/My Drive/web/climbing-web/sw/yt-dlp/yt-dlp.exe");
    private static final String REMOTE_BACKUP_DIR = "/opt/varden-infra/backups";
    private static final List<Integer> privateEmbeddedVideosToIgnore = List.of(36370, 36374, 36379, 36380, 36381, 36383, 36388);

    public static void main(String[] args) throws Exception {
    	var locator = BatchBootstrapper.createLocator();
        var txManager = locator.getService(TransactionManager.class);
        var mediaRepo = locator.getService(MediaRepository.class);
        for (Path p : List.of(LOCAL_DB_BASE_PATH, LOCAL_INFRA_PATH, LOCAL_MEDIA_ROOT, LOCAL_FFMPEG_PATH, LOCAL_YT_DLP_PATH)) {
            if (!Files.exists(p)) {
                throw new RuntimeException(p.toString() + " not found");
            }
        }
        logger.debug("DataSftpDownloadTask started");
        new DataSftpDownloadTask(SSH_HOST, SSH_USER, SSH_KEY_PATH, LOCAL_DB_BASE_PATH, LOCAL_INFRA_PATH, REMOTE_BACKUP_DIR).run();
        
        logger.debug("S3BucketDownloadBatch started");
        new S3BucketDownloadBatch(LOCAL_MEDIA_ROOT).run();
        
        logger.debug("Starting FixMedia background embedding sync task.");
        new FixMedia(txManager, mediaRepo, LOCAL_MEDIA_ROOT, LOCAL_FFMPEG_PATH, LOCAL_YT_DLP_PATH, privateEmbeddedVideosToIgnore).run();
        
        logger.debug("FixMediaAnalyze started");
        new FixMediaAnalyze(LOCAL_MEDIA_ROOT, txManager, mediaRepo).run();
        
        logger.debug("S3BucketUploadBatch started");
        new S3BucketUploadBatch(LOCAL_MEDIA_ROOT).run();
        
        boolean runS3BucketDeleteResized = false;
        if (runS3BucketDeleteResized) {
            logger.debug("S3BucketDeleteResized started");
            new S3BucketDeleteResized().run();
        }
        else {
            logger.debug("S3BucketDeleteResized skipped");
        }
        logger.info("VardenMaintenanceOrchestrator finished successfully.");
    }
}
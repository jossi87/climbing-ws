package com.buldreinfo.batch.maintenance;

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
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.MediaRepository.MediaPendingAnalysis;
import com.buldreinfo.service.ImageClassifierService;

public class FixMediaAnalyze {
	private final ImageClassifierService imageClassifierService;
    private final MediaRepository mediaRepo;
    private final Path localBucketRoot;
    private static final Logger logger = LogManager.getLogger();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

    public FixMediaAnalyze(Path localBucketRoot, ImageClassifierService imageClassifierService, MediaRepository mediaRepo) {
    	this.localBucketRoot = localBucketRoot;
    	this.imageClassifierService = imageClassifierService;
        this.mediaRepo = mediaRepo;
    }

    public void run() {
        List<MediaPendingAnalysis> tasks = mediaRepo.getMediaPendingAnalysis();
        logger.debug("Run MediaAnalyze on {} items", tasks.size());
        for (MediaPendingAnalysis t : tasks) {
            executor.submit(() -> {
                try {
                    processTask(t);
                } catch (Exception e) {
                    warnings.add("Error processing task=" + t + ": " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("Executor interrupted", e);
            Thread.currentThread().interrupt();
        }
        for (String w : warnings) {
            logger.warn(w);
        }
        logger.debug("Updating cache columns...");
        mediaRepo.updateMediaFocusAndActionStatus();
        logger.debug("Done");
    }
    
    public void processTask(MediaPendingAnalysis t) {
        try {
            Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(t.id()));
            var result = imageClassifierService.analyze(Files.readAllBytes(originalJpg));
                try {
                    mediaRepo.saveMediaAnalysis(t.id(), t.width(), t.height(), result.hexColor(), result.labels(), result.objects(), false);
                } catch (Exception e) {
                    warnings.add("Failed to save media id=" + t.id() + ": " + e.getMessage());
                }
        } catch (Exception e) {
            warnings.add("Failed to process/analyze media id=" + t.id() + ": " + e.getMessage());
                try {
                    mediaRepo.saveMediaAnalysis(t.id(), t.width(), t.height(), null, null, null, true);
                } catch (Exception dbEx) {
                    logger.error("Could not save failure state for id=" + t.id(), dbEx);
                }
        }
    }

    private Path getLocalPath(String s3Key) {
        return localBucketRoot.resolve(s3Key);
    }
}
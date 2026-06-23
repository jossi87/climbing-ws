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
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.service.ImageClassifierService;

public class FixMediaAnalyze {
	private final ImageClassifierService imageClassifierService;
	private final ClimbingTransactionManager txManager;
    private final MediaRepository mediaRepo;
    private final Path localBucketRoot;
    private record Task(int id, int width, int height) {}
    private static final Logger logger = LogManager.getLogger();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

    public FixMediaAnalyze(Path localBucketRoot, ImageClassifierService imageClassifierService, ClimbingTransactionManager txManager, MediaRepository mediaRepo) {
    	this.localBucketRoot = localBucketRoot;
    	this.imageClassifierService = imageClassifierService;
    	this.txManager = txManager;
        this.mediaRepo = mediaRepo;
    }

    public void run() throws Exception {
        List<Task> tasks = new ArrayList<>();
        txManager.executeInTransaction(() -> {
        	var c = txManager.getConnection();
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT id, width, height
                    FROM media m
                    WHERE NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
                    GROUP BY id, width, height
                    """);
                    ResultSet rst = ps.executeQuery()) {
                while (rst.next()) {
                    int id = rst.getInt("id");
                    int width = rst.getInt("width");
                    int height = rst.getInt("height");
                    tasks.add(new Task(id, width, height));
                }
            } catch (SQLException e) {
            	throw new RuntimeException(e.getMessage(), e);
            }
        });
        logger.debug("Run MediaAnalyze on {} items", tasks.size());
        for (Task t : tasks) {
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
        txManager.executeInTransaction(() -> {
        	var c = txManager.getConnection();
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE media_ml_analysis mla
                    JOIN media m ON mla.media_id = m.id
                    LEFT JOIN (
                        SELECT media_id, x_min, x_max, y_min, y_max
                        FROM (
                            SELECT media_id, x_min, x_max, y_min, y_max,
                                   ROW_NUMBER() OVER (PARTITION BY media_id ORDER BY y_min ASC) rn
                            FROM media_ml_object
                            WHERE name = 'Person'
                        ) t
                        WHERE rn = 1
                    ) best_person ON mla.media_id = best_person.media_id
                    SET 
                        mla.focus_x = CASE 
                            WHEN best_person.y_min IS NOT NULL THEN ROUND(((best_person.x_min + best_person.x_max) / 2) * 100)
                            ELSE mla.focus_x
                        END,
                                             
                        mla.focus_y = CASE 
                            WHEN best_person.y_min IS NOT NULL THEN 
                                CASE 
                                    WHEN m.height > m.width THEN 
                                        CASE 
                                            WHEN best_person.y_max > 0.80 AND (best_person.y_max - best_person.y_min) < 0.60 
                                                THEN ROUND(best_person.y_max * 100)
                                            ELSE ROUND((best_person.y_min + (best_person.y_max - best_person.y_min) * 0.85) * 100)
                                        END
                                    ELSE ROUND(((best_person.y_min + best_person.y_max) / 2) * 100)
                                END
                            ELSE mla.focus_y
                        END,
                    
                        mla.is_action_shot = IF(best_person.y_min IS NOT NULL, 1, 0)
                    WHERE mla.media_id > 0;
                    """)) {
                ps.execute();
            } catch (SQLException e) {
            	throw new RuntimeException(e.getMessage(), e);
            }
        });
        logger.debug("Done");
    }
    
    public void processTask(Task t) throws Exception {
        try {
            Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(t.id));
            var result = imageClassifierService.analyze(Files.readAllBytes(originalJpg));
            txManager.executeInTransaction(() -> {
                try {
                    mediaRepo.saveMediaAnalysis(t.id, t.width, t.height, result.hexColor(), result.labels(), result.objects(), false);
                } catch (Exception e) {
                    warnings.add("Failed to save media id=" + t.id + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            warnings.add("Failed to process/analyze media id=" + t.id + ": " + e.getMessage());
            txManager.executeInTransaction(() -> {
                try {
                    mediaRepo.saveMediaAnalysis(t.id, t.width, t.height, null, null, null, true);
                } catch (Exception dbEx) {
                    logger.error("Could not save failure state for id=" + t.id, dbEx);
                }
            });
        }
    }

    private Path getLocalPath(String s3Key) {
        return localBucketRoot.resolve(s3Key);
    }
}
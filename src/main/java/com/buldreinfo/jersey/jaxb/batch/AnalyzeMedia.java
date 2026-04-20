package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.helpers.ImageClassifier;

public class AnalyzeMedia {
	private final static String LOCAL_BUCKET_ROOT = "G:/My Drive/web/climbing-web/s3_bucket_climbing_web";
	private record Task(int id, int width, int height) {}
	private static Logger logger = LogManager.getLogger();
	public static void main(String[] args) {
		new AnalyzeMedia();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(8);
	private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

	public AnalyzeMedia() {
		List<Task> tasks = new ArrayList<>();
		Server.runSql((_, c) -> {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT id, width, height
					FROM (SELECT m.id, m.width, m.height
					      FROM problem p
					      JOIN tick t ON p.id=t.problem_id AND t.stars=2
					      JOIN media_problem mp ON p.id=mp.problem_id
					      JOIN media m ON mp.media_id=m.id AND m.deleted_timestamp IS NULL
					        AND NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
					
					      UNION
					
					      SELECT m.id, m.width, m.height
					      FROM area a
					      JOIN media_area ma ON a.id=ma.area_id
					      JOIN media m ON ma.media_id=m.id AND m.deleted_timestamp IS NULL
					        AND NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
					
					      UNION
					
					      SELECT m.id, m.width, m.height
					      FROM sector s
					      JOIN media_sector ms ON s.id=ms.sector_id
					      JOIN media m ON ms.media_id=m.id AND m.deleted_timestamp IS NULL
					        AND NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
					) x
					GROUP BY id, width, height
					""");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					tasks.add(new Task(id, width, height));
				}
			}
		});
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
		logger.debug("Updating cache columns...");
		Server.runSql((_, c) -> {
			try (PreparedStatement ps = c.prepareStatement("""
					UPDATE media_ml_analysis mla
					JOIN media m ON mla.media_id = m.id
					LEFT JOIN (
					    SELECT media_id, x_min, x_max, y_min, y_max
					    FROM (
					        SELECT media_id, x_min, x_max, y_min, y_max,
					               -- Order by y_min to find the climber highest up the wall
					               ROW_NUMBER() OVER (PARTITION BY media_id ORDER BY y_min ASC) rn
					        FROM media_ml_object
					        WHERE name = 'Person'
					    ) t
					    WHERE rn = 1
					) best_person ON mla.media_id = best_person.media_id
					SET 
					    -- Update focus only if AI found a person
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
					
					    -- Action shot now strictly depends on AI person detection
					    mla.is_action_shot = IF(best_person.y_min IS NOT NULL, 1, 0)
					WHERE mla.media_id > 0;
					""")) {
				ps.execute();
			}
		});
		for (String w : warnings) {
			logger.warn(w);
		}
		logger.debug("Done");
	}
	
	public void processTask(Task t) {
	    try {
	        Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(t.id));
	        var result = ImageClassifier.analyze(Files.readAllBytes(originalJpg));
	        Server.runSql((dao, c) -> {
	            try {
	                dao.saveMediaAnalysis(c, t.id, t.width, t.height, result.hexColor(), result.labels(), result.objects(), false);
	            } catch (Exception e) {
	                warnings.add("Failed to save media id=" + t.id + ": " + e.getMessage());
	            }
	        });
	    } catch (Exception e) {
	        warnings.add("Failed to process/analyze media id=" + t.id + ": " + e.getMessage());
	        Server.runSql((dao, c) -> {
	            try {
	                dao.saveMediaAnalysis(c, t.id, t.width, t.height, null, null, null, true);
	            } catch (Exception dbEx) {
	                logger.error("Could not save failure state for id=" + t.id, dbEx);
	            }
	        });
	    }
	}

	private Path getLocalPath(String s3Key) {
		return Paths.get(LOCAL_BUCKET_ROOT, s3Key);
	}
}
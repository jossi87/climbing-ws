package com.buldreinfo.jersey.jaxb.batch;

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

/**
 * TODO: Schedule this automatically, it should run on newer images automatic. We should also process the remaining images (remove conditions).
 */
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
					SELECT m.id, m.image_width, m.image_height
					FROM problem p
					JOIN tick t ON p.id=t.problem_id AND t.stars=3
					JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
					JOIN media m ON mp.media_id=m.id AND m.deleted_timestamp IS NULL
					  AND NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
					GROUP BY m.id, m.image_width, m.image_height
					""");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int imageWidth = rst.getInt("image_width");
					int imageHeight = rst.getInt("image_height");
					tasks.add(new Task(id, imageWidth, imageHeight));
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
					JOIN media m ON mla.media_id=m.id
					JOIN (SELECT media_id, x_min, x_max, y_min, y_max
					      FROM (SELECT media_id, x_min, x_max, y_min, y_max,
					                   ROW_NUMBER() OVER (PARTITION BY media_id ORDER BY score DESC) rn
					            FROM media_ml_object
					            WHERE name='Person'
					           ) t
					      WHERE rn=1
					) best_person ON mla.media_id=best_person.media_id
					SET -- X centered on the person
					    mla.focus_x=ROUND(((best_person.x_min+best_person.x_max)/2)*100),
					    
					    -- Y logic: Elastic focus for portrait images
					    mla.focus_y=CASE 
					      WHEN m.height > m.width THEN 
					        CASE 
					          WHEN best_person.y_max > 0.80 AND (best_person.y_max - best_person.y_min) < 0.60 
					            THEN ROUND(best_person.y_max*100)
					          ELSE ROUND((best_person.y_min+(best_person.y_max - best_person.y_min)*0.85)*100)
					        END
					      ELSE ROUND(((best_person.y_min+best_person.y_max)/2)*100)
					    END,
					    -- Found a person, so it's an action shot
					    mla.is_action_shot=1
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
	        ImageClassifier classifier = new ImageClassifier();
	        var result = classifier.analyze(originalJpg.toString());
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
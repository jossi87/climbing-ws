package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
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
	private static Logger logger = LogManager.getLogger();
	public static void main(String[] args) {
		new AnalyzeMedia();
	}
	private final ExecutorService executor = Executors.newFixedThreadPool(8);
	private final List<String> warnings = new ArrayList<>();
	
	public AnalyzeMedia() {
		List<Integer> mediaIds = new ArrayList<>();
		Server.runSql((_, c) -> {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT m.id
					FROM problem p
					JOIN tick t ON p.id=t.problem_id AND t.stars=3
					JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
					JOIN media m ON mp.media_id=m.id AND m.deleted_timestamp IS NULL
					  AND NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
					GROUP BY m.id
					""");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					mediaIds.add(rst.getInt("id"));
				}
			}
		});
		for (int id : mediaIds) {
			executor.submit(() -> {
				try {
					processTask(id);
				} catch (Exception e) {
					warnings.add("Error processing id=" + id + ": " + e.getMessage());
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
		logger.debug("Updating cache columns (Focus and Tags)...");
		Server.runSql((_, c) -> {
			try (PreparedStatement ps = c.prepareStatement("""
					UPDATE media_ml_analysis mla
					JOIN (
					    SELECT media_id,
					           ROUND((x_min + x_max) / 2 * 100) as calc_x,
					           ROUND((y_min + y_max) / 2 * 100) as calc_y
					    FROM (SELECT media_id, x_min, x_max, y_min, y_max,
					                 ROW_NUMBER() OVER (PARTITION BY media_id ORDER BY score DESC) as rn
					          FROM media_ml_object
					          WHERE name = 'Person'
					    ) t
					    WHERE rn = 1
					) best_person ON mla.media_id = best_person.media_id
					SET mla.focus_x = best_person.calc_x,
					    mla.focus_y = best_person.calc_y;
					""")) {
				ps.execute();
			}
		});
		for (String w : warnings) {
			logger.warn(w);
		}
		logger.debug("Done");
	}

	public void processTask(int mediaId) {
		try {
			Path originalJpg = getLocalPath(S3KeyGenerator.getOriginalJpg(mediaId));
			ImageClassifier classifier = new ImageClassifier();
	        var result = classifier.analyze(originalJpg.toString());
	        Server.runSql((dao, c) -> {
	            try {
	                dao.saveMediaAnalysis(c, mediaId, result.hexColor(), result.labels(), result.objects());
	            } catch (Exception e) {
	                warnings.add("Failed to save media id=" + mediaId + ": " + e.getMessage());
	            }
	        });
		} catch (Exception e) {
			warnings.add("Failed to process/analyze media id=" + mediaId + ": " + e.getMessage());
		}
	}

	private Path getLocalPath(String s3Key) {
		return Paths.get(LOCAL_BUCKET_ROOT, s3Key);
	}
}
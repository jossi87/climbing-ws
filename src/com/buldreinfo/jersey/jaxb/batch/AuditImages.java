package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;

public class AuditImages {
	private static final Logger logger = LogManager.getLogger();
	private final static String LOCAL_BUCKET_ROOT = "G:/My Drive/web/buldreinfo/s3_bucket_climbing_web";

	public static void main(String[] args) {
		new AuditImages().runSanityCheck();
	}

	private final ExecutorService executor = Executors.newFixedThreadPool(16);
	private final AtomicInteger totalChecked = new AtomicInteger(0);
	private final AtomicInteger missingCount = new AtomicInteger(0);
	private final AtomicInteger zeroByteCount = new AtomicInteger(0);

	public void runSanityCheck() {
		Server.runSql((_, c) -> {
			String sql = "SELECT id, suffix FROM media WHERE is_movie=0";
			try (PreparedStatement ps = c.prepareStatement(sql);
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					final int id = rst.getInt("id");
					final String suffix = rst.getString("suffix");
					executor.submit(() -> {
						totalChecked.incrementAndGet();
						String objectKey = S3KeyGenerator.getOriginalJpg(id);
						if (suffix.equals("png")) {
							objectKey = objectKey.replace(".jpg", ".png");
						}
						Path originalJpg = getLocalPath(objectKey);
						try {
							if (!Files.exists(originalJpg)) {
								missingCount.incrementAndGet();
								logger.error("MISSING: id={} at {}", id, originalJpg);
							}
							else if (Files.size(originalJpg) == 0) {
								zeroByteCount.incrementAndGet();
								logger.error("ZERO-BYTE: id={} at {}", id, originalJpg);
							}
						} catch (IOException e) {
							logger.error("IO Error for id={}: {}", id, e.getMessage());
						}
					});
				}
			}
			executor.shutdown();
			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				logger.error("Audit interrupted: " + e.getMessage());
			}
			logger.info("--- Image Audit Results ---");
			logger.info("Total Images Checked: {}", totalChecked.get());
			logger.info("Missing Files:        {}", missingCount.get());
			logger.info("Zero-byte Files:      {}", zeroByteCount.get());
			logger.info("Healthy Files:        {}", (totalChecked.get() - missingCount.get() - zeroByteCount.get()));
			if (missingCount.get() > 0 || zeroByteCount.get() > 0) {
				logger.warn("Audit complete. Found issues that need attention.");
			}
			else {
				logger.info("Audit complete. All image files are healthy!");
			}
		});
	}

	private Path getLocalPath(String s3Key) {
		return Paths.get(LOCAL_BUCKET_ROOT, s3Key);
	}
}
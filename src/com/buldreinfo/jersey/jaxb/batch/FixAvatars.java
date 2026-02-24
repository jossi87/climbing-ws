package com.buldreinfo.jersey.jaxb.batch;

import java.io.InputStream;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.google.common.base.Stopwatch;

public class FixAvatars {
	private record Task (int id, String picture) {}
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) throws InterruptedException {
		var worker = new FixAvatars();
		var tasks = worker.getTasks();
		worker.run(tasks);
	}
	
	private FixAvatars() {
		// Sealed
	}
	
	private List<Task> getTasks() {
		List<Task> res = new ArrayList<>();
		Server.runSql((_, c) -> {
			try (PreparedStatement ps = c.prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL ORDER BY u.id");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					res.add(new Task(id, picture));
				}
			}
		});
		return res;
	}

	private void run(List<Task> tasks) throws InterruptedException {
		var stopwatch = Stopwatch.createStarted();
		var executor = Executors.newFixedThreadPool(16);
		StorageManager storage = StorageManager.getInstance();
		tasks.forEach(t -> {
			executor.submit(() -> {
				String originalKey = S3KeyGenerator.getOriginalUserAvatar(t.id);
				String resizedKey = S3KeyGenerator.getWebUserAvatar(t.id);
				
				try {
					boolean originalExists = storage.exists(originalKey);
					if (!originalExists && t.picture.startsWith("http")) {
						try (InputStream is = URI.create(t.picture).toURL().openStream()) {
							byte[] bytes = is.readAllBytes();
							storage.uploadBytes(originalKey, bytes, StorageType.JPG);
							originalExists = true;
						} catch (Exception e) {
							logger.warn("Could not download image on userId={}, error={}", t.id, e.getMessage());
						}
					}
					if (originalExists && !storage.exists(resizedKey)) {
						byte[] originalBytes = storage.downloadBytes(originalKey);
						ImageHelper.saveAvatarThumb(t.id, originalBytes);
					}
				} catch (Exception e) {
					logger.error("Failed to process avatar for userId={}: {}", t.id, e.getMessage());
				}
			});
		});
		executor.shutdown();
		executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		logger.info("run(tasks.size()={}) - duration={}", tasks.size(), stopwatch);
	}
}
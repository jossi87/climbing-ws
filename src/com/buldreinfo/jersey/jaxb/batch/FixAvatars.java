package com.buldreinfo.jersey.jaxb.batch;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
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
		tasks.forEach(t -> {
			executor.submit(() -> {
				Path original = IOHelper.getPathOriginalUsers(t.id);
				Path resized = IOHelper.getPathWebUsers(t.id);
				if (!Files.exists(original) && t.picture.startsWith("http")) {
					try (InputStream is = URI.create(t.picture).toURL().openStream()) {
						Files.copy(is, original, StandardCopyOption.REPLACE_EXISTING);
					}
					catch (Exception e) {
						logger.warn("Could not download image on userId={}, error={}", t.id, e.getMessage());
					}
				}
				if (Files.exists(original) && !Files.exists(resized)) {
					ImageHelper.saveAvatarThumb(original, resized);
				}
			});
		});
		executor.shutdown(); // Tell executor to not accept any more jobs
		executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
		logger.info("run(tasks.size()={}) - duration={}", tasks.size(), stopwatch);
	}
}
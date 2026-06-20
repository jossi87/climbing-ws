package com.buldreinfo.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.buldreinfo.Application;
import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;

public class FillActivity {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws Exception {
		var context = new SpringApplicationBuilder(Application.class)
				.web(WebApplicationType.NONE)
				.run(args);
		var txManager = context.getBean(ClimbingTransactionManager.class);
		var activityRepo = context.getBean(ActivityRepository.class);
		txManager.executeInTransaction(() -> {
			var c = txManager.getConnection();
			try (PreparedStatement ps = c.prepareStatement("SELECT id FROM problem ORDER BY id")) {
				try (ResultSet rst = ps.executeQuery()) {
					int done = 0;
					while (rst.next()) {
						activityRepo.fillActivity(rst.getInt("id"));
						if ((++done) % 50 == 0) {
							logger.debug("Done with " + done + " problems");
						}
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		});
	}
}
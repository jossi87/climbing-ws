package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.dao.ActivityRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;

public class FillActivity {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws Exception {
		var locator = BatchBootstrapper.createLocator();
		var txManager = locator.getService(TransactionManager.class);
		var activityRepo = locator.getService(ActivityRepository.class);
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
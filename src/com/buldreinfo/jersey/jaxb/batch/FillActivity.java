package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;

public class FillActivity {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		new FillActivity();
	}

	public FillActivity() {
		Server.runSql(c -> {
			try (PreparedStatement ps = c.prepareStatement("SELECT id FROM problem ORDER BY id")) {
				try (ResultSet rst = ps.executeQuery()) {
					int done = 0;
					while (rst.next()) {
						Server.getDao().fillActivity(c, rst.getInt("id"));
						if ((++done) % 50 == 0) {
							logger.debug("Done with " + done + " problems");
						}
					}
				}
			}
		});
	}
}
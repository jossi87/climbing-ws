package com.buldreinfo.jersey.jaxb.batch;

import java.io.InputStream;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;

public class FixAvatars {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		Server.runSql((dao, c) -> {
			int counter = 0;
			try (PreparedStatement ps = c.prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL AND u.picture LIKE 'https%' ORDER BY u.id DESC");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					try (InputStream is = URI.create(picture).toURL().openStream()) {
						ImageHelper.saveAvatar(id, is, false);
						if (++counter % 250 == 0) {
							logger.debug("Done with {} users, id={}", counter, id);
						}
					}
				}
			}
		});
	}
}

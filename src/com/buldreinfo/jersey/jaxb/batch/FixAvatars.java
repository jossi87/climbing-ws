package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;

public class FixAvatars {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			int counter = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL ORDER BY u.id DESC");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					ImageHelper.saveAvatar(id, picture);
					if (++counter % 250 == 0) {
						logger.debug("Done with {} users, id={}", counter, id);
					}
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}

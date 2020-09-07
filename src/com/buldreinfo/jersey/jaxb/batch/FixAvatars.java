package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.buldreinfo.jersey.jaxb.db.BuldreinfoRepository;
import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class FixAvatars {
	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Path originalFolder = Paths.get("C:/users/joste_000/desktop/test/original");
			final Path resizedFolder = Paths.get("C:/users/joste_000/desktop/test/resized");
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, u.picture FROM user u WHERE u.picture IS NOT NULL");
					ResultSet rst = ps.executeQuery();) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String picture = rst.getString("picture");
					BuldreinfoRepository.downloadUserImage(originalFolder, resizedFolder, id, picture);
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}

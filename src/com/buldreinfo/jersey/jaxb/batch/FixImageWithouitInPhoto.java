package com.buldreinfo.jersey.jaxb.batch;

import java.awt.Desktop;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.base.Strings;

/**
 * Done with:
 * - user_id=397 (Stian Engelsvoll) until idMedia=25225 (2021-05-02)
 * - user_id=1056 (Jarle Risa) until idMedia=25254 (2021-05-02)
 * - user_id=25137 (Tore Årthun) until id_media=25137 (2021-05-02)
 */
public class FixImageWithouitInPhoto {
	private static Logger logger = LogManager.getLogger();
	private static final int END_SIGNAL = 0;
	private static final int CREATOR_USER_ID = -1; // TODO
	private static final int MIN_MEDIA_ID = 0; // TODO

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM media WHERE deleted_user_id IS NULL AND uploader_user_id=? AND id NOT IN (SELECT media_id FROM media_user) AND id>=? ORDER BY id")) {
				ps.setInt(1, CREATOR_USER_ID);
				ps.setInt(2, MIN_MEDIA_ID);
				try (ResultSet rst = ps.executeQuery();
						Scanner scanner = new Scanner(System.in)) {
					List<String> updates = new ArrayList<>();
					while (rst.next()) {
						int id = rst.getInt("id");
						final Path jpg = FixMedia.root.resolve("web/jpg").resolve(String.valueOf(id/100*100)).resolve(id + ".jpg");
						Desktop.getDesktop().open(jpg.toFile());
						int userId = getUser(id, c, scanner);
						if (userId == END_SIGNAL) {
							break;
						}
						else if (userId != -1) {
							updates.add(String.format("INSERT INTO media_user (media_id, user_id) VALUES (%d, %d);", id, userId));
						}
					}
					for (String sql : updates) {
						System.out.println(sql);
					}
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private static int getUser(int id, DbConnection c, Scanner scanner) throws SQLException {
		int res = -1;
		System.out.print(id + " - First part of name (stop loop with \"END\"): ");
		String name = scanner.nextLine();
		if (!Strings.isNullOrEmpty(name)) {
			if (name.equalsIgnoreCase("end")) {
				return END_SIGNAL;
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM user WHERE concat(firstname,' ',lastname) LIKE ?")) {
				ps.setString(1, name + "%");
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						if (res != -1) {
							logger.warn("Found more than one user with name: " + name);
							res = -1;
							break;
						}
						else {
							res = rst.getInt("id");
						}
					}
				}
			}
		}
		if (!Strings.isNullOrEmpty(name) && res == -1) {
			logger.warn("Could not find: " + name);
		}
		return res;
	}
}
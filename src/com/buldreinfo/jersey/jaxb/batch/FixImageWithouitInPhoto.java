package com.buldreinfo.jersey.jaxb.batch;

import java.awt.Desktop;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.google.common.base.Strings;

public class FixImageWithouitInPhoto {
	private static Logger logger = LogManager.getLogger();
	private static final int END_SIGNAL = 0;
	private static final int MIN_MEDIA_ID = 25254; // TODO

	public static void main(String[] args) {
		Server.runSql((dao, c) -> {
			try (PreparedStatement ps = c.prepareStatement("SELECT m.id FROM media m, media_problem mp, problem p, sector s, area a WHERE m.id=mp.media_id AND mp.problem_id=p.id AND p.sector_id=s.id AND s.area_id=a.id AND a.region_id NOT IN (2,3,5,6,7,8,9,10,13,14,15) AND m.id NOT IN (SELECT media_id FROM media_user) AND deleted_user_id is null AND uploader_user_id!=1 AND m.id>=? ORDER BY m.id")) {
				ps.setInt(1, MIN_MEDIA_ID);
				try (ResultSet rst = ps.executeQuery();
						Scanner scanner = new Scanner(System.in)) {
					List<String> updates = new ArrayList<>();
					while (rst.next()) {
						int id = rst.getInt("id");
						final Path jpg = IOHelper.getPathMediaWebJpg(id);
						Desktop.getDesktop().open(jpg.toFile());
						int userId = getUser(c, id, scanner);
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
		});
	}

	private static int getUser(Connection c, int id, Scanner scanner) throws SQLException {
		int res = -1;
		System.out.print(id + " - First part of name (stop loop with \"END\"): ");
		String name = scanner.nextLine();
		if (!Strings.isNullOrEmpty(name)) {
			if (name.equalsIgnoreCase("end")) {
				return END_SIGNAL;
			}
			try (PreparedStatement ps = c.prepareStatement("SELECT id FROM user WHERE concat(firstname,' ',lastname) LIKE ?")) {
				ps.setString(1, name + "%");
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						if (res != -1) {
							logger.warn("Found more than one user with name: " + name);
							res = -1;
							break;
						}
						res = rst.getInt("id");
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
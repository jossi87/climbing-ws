package com.buldreinfo.jersey.jaxb.batch.migrate8anu;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.batch.migrate8anu.beans.Root;
import com.buldreinfo.jersey.jaxb.batch.migrate8anu.beans.Tick;
import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class Migrater {
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws IOException {
		int userId = 2562; // Jan
		// https://www.8a.nu/api/users/62809/ascents?category=sportclimbing&pageIndex=0&pageSize=400&sortfield=grade_desc&timeFilter=0&gradeFilter=0&typeFilter=&isAscented=true
		Path p = Paths.get("c:/users/joste_000/desktop/0.json");
		new Migrater(userId, p);
	}

	public Migrater(int userId, Path p) throws IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction();
				BufferedReader reader = Files.newReader(p.toFile(), Charset.forName("UTF-8"))) {
			Gson gson = new Gson();
			Root r = gson.fromJson(reader, Root.class);
			for (Tick t : r.getAscents()) {
				List<Integer> problemIds = new ArrayList<>();
				try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id FROM problem p, sector s, area a WHERE p.sector_id=s.id AND s.area_id=a.id AND a.region_id!=1 AND a.name=? AND p.name=?")) {
					ps.setString(1, t.getCragName());
					ps.setString(2, t.getZlaggableName());
					try (ResultSet rst = ps.executeQuery()) {
						problemIds.add(rst.getInt("id"));
					}
				}
				if (problemIds.isEmpty()) {
					logger.warn("Could not find problem: " + t);
				}
				else if(problemIds.size() > 1) {
					logger.warn("More than one match on problem: " + t);
				}
				else {
					tick(c, userId, problemIds.get(0), t);
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private void tick(DbConnection c, int userId, int problemId, Tick t) throws SQLException, ParseException {
		final String date = t.getDate().substring(0, 10);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, date FROM tick WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, userId);
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				int id = rst.getInt("id");
				Date d = rst.getDate("date");
				String dt = d == null? null : sdf.format(d);
				if (!dt.equals(date)) {
					try (PreparedStatement psUpdate = c.getConnection().prepareStatement("UPDATE tick SET date=? WHERE id=?")) {
						psUpdate.setDate(1, new java.sql.Date(sdf.parse(date).getTime()));
						psUpdate.setInt(2, id);
						psUpdate.executeUpdate();
						logger.debug("Update date on tick: " + t);
					}
				}
			}
		}
	}
}

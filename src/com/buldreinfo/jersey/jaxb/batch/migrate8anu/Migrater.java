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
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;

public class Migrater {
	private static Logger logger = LogManager.getLogger();
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	private final Setup setup;

	/**
	 * TODO: Default: Only update date on existing tick, and only if date in buldreinfo is null
	 */
	public static void main(String[] args) throws IOException {
		/**
		 * DONE (brattelinjer):
		 * - Andreas Ladstein (2020-07-27)
		 * - Eivind Helgøy (2020-07-27)
		 * - Jan Robert (2020-07-25)
		 * - Leiv Aspelund (2020-07-27)
		 * - Matteo Gennaro (2020-07-26)
		 * - Stian Engelsvoll (2020-07-26)
		 * - Thomas Holgersen (2020-07-26)
		 */
		int userId = 1955;
		// https://www.8a.nu/api/users/62809/ascents?category=sportclimbing&pageIndex=0&pageSize=400&sortfield=grade_desc&timeFilter=0&gradeFilter=0&typeFilter=&isAscented=true
		Path p = Paths.get("c:/users/joste_000/desktop/0.json");
		new Migrater(userId, p);
	}

	public Migrater(int userId, Path p) throws IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction();
				BufferedReader reader = Files.newReader(p.toFile(), Charset.forName("UTF-8"))) {
			c.getConnection().setAutoCommit(true);
			this.setup = new MetaHelper().getSetup(4);
			Gson gson = new Gson();
			Root r = gson.fromJson(reader, Root.class);
			for (Tick t : r.getAscents()) {
				if (!Strings.isNullOrEmpty(t.getCountrySlug()) && Lists.newArrayList("united-kingdom","turkey","australia","sweden","spain","italy","greece","thailand","france","germany").contains(t.getCountrySlug())) {
					continue;
				}
				if (!Strings.isNullOrEmpty(t.getCragName()) && Lists.newArrayList("tortelsvik","hellen festning","blåbærveggen","krabbeveggen","oldtidshelleren","bratthammaren","gandalfveggen","skyggehelleren","lofoten","pianokrakken","kvarven","storheia","røyrvika","bobleveggen","claret","turøy","avsnes","viitapohja","bøtteveggen","slettafossen","avsnes","bjørnebakken","piratbukta","finnvika","ekne","festvåg","krågedal","hodnedalen","mustavuori","lofotoen","paradiset","dronningen","helleneset","oddane","kastetskogen","missiveggen","fiskesleppet","neseveggen","bymuren","selvågen","hjernemasseveggen","vikso","hvarnes","sandviken","kastetskogen","gjøkeredet","veatåa","bolstadøyri","oksåsen","bikkjeveggen","uteveggen","møtteveggen","syltøy","hjalla","glesnes","propagandaveggen","stjerneveggen","drømmehagen","mojavato","myggveggen","loven","hjallaveggen","vågeveggen","sageveggen","rævuri","mostraumen","perleveggen","paradis","stryn","furunkulose","goltastraumen","loddefjord","sykehusveggen","flatanger","buråsen","sødal","hell","bukkespranget","tjøm","mjelvahammeren","laxefeltet","sødal").contains(t.getCragName().toLowerCase())) {
					continue;
				}
				List<Integer> problemIds = new ArrayList<>();
				switch (t.getZlaggableName()) {	
				// case "": problemIds.add(); break;
				default:
					// Search in db
					try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name, p.id FROM problem p, sector s, area a WHERE p.sector_id=s.id AND s.area_id=a.id AND a.region_id!=1 AND p.name=?")) {
						ps.setString(1, t.getZlaggableName());
						try (ResultSet rst = ps.executeQuery()) {
							while (rst.next()) {
								String areaName = rst.getString("name");
								int id = rst.getInt("id");
								if (areaName.equals(t.getCragName())) {
									problemIds.clear();
									problemIds.add(id);
									break;
								}
								problemIds.add(id);
							}
						}
					}
				}
				if (problemIds.isEmpty() || problemIds.size() > 1) {
					logger.warn("case \"" + t.getZlaggableName() + "\": problemIds.add(); break;\t\t\t\t\t\t\t\t\t\t\t" + t);
				}
				else {
					tick(c, userId, problemIds.get(0), t);
				}
			}
			System.out.println("Ikke oppdater gamle ticker? Sjekk resultat fra query under foer commit...");
			System.out.println("UPDATE problem p, tick t SET t.grade=p.grade WHERE p.id=t.problem_id AND t.user_id=" + userId + " AND p.grade!=t.grade AND ( (p.grade IN (43,44) AND t.grade IN (43,44)) OR (p.grade IN (37,38) AND t.grade IN (37,38)) OR (p.grade IN (35,36) AND t.grade IN (35,36)) OR (p.grade IN (33,34) AND t.grade IN (33,34))  OR (p.grade IN (31,32) AND t.grade IN (31,32)) )");
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private void tick(DbConnection c, int userId, int problemId, Tick t) throws SQLException, ParseException {
		final String date = t.getDate().substring(0, 10);
		final Date dateObj = new java.sql.Date(sdf.parse(date).getTime());
		// Update date (if ticked without date)
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, date FROM tick WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, userId);
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					Date d = rst.getDate("date");
					if (d == null) {
						try (PreparedStatement psUpdate = c.getConnection().prepareStatement("UPDATE tick SET date=? WHERE id=?")) {
							psUpdate.setDate(1, dateObj);
							psUpdate.setInt(2, id);
							psUpdate.executeUpdate();
							logger.debug("Update date on tick: " + t);
							c.getBuldreinfoRepo().fillActivity(problemId);
						}
					}
					return; // Tick is already existing!
				}
			}
		}
		// Create tick
		int stars = 0;
		switch (t.getRating()) {
		case 0: stars = 0; break;
		case 1: stars = 1; break;
		case 2: stars = 1; break;
		case 3: stars = 2; break;
		case 4: stars = 2; break;
		case 5: stars = 3; break;
		default: throw new RuntimeException("Invalid rating: " + t);
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade, comment, stars) VALUES (?, ?, ?, ?, ?, ?)")) {
			ps.setInt(1, problemId);
			ps.setInt(2, userId);
			ps.setDate(3, dateObj);
			ps.setInt(4, GradeHelper.stringToInt(setup, t.getDifficulty()));
			ps.setString(5, t.getComment());
			ps.setDouble(6, stars);
			ps.execute();
			logger.debug("Created tick: " + t);
			c.getBuldreinfoRepo().fillActivity(problemId);
		}
	}
}

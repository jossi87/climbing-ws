package com.buldreinfo.jersey.jaxb.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.Auth0Profile;
import com.buldreinfo.jersey.jaxb.beans.GradeSystem;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.helpers.HitsFormatter;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Area.AreaSectorOrder;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.DangerousProblem;
import com.buldreinfo.jersey.jaxb.model.DangerousSector;
import com.buldreinfo.jersey.jaxb.model.ExternalLink;
import com.buldreinfo.jersey.jaxb.model.FaAid;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumMedia;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumProblems;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumTicks;
import com.buldreinfo.jersey.jaxb.model.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaInfo;
import com.buldreinfo.jersey.jaxb.model.MediaMetadata;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElementType;
import com.buldreinfo.jersey.jaxb.model.NewMedia;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemComment;
import com.buldreinfo.jersey.jaxb.model.ProblemSection;
import com.buldreinfo.jersey.jaxb.model.ProblemTick;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileStatistics;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.ProfileTodoArea;
import com.buldreinfo.jersey.jaxb.model.ProfileTodoProblem;
import com.buldreinfo.jersey.jaxb.model.ProfileTodoSector;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.buldreinfo.jersey.jaxb.model.SectorProblemOrder;
import com.buldreinfo.jersey.jaxb.model.Site;
import com.buldreinfo.jersey.jaxb.model.Slope;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.TickRepeat;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.TocArea;
import com.buldreinfo.jersey.jaxb.model.TocPitch;
import com.buldreinfo.jersey.jaxb.model.TocProblem;
import com.buldreinfo.jersey.jaxb.model.TocRegion;
import com.buldreinfo.jersey.jaxb.model.TocSector;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.TodoProblem;
import com.buldreinfo.jersey.jaxb.model.TodoSector;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.TopUser;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.TypeNumTickedTodo;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserRegion;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Dao {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String ACTIVITY_TYPE_TICK_REPEAT = "TICK_REPEAT";
	private static Logger logger = LogManager.getLogger();
	private final Gson gson = new Gson();

	public Dao() {
	}

	public void addProblemMedia(Connection c, Optional<Integer> authUserId, Problem p, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		for (NewMedia m : p.getNewMedia()) {
			final int idSector = 0;
			final int idArea = 0;
			final int idGuestbook = 0;
			addNewMedia(c, authUserId, p.getId(), m.pitch(), m.trivia(), idSector, idArea, idGuestbook, m, multiPart);
		}
		fillActivity(c, p.getId());
	}

	public void deleteMedia(Connection c, Optional<Integer> authUserId, int id) throws SQLException {
		List<Integer> idProblems = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT problem_id FROM media_problem WHERE media_id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idProblems.add(rst.getInt("problem_id"));
				}
			}
		}

		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) LEFT JOIN guestbook g ON (p.id=g.problem_id)) LEFT JOIN media_guestbook mg ON (g.id=mg.guestbook_id AND mg.media_id=?)) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mg.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			ps.setInt(5, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
		try (PreparedStatement ps = c.prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, id);
			ps.execute();
		}

		for (int idProblem : idProblems) {
			fillActivity(c, idProblem);
		}
	}

	public void ensureCoordinatesInDbWithElevationAndId(Connection c, List<Coordinates> coordinates) throws SQLException, InterruptedException {
		if (coordinates != null && !coordinates.isEmpty()) {
			// First round coordinates to 10 digits (to match database type)
			coordinates.forEach(coord -> coord.roundCoordinatesToMaximum10digitsAfterComma());
			// Ensure coordinates exists in db
			try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)")) {
				for (Coordinates coord : coordinates) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					// Use elevation from GPX/TCX if available, this has better quality compared to Google Elevation API
					if (coord.getElevationSource() != null) {
						ps.setDouble(3, coord.getElevation());
						ps.setString(4, coord.getElevationSource());
					}
					else {
						ps.setNull(3, Types.DOUBLE);
						ps.setNull(4, Types.VARCHAR);
					}
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		fillMissingElevations(c);
		if (coordinates != null && !coordinates.isEmpty()) {
			// Fetch correct id's and elevation's (id can be wrong in coordinates - user might have changed latitude/longitude on existing id)
			for (Coordinates coord : coordinates) {
				try (PreparedStatement ps = c.prepareStatement("SELECT id, elevation, elevation_source FROM coordinates WHERE latitude=? AND longitude=?")) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					try (ResultSet rst = ps.executeQuery()) {
						while (rst.next()) {
							int id = rst.getInt("id");
							double elevation = rst.getDouble("elevation");
							String elevationSource = rst.getString("elevation_source");
							coord.setId(id);
							coord.setElevation(elevation, elevationSource);
						}
					}
				}
			}
		}
	}

	public void fillActivity(Connection c, int idProblem) throws SQLException {
		/**
		 * Delete existing activities on problem
		 */
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM activity WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}

		/**
		 * FA
		 */
		LocalDateTime problemActivityTimestamp = null;
		boolean exists = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT fa_date, last_updated FROM problem WHERE id=?")) {
			ps.setInt(1, idProblem);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					exists = true;
					LocalDate faDate = rst.getObject("fa_date", LocalDate.class);
					LocalDateTime lastUpdated = rst.getObject("last_updated", LocalDateTime.class);
					if (faDate != null && lastUpdated != null) {
						problemActivityTimestamp = faDate.atTime(lastUpdated.getHour(), lastUpdated.getMinute(), lastUpdated.getSecond());
					}
					else if (faDate != null) {
						problemActivityTimestamp = faDate.atStartOfDay();
					}
				}
			}
		}
		if (!exists) {
			return;
		}
		try (PreparedStatement psAddActivity = c.prepareStatement("INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id, tick_repeat_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
			psAddActivity.setObject(1, problemActivityTimestamp != null? problemActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
			psAddActivity.setString(2, ACTIVITY_TYPE_FA);
			psAddActivity.setInt(3, idProblem);
			psAddActivity.setNull(4, Types.INTEGER);
			psAddActivity.setNull(5, Types.INTEGER);
			psAddActivity.setNull(6, Types.INTEGER);
			psAddActivity.setNull(7, Types.INTEGER);
			psAddActivity.addBatch();


			/**
			 * Media
			 */
			try (PreparedStatement ps = c.prepareStatement("SELECT m.id, m.date_created FROM media_problem mp, media m WHERE mp.problem_id=? AND mp.media_id=m.id AND m.deleted_timestamp IS NULL ORDER BY m.date_created DESC")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					LocalDateTime useMediaActivityTimestamp = null;
					while (rst.next()) {
						int id = rst.getInt("id");
						LocalDateTime mediaActivityTimestamp = rst.getObject("date_created", LocalDateTime.class);
						if (mediaActivityTimestamp == null || (problemActivityTimestamp != null && Math.abs(ChronoUnit.DAYS.between(problemActivityTimestamp, mediaActivityTimestamp)) <= 7)) {
							useMediaActivityTimestamp = problemActivityTimestamp;
						}
						else if (useMediaActivityTimestamp == null || Math.abs(ChronoUnit.DAYS.between(useMediaActivityTimestamp, mediaActivityTimestamp)) > 7) {
							useMediaActivityTimestamp = mediaActivityTimestamp;
						}
						psAddActivity.setObject(1, useMediaActivityTimestamp != null? useMediaActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
						psAddActivity.setString(2, ACTIVITY_TYPE_MEDIA);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setInt(4, id);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Tick
			 */
			try (PreparedStatement ps = c.prepareStatement("SELECT user_id, date, created FROM tick WHERE problem_id=? ORDER BY date, created")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int userId = rst.getInt("user_id");
						LocalDate tickDate = rst.getObject("date", LocalDate.class);
						LocalDateTime tickCreated = rst.getObject("created", LocalDateTime.class);
						LocalDateTime tickActivityTimestamp = null;
						if (tickDate != null && tickCreated != null) {
							if (tickCreated.toLocalDate().isAfter(tickDate)) {
								// Tick created on different date, use end of day in activity order
								tickActivityTimestamp = tickDate.atTime(23, 59, 59);
							}
							else {
								// Tick created on same date as FA, use HHMMSS in activity order
								tickActivityTimestamp = tickDate.atTime(tickCreated.getHour(), tickCreated.getMinute(), tickCreated.getSecond());
							}
						}
						else if (tickDate != null) {
							tickActivityTimestamp = tickDate.atStartOfDay();
						}
						psAddActivity.setObject(1, tickActivityTimestamp != null? tickActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
						psAddActivity.setString(2, ACTIVITY_TYPE_TICK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setInt(5, userId);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Tick repeat
			 */
			try (PreparedStatement ps = c.prepareStatement("SELECT r.id, t.user_id, r.date, r.created FROM tick t, tick_repeat r WHERE t.problem_id=? AND t.id=r.tick_id ORDER BY r.tick_id, r.date, r.id")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						int userId = rst.getInt("user_id");
						LocalDate tickDate = rst.getObject("date", LocalDate.class);
						LocalDateTime tickCreated = rst.getObject("created", LocalDateTime.class);
						LocalDateTime tickRepeatActivityTimestamp = null;
						if (tickDate != null && tickCreated != null) {
							tickRepeatActivityTimestamp = tickDate.atTime(tickCreated.getHour(), tickCreated.getMinute(), tickCreated.getSecond());
						}
						else if (tickDate != null) {
							tickRepeatActivityTimestamp = tickDate.atStartOfDay();
						}
						psAddActivity.setObject(1, tickRepeatActivityTimestamp != null? tickRepeatActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
						psAddActivity.setString(2, ACTIVITY_TYPE_TICK_REPEAT);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setInt(5, userId);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setInt(7, id);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Guestbook
			 */
			try (PreparedStatement ps = c.prepareStatement("SELECT id, post_time FROM guestbook WHERE problem_id=? ORDER BY post_time")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						LocalDateTime postTime = rst.getObject("post_time", LocalDateTime.class);
						psAddActivity.setObject(1, postTime != null? postTime : LocalDate.EPOCH.atStartOfDay());
						psAddActivity.setString(2, ACTIVITY_TYPE_GUESTBOOK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setInt(6, id);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Execute psAddActivity
			 */
			psAddActivity.executeBatch();
		}
	}

	public List<Activity> getActivity(Connection c, Optional<Integer> authUserId, Setup setup, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media) throws SQLException {
		// GROUP_CONCAT has a max length 1024 characters by default, use avoid exception
		try (PreparedStatement ps = c.prepareStatement("SET SESSION group_concat_max_len = 1000000")) {
			ps.execute();
		}

		Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Activity> res = new ArrayList<>();
		/**
		 * Fetch activities to return
		 */
		final Set<Integer> faActivitityIds = new HashSet<>();
		final Set<Integer> tickActivitityIds = new HashSet<>();
		final Set<Integer> tickRepeatActivitityIds = new HashSet<>();
		final Set<Integer> mediaActivitityIds = new HashSet<>();
		final Set<Integer> guestbookActivitityIds = new HashSet<>();
		String sqlStr = """
				SELECT x.activity_timestamp, a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, x.problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, t.subtype problem_subtype, p.grade, GROUP_CONCAT(DISTINCT concat(x.id,'-',x.type) SEPARATOR ',') activities 
				FROM ((((((activity x INNER JOIN problem p ON x.problem_id=p.id) INNER JOIN type t ON p.type_id=t.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND (r.id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				  """ +
				  ((lowerGrade == 0 && fa && comments && ticks && media && idArea == 0 && idSector == 0)? " AND x.activity_timestamp>DATE_SUB(NOW(),INTERVAL 3 MONTH) " : "") + // Only look at activity for the last three months when loading frontpage, this improves speed significantly
				  (lowerGrade == 0? "" : " AND p.grade>=" + lowerGrade + " ") +
				  (fa? "" : " AND x.type!='FA' ") +
				  (comments? "" : " AND x.type!='GUESTBOOK' ") +
				  (ticks? "" : " AND x.type!='TICK' AND x.type!='TICK_REPEAT' ") +
				  (media? "" : " AND x.type!='MEDIA' ") +
				  (idArea==0? "" : " AND a.id=" + idArea + " ") +
				  (idSector==0? "" : " AND s.id=" + idSector + " ") +
				  		"""
				  		GROUP BY x.activity_timestamp, a.id, a.locked_admin, a.locked_superadmin, a.name, s.id, s.locked_admin, s.locked_superadmin, s.name, x.problem_id, p.locked_admin, p.locked_superadmin, p.name, p.grade
				  		ORDER BY -x.activity_timestamp, x.problem_id DESC LIMIT 100
				  		""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					LocalDateTime activityTimestamp = rst.getObject("activity_timestamp", LocalDateTime.class);
					int areaId = rst.getInt("area_id");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String problemName = rst.getString("problem_name");
					String problemSubtype = rst.getString("problem_subtype");
					String grade = setup.gradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
					Set<Integer> activityIds = new HashSet<>();
					String activities = rst.getString("activities");
					for (String activity : activities.split(",")) {
						String[] str = activity.split("-");
						int idActivity = Integer.parseInt(str[0]);
						String type = str[1];
						activityIds.add(idActivity);
						switch (type) {
						case ACTIVITY_TYPE_FA -> faActivitityIds.add(idActivity);
						case ACTIVITY_TYPE_TICK -> tickActivitityIds.add(idActivity);
						case ACTIVITY_TYPE_TICK_REPEAT -> tickRepeatActivitityIds.add(idActivity);
						case ACTIVITY_TYPE_GUESTBOOK -> guestbookActivitityIds.add(idActivity);
						case ACTIVITY_TYPE_MEDIA -> mediaActivitityIds.add(idActivity);
						default -> throw new IllegalArgumentException("Invalid type: " + type + " on idActivity=" + idActivity + " (acitivities=" + activities + ")");
						}
					}

					String timeAgo = TimeAgo.getTimeAgo(activityTimestamp.toLocalDate());
					res.add(new Activity(activityIds, timeAgo, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, problemId, problemLockedAdmin, problemLockedSuperadmin, problemName, problemSubtype, grade));
				}
			}
		}

		if (!tickActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, t.comment description, t.stars, t.grade 
					FROM activity a, tick t, user u
					WHERE a.id IN (%s) 
					  AND a.user_id=u.id AND a.problem_id=t.problem_id AND u.id=t.user_id
					""".formatted(Joiner.on(",").join(tickActivitityIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						long avatarCrc32 = rst.getLong("avatar_crc32");
						String description = rst.getString("description");
						int stars = rst.getInt("stars");
						String personalGrade = setup.gradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						a.setTick(false, userId, name, avatarCrc32, description, stars, personalGrade);
					}
				}
			}
		}

		if (!tickRepeatActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, r.comment description, t.stars, t.grade 
					FROM activity a, tick t, tick_repeat r, user u 
					WHERE a.id IN (%s) 
					  AND a.user_id=u.id AND a.problem_id=t.problem_id AND a.tick_repeat_id=r.id AND t.id=r.tick_id AND u.id=t.user_id
					""".formatted(Joiner.on(",").join(tickRepeatActivitityIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						long avatarCrc32 = rst.getLong("avatar_crc32");
						String description = rst.getString("description");
						int stars = rst.getInt("stars");
						String personalGrade = setup.gradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						a.setTick(true, userId, name, avatarCrc32, description, stars, personalGrade);
					}
				}
			}
		}

		if (!guestbookActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, g.message, mg.media_id, m.checksum 
					FROM (((activity a INNER JOIN guestbook g ON a.guestbook_id=g.id) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id) LEFT JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0) 
					WHERE a.id IN (%s)
					""".formatted(Joiner.on(",").join(guestbookActivitityIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						long avatarCrc32 = rst.getLong("avatar_crc32");
						String message = rst.getString("message");
						a.setGuestbook(userId, name, avatarCrc32, message);

						int mediaId = rst.getInt("media_id");
						long crc32 = rst.getInt("checksum");
						if (mediaId > 0) {
							boolean isMovie = false;
							String embedUrl = null;
							a.addMedia(mediaId, crc32, isMovie, embedUrl);
						}
					}
				}
			}
		}

		if (!faActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, u.id user_id, CRC32(u.picture) avatar_crc32, p.description, MAX(m.id) random_media_id, MAX(m.checksum) random_media_crc32 
					FROM ((((activity a INNER JOIN problem p ON a.problem_id=p.id) LEFT JOIN fa ON p.id=fa.problem_id) LEFT JOIN user u ON fa.user_id=u.id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0) 
					WHERE a.id IN (%s) 
					GROUP BY a.id, u.firstname, u.lastname, u.id, u.picture, p.description
					ORDER BY u.firstname, u.lastname
					""".formatted(Joiner.on(",").join(faActivitityIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						String name = rst.getString("name");
						int userId = rst.getInt("user_id");
						long avatarCrc32 = rst.getLong("avatar_crc32");
						String description = rst.getString("description");
						int problemRandomMediaId = rst.getInt("random_media_id");
						long problemRandomMediaCrc32 = rst.getInt("random_media_crc32");
						a.addFa(name, userId, avatarCrc32, description, problemRandomMediaId, problemRandomMediaCrc32);
					}
				}
			}
		}

		if (!mediaActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, m.id media_id, m.checksum media_crc32, m.is_movie, m.embed_url 
					FROM activity a, media m, media_problem mp 
					WHERE a.id IN (%s) 
					 AND a.media_id=m.id AND m.id=mp.media_id AND a.problem_id=mp.problem_id
					ORDER BY m.is_movie, mp.sorting, m.id
					""".formatted(Joiner.on(",").join(mediaActivitityIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int mediaId = rst.getInt("media_id");
						long mediaCrc32 = rst.getInt("media_crc32");
						boolean isMovie = rst.getBoolean("is_movie");
						String embedUrl = rst.getString("embed_url");
						a.addMedia(mediaId, mediaCrc32, isMovie, embedUrl);
					}
				}
			}
		}
		logger.debug("getActivity(authUserId={}, setup={}) - res.size()={}, duration={}", authUserId, setup, res.size(), stopwatch);
		return res;
	}

	public List<Administrator> getAdministrators(Connection c, int idRegion) throws SQLException {
		List<Administrator> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
				       CASE WHEN u.email_visible_to_all=1 THEN GROUP_CONCAT(DISTINCT e.email ORDER BY e.email SEPARATOR ';') END emails,
				       CRC32(u.picture) avatar_crc32,
				       DATE_FORMAT(MAX(l.when),'%Y.%m.%d') last_login
				FROM ((user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN user_region ur ON (u.id=ur.user_id AND l.region_id=ur.region_id) LEFT JOIN user_email e ON u.id=e.user_id)
				WHERE l.region_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)
				GROUP BY u.id, u.firstname, u.lastname, u.picture
				ORDER BY TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))
				""")) {
			ps.setInt(1, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					String emailsStr = rst.getString("emails");
					List<String> emails = Strings.isNullOrEmpty(emailsStr) ? null : Splitter.on(';')
							.trimResults()
							.omitEmptyStrings()
							.splitToList(emailsStr);
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String lastLogin = rst.getString("last_login");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new Administrator(userId, name, emails, avatarCrc32, timeAgo));
				}
			}
		}
		return res;
	}

	public Area getArea(Connection c, Setup s, Optional<Integer> authUserId, int reqId, boolean updateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (updateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Area a = null;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				FROM ((area a INNER JOIN region r ON a.region_id=r.id) LEFT JOIN coordinates c ON a.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE a.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				GROUP BY r.id, r.url, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.name, a.sun_from_hour, a.sun_to_hour, a.description,
				         c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			ps.setInt(3, s.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					boolean noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaArea(c, authUserId, reqId, false, 0, 0, 0);
					if (allMedia != null && allMedia.size() > 0) {
						media = allMedia.stream().filter(x -> !x.trivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.trivia()).collect(Collectors.toList());
						}
					}
					var externalLinks = getExternalLinksArea(c, reqId, false);
					a = new Area(null, regionId, canonical, reqId, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, -1, -1, media, triviaMedia, null, externalLinks, pageViews);
				}
			}
		}
		if (a == null) {
			// Area not found, see if it's visible on a different domain
			Redirect res = getCanonicalUrl(c, reqId, 0, 0);
			if (!Strings.isNullOrEmpty(res.redirectUrl())) {
				return new Area(res.redirectUrl(), -1, null, -1, false, false, false, false, null, null, false, 0, 0, null, null, null, 0, 0, null, null, null, null, null);
			}
		}
		Preconditions.checkNotNull(a, "Could not find area with id=" + reqId);
		Map<Integer, Area.AreaSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, MAX(m.id) media_id, MAX(m.checksum) media_crc32
				FROM (((((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN problem p ON s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1) LEFT JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.sun_from_hour, s.sun_to_hour, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual ORDER BY s.sorting, s.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int sorting = rst.getInt("sorting");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates parking = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					CompassDirection wallDirectionCalculated = getCompassDirection(s, rst.getInt("compass_direction_id_calculated"));
					CompassDirection wallDirectionManual = getCompassDirection(s, rst.getInt("compass_direction_id_manual"));
					int randomMediaId = rst.getInt("media_id");
					long randomMediaCrc32 = rst.getInt("media_crc32");
					if (randomMediaId == 0) {
						boolean inherited = false;
						boolean showHiddenMedia = true; // Show everything to ensure image in area overview
						List<Media> x = getMediaSector(c, s, authUserId, id, 0, inherited, 0, 0, 0, showHiddenMedia);
						if (!x.isEmpty()) {
							randomMediaId = x.get(0).id();
						}
					}
					Area.AreaSector as = a.addSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, wallDirectionCalculated, wallDirectionManual, randomMediaId, randomMediaCrc32);
					sectorLookup.put(id, as);
					for (SectorProblem sp : getSectorProblems(c, s, authUserId, as.getId())) {
						as.getProblems().add(sp);
					}
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(c, sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idSectorOutline.get(idSector));
				sectorLookup.get(idSector).setOutline(outline);
			}
			// Fill sector ascents
			getSectorSlopes(c, true, sectorLookup.keySet()).entrySet().forEach(e -> sectorLookup.get(e.getKey().intValue()).setApproach(e.getValue()));			
			// Fill sector descents
			getSectorSlopes(c, false, sectorLookup.keySet()).entrySet().forEach(e -> sectorLookup.get(e.getKey().intValue()).setDescent(e.getValue()));
		}
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT s.id,
				       CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END type,
				       COUNT(DISTINCT p.id) num,
				       COUNT(DISTINCT CASE WHEN f.user_id IS NOT NULL OR t.user_id IS NOT NULL THEN p.id END) num_ticked,
				       COUNT(DISTINCT td.id) num_todo
				FROM area a
				     INNER JOIN sector s ON a.id=s.area_id
					 INNER JOIN problem p ON s.id=p.sector_id
					 INNER JOIN type ty ON p.type_id=ty.id
					 LEFT JOIN fa f ON p.id=f.problem_id AND f.user_id=?
					 LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?
					 LEFT JOIN todo td ON p.id=td.problem_id AND td.user_id=?
					 LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE a.id=?
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END
				ORDER BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, authUserId.orElse(0));
			ps.setInt(4, authUserId.orElse(0));
			ps.setInt(5, reqId);
			Map<String, TypeNumTickedTodo> lookup = new LinkedHashMap<>();
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("id");
					String type = rst.getString("type");
					int num = rst.getInt("num");
					int numTicked = rst.getInt("num_ticked");
					int numTodo = rst.getInt("num_todo");
					TypeNumTickedTodo typeNumTickedTodo = new TypeNumTickedTodo(type, num, numTicked, numTodo);
					// Sector
					Optional<Area.AreaSector> optSector = a.getSectors().stream().filter(x -> x.getId() == sectorId).findAny();
					if (optSector.isPresent()) {
						optSector.get().getTypeNumTickedTodo().add(typeNumTickedTodo);
					}
					// Area
					TypeNumTickedTodo areaTnt = lookup.get(type);
					if (areaTnt == null) {
						areaTnt = new TypeNumTickedTodo(type, num, numTicked, numTodo);
						a.getTypeNumTickedTodo().add(areaTnt);
						lookup.put(type, areaTnt);
					}
					else {
						areaTnt.addNum(num);
						areaTnt.addTicked(numTicked);
						areaTnt.addTodo(numTodo);
					}
				}
			}
		}
		a.orderSectors();
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return a;
	}

	public Collection<Area> getAreaList(Connection c, Optional<Integer> authUserId, int reqIdRegion) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Area> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN coordinates c ON a.coordinates_id=c.id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY r.id, r.url, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits ORDER BY replace(replace(replace(lower(a.name),'æ','zx'),'ø','zy'),'å','zz')")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					boolean noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					if (comment != null) {
						int ix = comment.indexOf("<strong>Forhold:</strong>");
						if (ix != -1) {
							comment = comment.substring(ix+25);
							ix = comment.indexOf("<strong>");
							comment = comment.substring(0, ix);
						}
					}
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int numSectors = rst.getInt("num_sectors");
					int numProblems = rst.getInt("num_problems");
					String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					res.add(new Area(null, idRegion, canonical, id, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, numSectors, numProblems, null, null, null, null, pageViews));
				}
			}
		}
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public synchronized Optional<Integer> getAuthUserId(Connection c, Auth0Profile profile) throws SQLException {
		Optional<Integer> authUserId = Optional.empty();
		String picture = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT e.user_id, u.picture FROM user_email e, user u WHERE e.user_id=u.id AND lower(e.email)=?")) {
			ps.setString(1, profile.email());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					authUserId = Optional.of(rst.getInt("user_id"));
					picture = rst.getString("picture");
				}
			}
		}
		if (authUserId.isEmpty()) {
			authUserId = Optional.of(addUser(c, profile.email(), profile.firstname(), profile.lastname(), profile.picture()));
		}
		else if (profile.picture() != null && (picture == null || !picture.equals(profile.picture()))) {
			if (picture != null && picture.contains("fbsbx.com") && !profile.picture().contains("fbsbx.com")) {
				logger.debug("Dont change from facebook-image, new image is most likely avatar with text...");
			}
			else if (picture != null && !picture.startsWith("https")) {
				logger.debug("User has uploaded an avatar, don't replace this with social media image");
			}
			else {
				try (InputStream is = URI.create(profile.picture()).toURL().openStream()) {
					ImageHelper.saveAvatar(authUserId.orElseThrow(), is);
					try (PreparedStatement ps = c.prepareStatement("UPDATE user SET picture=? WHERE id=?")) {
						ps.setString(1, profile.picture());
						ps.setInt(2, authUserId.orElseThrow());
						ps.executeUpdate();
					}
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		c.commit(); // Commit in synchronised function to avoid multiple parallel transactions inserting the same user...
		logger.debug("getAuthUserId(profile={}) - authUserId={}", profile, authUserId);
		return authUserId;
	}

	public Redirect getCanonicalUrl(Connection c, int idArea, int idSector, int idProblem) throws SQLException {
		String sqlStr = null;
		int id = 0;
		if (idArea > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/area/',a.id) url FROM region r, area a WHERE r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/sector/',s.id) url FROM region r, area a, sector s WHERE r.id=a.region_id AND a.id=s.area_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/problem/',p.id) url FROM region r, area a, sector s, problem p WHERE r.id=a.region_id AND a.id=s.area_id AND s.id=p.sector_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=?";
			id = idProblem;
		}
		Preconditions.checkArgument(id > 0 && sqlStr != null, "Invalid parameters: idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		Redirect res = null;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = Redirect.fromRedirectUrl(rst.getString("url"));
				}
			}
		}
		Preconditions.checkNotNull(res, "Could not find canonical url for idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		return res;
	}

	public Collection<GradeDistribution> getContentGraph(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = "WITH x AS ("
				+ " SELECT g.base_no grade_base_no, x.region, x.t, COUNT(id_problem) num"
				+ " FROM (SELECT r.name region, s.sorting, ty.subtype t, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(t.grade) + 1)) grade_id, p.id id_problem"
				+ "   FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN tick t ON (p.id=t.problem_id AND t.grade>0)"
				+ "   WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "     AND (a.region_id=? OR ur.user_id IS NOT NULL)"
				+ "     AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ "   GROUP BY s.name, ty.subtype, p.id) x, grade g"
				+ " WHERE x.grade_id=g.grade_id AND g.t=?"
				+ " GROUP BY x.region, g.base_no, x.t"
				+ " )"
				+ " SELECT g.base_no grade, x.region, COALESCE(x.t,'Boulder') t, num"
				+ " FROM (SELECT g.base_no, MIN(g.grade_id) sort FROM grade g WHERE g.t=? GROUP BY g.base_no) g LEFT JOIN x ON g.base_no=x.grade_base_no"
				+ " ORDER BY g.sort, x.region, x.t";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			ps.setString(4, setup.gradeSystem().toString());
			ps.setString(5, setup.gradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					GradeDistribution g = res.get(grade);
					if (g == null) {
						g = new GradeDistribution(grade);
						res.put(grade, g);
					}
					String region = rst.getString("region");
					if (region != null) {
						String t = rst.getString("t");
						int num = rst.getInt("num");
						g.addSector(region, t, num);
					}
				}
			}
		}
		return res.values();
	}

	public Collection<DangerousArea> getDangerous(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Map<Integer, DangerousArea> areasLookup = new LinkedHashMap<>();
		Map<Integer, DangerousSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.broken problem_broken, p.nr problem_nr, p.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, DATE_FORMAT(g.post_time,'%Y.%m.%d') post_time, g.message FROM ((((((area a INNER JOIN region r ON r.id=a.region_id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour, s.id, s.name, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, s.sun_from_hour, s.sun_to_hour, p.id, p.broken, p.nr, p.grade, p.name, p.locked_admin, p.locked_superadmin, u.firstname, u.lastname, g.post_time, g.message ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					DangerousArea a = areasLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new DangerousArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour, new ArrayList<>());
						areasLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					DangerousSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						CompassDirection sectorWallDirectionCalculated = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
						int sectorSunToHour = rst.getInt("sector_sun_to_hour");
						s = new DangerousSector(sectorId, sectorUrl, sectorName, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin, sectorSunFromHour, sectorSunToHour, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("problem_id");
					String url = rst.getString("problem_url");
					String broken = rst.getString("problem_broken");
					int nr = rst.getInt("problem_nr");
					int grade = rst.getInt("problem_grade");
					boolean lockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String name = rst.getString("problem_name");
					String postBy = rst.getString("name");
					String postWhen = rst.getString("post_time");
					String postTxt = rst.getString("message");
					s.problems().add(new DangerousProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, setup.gradeConverter().getGradeFromIdGrade(grade), postBy, postWhen, postTxt));
				}
			}
		}
		return areasLookup.values();
	}

	public List<Integer> getFaYears(Connection c, int regionId) throws SQLException {
		List<Integer> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT year(p.fa_date) fa_year
				FROM area a, sector s, problem p
				WHERE a.region_id=? AND a.id=s.area_id AND s.id=p.sector_id
				  AND p.fa_date IS NOT NULL GROUP BY year(p.fa_date) ORDER BY year(p.fa_date) DESC
				""")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int faYear = rst.getInt("fa_year");
					res.add(faYear);
				}
			}
		}
		return res;
	}

	public FrontpageNumMedia getFrontpageNumMedia(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		FrontpageNumMedia res = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN mp.id END) num_images, COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN mp.id END) num_movies FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND a.trash IS NULL AND s.trash IS NULL AND p.trash IS NULL AND m.deleted_user_id IS NULL AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int numImages = rst.getInt("num_images");
					int numMovies = rst.getInt("num_movies");
					res = new FrontpageNumMedia(numImages, numMovies);
				}
			}
		}
		logger.debug("getFrontpageNumMedia(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public FrontpageNumProblems getFrontpageNumProblems(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		FrontpageNumProblems res = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT p.id) num_problems, COUNT(DISTINCT CASE WHEN p.coordinates_id IS NOT NULL THEN p.id END) num_problems_with_coordinates, COUNT(DISTINCT svg.problem_id) num_problems_with_topo FROM (((((area a INNER JOIN region r ON a.region_id=r.id AND a.trash IS NULL) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id AND s.trash IS NULL) INNER JOIN problem p ON s.id=p.sector_id AND p.trash IS NULL) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN svg ON p.id=svg.problem_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int numProblems = rst.getInt("num_problems");
					int numProblemsWithCoordinates = rst.getInt("num_problems_with_coordinates");
					int numProblemsWithTopo = rst.getInt("num_problems_with_topo");
					res = new FrontpageNumProblems(numProblems, numProblemsWithCoordinates, numProblemsWithTopo);
				}
			}
		}
		logger.debug("getFrontpageNumProblems(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public FrontpageNumTicks getFrontpageNumTicks(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		FrontpageNumTicks res = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT t.id) num_ticks FROM (((((tick t INNER JOIN problem p ON t.problem_id=p.id AND p.trash IS NULL) INNER JOIN sector s ON p.sector_id=s.id AND s.trash IS NULL) INNER JOIN area a ON s.area_id=a.id AND a.trash IS NULL) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int numTicks = rst.getInt("num_ticks");
					res = new FrontpageNumTicks(numTicks);
				}
			}
		}
		logger.debug("getFrontpageNumTicks(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public FrontpageRandomMedia getFrontpageRandomMedia(Connection c, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		FrontpageRandomMedia res = null;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT m.id id_media, m.checksum, m.width, m.height, a.id id_area, a.name area, s.id id_sector, s.name sector, p.id id_problem, p.name problem,
				    ROUND((IFNULL(SUM(NULLIF(t.grade, -1)), 0) + p.grade) / (COUNT(CASE WHEN t.grade > 0 THEN t.id END) + 1)) grade,
				    CONCAT('{"id":', u.id, ',"name":"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))), '","avatarCrc32":', CRC32(u.picture), '}') photographer,
				    GROUP_CONCAT(DISTINCT CONCAT('{"id":', u2.id, ',"name":"', TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname, ''))), '","avatarCrc32":', CRC32(u2.picture), '}') SEPARATOR ', ') tagged
				FROM (SELECT m_sub.id
				      FROM media m_sub
				      INNER JOIN media_problem mp_sub ON m_sub.id=mp_sub.media_id
				      INNER JOIN problem p_sub ON mp_sub.problem_id=p_sub.id
				      INNER JOIN sector s_sub ON p_sub.sector_id=s_sub.id
				      INNER JOIN area a_sub ON s_sub.area_id=a_sub.id
				      INNER JOIN region r_sub ON a_sub.region_id=r_sub.id
				      WHERE r_sub.id=?
				        AND m_sub.deleted_user_id IS NULL
				        AND a_sub.trash IS NULL
				        AND s_sub.trash IS NULL
				        AND p_sub.trash IS NULL
				        AND a_sub.access_closed IS NULL
				        AND s_sub.access_closed IS NULL
				        AND m_sub.is_movie=0
				        AND mp_sub.trivia=0
				        AND p_sub.locked_admin=0
				        AND p_sub.locked_superadmin=0
				        AND s_sub.locked_admin=0
				        AND s_sub.locked_superadmin=0
				        AND a_sub.locked_admin=0
				        AND a_sub.locked_superadmin=0
				      ORDER BY RAND()
				      LIMIT 1) random_id
				     INNER JOIN media m ON m.id=random_id.id
				     INNER JOIN media_problem mp ON (m.is_movie=0 AND m.id=mp.media_id AND mp.trivia=0)
				     INNER JOIN problem p ON mp.problem_id=p.id AND p.locked_admin=0 AND p.locked_superadmin=0
				     INNER JOIN sector s ON p.sector_id=s.id AND s.locked_admin=0 AND s.locked_superadmin=0
				     INNER JOIN area a ON s.area_id=a.id AND a.locked_admin=0 AND a.locked_superadmin=0
				     INNER JOIN region r ON a.region_id=r.id
				     INNER JOIN user u ON m.photographer_user_id=u.id
				     LEFT JOIN tick t ON p.id=t.problem_id
				     LEFT JOIN media_user mu ON m.id=mu.media_id
				     LEFT JOIN user u2 ON mu.user_id=u2.id
				WHERE r.id=?
				  AND m.deleted_user_id IS NULL
				  AND a.trash IS NULL
				  AND s.trash IS NULL
				  AND p.trash IS NULL
				  AND a.access_closed IS NULL
				  AND s.access_closed IS NULL
				GROUP BY m.id, m.checksum, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname;
				""")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id_media");
					long crc32 = rst.getInt("checksum");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int idArea = rst.getInt("id_area");
					String area = rst.getString("area");
					int idSector = rst.getInt("id_sector");
					String sector = rst.getString("sector");
					int idProblem = rst.getInt("id_problem");
					String problem = rst.getString("problem");
					int grade = rst.getInt("grade");
					String photographerJson = rst.getString("photographer");
					String taggedJson = rst.getString("tagged");
					User photographer = photographerJson == null? null : gson.fromJson(photographerJson, User.class);
					List<User> tagged = taggedJson == null? null : gson.fromJson("[" + taggedJson + "]", new TypeToken<List<User>>(){});
					res = new FrontpageRandomMedia(idMedia, crc32, width, height, idArea, area, idSector, sector, idProblem, problem, setup.gradeConverter().getGradeFromIdGrade(grade), photographer, tagged);
				}
			}
		}
		logger.debug("getFrontpageRandomMedia(setup={}) - res={}, duration={}", setup, res, stopwatch);
		return res;
	}

	public Collection<GradeDistribution> getGradeDistribution(Connection c, Optional<Integer> authUserId, Setup setup, int optionalAreaId, int optionalSectorId) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = "WITH x AS ("
				+ "  SELECT g.base_no grade_base_no, x.sorting, x.sector, x.t, COUNT(id_problem) num"
				+ "  FROM (SELECT s.name sector, s.sorting, ty.subtype t, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(t.grade) + 1)) grade_id, p.id id_problem"
				+ "    FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN tick t ON (p.id=t.problem_id AND t.grade>0)"
				+ (optionalSectorId!=0? " WHERE p.sector_id=?" : " WHERE a.id=?")
				+ "      AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ "    GROUP BY s.name, ty.subtype, p.id) x, grade g"
				+ "  WHERE x.grade_id=g.grade_id AND g.t=?"
				+ "  GROUP BY x.sorting, x.sector, g.base_no, x.t"
				+ ")"
				+ " SELECT g.base_no grade, x.sector, COALESCE(x.t,'Boulder') t, num"
				+ " FROM (SELECT g.base_no, MIN(g.grade_id) sort FROM grade g WHERE g.t=? GROUP BY g.base_no) g LEFT JOIN x ON g.base_no=x.grade_base_no"
				+ " ORDER BY g.sort, x.sorting, x.sector, x.t";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, optionalSectorId!=0? optionalSectorId : optionalAreaId);
			ps.setString(3, setup.gradeSystem().toString());
			ps.setString(4, setup.gradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					GradeDistribution g = res.get(grade);
					if (g == null) {
						g = new GradeDistribution(grade);
						res.put(grade, g);
					}
					String sector = rst.getString("sector");
					if (sector != null) {
						String t = rst.getString("t");
						int num = rst.getInt("num");
						g.addSector(sector, t, num);
					}
				}
			}
		}
		return res.values();
	}

	public Media getMedia(Connection c, Optional<Integer> authUserId, int id) throws SQLException {
		Media res = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((media m INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE m.id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = null;
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					res = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false, 0, 0, 0, null);
				}
			}
		}
		return res;
	}

	public List<PermissionUser> getPermissions(Connection c, Optional<Integer> authUserId, int idRegion) throws SQLException {
		ensureSuperadminWriteRegion(c, authUserId, idRegion);
		// Return users
		List<PermissionUser> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT x.id, x.name, CRC32(x.picture) avatar_crc32, DATE_FORMAT(MAX(x.last_login),'%Y.%m.%d') last_login, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write
				FROM (SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, u.picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM (user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN user_region ur ON u.id=ur.user_id AND l.region_id=ur.region_id WHERE l.region_id=? GROUP BY u.id, u.firstname, u.lastname, u.picture UNION SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, u.picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM user u, user_region ur, user_login l
				WHERE u.id=ur.user_id AND ur.region_id=? AND u.id=l.user_id GROUP BY u.id, u.firstname, u.lastname, u.picture) x
				GROUP BY x.id, x.name, x.picture, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write
				ORDER BY IFNULL(x.superadmin_write,0) DESC, IFNULL(x.superadmin_read,0) DESC, IFNULL(x.admin_write,0) DESC, IFNULL(x.admin_read,0) DESC, x.name
				""")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String lastLogin = rst.getString("last_login");
					boolean adminRead = rst.getBoolean("admin_read");
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superadminRead = rst.getBoolean("superadmin_read");
					boolean superadminWrite = rst.getBoolean("superadmin_write");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new PermissionUser(userId, name, avatarCrc32, timeAgo, adminRead, adminWrite, superadminRead, superadminWrite, authUserId.orElse(0)==userId));
				}
			}
		}
		return res;
	}

	public Problem getProblem(Connection c, Optional<Integer> authUserId, Setup s, int reqId, boolean showHiddenMedia, boolean updateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (updateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		List<Integer> todoIdProblems = new ArrayList<>();
		ProfileTodo todo = getProfileTodo(c, authUserId, s, authUserId.orElse(0));
		if (todo != null) {
			for (ProfileTodoArea ta : todo.areas()) {
				for (ProfileTodoSector ts : ta.sectors()) {
					for (ProfileTodoProblem tp : ts.problems()) {
						todoIdProblems.add(tp.getId());
					}
				}
			}
		}
		Problem p = null;
		try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, s.access_info sector_access_info, s.access_closed sector_access_closed, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour, sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, CONCAT(r.url,'/problem/',p.id) canonical, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,
				       ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, p.grade original_grade, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				       group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\",\"avatarCrc32\":', crc32(COALESCE(u.picture,'')), '}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,
				       COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars,
				       MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype,
				       p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent
				FROM ((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND p.id=?
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				  AND (r.id=? OR ur.user_id IS NOT NULL)
				GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.name, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, s.id, s.locked_admin, s.locked_superadmin, s.name, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, sc.id, sc.latitude, sc.longitude, sc.elevation, sc.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, p.grade, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, p.fa_date, ty.id, ty.type, ty.subtype, p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent
				ORDER BY p.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, authUserId.orElse(0));
			ps.setInt(4, s.idRegion());
			ps.setInt(5, reqId);
			ps.setInt(6, s.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaName = rst.getString("area_name");
					String areaAccessInfo = rst.getString("area_access_info");
					String areaAccessClosed = rst.getString("area_access_closed");
					boolean areaNoDogsAllowed = rst.getBoolean("area_no_dogs_allowed");
					int areaSunFromHour = rst.getInt("area_sun_from_hour");
					int areaSunToHour = rst.getInt("area_sun_to_hour");
					int sectorId = rst.getInt("sector_id");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					String sectorAccessInfo = rst.getString("sector_access_info");
					String sectorAccessClosed = rst.getString("sector_access_closed");
					int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
					int sectorSunToHour = rst.getInt("sector_sun_to_hour");
					int parkingidCoordinates = rst.getInt("sector_parking_coordinates_id");
					Coordinates sectorParking = parkingidCoordinates == 0? null : new Coordinates(parkingidCoordinates, rst.getDouble("sector_parking_latitude"), rst.getDouble("sector_parking_longitude"), rst.getDouble("sector_parking_elevation"), rst.getString("sector_parking_elevation_source"));
					List<Coordinates> sectorOutline = getSectorOutline(c, sectorId);
					CompassDirection sectorWallDirectionCalculated = getCompassDirection(s, rst.getInt("sector_compass_direction_id_calculated"));
					CompassDirection sectorWallDirectionManual = getCompassDirection(s, rst.getInt("sector_compass_direction_id_manual"));
					Slope sectorApproach = getSectorSlopes(c, true, Collections.singleton(sectorId)).getOrDefault(sectorId, null);
					Slope sectorDescent = getSectorSlopes(c, false, Collections.singleton(sectorId)).getOrDefault(sectorId, null);
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int originalGrade = rst.getInt("original_grade");
					String faDate = rst.getString("fa_date");
					String faDateHr = rst.getString("fa_date_hr");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String faStr = rst.getString("fa");
					List<User> fa = Strings.isNullOrEmpty(faStr) ? null : gson.fromJson("[" + faStr + "]", new TypeToken<List<User>>(){});
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaProblem(c, s, authUserId, areaId, sectorId, id, showHiddenMedia);
					if (allMedia != null && allMedia.size() > 0) {
						media = allMedia.stream().filter(x -> !x.trivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.trivia()).collect(Collectors.toList());
						}
					}
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					String trivia = rst.getString("trivia");
					String startingAltitude = rst.getString("starting_altitude");
					String aspect = rst.getString("aspect");
					String routeLength = rst.getString("route_length");
					String descent = rst.getString("descent");

					SectorProblem neighbourPrev = null;
					SectorProblem neighbourNext = null;
					List<SectorProblem> problems = getSectorProblems(c, s, authUserId, sectorId);
					if (problems.size() > 1) {
						for (int i = 0; i < problems.size(); i++) {
							SectorProblem prob = problems.get(i);
							if (prob.id() == id) {
								neighbourPrev = problems.get((i == 0? problems.size()-1 : i-1));
								neighbourNext = problems.get((i == problems.size()-1? 0 : i+1));
								if (neighbourPrev.id() == neighbourNext.id()) {
									if (nr < neighbourPrev.nr()) {
										neighbourPrev = null;
									}
									else {
										neighbourNext = null;
									}
								}
							}
						}
					}
					var externalLinks = getExternalLinksProblem(c, reqId, false);
					externalLinks.addAll(getExternalLinksSector(c, sectorId, true));
					externalLinks.addAll(getExternalLinksArea(c, areaId, true));
					p = new Problem(null, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour,
							sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed,
							sectorSunFromHour, sectorSunToHour,
							sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorApproach, sectorDescent,
							neighbourPrev, neighbourNext,
							canonical, id, broken, false, lockedAdmin, lockedSuperadmin, nr, name, rock, comment,
							s.gradeConverter().getGradeFromIdGrade(grade),
							s.gradeConverter().getGradeFromIdGrade(originalGrade), faDate, faDateHr, fa, coordinates,
							media, numTicks, stars, ticked, null, t, todoIdProblems.contains(id), externalLinks, pageViews,
							trivia, triviaMedia, startingAltitude, aspect, routeLength, descent);
				}
			}
		}
		if (p == null) {
			// Poblem not found, see if it's visible on a different domain
			Redirect res = getCanonicalUrl(c, 0, 0, reqId);
			if (!Strings.isNullOrEmpty(res.redirectUrl())) {
				return new Problem(res.redirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, null, null, 0, 0, false, null, null, false, null, null, null, null, null, null, null, null);
			}
		}

		Preconditions.checkNotNull(p, "Could not find problem with id=" + reqId);
		// Ascents
		Map<Integer, ProblemTick> tickLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT t.id id_tick, u.id id_user, CRC32(u.picture) avatar_crc32, CAST(t.date AS char) date, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, t.comment, t.stars, t.grade
				FROM tick t, user u
				WHERE t.problem_id=? AND t.user_id=u.id
				ORDER BY t.date DESC, t.id DESC
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id_tick");
					int idUser = rst.getInt("id_user");
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String date = rst.getString("date");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					double stars = rst.getDouble("stars");
					int idGrade = rst.getInt("grade");
					String grade = null;
					boolean noPersonalGrade = false;
					if (idGrade == -1) {
						noPersonalGrade = true;
					}
					else {
						grade = s.gradeConverter().getGradeFromIdGrade(idGrade);
					}
					boolean writable = idUser == authUserId.orElse(0);
					ProblemTick t = p.addTick(id, idUser, avatarCrc32, date, name, grade, noPersonalGrade, comment, stars, writable);
					tickLookup.put(id, t);
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT r.id, r.tick_id, r.date, r.comment
				FROM tick t, tick_repeat r
				WHERE t.problem_id=? AND t.id=r.tick_id
				ORDER BY r.tick_id, r.date, r.id
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int tickId = rst.getInt("tick_id");
					String date = rst.getString("date");
					String comment = rst.getString("comment");
					tickLookup.get(tickId).addRepeat(id, tickId, date, comment);
				}
			}
		}
		// Todos
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT u.id, CRC32(u.picture) avatar_crc32, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name
				FROM todo t, user u
				WHERE t.user_id=u.id AND t.problem_id=?
				ORDER BY u.firstname, u.lastname
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idUser = rst.getInt("id");
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String name = rst.getString("name");
					p.addTodo(idUser, avatarCrc32, name);
				}
			}
		}
		// Comments
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT g.id, CAST(g.post_time AS char) date, u.id user_id, CRC32(u.picture) avatar_crc32, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, g.message, g.danger, g.resolved
				FROM guestbook g, user u
				WHERE g.problem_id=? AND g.user_id=u.id
				ORDER BY g.post_time DESC
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String date = rst.getString("date");
					int idUser = rst.getInt("user_id");
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String name = rst.getString("name");
					String message = rst.getString("message");
					boolean danger = rst.getBoolean("danger");
					boolean resolved = rst.getBoolean("resolved");
					List<Media> media = getMediaGuestbook(c, authUserId, id);
					p.addComment(id, date, idUser, avatarCrc32, name, message, danger, resolved, media);
				}
				if (p.getComments() != null && !p.getComments().isEmpty()) {
					// Enable editing on last comment in thread if it is written by authenticated user
					Optional<ProblemComment> lastComment = p.getComments().stream().max(Comparator.comparing(ProblemComment::getId));
					if (lastComment.isPresent() && lastComment.get().getIdUser() == authUserId.orElse(0)) {
						lastComment.get().setEditable(true);
					}
				}
			}
		}
		// Sections
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT ps.id, ps.nr, ps.description, ps.grade
				FROM problem_section ps
				WHERE ps.problem_id=?
				ORDER BY ps.nr
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					int grade = rst.getInt("grade");
					List<Media> sectionMedia = null;
					if (p.getMedia() != null) {
						sectionMedia = p.getMedia()
								.stream()
								.filter(x -> x.pitch() == nr)
								.toList();
						p.getMedia().removeAll(sectionMedia);
					}
					p.addSection(id, nr, description, s.gradeConverter().getGradeFromIdGrade(grade), sectionMedia);
				}
			}
		}
		// First aid ascent
		if (!s.gradeSystem().equals(GradeSystem.BOULDER)) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT DATE_FORMAT(a.aid_date,'%Y-%m-%d') aid_date, DATE_FORMAT(a.aid_date,'%d/%m-%y') aid_date_hr, a.aid_description, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32
					FROM (fa_aid a LEFT JOIN fa_aid_user au ON a.problem_id=au.problem_id) LEFT JOIN user u ON au.user_id=u.id
					WHERE a.problem_id=?
					""")) {
				ps.setInt(1, p.getId());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String aidDate = rst.getString("aid_date");
						String aidDateHr = rst.getString("aid_date_hr");
						String aidDescription = rst.getString("aid_description");
						FaAid faAid = p.getFaAid();
						if (faAid == null) {
							faAid = new FaAid(p.getId(), aidDate, aidDateHr, aidDescription, new ArrayList<>());
							p.setFaAid(faAid);
						}
						int userId = rst.getInt("id");
						if (userId != 0) {
							String userName = rst.getString("name");
							long avatarCrc32 = rst.getLong("avatar_crc32");
							User user = User.from(userId, userName, avatarCrc32);
							faAid.users().add(user);
						}
					}
				}
			}
		}
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}) - duration={} - p={}", authUserId, s.idRegion(), reqId, stopwatch, p);
		return p;
	}

	public Profile getProfile(Connection c, Optional<Integer> authUserId, Setup setup, int reqUserId) throws SQLException {
		int userId = reqUserId > 0? reqUserId : authUserId.orElse(0);
		Preconditions.checkArgument(userId > 0);
		Profile res = null;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT CRC32(u.picture) avatar_crc32, u.firstname, u.lastname, u.email_visible_to_all,
				       CASE WHEN u.email_visible_to_all=1 THEN GROUP_CONCAT(DISTINCT e.email ORDER BY e.email SEPARATOR ';') END emails,
				                   MAX(l.when) last_login
				FROM (user u LEFT JOIN user_email e ON u.id=e.user_id) LEFT JOIN user_login l ON u.id=l.user_id
				WHERE u.id=?
				GROUP BY u.id, u.picture, u.firstname, u.lastname
				""")) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					long avatarCrc32 = rst.getLong("avatar_crc32");
					String firstname = rst.getString("firstname");
					String lastname = rst.getString("lastname");
					boolean emailVisibleToAll = rst.getBoolean("email_visible_to_all");
					String emailsStr = rst.getString("emails");
					List<String> emails = Strings.isNullOrEmpty(emailsStr) ? null : Splitter.on(';')
							.trimResults()
							.omitEmptyStrings()
							.splitToList(emailsStr);
					List<UserRegion> userRegions = userId == authUserId.orElse(0)? getUserRegion(c, authUserId, setup) : null;
					LocalDateTime lastLogin = rst.getObject("last_login", LocalDateTime.class);
					String lastActivity = lastLogin == null ? null : TimeAgo.getTimeAgo(lastLogin.toLocalDate());
					res = new Profile(userId, avatarCrc32, firstname, lastname, emailVisibleToAll, emails, userRegions, lastActivity);
				}
			}
		}
		return res;
	}

	public List<Media> getProfileMediaCapturedArea(Connection c, Optional<Integer> authUserId, int reqId) throws SQLException {
		String sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, MAX(a.name) location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(a.id) area_id FROM ((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_area ma ON m.id=ma.media_id) INNER JOIN area a ON ma.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		List<Media> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int areaId = rst.getInt("area_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/area/" + areaId;
					Media m = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false, 0, 0, 0, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public List<Media> getProfileMediaCapturedSector(Connection c, Optional<Integer> authUserId, int reqId) throws SQLException {
		String sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(s.name),' (',MAX(a.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(s.id) sector_id  FROM (((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_sector ms ON m.id=ms.media_id) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		List<Media> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int sectorId = rst.getInt("sector_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/sector/" + sectorId;
					Media m = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false, 0, 0, 0, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public List<Media> getProfileMediaProblem(Connection c, Optional<Integer> authUserId, int reqId, boolean captured) throws SQLException {
		String sqlStr = null;
		if (captured) {
			sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, MAX(mp.pitch) pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(p.id) problem_id  FROM ((((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		}
		else {
			sqlStr = "SELECT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(p.id) problem_id FROM ((((((((user u INNER JOIN media_user mu ON u.id=? AND u.id=mu.user_id) INNER JOIN media m ON mu.media_id=m.id AND m.deleted_user_id IS NULL) INNER JOIN user c ON m.photographer_user_id=c.id) INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY u.firstname, u.lastname, u.picture, m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, mp.pitch, c.firstname, c.lastname ORDER BY m.id DESC";
		}
		List<Media> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int problemId = rst.getInt("problem_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/problem/" + problemId;
					Media m = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false, 0, 0, 0, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public ProfileStatistics getProfileStatistics(Connection c, Optional<Integer> authUserId, Setup setup, int reqId) throws SQLException {
		Map<Integer, ProfileStatistics.ProfileStatisticsTick> idProblemTickMap = new HashMap<>();
		ProfileStatistics res = new ProfileStatistics();
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m_a2.is_movie=0 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=0 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=0 THEN m_p2.id END) num_images_created, COUNT(DISTINCT CASE WHEN m_a2.is_movie=1 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=1 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=1 THEN m_p2.id END) num_videos_created FROM ((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL AND m_a2.photographer_user_id=u.id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL AND m_s2.photographer_user_id=u.id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL AND m_p2.photographer_user_id=u.id")) {
			ps.setInt(1, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImagesCreated(rst.getInt("num_images_created"));
					res.setNumVideosCreated(rst.getInt("num_videos_created"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=0 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=0 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=0 THEN m_p.id END) num_image_tags, COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=1 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=1 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=1 THEN m_p.id END) num_video_tags FROM (((((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL) LEFT JOIN media_user mu_a ON m_a2.id=mu_a.media_id AND u.id=mu_a.user_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL) LEFT JOIN media_user mu_s ON m_s2.id=mu_s.media_id AND u.id=mu_s.user_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL) LEFT JOIN media_user mu_p ON m_p2.id=mu_p.media_id AND u.id=mu_p.user_id")) {
			ps.setInt(1, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImageTags(rst.getInt("num_image_tags"));
					res.setNumVideoTags(rst.getInt("num_video_tags"));
				}
			}
		}

		// Tick
		String sqlStr = "SELECT r.name region_name, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id id_tick, 0 id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr, CASE WHEN t.id IS NULL THEN -1 ELSE t.stars END stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL AND t.grade>=0 THEN t.grade ELSE p.grade END) grade, CASE WHEN t.id IS NOT NULL AND t.grade=-1 THEN 1 ELSE 0 END no_personal_grade"
				+ " FROM ((((((((problem p INNER JOIN type ty ON p.type_id=ty.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)"
				+ " WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					String subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					boolean noPersonalGrade = rst.getBoolean("no_personal_grade");
					ProfileStatistics.ProfileStatisticsTick tick = res.addTick(regionName, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, setup.gradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
					idProblemTickMap.put(idProblem, tick);
				}
			}
		}
		// Tick_repeat
		sqlStr = "SELECT r.name region_name, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id id_tick, tr.id id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, tr.comment, DATE_FORMAT(tr.date,'%Y-%m-%d') date, DATE_FORMAT(tr.date,'%d/%m-%y') date_hr, t.stars, 0 fa, t.grade"
				+ " FROM ((((((((problem p INNER JOIN type ty ON p.type_id=ty.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN tick t ON p.id=t.problem_id AND t.user_id=?) INNER JOIN tick_repeat tr ON t.id=tr.tick_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, tr.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, tr.comment, tr.date, t.stars, t.grade";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					String subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					boolean noPersonalGrade = false;
					res.addTick(regionName, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, setup.gradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
				}
			}
		}
		// First aid ascent
		if (!setup.gradeSystem().equals(GradeSystem.BOULDER)) {
			try (PreparedStatement ps = c.prepareStatement("SELECT r.name region_name, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr" +
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId.orElse(0));
				ps.setInt(3, setup.idRegion());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String regionName = rst.getString("region_name");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int numPitches = rst.getInt("num_pitches");
						int idProblem = rst.getInt("id_problem");
						boolean lockedAdmin = rst.getBoolean("locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						if (!Strings.isNullOrEmpty(comment)) {
							comment = "First ascent (AID): " + comment;
						}
						else {
							comment = "First ascent (AID)";
						}
						String date = rst.getString("date");
						String dateHr = rst.getString("date_hr");
						int grade = 0;
						boolean noPersonalGrade = false;
						Optional<ProfileStatistics.ProfileStatisticsTick> optTick = res.getTicks().stream()
								.filter(x -> x.getIdProblem() == idProblem)
								.findAny();
						if (optTick.isPresent()) {
							// User has ticked route, update this (don't add an extra First Ascent (AID))
							ProfileStatistics.ProfileStatisticsTick tick = optTick.get();
							tick.setFa(true);
							if (tick.getDate() == null && date != null) {
								tick.setDate(date);
							}
							if (tick.getDateHr() == null && dateHr != null) {
								tick.setDateHr(dateHr);
							}
						}
						else {
							ProfileStatistics.ProfileStatisticsTick tick = res.addTick(regionName, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, 0, 0, "Aid", numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, 0, true, setup.gradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
							idProblemTickMap.put(idProblem, tick);
						}
					}
				}
			}
		}
		if (!idProblemTickMap.isEmpty()) {
			// Fill coordinates
			Map<Integer, Coordinates> idProblemCoordinates = getProblemCoordinates(c, idProblemTickMap.keySet());
			for (int idProblem : idProblemCoordinates.keySet()) {
				Coordinates coordinates = idProblemCoordinates.get(idProblem);
				idProblemTickMap.get(idProblem).setCoordinates(coordinates);
			}
		}
		// Order ticks
		res.getTicks().sort((t1, t2) -> -ComparisonChain
				.start()
				.compare(Strings.nullToEmpty(t1.getDate()), Strings.nullToEmpty(t2.getDate()))
				.compare(t1.getId(), t2.getId())
				.compare(t1.getIdProblem(), t2.getIdProblem())
				.result());
		for (int i = 0; i < res.getTicks().size(); i++) {
			res.getTicks().get(i).setNum(i);
		}
		return res;
	}

	public ProfileTodo getProfileTodo(Connection c, Optional<Integer> authUserId, Setup setup, int reqId) throws SQLException {
		final int userId = reqId > 0? reqId : authUserId.orElse(0);
		ProfileTodo res = new ProfileTodo(new ArrayList<>());

		// Build lists
		Map<Integer, ProfileTodoArea> areaLookup = new HashMap<>();
		Map<Integer, ProfileTodoSector> sectorLookup = new HashMap<>();
		Map<Integer, ProfileTodoProblem> problemLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.nr problem_nr, p.name problem_name, p.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN todo t ON p.id=t.problem_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND t.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY r.url, t.id, a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.locked_admin, s.locked_superadmin, s.name, p.id, p.nr, p.name, p.grade, p.locked_admin, p.locked_superadmin ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			ps.setInt(4, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					ProfileTodoArea a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						a = new ProfileTodoArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin, new ArrayList<>());
						res.areas().add(a);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					ProfileTodoSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new ProfileTodoSector(sectorId, sectorUrl, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int todoId = rst.getInt("todo_id");
					int problemId = rst.getInt("problem_id");
					String problemUrl = rst.getString("problem_url");
					int problemNr = rst.getInt("problem_nr");
					String problemName = rst.getString("problem_name");
					int problemGrade = rst.getInt("problem_grade");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					ProfileTodoProblem p = new ProfileTodoProblem(todoId, problemId, problemUrl, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, setup.gradeConverter().getGradeFromIdGrade(problemGrade));
					s.problems().add(p);
					problemLookup.put(problemId, p);
				}
			}
		}
		if (!problemLookup.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT t.problem_id, u.id, u.firstname, u.lastname
					FROM todo t, user u
					WHERE t.user_id=u.id AND t.user_id!=? AND problem_id IN (%s)
					ORDER BY t.problem_id, u.firstname, u.lastname
					""".formatted(Joiner.on(",").join(problemLookup.keySet())))) {
				ps.setInt(1, userId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int problemId = rst.getInt("problem_id");
						int id = rst.getInt("id");
						String firstname = rst.getString("firstname");
						String lastname = rst.getString("lastname");
						problemLookup.get(problemId).getPartners().add(User.from(id, firstname, lastname));
					}
				}
			}
			if (!problemLookup.isEmpty()) {
				// Fill coordinates
				Map<Integer, Coordinates> idProblemCoordinates = getProblemCoordinates(c, problemLookup.keySet());
				for (int idProblem : idProblemCoordinates.keySet()) {
					Coordinates coordinates = idProblemCoordinates.get(idProblem);
					problemLookup.get(idProblem).setCoordinates(coordinates);
				}
			}
		}
		// Sort areas (ae, oe, aa is sorted wrong by MySql):
		res.areas().sort(Comparator.comparing(ProfileTodoArea::name));
		logger.debug("getProfileTodo(authUserId={}, idRegion={}, reqId={}) - res.areas().size()={}", authUserId, setup.idRegion(), reqId, res.areas().size());
		return res;
	}

	public List<Search> getSearch(Connection c, Optional<Integer> authUserId, Setup setup, String search) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		String searchRegexPattern = "(^|\\W)" + search;
		List<Search> areas = new ArrayList<>();
	    List<Search> externalAreas = new ArrayList<>();
	    List<Search> sectors = new ArrayList<>();
	    List<Search> problems = new ArrayList<>();
	    List<Search> users = new ArrayList<>();
	    Set<Integer> areaIdsVisible = new HashSet<>();
	    String sqlStr = """
	    		WITH req AS (
					SELECT ? auth_user_id, ? region_id, ? search_regex
				)
				-- Areas
				(SELECT 'AREA' result_type, a.id, a.name main_title, NULL sub_title, 
				        a.locked_admin, a.locked_superadmin, MAX(m.id) media_id, MAX(m.checksum) media_crc32, a.hits, 
				        NULL external_url, 0 grade, NULL rock
				 FROM area a
				 JOIN region r ON a.region_id=r.id
				 JOIN region_type rt ON r.id=rt.region_id
				 CROSS JOIN req
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN media_area ma ON a.id=ma.area_id
				 LEFT JOIN media m ON ma.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				 WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				   AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				   AND REGEXP_LIKE(a.name, req.search_regex, 'i')
				   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.hits
				 ORDER BY a.hits DESC, a.name LIMIT 8)
				
				UNION ALL
				
				-- External Areas
				(SELECT 'EXTERNAL' result_type, a_ext.id, a_ext.name, r_ext.name, 
				        0, 0, 0, 0, a_ext.hits, 
				        CONCAT(r_ext.url, '/area/', a_ext.id), 0, NULL
				 FROM region r
				 CROSS JOIN req
				 JOIN region_type rt ON r.id=rt.region_id
				 JOIN region_type rt_ext ON rt.type_id=rt_ext.type_id
				 JOIN region r_ext ON rt_ext.region_id=r_ext.id
				 JOIN area a_ext ON r_ext.id=a_ext.region_id AND a_ext.locked_admin=0 AND a_ext.locked_superadmin=0
				 WHERE r.id=req.region_id AND r.id != r_ext.id 
				   AND REGEXP_LIKE(a_ext.name, req.search_regex, 'i')
				 GROUP BY r_ext.url, a_ext.id, a_ext.name, r_ext.name, a_ext.hits
				 ORDER BY a_ext.hits DESC, a_ext.name LIMIT 3)
				
				UNION ALL
				
				-- Sectors
				(SELECT 
				    'SECTOR' result_type, s.id, s.name, a.name, 
				    s.locked_admin, s.locked_superadmin, MAX(m.id), MAX(m.checksum), s.hits, 
				    NULL, 0, NULL
				 FROM area a
				 JOIN sector s ON a.id=s.area_id
				 JOIN region r ON a.region_id=r.id
				 JOIN region_type rt ON r.id=rt.region_id
				 CROSS JOIN req
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN media_sector ms ON s.id=ms.sector_id
				 LEFT JOIN media m ON ms.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				 WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				   AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				   AND REGEXP_LIKE(s.name, req.search_regex, 'i')
				   AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				 GROUP BY s.id, a.name, s.name, s.locked_admin, s.locked_superadmin, s.hits
				 ORDER BY s.hits DESC, a.name, s.name LIMIT 8)
				
				UNION ALL
				
				-- Problems
				(SELECT 'PROBLEM' result_type, p.id, p.name, CONCAT(a.name, ' / ', s.name), 
				        p.locked_admin, p.locked_superadmin, MAX(m.id), MAX(m.checksum), p.hits, 
				        NULL, 
				        ROUND((IFNULL(SUM(NULLIF(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)), 
				        p.rock
				 FROM area a
				 JOIN sector s ON a.id=s.area_id
				 JOIN problem p ON s.id=p.sector_id
				 JOIN region r ON a.region_id=r.id
				 JOIN region_type rt ON r.id=rt.region_id
				 CROSS JOIN req
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
				 LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				 LEFT JOIN tick t ON p.id=t.problem_id
				 WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				   AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				   AND (REGEXP_LIKE(p.name, req.search_regex, 'i') OR REGEXP_LIKE(p.rock, req.search_regex, 'i'))
				   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				 GROUP BY a.name, s.name, p.id, p.name, p.rock, p.grade, p.locked_admin, p.locked_superadmin, p.hits
				 ORDER BY p.hits DESC, p.name LIMIT 8)
				
				UNION ALL
				
				-- Users
				(SELECT 'USER' result_type, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), NULL, 
				        0, 0, 0, CRC32(u.picture), 0, 
				        NULL, 0, NULL
				 FROM user u
				 CROSS JOIN req
				 WHERE REGEXP_LIKE(CONCAT(' ', u.firstname, ' ', COALESCE(u.lastname,'')), req.search_regex, 'i')
				 ORDER BY TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) LIMIT 8);
	    		""";
	    try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
	        ps.setInt(1, authUserId.orElse(0));
	        ps.setInt(2, setup.idRegion());
	        ps.setString(3, searchRegexPattern);
	        try (ResultSet rst = ps.executeQuery()) {
	            while (rst.next()) {
	                String type = rst.getString("result_type");
	                int id = rst.getInt("id");
	                String title = rst.getString("main_title");
	                String subTitle = rst.getString("sub_title");
	                long hits = rst.getLong("hits");
	                String pageViews = (hits > 0) ? HitsFormatter.formatHits(hits) : null;
	                int mediaId = rst.getInt("media_id");
	                long mediaCrc32 = rst.getInt("media_crc32");
	                boolean lockedAdmin = rst.getBoolean("locked_admin");
	                boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
	                switch (type) {
	                    case "AREA" -> {
	                        areaIdsVisible.add(id);
	                        areas.add(new Search(title, null, "/area/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin, hits, pageViews));
	                    }
	                    case "EXTERNAL" -> {
	                        externalAreas.add(new Search(title, subTitle, null, rst.getString("external_url"), null, 0, 0, false, false, hits, pageViews));
	                    }
	                    case "SECTOR" -> {
	                        sectors.add(new Search(title, subTitle, "/sector/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin, hits, pageViews));
	                    }
	                    case "PROBLEM" -> {
	                        int grade = rst.getInt("grade");
	                        String rock = rst.getString("rock");
	                        String fullTitle = title + " [" + setup.gradeConverter().getGradeFromIdGrade(grade) + "]";
	                        String fullSub = subTitle + (rock == null ? "" : " (rock: " + rock + ")");
	                        problems.add(new Search(fullTitle, fullSub, "/problem/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin, hits, pageViews));
	                    }
	                    case "USER" -> {
	                        long avatarCrc32 = rst.getLong("media_crc32"); 
	                        String mediaUrl = (avatarCrc32 == 0) ? null : IOHelper.getFullUrlAvatar(setup, id, avatarCrc32);
	                        users.add(new Search(title, null, "/user/" + id, null, mediaUrl, 0, 0, false, false, 0, null));
	                    }
	                }
	            }
	        }
	    }
	    // Truncate logic
	    while (areas.size() + sectors.size() + problems.size() + users.size() > 10) {
	        if (problems.size() > 5) {
	        	problems.remove(problems.size() - 1);
	        }
	        else if (areas.size() > 2) {
	        	areas.remove(areas.size() - 1);
	        }
	        else if (sectors.size() > 2) {
	        	sectors.remove(sectors.size() - 1);
	        }
	        else if (users.size() > 1) {
	        	users.remove(users.size() - 1);
	        }
	    }
	    // Filter External Areas
	    List<Search> filteredExternal = externalAreas.stream()
	            .filter(ea -> {
	                try {
	                    String url = ea.externalurl();
	                    int extId = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1));
	                    return !areaIdsVisible.contains(extId);
	                } catch (Exception e) { return true; }
	            }).toList();
	    // Assemble and Final Sort
	    List<Search> res = new ArrayList<>();
	    res.addAll(areas);
	    res.addAll(sectors);
	    res.addAll(problems);
	    res.sort((r1, r2) -> Long.compare(r2.hits(), r1.hits()));
	    res.addAll(users);
	    res.addAll(filteredExternal);
        logger.debug("getSearch(search={}) - res.size()={}, duration={})", search, res.size(), stopwatch);
        return res;
	}

	public Sector getSector(Connection c, Optional<Integer> authUserId, boolean orderByGrade, Setup setup, int reqId, boolean updateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (updateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE sector SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Sector s = null;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, a.name area_name, a.description area_description, CONCAT(r.url,'/sector/',s.id) canonical, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits
				FROM area a INNER JOIN region r ON a.region_id=r.id
				INNER JOIN sector s ON a.id=s.area_id
				LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE s.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaAccessInfo = rst.getString("area_access_info");
					String areaAccessClosed = rst.getString("area_access_closed");
					boolean areaNoDogsAllowed = rst.getBoolean("area_no_dogs_allowed");
					int areaSunFromHour = rst.getInt("area_sun_from_hour");
					int areaSunToHour = rst.getInt("area_sun_to_hour");
					String areaName = rst.getString("area_name");
					String areaComment = rst.getString("area_description");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates parking = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					List<Coordinates> sectorOutline = getSectorOutline(c, reqId);
					Slope sectorApproach = getSectorSlopes(c, true, Collections.singleton(reqId)).getOrDefault(reqId, null);
					Slope sectorDescent = getSectorSlopes(c, false, Collections.singleton(reqId)).getOrDefault(reqId, null);
					CompassDirection wallDirectionCalculated = getCompassDirection(setup, rst.getInt("compass_direction_id_calculated"));
					CompassDirection wallDirectionManual = getCompassDirection(setup, rst.getInt("compass_direction_id_manual"));
					String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaSector(c, setup, authUserId, reqId, 0, false, areaId, 0, 0, false);
					allMedia.addAll(getMediaArea(c, authUserId, areaId, true, 0, reqId, 0));
					if (!allMedia.isEmpty()) {
						media = allMedia.stream().filter(x -> !x.trivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.trivia()).collect(Collectors.toList());
						}
					}

					if (media != null && media.isEmpty()) {
						media = null;
					}
					var externalLinks = getExternalLinksSector(c, reqId, false);
					externalLinks.addAll(getExternalLinksArea(c, areaId, true));
					s = new Sector(null, orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, areaName, areaComment, canonical, reqId, false, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, sectorOutline, wallDirectionCalculated, wallDirectionManual, sectorApproach, sectorDescent, media, triviaMedia, null, externalLinks, pageViews);
				}
			}
		}
		if (s == null) {
			// Sector not found, see if it's visible on a different domain
			Redirect res = getCanonicalUrl(c, 0, reqId, 0);
			if (!Strings.isNullOrEmpty(res.redirectUrl())) {
				return new Sector(res.redirectUrl(), false, 0, false, false, null, null, false, 0, 0, null, null, null, 0, false, false, false, null, null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null);
			}
		}

		Preconditions.checkNotNull(s, "Could not find sector with id=" + reqId);
		try (PreparedStatement ps = c.prepareStatement("SELECT s.id, s.locked_admin, s.locked_superadmin, s.name, s.sorting FROM ((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.sorting ORDER BY s.sorting, s.name")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, s.getAreaId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					int sorting = rst.getInt("sorting");
					s.addSector(id, lockedAdmin, lockedSuperadmin, name, sorting);
				}
			}
		}
		s.orderSectors();
		for (SectorProblem sp : getSectorProblems(c, setup, authUserId, reqId)) {
			s.addProblem(sp);
		}
		if (!s.getProblems().isEmpty() && orderByGrade) {
			Collections.sort(s.getProblems(), Comparator.comparing(SectorProblem::gradeNumber).reversed());
		}
		logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, stopwatch);
		return s;
	}

	public List<Setup> getSetups(Connection c) throws SQLException {
		List<Setup> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id id_region, r.title, r.description, REPLACE(REPLACE(r.url,'https://',''),'http://','') domain, r.latitude, r.longitude, r.default_zoom, t.group FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id GROUP BY r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group ORDER BY r.id")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					String title = rst.getString("title");
					String description = rst.getString("description");
					String domain = rst.getString("domain");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					int defaultZoom = rst.getInt("default_zoom");
					String group = rst.getString("group");
					GradeSystem gradeSystem = switch (group) {
					case "Bouldering" -> GradeSystem.BOULDER;
					case "Climbing" -> GradeSystem.CLIMBING;
					case "Ice" -> GradeSystem.ICE;
					default -> throw new IllegalArgumentException("Invalid group: " + group);
					};
					List<CompassDirection> compassDirections = getCompassDirections(c);
					GradeConverter gradeConverter = new GradeConverter(getGrades(c, gradeSystem));
					res.add(Setup.newBuilder(domain, gradeSystem)
							.withIdRegion(idRegion)
							.withTitle(title)
							.withDescription(description)
							.withDefaultCenter(new LatLng(latitude, longitude))
							.withDefaultZoom(defaultZoom)
							.withCompassDirections(compassDirections)
							.withGradeConverter(gradeConverter)
							.build());
				}
			}
		}
		logger.debug("getSetups() - res.size()={}", res.size());
		return res;
	}

	public String getSitemapTxt(Connection c, Setup setup) throws SQLException {
		List<String> urls = new ArrayList<>();
		// Fixed urls
		urls.add(setup.url());
		urls.add(setup.url() + "/about");
		urls.add(setup.url() + "/areas");
		urls.add(setup.url() + "/filter");
		urls.add(setup.url() + "/gpl-3.0.txt");
		urls.add(setup.url() + "/problems");
		urls.add(setup.url() + "/sites/bouldering");
		urls.add(setup.url() + "/sites/climbing");
		urls.add(setup.url() + "/sites/ice");
		// Users
		try (PreparedStatement ps = c.prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=t.problem_id GROUP BY t.user_id")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					urls.add(setup.url() + "/user/" + userId);
				}
			}
		}
		// Areas, sectors, problems
		try (PreparedStatement ps = c.prepareStatement("SELECT CONCAT('/area/', a.id) suffix FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					urls.add(setup.url() + rst.getString("suffix"));
				}
			}
		}
		return Joiner.on("\r\n").join(urls);
	}

	public List<Site> getSites(Connection c, int currIdRegion) throws SQLException {
		Map<Integer, Site> regionLookup = new LinkedHashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id region_id, t.group, r.name, r.url FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String group = rst.getString("group");
					String name = rst.getString("name");
					String url = rst.getString("url");
					boolean active = idRegion == currIdRegion;
					regionLookup.put(idRegion, new Site(group, name, url, active, new ArrayList<>()));
				}
			}
		}
		if (!regionLookup.isEmpty()) {
			// Fill region outlines
			Multimap<Integer, Coordinates> idRegionOutline = getRegionOutlines(c, regionLookup.keySet());
			for (int idRegion : idRegionOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idRegionOutline.get(idRegion));
				regionLookup.get(idRegion).outline().addAll(outline);
			}
		}
		return Lists.newArrayList(regionLookup.values());
	}

	public Ticks getTicks(Connection c, Optional<Integer> authUserId, Setup setup, int page) throws SQLException {
		final int take = 200;
		int numTicks = 0;
		int skip = (page-1)*take;
		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, t.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN tick t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?"
				+ "  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "    AND (r.id=? OR ur.user_id IS NOT NULL)"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, t.grade, p.name, p.locked_admin, p.locked_superadmin, t.date, u.firstname, u.lastname"
				+ " ORDER BY t.date DESC, problem_name, name";
		List<PublicAscent> ticks = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					numTicks++;
					if ((numTicks-1) < skip || ticks.size() == take) {
						continue;
					}
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					int problemGrade = rst.getInt("problem_grade");
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, problemId, setup.gradeConverter().getGradeFromIdGrade(problemGrade), problemName, problemLockedAdmin, problemLockedSuperadmin, date, name));
				}
			}
		}
		int numPages = (int)(Math.ceil(numTicks / 200f));
		Ticks res = new Ticks(ticks, page, numPages);
		logger.debug("getTicks(authUserId={}, idRegion={}, page={}) - ticks.size()={}", authUserId, setup.idRegion(), page, ticks.size());
		return res;
	}

	public Toc getToc(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, TocRegion> regionLookup = new LinkedHashMap<>();
		Map<Integer, TocArea> areaLookup = new HashMap<>();
		Map<Integer, TocSector> sectorLookup = new HashMap<>();
		int numProblems = 0;
		String sqlStr = """
				SELECT r.id region_id, r.name region_name, a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, ac.id area_coordinates_id, ac.latitude area_latitude, ac.longitude area_longitude, ac.elevation area_elevation, ac.elevation_source area_elevation_source, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour,
				       s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.sorting sector_sorting, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour, sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       p.id, CONCAT(r.url,'/problem/',p.id) url, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, 
				       group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa, year(p.fa_date) fa_year,
				       COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars, 
				       MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked,
				       CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END todo,
				       ty.id type_id, ty.type, ty.subtype, COUNT(DISTINCT ps.id) num_pitches 
				FROM (((((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates ac ON a.coordinates_id=ac.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN todo ON (p.id=todo.problem_id AND todo.user_id=?)) LEFT JOIN problem_section ps ON p.id=ps.problem_id 
				WHERE (a.region_id=? OR ur.user_id IS NOT NULL) 
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 
				GROUP BY r.id, r.name, r.url, a.id, a.name, ac.id, ac.latitude, ac.longitude, ac.elevation, ac.elevation_source, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour, s.sorting, s.id, s.name, s.sorting, s.sun_from_hour, s.sun_to_hour, sc.id, sc.latitude, sc.longitude, sc.elevation, sc.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, year(p.fa_date), c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, p.grade, ty.id, ty.type, ty.subtype, todo.id
				ORDER BY r.name, a.name, s.sorting, s.name, p.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			ps.setInt(4, authUserId.orElse(0));
			ps.setInt(5, authUserId.orElse(0));
			ps.setInt(6, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Region
					int regionId = rst.getInt("region_id");
					TocRegion r = regionLookup.get(regionId);
					if (r == null) {
						String regionName = rst.getString("region_name");
						r = new TocRegion(regionId, regionName, new ArrayList<>());
						regionLookup.put(regionId, r);
					}
					// Area
					int areaId = rst.getInt("area_id");
					TocArea a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						int areaidCoordinates = rst.getInt("area_coordinates_id");
						Coordinates areaCoordinates = areaidCoordinates == 0? null : new Coordinates(areaidCoordinates, rst.getDouble("area_latitude"), rst.getDouble("area_longitude"), rst.getDouble("area_elevation"), rst.getString("area_elevation_source"));
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new TocArea(areaId, areaUrl, areaName, areaCoordinates, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour, new ArrayList<>());
						r.areas().add(a);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					TocSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						int sectorSorting = rst.getInt("sector_sorting");
						int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
						int sectorSunToHour = rst.getInt("sector_sun_to_hour");
						int sectorParkingidCoordinates = rst.getInt("sector_parking_coordinates_id");
						Coordinates sectorParking = sectorParkingidCoordinates == 0? null : new Coordinates(sectorParkingidCoordinates, rst.getDouble("sector_parking_latitude"), rst.getDouble("sector_parking_longitude"), rst.getDouble("sector_parking_elevation"), rst.getString("sector_parking_elevation_source"));
						CompassDirection sectorWallDirectionCalculated = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new TocSector(sectorId, sectorUrl, sectorName, sectorSorting, sectorParking, new ArrayList<>(), sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin, sectorSunFromHour, sectorSunToHour, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					String name = rst.getString("name");
					String description = rst.getString("description");
					int faYear = rst.getInt("fa_year");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int grade = rst.getInt("grade");
					String fa = rst.getString("fa");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean todo = rst.getBoolean("todo");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int numPitches = rst.getInt("num_pitches");
					TocProblem p = new TocProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, description, coordinates, setup.gradeConverter().getGradeFromIdGrade(grade), fa, faYear, numTicks, stars, ticked, todo, t, numPitches);
					s.problems().add(p);
					numProblems++;
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(c, sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				sectorLookup.get(idSector).outline().addAll(idSectorOutline.get(idSector));
			}
		}
		Toc res = new Toc(regionLookup.size(), areaLookup.size(), sectorLookup.size(), numProblems, Lists.newArrayList(regionLookup.values()));
		res.regions().forEach(r -> {
			r.areas().sort(Comparator.comparing(TocArea::name)); // Sorting (ae, oe, aa is sorted wrong by MySQL)
			r.areas().forEach(a -> a.orderSectors());
		});
		logger.debug("getToc(authUserId={}, setup={}) - duration={}", authUserId, setup, stopwatch);
		return res;
	}
	
	public List<TocPitch> getTocPitches(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		List<TocPitch> res = new ArrayList<>();
		String sqlStr = """
				SELECT r.name region_name, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name problem_name, ps.nr pitch, ps.grade, ps.description
				FROM area a INNER JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)
				JOIN problem_section ps ON p.id=ps.problem_id
				JOIN grade g
				JOIN type ty ON p.type_id=ty.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY r.name, r.url, p.id, a.name, s.name, p.name, ps.nr, ps.grade, ps.description
				ORDER BY r.name, a.name, s.sorting, s.name, p.nr, ps.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					String url = rst.getString("url");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					String problemName = rst.getString("problem_name");
					int pitch = rst.getInt("pitch");
					int grade = rst.getInt("grade");
					String description = rst.getString("description");
					res.add(new TocPitch(regionName, url, areaName, sectorName, problemName, pitch, setup.gradeConverter().getGradeFromIdGrade(grade), description));
				}
			}
		}
		return res;
	}

	public Todo getTodo(Connection c, Optional<Integer> authUserId, Setup setup, int idArea, int idSector) throws SQLException {
		Todo res = new Todo(new ArrayList<>());
		Map<Integer, TodoSector> sectorLookup = new HashMap<>();
		Map<Integer, TodoProblem> problemLookup = new HashMap<>();
		String condition = null;
		int id = 0;
		if (idSector > 0) {
			condition = "s.id=?";
			id = idSector;
		}
		else if (idArea > 0) {
			condition = "a.id=?";
			id = idArea;
		}
		else {
			throw new RuntimeException("Invalid arguments");
		}
		String sqlStr = "SELECT s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, p.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,"
				+ " u.id user_id, u.firstname user_firstname, u.lastname user_lastname"
				+ " FROM (((((region r INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN todo t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE " + condition
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " ORDER BY a.name, s.sorting, s.name, p.nr, u.firstname, u.lastname";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Sector
					int sectorId = rst.getInt("sector_id");
					TodoSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new TodoSector(sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, new ArrayList<>());
						res.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int problemId = rst.getInt("problem_id");
					TodoProblem p = problemLookup.get(problemId);
					if (p == null) {
						int problemNr = rst.getInt("problem_nr");
						String problemName = rst.getString("problem_name");
						int problemGrade = rst.getInt("problem_grade");
						boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
						boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
						p = new TodoProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, setup.gradeConverter().getGradeFromIdGrade(problemGrade), new ArrayList<>());
						s.problems().add(p);
						problemLookup.put(problemId, p);
					}
					// Partner
					int userId = rst.getInt("user_id");
					String userFirstname = rst.getString("user_firstname");
					String userLastname = rst.getString("user_lastname");
					p.partners().add(User.from(userId, userFirstname, userLastname));
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idArea={}, idSector)={}) - res={}", authUserId, setup.idRegion(), idArea, idSector, res);
		return res;
	}

	public Collection<Top> getTop(Connection c, Optional<Integer> authUserId, int areaId, int sectorId) throws SQLException {
		Map<Double, Top> topByPercentage = new LinkedHashMap<>();
		String condition = (sectorId>0? "s.id=" + sectorId : "a.id=" + areaId);
		String sqlStr = """
				WITH x AS (
				  SELECT COUNT(p.id) sum
				  FROM area a, sector s, problem p
				  WHERE %s 
				    AND a.id=s.area_id AND s.id=p.sector_id AND p.grade!=0 AND p.broken IS NULL)
				 SELECT y.user_id, y.name, y.avatar_crc32, ROUND(SUM(y.sum)/x.sum*100,2) percentage
				 FROM (
				  SELECT 'tick' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, COUNT(p.id) sum
				  FROM area a, sector s, problem p, tick t, user u
				  WHERE %s
				    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=t.problem_id AND t.user_id=u.id
				  GROUP BY u.id, u.firstname, u.lastname, u.picture
				  UNION
				  SELECT 'fa' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, COUNT(p.id) sum
				  FROM area a, sector s, problem p, fa f, user u
				  WHERE %s
				    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=f.problem_id AND f.user_id=u.id
				    AND (p.id, u.id) NOT IN (SELECT problem_id, user_id FROM tick)
				  GROUP BY u.id, u.firstname, u.lastname, u.picture
				  UNION
				  SELECT 'fa_aid' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CRC32(u.picture) avatar_crc32, COUNT(p.id) sum
				  FROM area a, sector s, problem p, fa_aid_user f, user u
				  WHERE %s
				    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=f.problem_id AND f.user_id=u.id
				    AND (p.id, u.id) NOT IN (SELECT problem_id, user_id FROM tick)
				  GROUP BY u.id, u.firstname, u.lastname, u.picture
				) y, x
				GROUP BY y.user_id, y.name, y.avatar_crc32, x.sum
				ORDER BY percentage DESC, name
				""".formatted(condition, condition, condition, condition);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			try (ResultSet rst = ps.executeQuery()) {
				double prevPercentage = 0;
				int rank = 0;
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					String name = rst.getString("name");
					long avatarCrc32 = rst.getLong("avatar_crc32");
					double percentage = rst.getDouble("percentage");
					if (prevPercentage != percentage) {
						rank++;
					}
					prevPercentage = percentage;
					boolean mine = authUserId.orElse(0) == userId;
					var top = topByPercentage.get(percentage);
					if (top == null) {
						top = new Top(rank, percentage, new ArrayList<>());
						topByPercentage.put(percentage, top);
					}
					top.users().add(new TopUser(userId, name, avatarCrc32, mine));
				}
			}
		}
		return topByPercentage.values();
	}

	public List<Trash> getTrash(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		ensureAdminWriteRegion(c, authUserId, setup.idRegion());
		List<Trash> res = new ArrayList<>();
		String sqlStr =
				// Area
				"SELECT a.id area_id, null sector_id, null problem_id, null media_id, a.name, DATE_FORMAT(a.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN user u ON a.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Sector
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, null media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(s.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user u ON s.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Problem
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, null media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(p.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN user u ON p.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NULL AND p.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Area)
				+ " UNION ALL"
				+ " SELECT a.id area_id, null sector_id, null problem_id, m.id media_id, a.name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN media_area ma ON a.id=ma.area_id) INNER JOIN media m ON ma.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Sector)
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, m.id media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN media_sector ms ON s.id=ms.sector_id) INNER JOIN media m ON ms.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Problem)
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, m.id media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN media_problem mp ON p.id=mp.problem_id) INNER JOIN media m ON mp.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Order results
				+ " ORDER BY trash DESC";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			ps.setInt(4, authUserId.orElseThrow());
			ps.setInt(5, setup.idRegion());
			ps.setInt(6, setup.idRegion());
			ps.setInt(7, authUserId.orElseThrow());
			ps.setInt(8, setup.idRegion());
			ps.setInt(9, setup.idRegion());
			ps.setInt(10, authUserId.orElseThrow());
			ps.setInt(11, setup.idRegion());
			ps.setInt(12, setup.idRegion());
			ps.setInt(13, authUserId.orElseThrow());
			ps.setInt(14, setup.idRegion());
			ps.setInt(15, setup.idRegion());
			ps.setInt(16, authUserId.orElseThrow());
			ps.setInt(17, setup.idRegion());
			ps.setInt(18, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int sectorId = rst.getInt("sector_id");
					int problemId = rst.getInt("problem_id");
					int mediaId = rst.getInt("media_id");
					String name = rst.getString("name");
					String when = rst.getString("trash");
					String by = rst.getString("trash_by");
					res.add(new Trash(areaId, sectorId, problemId, mediaId, name, when, by));
				}
			}
		}
		return res;
	}

	public List<Type> getTypes(Connection c, int regionId) throws SQLException {
		List<Type> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String type = rst.getString("type");
					String subtype = rst.getString("subtype");
					res.add(new Type(id, type, subtype));
				}
			}
		}
		return res;
	}

	public List<UserRegion> getUserRegion(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		List<UserRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id, r.name, CASE WHEN r.id=? OR ur.admin_read=1 OR ur.admin_write=1 OR ur.superadmin_read=1 OR ur.superadmin_write=1 THEN 1 ELSE 0 END read_only, ur.region_visible, CASE WHEN ur.superadmin_write=1 THEN 'Superadmin' WHEN ur.superadmin_read=1 THEN 'Superadmin (read)' WHEN ur.admin_read=1 THEN 'Admin (read)' WHEN ur.admin_write=1 THEN 'Admin' END role FROM (region r INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) GROUP BY r.id, r.name ORDER BY r.name")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String role = rst.getString("role");
					boolean readOnly = rst.getBoolean("read_only");
					boolean enabled = readOnly || rst.getBoolean("region_visible");
					res.add(new UserRegion(id, name, role, enabled, readOnly));
				}
			}
		}
		return res;
	}

	public List<User> getUserSearch(Connection c, Optional<Integer> authUserId, String value) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in...");
		List<User> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(value)) {
			String searchRegexPattern = "(^|\\W)" + value;
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT u.id, u.firstname, u.lastname
					FROM user u
					WHERE regexp_like(TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))),?,'i')
					ORDER BY u.firstname, u.lastname
					""")) {
				ps.setString(1, searchRegexPattern);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						String firstname = rst.getString("firstname");
						String lastname = rst.getString("lastname");
						res.add(User.from(id, firstname, lastname));
					}
				}
			}
			// Add id to users with duplicate name
			for (User u : res.stream()
					.collect(Collectors.groupingBy(User::name))
					.values().stream()
					.filter(list -> list.size() > 1)
					.flatMap(List::stream)
					.collect(Collectors.toList())) {
				res.set(res.indexOf(u), u.withIdAsNameSuffix());
			}
		}
		return res;
	}

	@SuppressWarnings("resource")
	public byte[] getUserTicks(Connection c, Optional<Integer> authUserId) throws SQLException, IOException {
		byte[] bytes;
		try (ExcelWorkbook workbook = new ExcelWorkbook()) {
			String sqlStr = "SELECT r.id region_id, ty.type, pt.subtype, COUNT(DISTINCT ps.id) num_pitches, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade" + 
					" FROM (((((((((problem p INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)" + 
					" WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY r.id, ty.type, pt.subtype, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setInt(3, authUserId.orElseThrow());
				try (ResultSet rst = ps.executeQuery()) {
					Map<String, ExcelSheet> sheets = new HashMap<>();
					while (rst.next()) {
						int regionId = rst.getInt("region_id");
						String type = rst.getString("type");
						String subType = rst.getString("subtype");
						int numPitches = rst.getInt("num_pitches");
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						LocalDate date = rst.getObject("date", LocalDate.class);
						int stars = rst.getInt("stars");
						boolean fa = rst.getBoolean("fa");
						String grade = Server.getSetups().stream()
								.filter(x -> x.idRegion() == regionId)
								.findAny()
								.orElseThrow(() -> new RuntimeException("Invalid regionId=" + regionId))
								.gradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						ExcelSheet sheet = sheets.get(type);
						if (sheet == null) {
							sheet = workbook.addSheet(type);
							sheets.put(type, sheet);
						}
						sheet.incrementRow();
						sheet.writeString("AREA", areaName);
						sheet.writeString("SECTOR", sectorName);
						if (subType != null) {
							sheet.writeString("TYPE", subType);
							sheet.writeInt("PITCHES", numPitches > 0? numPitches : 1);
						}
						sheet.writeString("NAME", name);
						sheet.writeString("FIRST ASCENT", fa? "Yes" : "No");
						sheet.writeDate("DATE", date);
						sheet.writeString("GRADE", grade);
						sheet.writeDouble("STARS", stars);
						sheet.writeString("DESCRIPTION", comment);
						sheet.writeHyperlink("URL", url);
					}
					sheets.values().forEach(sheet -> sheet.close());
				}
			}
			sqlStr = "SELECT r.id region_id, ty.type, pt.subtype, COUNT(DISTINCT ps.id) num_pitches, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, tr.comment, DATE_FORMAT(tr.date,'%Y-%m-%d') date, t.stars, 0 fa, t.grade grade"
					+ " FROM (((((((((problem p INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN tick t ON p.id=t.problem_id AND t.user_id=?) INNER JOIN tick_repeat tr ON t.id=tr.tick_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
					+ " WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
					+ " GROUP BY r.id, ty.type, pt.subtype, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, p.id, p.locked_admin, p.locked_superadmin, p.name, tr.comment, p.fa_date, tr.date, t.stars, t.grade, p.grade"
					+ " ORDER BY ty.type, a.name, s.name, p.name, tr.date";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, authUserId.orElseThrow());
				try (ResultSet rst = ps.executeQuery()) {
					Map<String, ExcelSheet> sheets = new HashMap<>();
					while (rst.next()) {
						int regionId = rst.getInt("region_id");
						String type = rst.getString("type") + " (repeats)";
						String subType = rst.getString("subtype");
						int numPitches = rst.getInt("num_pitches");
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						LocalDate date = rst.getObject("date", LocalDate.class);
						int stars = rst.getInt("stars");
						boolean fa = rst.getBoolean("fa");
						String grade = Server.getSetups().stream()
								.filter(x -> x.idRegion() == regionId)
								.findAny()
								.orElseThrow(() -> new RuntimeException("Invalid regionId=" + regionId))
								.gradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						ExcelSheet sheet = sheets.get(type);
						if (sheet == null) {
							sheet = workbook.addSheet(type);
							sheets.put(type, sheet);
						}
						sheet.incrementRow();
						sheet.writeString("AREA", areaName);
						sheet.writeString("SECTOR", sectorName);
						if (subType != null) {
							sheet.writeString("TYPE", subType);
							sheet.writeInt("PITCHES", numPitches > 0? numPitches : 1);
						}
						sheet.writeString("NAME", name);
						sheet.writeString("FIRST ASCENT", fa? "Yes" : "No");
						sheet.writeDate("DATE", date);
						sheet.writeString("GRADE", grade);
						sheet.writeDouble("STARS", stars);
						sheet.writeString("DESCRIPTION", comment);
						sheet.writeHyperlink("URL", url);
					}
					sheets.values().forEach(sheet -> sheet.close());
				}
			}
			sqlStr = "SELECT r.id region_id, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, aid.aid_description comment, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date" + 
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY r.id, ty.type, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, authUserId.orElseThrow());
				ExcelSheet sheet = null;
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						if (sheet == null) {
							sheet = workbook.addSheet("First_AID_Ascent");
						}
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						LocalDate date = rst.getObject("date", LocalDate.class);
						sheet.incrementRow();
						sheet.writeString("AREA", areaName);
						sheet.writeString("SECTOR", sectorName);
						sheet.writeString("NAME", name);
						sheet.writeDate("DATE", date);
						sheet.writeString("DESCRIPTION", comment);
						sheet.writeHyperlink("URL", url);
					}
				}
				if (sheet != null) {
					sheet.close();
				}
			}
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				workbook.write(os);
				bytes = os.toByteArray();
			}
		}
		return bytes;
	}

	public void moveMedia(Connection c, Optional<Integer> authUserId, int id, boolean left, int toIdArea, int toIdSector, int toIdProblem) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("problem_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");

		if (toIdArea > 0) {
			if (problemId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_problem WHERE media_id=? AND problem_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, problemId);
					ps.execute();
				}
			}
			else if (sectorId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_sector WHERE media_id=? AND sector_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, sectorId);
					ps.execute();
				}
			}
			else {
				throw new IllegalArgumentException("Invalid location on media with id=" + id);
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_area (media_id, area_id) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setInt(2, toIdArea);
				ps.execute();
			}
		}
		else if (toIdSector > 0) {
			if (problemId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_problem WHERE media_id=? AND problem_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, problemId);
					ps.execute();
				}
			}
			else if (areaId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_area WHERE media_id=? AND area_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, areaId);
					ps.execute();
				}
			}
			else {
				throw new IllegalArgumentException("Invalid location on media with id=" + id);
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_sector (media_id, sector_id) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setInt(2, toIdSector);
				ps.execute();
			}
		}
		else if (toIdProblem > 0) {
			if (sectorId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_sector WHERE media_id=? AND sector_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, sectorId);
					ps.execute();
				}
			}
			else if (areaId > 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_area WHERE media_id=? AND area_id=?")) {
					ps.setInt(1, id);
					ps.setInt(2, areaId);
					ps.execute();
				}
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_problem (media_id, problem_id) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setInt(2, toIdProblem);
				ps.execute();
			}
		}
		else { // Move image left/right
			String table = null;
			String column = null;
			String extraOrder = "";
			int columnId = 0;
			if (areaId > 0) {
				table = "media_area";
				column = "area_id";
				columnId = areaId;
			} else if (sectorId > 0) {
				table = "media_sector";
				column = "sector_id";
				columnId = sectorId;
			} else {
				table = "media_problem";
				column = "problem_id";
				columnId = problemId;
				extraOrder = "IFNULL(pitch,0), ";
			}
			List<Integer> idMediaList = new ArrayList<>();
			try (PreparedStatement ps = c.prepareStatement("SELECT m.id FROM " + table + " x, media m WHERE x." + column + "=? AND x.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0 ORDER BY " + extraOrder + " -x.sorting DESC, m.id")) {
				ps.setInt(1, columnId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int idMedia = rst.getInt("id");
						idMediaList.add(idMedia);
					}
				}
			}
			final int ixToMove = idMediaList.indexOf(id);
			idMediaList.remove(ixToMove);
			Preconditions.checkArgument(ixToMove>=0, "Could not find " + id + " in " + idMediaList);
			if (left) {
				if (ixToMove == 0) {
					idMediaList.add(id); // Move from start to end
				} else {
					idMediaList.add(ixToMove-1, id);
				}
			} else {
				if (ixToMove == idMediaList.size()) {
					idMediaList.add(0, id); // Move from end to start
				} else {
					idMediaList.add(ixToMove+1, id);
				}
			}
			try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET sorting=? WHERE " + column + "=? AND media_id=?")) {
				int sorting = 0;
				for (int idMedia : idMediaList) {
					ps.setInt(1, ++sorting);
					ps.setInt(2, columnId);
					ps.setInt(3, idMedia);
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}

		if (problemId > 0) {
			fillActivity(c, problemId);
		}
	}

	public void rotateMedia(Connection c, int idRegion, Optional<Integer> authUserId, int idMedia, int degrees) throws IOException, SQLException, InterruptedException {
		// Rotate allowed for administrators + user who uploaded specific image
		boolean uploadedByMe = getMedia(c, authUserId, idMedia).uploadedByMe();
		if (!uploadedByMe) {
			ensureAdminWriteRegion(c, authUserId, idRegion);
		}
		Rotation r = switch (degrees) {
		case 90 -> Rotation.CW_90;
		case 180 -> Rotation.CW_180;
		case 270 -> Rotation.CW_270;
		default -> throw new IllegalArgumentException("Cannot rotate image " + degrees + " degrees (legal degrees = 90, 180, 270)");
		};
		ImageHelper.rotateImage(this, c, idMedia, r);
	}

	public void saveAvatar(Connection c, Optional<Integer> authUserId, InputStream is) throws SQLException {
		ImageHelper.saveAvatar(authUserId.orElseThrow(), is);
		try (PreparedStatement ps = c.prepareStatement("UPDATE user SET picture=? WHERE id=?")) {
			ps.setString(1, LocalDateTime.now().toString());
			ps.setInt(2, authUserId.orElseThrow());
			ps.executeUpdate();
		}
	}

	public Redirect setArea(Connection c, Setup s, Optional<Integer> authUserId, Area a, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkArgument(s.idRegion() > 0, "Insufficient credentials");
		ensureAdminWriteRegion(c, authUserId, s.idRegion());
		int idArea = -1;
		final boolean isLockedAdmin = a.isLockedSuperadmin()? false : a.isLockedAdmin();
		boolean setPermissionRecursive = false;
		if (a.getCoordinates() != null) {
			if (a.getCoordinates().getLatitude() == 0 || a.getCoordinates().getLongitude() == 0) {
				a.setCoordinates(null);
			}
			else {
				ensureCoordinatesInDbWithElevationAndId(c, Lists.newArrayList(a.getCoordinates()));
			}
		}
		if (a.getId() > 0) {
			ensureAdminWriteArea(c, authUserId, a.getId());
			Area currArea = getArea(c, s, authUserId, a.getId(), false);
			setPermissionRecursive = currArea.isLockedAdmin() != isLockedAdmin || currArea.isLockedSuperadmin() != a.isLockedSuperadmin();
			try (PreparedStatement ps = c.prepareStatement("UPDATE area SET name=?, description=?, coordinates_id=?, locked_admin=?, locked_superadmin=?, for_developers=?, access_info=?, access_closed=?, no_dogs_allowed=?, sun_from_hour=?, sun_to_hour=?, trash=CASE WHEN ? THEN NOW() ELSE NULL END, trash_by=? WHERE id=?")) {
				ps.setString(1, GlobalFunctions.stripString(a.getName()));
				ps.setString(2, GlobalFunctions.stripString(a.getComment()));
				setNullablePositiveInteger(ps, 3, a.getCoordinates() == null? 0 : a.getCoordinates().getId());
				ps.setBoolean(4, isLockedAdmin);
				ps.setBoolean(5, a.isLockedSuperadmin());
				ps.setBoolean(6, a.isForDevelopers());
				ps.setString(7, GlobalFunctions.stripString(a.getAccessInfo()));
				ps.setString(8, GlobalFunctions.stripString(a.getAccessClosed()));
				ps.setBoolean(9, a.isNoDogsAllowed());
				setNullablePositiveDouble(ps, 10, a.getSunFromHour());
				setNullablePositiveDouble(ps, 11, a.getSunToHour());
				ps.setBoolean(12, a.isTrash());
				ps.setInt(13, a.isTrash()? authUserId.orElseThrow() : 0);
				ps.setInt(14, a.getId());
				ps.execute();
			}
			idArea = a.getId();

			// Sector order
			if (a.getSectorOrder() != null) {
				for (AreaSectorOrder x : a.getSectorOrder()) {
					try (PreparedStatement ps = c.prepareStatement("UPDATE sector SET sorting=? WHERE id=?")) {
						ps.setInt(1, x.sorting());
						ps.setInt(2, x.id());
						ps.execute();
					}
				}
			}

			// Also update sectors and problems (last_updated and locked)
			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.locked_admin=?, a.locked_superadmin=?, s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE a.id=?";
				try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, a.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, a.isLockedSuperadmin());
					ps.setBoolean(5, isLockedAdmin);
					ps.setBoolean(6, a.isLockedSuperadmin());
					ps.setInt(7, a.getId());
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?";
				try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
					ps.setInt(1, idArea);
					ps.execute();
				}
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO area (android_id, region_id, name, description, coordinates_id, locked_admin, locked_superadmin, for_developers, access_info, access_closed, no_dogs_allowed, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.idRegion());
				ps.setString(3, GlobalFunctions.stripString(a.getName()));
				ps.setString(4, GlobalFunctions.stripString(a.getComment()));
				setNullablePositiveInteger(ps, 5, a.getCoordinates() == null? 0 : a.getCoordinates().getId());
				ps.setBoolean(6, isLockedAdmin);
				ps.setBoolean(7, a.isLockedSuperadmin());
				ps.setBoolean(8, a.isForDevelopers());
				ps.setString(9, GlobalFunctions.stripString(a.getAccessInfo()));
				ps.setString(10, GlobalFunctions.stripString(a.getAccessClosed()));
				ps.setBoolean(11, a.isNoDogsAllowed());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idArea = rst.getInt(1);
					}
				}
			}
		}
		if (idArea == -1) {
			throw new SQLException("idArea == -1");
		}
		// New media
		if (a.getNewMedia() != null) {
			for (NewMedia m : a.getNewMedia()) {
				final int idProblem = 0;
				final int pitch = 0;
				final int idSector = 0;
				final int idGuestbook = 0;
				addNewMedia(c, authUserId, idProblem, pitch, m.trivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		upsertExternalLinks(c, a.getExternalLinks(), idArea, 0, 0);
		if (a.isTrash()) {
			return Redirect.fromRoot();
		}
		return Redirect.fromIdArea(idArea);
	}

	public void setMediaMetadata(Connection c, int idMedia, int height, int width, LocalDateTime dateTaken) throws SQLException, IOException {
		Path webp = IOHelper.getPathMediaWebWebp(idMedia);
		Path webm = IOHelper.getPathMediaWebWebm(idMedia);
		Path p = Files.exists(webm)? webm : webp;
		Preconditions.checkArgument(Files.exists(p));
		String sqlStr = dateTaken == null?
				"UPDATE media SET checksum=?, width=?, height=? WHERE id=?" :
					"UPDATE media SET date_taken=?, checksum=?, width=?, height=? WHERE id=?";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 0;
			if (dateTaken != null) {
				ps.setObject(++ix, dateTaken);
			}
			ps.setInt(++ix, com.google.common.io.Files.asByteSource(p.toFile()).hash(Hashing.crc32()).asInt());
			ps.setInt(++ix, width);
			ps.setInt(++ix, height);
			ps.setInt(++ix, idMedia);
			ps.execute();
		}
		logger.debug("setMediaMetadata(idMedia={}, height={}, width={}, dateTaken={}) - success", idMedia, height, width, dateTaken);
	}

	public Redirect setProblem(Connection c, Optional<Integer> authUserId, Setup s, Problem p, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		final boolean orderByGrade = s.gradeSystem().equals(GradeSystem.BOULDER);
		final LocalDate dt = Strings.isNullOrEmpty(p.getFaDate())? null : LocalDate.parse(p.getFaDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		int idProblem = -1;
		final boolean isLockedAdmin = p.isLockedSuperadmin()? false : p.isLockedAdmin();
		if (p.getCoordinates() != null) {
			if (p.getCoordinates().getLatitude() == 0 || p.getCoordinates().getLongitude() == 0) {
				p.setCoordinates(null);
			}
			else {
				ensureCoordinatesInDbWithElevationAndId(c, Lists.newArrayList(p.getCoordinates()));
			}
		}
		tryFixSectorOrdering(c, p.getSectorId(), p.getId(), p.getNr());
		if (p.getId() > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)) SET p.name=?, p.rock=?, p.description=?, p.grade=?, p.fa_date=?, p.coordinates_id=?, p.broken=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, route_length=?, descent=?, p.trash=CASE WHEN ? THEN NOW() ELSE NULL END, p.trash_by=?, p.last_updated=now() WHERE p.id=?")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setString(2, GlobalFunctions.stripString(p.getName()));
				ps.setString(3, GlobalFunctions.stripString(p.getRock()));
				ps.setString(4, GlobalFunctions.stripString(p.getComment()));
				ps.setInt(5, s.gradeConverter().getIdGradeFromGrade(p.getOriginalGrade()));
				ps.setObject(6, dt);
				setNullablePositiveInteger(ps, 7, p.getCoordinates() == null? 0 : p.getCoordinates().getId());
				ps.setString(8, GlobalFunctions.stripString(p.getBroken()));
				ps.setBoolean(9, isLockedAdmin);
				ps.setBoolean(10, p.isLockedSuperadmin());
				ps.setInt(11, p.getNr());
				ps.setInt(12, p.getT().id());
				ps.setString(13, GlobalFunctions.stripString(p.getTrivia()));
				ps.setString(14, GlobalFunctions.stripString(p.getStartingAltitude()));
				ps.setString(15, GlobalFunctions.stripString(p.getAspect()));
				ps.setString(16, GlobalFunctions.stripString(p.getRouteLength()));
				ps.setString(17, GlobalFunctions.stripString(p.getDescent()));
				ps.setBoolean(18, p.isTrash());
				ps.setInt(19, p.isTrash()? authUserId.orElseThrow() : 0);
				ps.setInt(20, p.getId());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idProblem = p.getId();
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem (android_id, sector_id, name, rock, description, grade, fa_date, coordinates_id, broken, locked_admin, locked_superadmin, nr, type_id, trivia, starting_altitude, aspect, route_length, descent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, p.getSectorId());
				ps.setString(3, GlobalFunctions.stripString(p.getName()));
				ps.setString(4, GlobalFunctions.stripString(p.getRock()));
				ps.setString(5, GlobalFunctions.stripString(p.getComment()));
				ps.setInt(6, s.gradeConverter().getIdGradeFromGrade(p.getOriginalGrade()));
				ps.setObject(7, dt);
				setNullablePositiveInteger(ps, 8, p.getCoordinates() == null? 0 : p.getCoordinates().getId());
				ps.setString(9, GlobalFunctions.stripString(p.getBroken()));
				ps.setBoolean(10, isLockedAdmin);
				ps.setBoolean(11, p.isLockedSuperadmin());
				ps.setInt(12, p.getNr() == 0 ? getSector(c, authUserId, orderByGrade, s, p.getSectorId(), false).getProblems().stream().map(x -> x.nr()).mapToInt(Integer::intValue).max().orElse(0) + 1 : p.getNr());
				ps.setInt(13, p.getT().id());
				ps.setString(14, GlobalFunctions.stripString(p.getTrivia()));
				ps.setString(15, GlobalFunctions.stripString(p.getStartingAltitude()));
				ps.setString(16, GlobalFunctions.stripString(p.getAspect()));
				ps.setString(17, GlobalFunctions.stripString(p.getRouteLength()));
				ps.setString(18, GlobalFunctions.stripString(p.getDescent()));
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idProblem = rst.getInt(1);
					}
				}
			}
		}
		if (idProblem == -1) {
			throw new SQLException("idProblem == -1");
		}
		// Also update last_updated on problem, sector and area
		String sqlStr = "UPDATE problem p, sector s, area a SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, idProblem);
			int res = ps.executeUpdate();
			if (res == 0) {
				throw new SQLException("Insufficient credentials");
			}
		}
		// New media
		if (p.getNewMedia() != null) {
			for (NewMedia m : p.getNewMedia()) {
				final int idSector = 0;
				final int idArea = 0;
				final int idGuestbook = 0;
				addNewMedia(c, authUserId, idProblem, m.pitch(), m.trivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		// FA
		if (p.getFa() != null) {
			Set<Integer> fas = new HashSet<>();
			try (PreparedStatement ps = c.prepareStatement("SELECT user_id FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						fas.add(rst.getInt("user_id"));
					}
				}
			}
			for (User x : p.getFa()) {
				Preconditions.checkArgument(x.id() != 0);
				if (x.id() > 0) { // Existing user
					boolean exists = fas.remove(x.id());
					if (!exists) {
						try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
							ps2.setInt(1, idProblem);
							ps2.setInt(2, x.id());
							ps2.execute();
						}
					}
				} else { // New user
					int idUser = addUser(c, null, x.name(), null, null);
					Preconditions.checkArgument(idUser > 0);
					try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
						ps2.setInt(1, idProblem);
						ps2.setInt(2, idUser);
						ps2.execute();
					}
				}
			}
			if (!fas.isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa WHERE problem_id=? AND user_id=?")) {
					for (int x : fas) {
						ps.setInt(1, idProblem);
						ps.setInt(2, x);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
			}
		}
		// Sections
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM problem_section WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}
		if (p.getSections() != null && p.getSections().size() > 1) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade) VALUES (?, ?, ?, ?)")) {
				for (ProblemSection section : p.getSections()) {
					ps.setInt(1, idProblem);
					ps.setInt(2, section.nr());
					ps.setString(3, GlobalFunctions.stripString(section.description()));
					ps.setInt(4, s.gradeConverter().getIdGradeFromGrade(section.grade()));
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// First aid ascent
		if (!s.gradeSystem().equals(GradeSystem.BOULDER)) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa_aid WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa_aid_user WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			if (p.getFaAid() != null) {
				FaAid faAid = p.getFaAid();
				final LocalDate aidDt = Strings.isNullOrEmpty(faAid.date())? null : LocalDate.parse(faAid.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO fa_aid (problem_id, aid_date, aid_description) VALUES (?, ?, ?)")) {
					ps.setInt(1, faAid.problemId());
					ps.setObject(2, aidDt);
					ps.setString(3, GlobalFunctions.stripString(faAid.description()));
					ps.execute();
				}
				if (!faAid.users().isEmpty()) {
					try (PreparedStatement ps = c.prepareStatement("INSERT INTO fa_aid_user (problem_id, user_id) VALUES (?, ?)")) {
						for (User u : faAid.users()) {
							int idUser = u.id();
							if (idUser <= 0) {
								idUser = addUser(c, null, u.name(), null, null);
							}
							Preconditions.checkArgument(idUser > 0);
							ps.setInt(1, faAid.problemId());
							ps.setInt(2, idUser);
							ps.addBatch();
						}
						ps.executeBatch();
					}	
				}
			}
		}
		upsertExternalLinks(c, p.getExternalLinks(), 0, 0, idProblem);
		fillActivity(c, idProblem);
		if (p.isTrash()) {
			return Redirect.fromIdSector(p.getSectorId());
		}
		return Redirect.fromIdProblem(idProblem);
	}

	public void setProfile(Connection c, Optional<Integer> authUserId, Setup setup, Profile profile, FormDataMultiPart multiPart) throws SQLException, IOException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.firstname()), "Firstname cannot be null");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.lastname()), "Lastname cannot be null");
		try (PreparedStatement ps = c.prepareStatement("UPDATE user SET firstname=?, lastname=?, email_visible_to_all=? WHERE id=?")) {
			ps.setString(1, profile.firstname());
			ps.setString(2, profile.lastname());
			ps.setBoolean(3, profile.emailVisibleToAll());
			ps.setInt(4, authUserId.orElseThrow());
			ps.execute();
		}
		var avatar = multiPart.getField("avatar");
		if (avatar != null) {
			try (InputStream is = avatar.getValueAs(InputStream.class)) {
				saveAvatar(c, authUserId, is);
			}
		}
	}

	public Redirect setSector(Connection c, Optional<Integer> authUserId, Setup setup, Sector s, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		int idSector = -1;
		final boolean isLockedAdmin = s.isLockedSuperadmin()? false : s.isLockedAdmin();
		boolean setPermissionRecursive = false;
		List<Coordinates> allCoordinates = new ArrayList<>();
		if (s.getOutline() != null && !s.getOutline().isEmpty()) {
			allCoordinates.addAll(s.getOutline());
		}
		if (s.getApproach() != null && s.getApproach().coordinates() != null && !s.getApproach().coordinates().isEmpty()) {
			allCoordinates.addAll(s.getApproach().coordinates());
		}
		if (s.getDescent() != null && s.getDescent().coordinates() != null && !s.getDescent().coordinates().isEmpty()) {
			allCoordinates.addAll(s.getDescent().coordinates());
		}
		if (s.getParking() != null) {
			if (s.getParking().getLatitude() == 0 || s.getParking().getLongitude() == 0) {
				s.setParking(null);
			}
			else {
				allCoordinates.add(s.getParking());
			}
		}
		ensureCoordinatesInDbWithElevationAndId(c, allCoordinates);
		// Sector
		if (s.getId() > 0) {
			Sector currSector = getSector(c, authUserId, false, setup, s.getId(), false);
			setPermissionRecursive = currSector.isLockedAdmin() != isLockedAdmin || currSector.isLockedSuperadmin() != s.isLockedSuperadmin();
			try (PreparedStatement ps = c.prepareStatement("UPDATE sector s, area a, user_region ur SET s.name=?, s.description=?, s.access_info=?, s.access_closed=?, s.sun_from_hour=?, s.sun_to_hour=?, s.parking_coordinates_id=?, s.locked_admin=?, s.locked_superadmin=?, s.compass_direction_id_calculated=?, s.compass_direction_id_manual=?, s.trash=CASE WHEN ? THEN NOW() ELSE NULL END, s.trash_by=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)")) {
				ps.setString(1, GlobalFunctions.stripString(s.getName()));
				ps.setString(2, GlobalFunctions.stripString(s.getComment()));
				ps.setString(3, GlobalFunctions.stripString(s.getAccessInfo()));
				ps.setString(4, GlobalFunctions.stripString(s.getAccessClosed()));
				setNullablePositiveDouble(ps, 5, s.getSunFromHour());
				setNullablePositiveDouble(ps, 6, s.getSunToHour());
				setNullablePositiveInteger(ps, 7, s.getParking() == null? 0 : s.getParking().getId());
				ps.setBoolean(8, isLockedAdmin);
				ps.setBoolean(9, s.isLockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.getOutline());
				setNullablePositiveInteger(ps, 10, calculatedWallDirection != null? calculatedWallDirection.id() : 0);
				setNullablePositiveInteger(ps, 11, s.getWallDirectionManual() != null? s.getWallDirectionManual().id() : 0);
				ps.setBoolean(12, s.isTrash());
				ps.setInt(13, s.isTrash()? authUserId.orElseThrow() : 0);
				ps.setInt(14, s.getId());
				ps.setInt(15, authUserId.orElseThrow());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idSector = s.getId();

			// Problem order
			if (s.getProblemOrder() != null) {
				setSectorProblemOrder(c, s.getProblemOrder());
			}

			// Also update problems (last_updated and locked) + last_updated on area
			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE s.id=?";
				try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, s.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, s.isLockedSuperadmin());
					ps.setInt(5, idSector);
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?";
				try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
					ps.setInt(1, idSector);
					ps.execute();
				}
			}
		} else {
			ensureAdminWriteArea(c, authUserId, s.getAreaId());
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO sector (android_id, area_id, name, description, access_info, access_closed, parking_coordinates_id, locked_admin, locked_superadmin, compass_direction_id_calculated, compass_direction_id_manual, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.getAreaId());
				ps.setString(3, s.getName());
				ps.setString(4, GlobalFunctions.stripString(s.getComment()));
				ps.setString(5, GlobalFunctions.stripString(s.getAccessInfo()));
				ps.setString(6, GlobalFunctions.stripString(s.getAccessClosed()));
				setNullablePositiveInteger(ps, 7, s.getParking() == null? 0 : s.getParking().getId());
				ps.setBoolean(8, isLockedAdmin);
				ps.setBoolean(9, s.isLockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.getOutline());
				setNullablePositiveInteger(ps, 10, calculatedWallDirection != null? calculatedWallDirection.id() : 0);
				setNullablePositiveInteger(ps, 11, s.getWallDirectionManual() != null? s.getWallDirectionManual().id() : 0);
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idSector = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idSector > 0, "idSector=" + idSector);
		// Outline
		if (s.getOutline() == null || s.getOutline().isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM sector_outline WHERE sector_id=?")) {
				ps.setInt(1, idSector);
				ps.execute();
			}
		} else {
			String coordinateIds = s.getOutline().stream()
					.map(Coordinates::getId)
					.map(String::valueOf)
					.collect(Collectors.joining(","));
			String sqlStr = String.format("DELETE FROM sector_outline WHERE sector_id=? AND coordinates_id NOT IN (%s)", coordinateIds);
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO sector_outline (sector_id, coordinates_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinates_id=?")) {
				int sorting = 0;
				for (Coordinates coord : s.getOutline()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.setInt(4, coord.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// Approach
		if (s.getApproach() == null || s.getApproach().coordinates() == null || s.getApproach().coordinates().isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM sector_approach WHERE sector_id=?")) {
				ps.setInt(1, idSector);
				ps.execute();
			}
		} else {
			String coordinateIds = s.getApproach().coordinates().stream()
					.map(Coordinates::getId)
					.map(String::valueOf)
					.collect(Collectors.joining(","));
			String sqlStr = String.format("DELETE FROM sector_approach WHERE sector_id=? AND coordinates_id NOT IN (%s)", coordinateIds);
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO sector_approach (sector_id, coordinates_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinates_id=?")) {
				int sorting = 0;
				for (Coordinates coord : s.getApproach().coordinates()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.setInt(4, coord.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// Descent
		if (s.getDescent() == null || s.getDescent().coordinates() == null || s.getDescent().coordinates().isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM sector_descent WHERE sector_id=?")) {
				ps.setInt(1, idSector);
				ps.execute();
			}
		} else {
			String coordinateIds = s.getDescent().coordinates().stream()
					.map(Coordinates::getId)
					.map(String::valueOf)
					.collect(Collectors.joining(","));
			String sqlStr = String.format("DELETE FROM sector_descent WHERE sector_id=? AND coordinates_id NOT IN (%s)", coordinateIds);
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO sector_descent (sector_id, coordinates_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinates_id=?")) {
				int sorting = 0;
				for (Coordinates coord : s.getDescent().coordinates()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.setInt(4, coord.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// New media
		if (s.getNewMedia() != null) {
			for (NewMedia m : s.getNewMedia()) {
				final int pitch = 0;
				final int idProblem = 0;
				final int idArea = 0;
				final int idGuestbook = 0;
				addNewMedia(c, authUserId, idProblem, pitch, m.trivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		upsertExternalLinks(c, s.getExternalLinks(), 0, idSector, 0);
		Redirect res = null;
		if (s.isTrash()) {
			res = Redirect.fromIdArea(s.getAreaId());
		}
		else {
			res = Redirect.fromIdSector(idSector);
		}
		logger.debug("setSector() - res={}", res);
		return res;
	}

	public void setTick(Connection c, Optional<Integer> authUserId, Setup setup, Tick t) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		// Remove from project list (if existing)
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, t.idProblem());
			ps.execute();
		}
		final LocalDate dt = Strings.isNullOrEmpty(t.date())? null : LocalDate.parse(t.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		logger.debug("setTick(authUserId={}, dt={}, t={}", authUserId, dt, t);
		if (t.delete()) {
			Preconditions.checkArgument(t.id() > 0, "Cannot delete a tick without id");
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")) {
				ps.setInt(1, t.id());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setInt(3, t.idProblem());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		}
		else if (t.id() == -1) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade, comment, stars) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, t.idProblem());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setObject(3, dt);
				ps.setInt(4, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(5, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(6, t.stars());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idTick = rst.getInt(1);
						upsertTickRepeats(c, idTick, t.repeats());
					}
				}
			}
		}
		else if (t.id() > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE tick SET date=?, grade=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setObject(1, dt);
				ps.setInt(2, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(3, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(4, t.stars());
				ps.setInt(5, t.id());
				ps.setInt(6, t.idProblem());
				ps.setInt(7, authUserId.orElseThrow());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
				upsertTickRepeats(c, t.id(), t.repeats());
			}
		} else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
		fillActivity(c, t.idProblem());
	}

	public void setUserRegion(Connection c, Optional<Integer> authUserId, int regionId, boolean delete) throws SQLException {
		if (delete) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM user_region WHERE user_id=? AND region_id=?")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
		else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_region (user_id, region_id, region_visible) VALUES (?, ?, 1)")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
	}

	public void toggleTodo(Connection c, Optional<Integer> authUserId, int problemId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in");
		Preconditions.checkArgument(problemId > 0, "Problem id not set");
		int todoId = -1;
		try (PreparedStatement ps = c.prepareStatement("SELECT id FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					todoId = rst.getInt("id");
				}
			}
		}
		if (todoId > 0) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todoId);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO todo (user_id, problem_id, created) VALUES (?, ?, now())")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, problemId);
				ps.execute();
			}
		}
	}

	public void trashRecover(Connection c, Setup setup, Optional<Integer> authUserId, int idArea, int idSector, int idProblem, int idMedia) throws SQLException {
		ensureSuperadminWriteRegion(c, authUserId, setup.idRegion());
		String sqlStr = null;
		int id = 0;
		// Important to check media first. A media in trash always has idArea, idSector or idProblem!
		if (idMedia > 0) {
			sqlStr = "UPDATE media SET deleted_user_id=NULL, deleted_timestamp=NULL WHERE id=?";
			id = idMedia;
		}
		else if (idArea > 0) {
			sqlStr = "UPDATE area SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "UPDATE sector SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "UPDATE problem SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idProblem;
		}
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			ps.execute();
		}
	}

	public void updateMediaInfo(Connection c, Optional<Integer> authUserId, MediaInfo m) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) LEFT JOIN guestbook g ON (p.id=g.problem_id)) LEFT JOIN media_guestbook mg ON (g.id=mg.guestbook_id AND mg.media_id=?)) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mg.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, m.mediaId());
			ps.setInt(3, m.mediaId());
			ps.setInt(4, m.mediaId());
			ps.setInt(5, m.mediaId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("mp.problem_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
		try (PreparedStatement ps = c.prepareStatement("UPDATE media SET description=? WHERE id=?")) {
			ps.setString(1, Strings.emptyToNull(m.description()));
			ps.setInt(2, m.mediaId());
			ps.execute();
		}
		if (areaId > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE media_area SET trivia=? WHERE media_id=? AND area_id=?")) {
				ps.setBoolean(1, m.trivia());
				ps.setInt(2, m.mediaId());
				ps.setInt(3, areaId);
				ps.execute();
			}
		}
		else if (sectorId > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE media_sector SET trivia=? WHERE media_id=? AND sector_id=?")) {
				ps.setBoolean(1, m.trivia());
				ps.setInt(2, m.mediaId());
				ps.setInt(3, sectorId);
				ps.execute();
			}
		}
		else if (problemId > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE media_problem SET pitch=?, trivia=? WHERE media_id=? AND problem_id=?")) {
				ps.setInt(1, m.pitch());
				ps.setBoolean(2, m.trivia());
				ps.setInt(3, m.mediaId());
				ps.setInt(4, problemId);
				ps.execute();
			}
		}
	}

	public void upsertComment(Connection c, Optional<Integer> authUserId, Setup s, Comment co, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		if (co.id() > 0) {
			List<ProblemComment> comments = getProblem(c, authUserId, s, co.idProblem(), false, false).getComments();
			Preconditions.checkArgument(!comments.isEmpty(), "No comment on problem " + co.idProblem());
			ProblemComment comment = comments.stream().filter(x -> x.getId() == co.id()).findAny().orElseThrow();
			if (comment.isEditable()) {
				if (co.delete()) {
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM guestbook WHERE id=?")) {
						ps.setInt(1, co.id());
						ps.execute();
					}
				}
				else {
					try (PreparedStatement ps = c.prepareStatement("UPDATE guestbook SET message=?, danger=?, resolved=? WHERE id=?")) {
						ps.setString(1, GlobalFunctions.stripString(co.comment()));
						ps.setBoolean(2, co.danger());
						ps.setBoolean(3, co.resolved());
						ps.setInt(4, co.id());
						ps.execute();
						if (co.newMedia() != null) {
							// New media
							for (NewMedia m : co.newMedia()) {
								final int idProblem = 0;
								final int idSector = 0;
								final int idArea = 0;
								addNewMedia(c, authUserId, idProblem, 0, m.trivia(), idSector, idArea, co.id(), m, multiPart);
							}
						}
					}
				}
			}
			else if (!comment.isDanger() && !comment.isResolved() && co.danger()) {
				try (PreparedStatement ps = c.prepareStatement("UPDATE guestbook SET danger=? WHERE id=?")) {
					ps.setBoolean(1, co.danger());
					ps.setInt(2, co.id());
					ps.execute();
				}
			}
			else {
				throw new IllegalArgumentException("Comment not editable by " + authUserId.orElseThrow() + ". Other users can only mark as dangerous - comment=" + comment);
			}
		} else {
			Preconditions.checkNotNull(GlobalFunctions.stripString(co.comment()));
			int parentId = 0;
			try (PreparedStatement ps = c.prepareStatement("SELECT MIN(id) FROM guestbook WHERE problem_id=?")) {
				ps.setInt(1, co.idProblem());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						parentId = rst.getInt(1);
					}
				}
			}

			try (PreparedStatement ps = c.prepareStatement("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id, danger, resolved) VALUES (now(), ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, GlobalFunctions.stripString(co.comment()));
				ps.setInt(2, co.idProblem());
				ps.setInt(3, authUserId.orElseThrow());
				setNullablePositiveInteger(ps, 4, parentId);
				ps.setBoolean(5, co.danger());
				ps.setBoolean(6, co.resolved());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idGuestbook = rst.getInt(1);
						if (co.newMedia() != null) {
							// New media
							for (NewMedia m : co.newMedia()) {
								final int idProblem = 0;
								final int idSector = 0;
								final int idArea = 0;
								addNewMedia(c, authUserId, idProblem, 0, m.trivia(), idSector, idArea, idGuestbook, m, multiPart);
							}
						}
					}
				}
			}
		}
		fillActivity(c, co.idProblem());
	}

	public void upsertMediaSvg(Connection c, Optional<Integer> authUserId, Setup setup, Media m) throws SQLException {
		ensureAdminWriteRegion(c, authUserId, setup.idRegion());
		// Clear existing
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_svg WHERE media_id=?")) {
			ps.setInt(1, m.id());
			ps.execute();
		}
		// Insert
		for (MediaSvgElement element : m.mediaSvgs()) {
			if (element.t().equals(MediaSvgElementType.PATH)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")) {
					ps.setInt(1, m.id());
					ps.setString(2, element.path());
					ps.execute();
				}
			}
			else if (element.t().equals(MediaSvgElementType.RAPPEL_BOLTED) || element.t().equals(MediaSvgElementType.RAPPEL_NOT_BOLTED)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, m.id());
					ps.setInt(2, element.rappelX());
					ps.setInt(3, element.rappelY());
					ps.setBoolean(4, element.t().equals(MediaSvgElementType.RAPPEL_BOLTED));
					ps.execute();
				}
			}
			else {
				throw new RuntimeException("Invalid type: " + element.t());
			}
		}
	}

	public void upsertPermissionUser(Connection c, int regionId, Optional<Integer> authUserId, PermissionUser u) throws SQLException {
		ensureSuperadminWriteRegion(c, authUserId, regionId);
		// Upsert
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?")) {
			ps.setInt(1, u.userId());
			ps.setInt(2, regionId);
			ps.setBoolean(3, u.adminRead());
			ps.setBoolean(4, u.adminWrite());
			ps.setBoolean(5, u.superadminRead());
			ps.setBoolean(6, u.superadminWrite());
			ps.setBoolean(7, u.adminRead());
			ps.setBoolean(8, u.adminWrite());
			ps.setBoolean(9, u.superadminRead());
			ps.setBoolean(10, u.superadminWrite());
			ps.execute();
		}
		// region_visible only set by user, if user have not asked to see a specific region --> remove row
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM user_region WHERE admin_read=0 AND admin_write=0 AND superadmin_read=0 AND superadmin_write=0 AND region_visible=0")) {
			ps.execute();
		}
	}

	public void upsertSvg(Connection c, Optional<Integer> authUserId, int problemId, int pitch, int mediaId, Svg svg) throws SQLException {
		ensureAdminWriteProblem(c, authUserId, problemId);
		// Delete/Insert/Update
		if (svg.delete() || GlobalFunctions.stripString(svg.path()) == null) {
			if (pitch == 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=? AND pitch IS NULL")) {
					ps.setInt(1, mediaId);
					ps.setInt(2, problemId);
					ps.execute();
				}
			}
			else {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=? AND pitch=?")) {
					ps.setInt(1, mediaId);
					ps.setInt(2, problemId);
					ps.setInt(3, pitch);
					ps.execute();
				}
			}
		} else if (svg.id() <= 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO svg (media_id, problem_id, pitch, path, has_anchor, anchors, trad_belay_stations, texts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				setNullablePositiveInteger(ps, 3, pitch);
				ps.setString(4, svg.path());
				ps.setBoolean(5, svg.hasAnchor());
				ps.setString(6, svg.anchors());
				ps.setString(7, svg.tradBelayStations());
				ps.setString(8, svg.texts());
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("UPDATE svg SET media_id=?, problem_id=?, pitch=?, path=?, has_anchor=?, anchors=?, trad_belay_stations=?, texts=? WHERE id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				setNullablePositiveInteger(ps, 3, pitch);
				ps.setString(4, svg.path());
				ps.setBoolean(5, svg.hasAnchor());
				ps.setString(6, svg.anchors());
				ps.setString(7, svg.tradBelayStations());
				ps.setString(8, svg.texts());
				ps.setInt(9, svg.id());
				ps.execute();
			}
		}
	}

	private int addNewMedia(Connection c, Optional<Integer> authUserId, int idProblem, int pitch, boolean trivia, int idSector, int idArea, int idGuestbook, NewMedia m, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		int idMedia = -1;
		logger.debug("addNewMedia(authUserId={}, idProblem={}, pitch={}, trivia={}, idSector={}, idArea={}, idGuestbook={}, m={}) initialized", authUserId, idProblem, pitch, trivia, idSector, idArea, idGuestbook, m);
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector > 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea > 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea == 0 && idGuestbook > 0));
		boolean alreadyExistsInDb = false;
		boolean isMovie = false;
		String suffix = null;
		if (Strings.isNullOrEmpty(m.name())) {
			// Embed video url
			Preconditions.checkNotNull(m.embedThumbnailUrl(), "embedThumbnailUrl required");
			Preconditions.checkNotNull(m.embedVideoUrl(), "embedVideoUrl required");
			// First check if video already exists in system, don't duplicate videos!
			try (PreparedStatement ps = c.prepareStatement("SELECT id FROM media WHERE embed_url=?")) {
				ps.setString(1, m.embedVideoUrl());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						alreadyExistsInDb = true;
						idMedia = rst.getInt(1);
					}
				}
			}
			suffix = "mp4";
			isMovie = true;
		}
		else {
			suffix = "jpg";
			isMovie = false;
		}

		/**
		 * DB
		 */
		if (!alreadyExistsInDb) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, embed_url) VALUES (?, ?, ?, ?, NOW(), ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setBoolean(1, isMovie);
				ps.setString(2, suffix);
				ps.setInt(3, getExistingOrInsertUser(c, m.photographer()));
				ps.setInt(4, authUserId.orElseThrow());
				ps.setString(5, GlobalFunctions.stripString(m.description()));
				ps.setString(6, m.embedVideoUrl());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idMedia = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idMedia > 0);
		if (idProblem > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_problem (media_id, problem_id, pitch, trivia, milliseconds) VALUES (?, ?, ?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idProblem);
				setNullablePositiveInteger(ps, 3, pitch);
				ps.setBoolean(4, trivia);
				ps.setLong(5, m.embedMilliseconds());
				ps.execute();
			}
		} else if (idSector > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_sector (media_id, sector_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idSector);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		} else if (idArea > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_area (media_id, area_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idArea);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		} else if (idGuestbook > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idGuestbook);
				ps.execute();
			}
		} else {
			throw new RuntimeException("Server error");
		}
		if (!alreadyExistsInDb) {
			if (m.inPhoto() != null && !m.inPhoto().isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
					for (User u : m.inPhoto()) {
						ps.setInt(1, idMedia);
						ps.setInt(2, getExistingOrInsertUser(c, u.name()));
						ps.execute();
					}
					ps.executeBatch();
				}
			}
			if (isMovie) {
				ImageHelper.saveImageFromEmbedVideo(this, c, idMedia, m.embedVideoUrl());
			}
			else {
				try (InputStream is = multiPart.getField(m.name()).getValueAs(InputStream.class)) {
					byte[] bytes = ByteStreams.toByteArray(is);
					ImageHelper.saveImage(this, c, idMedia, bytes);
				}
			}
		}
		return idMedia;
	}

	private int addUser(Connection c, String email, String firstname, String lastname, String picture) throws SQLException {
		int id = -1;
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO user (firstname, lastname, picture) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, firstname);
			ps.setString(2, lastname);
			ps.setString(3, picture);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					id = rst.getInt(1);
					logger.debug("addUser(email={}, firstname={}, lastname={}, picture={}) - getInt(1)={}", email, firstname, lastname, picture, id);
				}
			}
		}
		Preconditions.checkArgument(id > 0, "id=" + id + ", firstname=" + firstname + ", lastname=" + lastname);
		if (!Strings.isNullOrEmpty(email)) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_email (user_id, email) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setString(2, email.toLowerCase());
				ps.execute();
			}
		}
		if (picture != null) {
			try (InputStream is = URI.create(picture).toURL().openStream()) {
				ImageHelper.saveAvatar(id, is);
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return id;
	}

	private void ensureAdminWriteArea(Connection c, Optional<Integer> authUserId, int areaId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, user_region ur WHERE a.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1")) {
			ps.setInt(1, areaId);
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteProblem(Connection c, Optional<Integer> authUserId, int problemId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, sector s, problem p, user_region ur WHERE p.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND a.id=s.area_id AND s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1")) {
			ps.setInt(1, problemId);
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteRegion(Connection c, Optional<Integer> authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureSuperadminWriteRegion(Connection c, Optional<Integer> authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void fillMissingElevations(Connection c) throws SQLException, InterruptedException {
		List<Coordinates> coordinatesMissingElevation = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					coordinatesMissingElevation.add(new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		if (!coordinatesMissingElevation.isEmpty()) {
			try {
				GeoHelper.fillMissingElevations(coordinatesMissingElevation);
				try (PreparedStatement ps = c.prepareStatement("UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?")) {
					for (Coordinates coord : coordinatesMissingElevation) {
						ps.setDouble(1, coord.getElevation());
						ps.setString(2, coord.getElevationSource());
						ps.setDouble(3, coord.getId());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	private CompassDirection getCompassDirection(Setup s, int id) {
		if (id == 0) {
			return null;
		}
		return s.compassDirections()
				.stream()
				.filter(cd -> cd.id() == id)
				.findAny()
				.get();
	}

	private List<CompassDirection> getCompassDirections(Connection c) throws SQLException {
		List<CompassDirection> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT id, direction FROM compass_direction ORDER BY id")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String direction = rst.getString("direction");
					res.add(new CompassDirection(id, direction));
				}
			}
		}
		return res;
	}

	private int getExistingOrInsertUser(Connection c, String name) throws SQLException {
		if (Strings.isNullOrEmpty(name)) {
			return 1049; // Unknown
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT id FROM user WHERE CONCAT(firstname, ' ', COALESCE(lastname,''))=?")) {
			ps.setString(1, name);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					return rst.getInt("id");
				}
			}
		}
		int usId = addUser(c, null, name, null, null);
		Preconditions.checkArgument(usId > 0);
		return usId;
	}

	private List<ExternalLink> getExternalLinksArea(Connection c, int areaId, boolean inherited) throws SQLException {
		List<ExternalLink> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT e.id, e.url, e.title
				FROM external_link_area ea, external_link e
				WHERE ea.area_id=? AND ea.external_link_id=e.id
				ORDER BY e.title
				""")) {
			ps.setInt(1, areaId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String title = rst.getString("title");
					res.add(new ExternalLink(id, url, title, inherited));
				}
			}
		}
		return res;
	}

	private List<ExternalLink> getExternalLinksProblem(Connection c, int problemId, boolean inherited) throws SQLException {
		List<ExternalLink> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT e.id, e.url, e.title
				FROM external_link_problem ep, external_link e
				WHERE ep.problem_id=? AND ep.external_link_id=e.id
				ORDER BY e.title
				""")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String title = rst.getString("title");
					res.add(new ExternalLink(id, url, title, inherited));
				}
			}
		}
		return res;
	}

	private List<ExternalLink> getExternalLinksSector(Connection c, int sectorId, boolean inherited) throws SQLException {
		List<ExternalLink> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT e.id, e.url, e.title
				FROM external_link_sector ea, external_link e
				WHERE ea.sector_id=? AND ea.external_link_id=e.id
				ORDER BY e.title
				""")) {
			ps.setInt(1, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String title = rst.getString("title");
					res.add(new ExternalLink(id, url, title, inherited));
				}
			}
		}
		return res;
	}

	private Map<Integer, String> getFaAidNamesOnSector(Connection c, int sectorId) throws SQLException {
		Map<Integer, String> res = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT p.id, group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa FROM problem p, fa_aid_user a, user u WHERE p.sector_id=? AND p.id=a.problem_id AND a.user_id=u.id GROUP BY p.id")) {
			ps.setInt(1, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idProblem = rst.getInt("id");
					String fa = rst.getString("fa");
					res.put(idProblem, fa);
				}
			}
		}
		return res;
	}

	private List<Grade> getGrades(Connection c, GradeSystem gradeSystem) throws SQLException {
		List<Grade> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT grade_id, grade FROM grade WHERE t=? ORDER BY grade_id")) {
			ps.setString(1, gradeSystem.toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int gradeId = rst.getInt("grade_id");
					String grade = rst.getString("grade");
					res.add(new Grade(gradeId, grade));
				}
			}
		}
		return res;
	}

	private List<Media> getMediaArea(Connection c, Optional<Integer> authUserId, int id, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, a.name location, ma.trivia, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((media m INNER JOIN media_area ma ON m.id=ma.media_id AND m.deleted_user_id IS NULL AND ma.area_id=?) INNER JOIN area a ON ma.area_id=a.id) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.uploader_user_id, m.checksum, ma.trivia, m.description, a.name, m.width, m.height, m.is_movie, m.embed_url, ma.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; // Don't inherit trivia image
					}
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, null));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaGuestbook(Connection c, Optional<Integer> authUserId, int id) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(p.name,' (',a.name,'/',s.name,')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id) INNER JOIN media_guestbook mg ON g.id=mg.guestbook_id) INNER JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL)) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE g.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, a.name, s.name, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, false, 0, 0, 0, null));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaProblem(Connection c, Setup s, Optional<Integer> authUserId, int areaId, int sectorId, int problemId, boolean showHiddenMedia) throws SQLException {
		List<Media> media = getMediaSector(c, s, authUserId, sectorId, problemId, true, areaId, 0, problemId, showHiddenMedia);
		try (PreparedStatement ps = c.prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, CONCAT(p.name,' (',a.name,'/',s.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, mp.trivia, ROUND(mp.milliseconds/1000) t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN media_problem mp ON p.id=mp.problem_id) INNER JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL)) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE p.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, p.name, s.name, a.name, m.description, m.width, m.height, m.is_movie, m.embed_url, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.trivia, mp.milliseconds, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = rst.getInt("pitch");
					boolean trivia = rst.getBoolean("trivia");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String t = rst.getString("t");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					if (embedUrl != null) {
						long seconds = Long.parseLong(t);
						if (seconds > 0) {
							if (embedUrl.contains("youtu")) {
								embedUrl += "?start=" + seconds;
							}
							else {
								embedUrl += "#t=" + seconds + "s";
							}
						}
					}
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					List<Svg> svgs = getSvgs(c, s, authUserId, idMedia, width, height);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, t, mediaSvgs, problemId, svgs, mediaMetadata, embedUrl, false, (svgs == null || svgs.isEmpty()? areaId : 0), sectorId, 0, null));
				}
			}
		}
		if (media != null && media.isEmpty()) {
			media = null;
		}
		return media;
	}

	private List<Media> getMediaSector(Connection c, Setup s, Optional<Integer> authUserId, int idSector, int optionalIdProblem, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem, boolean showHiddenMedia) throws SQLException {
		List<Media> allMedia = new ArrayList<>();
		Set<Media> mediaWithRequestedTopoLine = new HashSet<>();
		String sqlStr = "SELECT m.id, m.uploader_user_id, m.checksum, ms.trivia, CONCAT(s.name,' (',a.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN media_sector ms ON s.id=ms.sector_id) INNER JOIN media m ON (ms.media_id=m.id AND m.deleted_user_id IS NULL) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE s.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, ms.trivia, m.description, s.name, a.name, m.width, m.height, m.is_movie, m.embed_url, ms.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, idSector);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId.orElse(0);
					long crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; // Don't inherit trivia image
					}
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(c, idMedia);
					List<Svg> svgs = getSvgs(c, s, authUserId, idMedia, width, height);
					MediaMetadata mediaMetadata = MediaMetadata.from(dateCreated, dateTaken, capturer, tagged, description, location);
					Media m = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, optionalIdProblem, svgs, mediaMetadata, embedUrl, inherited,
							(svgs == null || svgs.isEmpty()? enableMoveToIdArea : 0),
							enableMoveToIdSector,
							(svgs == null || svgs.stream().filter(x -> x.problemId() != enableMoveToIdProblem).findAny().isEmpty()? enableMoveToIdProblem : 0),
							null);
					if (optionalIdProblem != 0 && svgs != null && svgs.stream().filter(svg -> svg.problemId() == optionalIdProblem).findAny().isPresent()) {
						mediaWithRequestedTopoLine.add(m);
					}
					allMedia.add(m);
				}
			}
		}
		// Figure out what to actually return
		if (!showHiddenMedia && !mediaWithRequestedTopoLine.isEmpty()) {
			// Only images without topo lines or images with topo lines for this problem
			return allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty() || mediaWithRequestedTopoLine.contains(m)).collect(Collectors.toList());
		}
		else if (!showHiddenMedia && s.gradeSystem().equals(GradeSystem.BOULDER) && optionalIdProblem != 0) {
			// In bouldering we don't want to show all rocks with lines if this one does not have a line
			return allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty()).collect(Collectors.toList());
		}
		return allMedia;
	}

	private List<MediaSvgElement> getMediaSvgElements(Connection c, int idMedia) throws SQLException {
		List<MediaSvgElement> res = null;
		try (PreparedStatement ps = c.prepareStatement("SELECT ms.id, ms.path, ms.rappel_x, ms.rappel_y, ms.rappel_bolted FROM media_svg ms WHERE ms.media_id=?")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					String path = rst.getString("path");
					if (path != null) {
						res.add(MediaSvgElement.fromPath(id, path));
					}
					else {
						int rappelX = rst.getInt("rappel_x");
						int rappelY = rst.getInt("rappel_y");
						boolean rappelBolted = rst.getBoolean("rappel_bolted");
						res.add(MediaSvgElement.fromRappel(id, rappelX, rappelY, rappelBolted));
					}
				}
			}
		}
		return res;
	}

	private Map<Integer, Coordinates> getProblemCoordinates(Connection c, Collection<Integer> idProblems) throws SQLException {
		Preconditions.checkArgument(!idProblems.isEmpty(), "idProblems is empty");
		Map<Integer, Coordinates> res = new HashMap<>();
		String in = ",?".repeat(idProblems.size()).substring(1);
		String sqlStr = "SELECT p.id id_problem, COALESCE(pc.id,c.id,sc.id,ac.id) coordinates_id, COALESCE(pc.latitude,c.latitude,sc.latitude,ac.latitude) latitude, COALESCE(pc.longitude,c.longitude,sc.longitude,ac.longitude) longitude, COALESCE(pc.elevation,c.elevation,sc.elevation,ac.elevation) elevation, COALESCE(pc.elevation_source,c.elevation_source,sc.elevation_source,ac.elevation_source) elevation_source FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) LEFT JOIN coordinates ac ON a.coordinates_id=ac.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates pc ON p.coordinates_id=pc.id) LEFT JOIN sector_outline so ON s.id=so.sector_id AND so.sorting=1) LEFT JOIN coordinates c ON so.coordinates_id=c.id) WHERE p.id IN (" + in + ")";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idProblems) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idProblem = rst.getInt("id_problem");
					int idCoordinates = rst.getInt("coordinates_id");
					if (idCoordinates > 0) {
						res.put(idProblem, new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source")));
					}
				}
			}
		}
		logger.debug("getProblemCoordinates(idProblems.size()={}) - res.size()={}", idProblems.size(), res.size());
		return res;
	}

	private Multimap<Integer, Coordinates> getRegionOutlines(Connection c, Collection<Integer> idRegions) throws SQLException {
		Preconditions.checkArgument(!idRegions.isEmpty(), "idProblems is empty");
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idRegions.size()).substring(1);
		String sqlStr = "SELECT ro.region_id id_region, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM region_outline ro, coordinates c WHERE ro.region_id IN (" + in + ") AND ro.coordinates_id=c.id ORDER BY ro.sorting";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idRegions) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					res.put(idRegion, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getRegionOutlines(idRegions.size()={}) - res.size()={}", idRegions.size(), res.size());
		return res;
	}

	private List<Coordinates> getSectorOutline(Connection c, int idSector) throws SQLException {
		Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(c, Collections.singleton(idSector));
		if (idSectorOutline == null || idSectorOutline.isEmpty()) {
			return null;
		}
		return Lists.newArrayList(idSectorOutline.get(idSector));
	}

	private Multimap<Integer, Coordinates> getSectorOutlines(Connection c, Collection<Integer> idSectors) throws SQLException {
		Preconditions.checkArgument(!idSectors.isEmpty(), "idSectors is empty");
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idSectors.size()).substring(1);
		String sqlStr = "SELECT so.sector_id id_sector, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM sector_outline so, coordinates c WHERE so.sector_id IN (" + in + ") AND so.coordinates_id=c.id ORDER BY so.sorting";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idSectors) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idSector = rst.getInt("id_sector");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					res.put(idSector, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getSectorOutlines(idSectors.size()={}) - res.size()={}", idSectors.size(), res.size());
		return res;
	}

	private List<SectorProblem> getSectorProblems(Connection c, Setup setup, Optional<Integer> authUserId, int sectorId) throws SQLException {
		List<SectorProblem> res = new ArrayList<>();
		Map<Integer, String> problemIdFirstAidAscentLookup = null;
		if (!setup.gradeSystem().equals(GradeSystem.BOULDER)) {
			problemIdFirstAidAscentLookup = getFaAidNamesOnSector(c, sectorId);
		}
		String sqlStr = """
				SELECT p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				       COUNT(DISTINCT ps.id) num_pitches,
				       COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN m.id END) num_images,
				       COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN m.id END) num_movies,
				       CASE WHEN MAX(svg.problem_id) IS NOT NULL THEN 1 ELSE 0 END has_topo,
				       group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,
				       p.fa_date,
				       COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars,
				       MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked,
				       CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END todo,
				       ty.id type_id, ty.type, ty.subtype,
				       danger.danger
				FROM ((((((((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN (media_problem mp LEFT JOIN media m ON (mp.media_id=m.id AND mp.trivia=0 AND m.deleted_user_id IS NULL)) ON p.id=mp.problem_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN todo ON (p.id=todo.problem_id AND todo.user_id=?)) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN (SELECT DISTINCT problem_id problem_id FROM svg) svg ON p.id=svg.problem_id
				WHERE p.sector_id=?
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.grade, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, p.fa_date, todo.id, ty.id, ty.type, ty.subtype, danger.danger
				ORDER BY p.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, authUserId.orElse(0));
			ps.setInt(4, authUserId.orElse(0));
			ps.setInt(5, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String fa = rst.getString("fa");
					if (problemIdFirstAidAscentLookup != null && problemIdFirstAidAscentLookup.containsKey(id)) {
						String faAid = "FA: " + problemIdFirstAidAscentLookup.get(id);
						if (fa == null) {
							fa = faAid;
						}
						else {
							fa = faAid + ". FFA: " + fa;
						}
					}
					LocalDate faDate = rst.getObject("fa_date", LocalDate.class);
					String faDateStr = faDate == null? null : DateTimeFormatter.ISO_LOCAL_DATE.format(faDate);
					int numPitches = rst.getInt("num_pitches");
					boolean hasImages = rst.getInt("num_images")>0;
					boolean hasMovies = rst.getInt("num_movies")>0;
					boolean hasTopo = rst.getBoolean("has_topo");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean todo = rst.getBoolean("todo");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					boolean danger = rst.getBoolean("danger");
					res.add(new SectorProblem(id, broken, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, setup.gradeConverter().getGradeFromIdGrade(grade), fa, faDateStr, numPitches, hasImages, hasMovies, hasTopo, coordinates, numTicks, stars, ticked, todo, t, danger));
				}
			}
		}
		return res;
	}

	private Map<Integer, Slope> getSectorSlopes(Connection c, boolean approachNotDescent, Collection<Integer> idSectors) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, Slope> res = new HashMap<>();
		Preconditions.checkArgument(!idSectors.isEmpty(), "idSectors is empty");
		Multimap<Integer, Coordinates> idSectorCoordinates = ArrayListMultimap.create();
		String sqlStr = """
				SELECT s.sector_id id_sector, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, st_distance_sphere(point(longitude, latitude),
				       point(lag(longitude) over (partition by s.sector_id order by s.sorting), lag(latitude) over (partition by s.sector_id order by s.sorting))) m
				FROM %s s, coordinates c
				WHERE s.sector_id IN (%s) AND s.coordinates_id=c.id
				ORDER BY s.sector_id, s.sorting
				""".formatted(
						approachNotDescent ? "sector_approach" : "sector_descent",
								",?".repeat(idSectors.size()).substring(1)
						);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idSectors) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				int prevIdSector = 0;
				double distance = 0;
				while (rst.next()) {
					int idSector = rst.getInt("id_sector");
					if (prevIdSector != idSector) {
						prevIdSector = idSector;
						distance = 0;
					}
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					distance += rst.getDouble("m");
					Coordinates coords = new Coordinates(id, latitude, longitude, elevation, elevationSource);
					coords.setDistance(distance);
					idSectorCoordinates.put(idSector, coords);
				}
			}
		}
		for (int idSector : idSectorCoordinates.keySet()) {
			List<Coordinates> coordinates = Lists.newArrayList(idSectorCoordinates.get(idSector));
			res.put(idSector, Slope.from(coordinates));
		}
		logger.debug("getSectorSlopes(approachNotDescent={}, idSectors.size()={}) - res.size()={}, duration={}", approachNotDescent, idSectors.size(), res.size(), stopwatch);
		return res;
	}

	private List<Svg> getSvgs(Connection c, Setup s, Optional<Integer> authUserId, int idMedia, int mediaWidth, int mediaHeight) throws SQLException {
		List<Svg> res = null;
		String sqlStr = """
				WITH x AS (
				  SELECT p.id problem_id, p.name problem_name, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, pt.subtype problem_subtype, p.nr,
						 ps.nr pitch, psg.grade problem_section_grade, psg.group problem_section_grade_group,
				         s.id, s.path, s.has_anchor, s.texts, s.anchors, s.trad_belay_stations, CASE WHEN p.type_id IN (1,2) THEN 1 ELSE 0 END prim,
				         MAX(CASE WHEN t.user_id=? OR fa.user_id THEN 1 ELSE 0 END) is_ticked, CASE WHEN t2.id IS NOT NULL THEN 1 ELSE 0 END is_todo, danger is_dangerous
				  FROM (((((((svg s INNER JOIN problem p ON s.problem_id=p.id) INNER JOIN type pt ON p.type_id=pt.id) LEFT JOIN fa ON (p.id=fa.problem_id AND fa.user_id=?))
				    LEFT JOIN problem_section ps ON (ps.problem_id=p.id AND ps.nr=s.pitch)) LEFT JOIN grade psg ON ps.grade=psg.grade_id AND psg.t=?)
				    LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN todo t2 ON p.id=t2.problem_id AND t2.user_id=?)
				    LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id
				  WHERE s.media_id=? AND p.trash IS NULL
				  GROUP BY p.id, p.name, pt.subtype, p.nr,
				           ps.id, ps.nr, psg.grade, psg.group,
				           s.id, s.path, s.has_anchor, s.texts, s.anchors, s.trad_belay_stations, t2.id, danger.danger
				)
				SELECT x.problem_id, x.problem_name, g.grade problem_grade, g.group problem_grade_group, x.problem_subtype, x.nr,
					   x.pitch, x.problem_section_grade, x.problem_section_grade_group,
				       x.id, x.path, x.has_anchor, x.texts, x.anchors, x.trad_belay_stations, x.prim, x.is_ticked, x.is_todo, x.is_dangerous
				FROM x INNER JOIN grade g ON x.grade=g.grade_id AND g.t=?
				ORDER BY x.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, authUserId.orElse(0));
			ps.setString(3, s.gradeSystem().toString());
			ps.setInt(4, authUserId.orElse(0));
			ps.setInt(5, idMedia);
			ps.setString(6, s.gradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					int pitch = rst.getInt("pitch");
					String problemGrade = rst.getString(pitch == 0? "problem_grade" : "problem_section_grade");
					int problemGradeGroup = rst.getInt(pitch == 0? "problem_grade_group" : "problem_section_grade_group");
					String problemSubtype = rst.getString("problem_subtype");
					int nr = rst.getInt("nr");
					int id = rst.getInt("id");
					String path = rst.getString("path");
					boolean hasAnchor = rst.getBoolean("has_anchor");
					String texts = rst.getString("texts");
					String anchors = rst.getString("anchors");
					String tradBelayStations = rst.getString("trad_belay_stations");
					boolean primary = rst.getBoolean("prim");
					boolean isTicked = rst.getBoolean("is_ticked");
					boolean isTodo = rst.getBoolean("is_todo");
					boolean isDangerous = rst.getBoolean("is_dangerous");
					res.add(new Svg(false, id, problemId, problemName, problemGrade, problemGradeGroup, problemSubtype, nr, pitch, path, hasAnchor, texts, anchors, tradBelayStations, primary, isTicked, isTodo, isDangerous));
				}
			}
		}
		return res;
	}

	private void setNullablePositiveDouble(PreparedStatement ps, int parameterIndex, double value) throws SQLException {
		if (value > 0) {
			ps.setDouble(parameterIndex, value);
		} else {
			ps.setNull(parameterIndex, Types.DOUBLE);
		}
	}

	private void setNullablePositiveInteger(PreparedStatement ps, int parameterIndex, int value) throws SQLException {
		if (value > 0) {
			ps.setInt(parameterIndex, value);
		} else {
			ps.setNull(parameterIndex, Types.INTEGER);
		}
	}

	private void setSectorProblemOrder(Connection c, List<SectorProblemOrder> lst) throws SQLException {
		if (!lst.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE problem SET nr=? WHERE id=?")) {
				for (SectorProblemOrder x : lst) {
					ps.setInt(1, x.nr());
					ps.setInt(2, x.id());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
	}

	/**
	 * When upserting a problem, the user can change the nr.
	 * This function will move other problems in this sector to make place for this update/insert.
	 * Ignore sectors with custom ordering (this is often the case when numbers are set to match a topo-image/pdf)
	 * We don't need to update this problems nr, it will be updated later by the endpoint.
	 */
	private void tryFixSectorOrdering(Connection c, int sectorId, int problemId, int problemNewNr) throws SQLException {
		List<SectorProblemOrder> lst = new ArrayList<>();
		if (problemId > 0) {
			// Existing problem, check if user changed nr
			String sqlStr = """
					WITH x AS (
					  SELECT p.sector_id, COUNT(p.id) num_problems, MAX(p.nr) max_num
					  FROM problem p
					  WHERE p.sector_id=?
					  GROUP BY p.sector_id
					)
					SELECT p.id
					FROM problem p_input, x,
					     problem p
					WHERE p_input.id=? AND p_input.nr!=?
					  AND p_input.sector_id=x.sector_id AND x.num_problems=x.max_num
					  AND p_input.sector_id=p.sector_id
					  AND p.id!=p_input.id
					ORDER BY p.nr
					""";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, sectorId);
				ps.setInt(2, problemId);
				ps.setInt(3, problemNewNr);
				try (ResultSet rst = ps.executeQuery()) {
					int nr = 0;
					while (rst.next()) {
						if (++nr == problemNewNr) {
							++nr;
						}
						int id = rst.getInt("id");
						lst.add(new SectorProblemOrder(id, null, nr));
					}
				}
			}
		}
		else if (problemNewNr != 0) {
			// New problem with specific nr
			String sqlStr = """
					WITH x AS (
					  SELECT p.sector_id, COUNT(p.id) num_problems, MAX(p.nr) max_num
					  FROM problem p
					  WHERE p.sector_id=?
					  GROUP BY p.sector_id
					)
					SELECT p.id
					FROM x, problem p
					WHERE x.num_problems=x.max_num
					  AND x.sector_id=p.sector_id
					ORDER BY p.nr
					""";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, sectorId);
				try (ResultSet rst = ps.executeQuery()) {
					int nr = 0;
					while (rst.next()) {
						if (++nr == problemNewNr) {
							++nr;
						}
						int id = rst.getInt("id");
						lst.add(new SectorProblemOrder(id, null, nr));
					}
				}
			}
		}
		setSectorProblemOrder(c, lst);
	}

	private void upsertExternalLinks(Connection c, List<ExternalLink> newLinks, int areaId, int sectorId, int problemId) throws SQLException {
		// Delete removed links
		List<ExternalLink> previousLinks = null;
		if (areaId > 0) {
			previousLinks = getExternalLinksArea(c, areaId, false);
		}
		else if (sectorId > 0) {
			previousLinks = getExternalLinksSector(c, sectorId, false);
		}
		else if (problemId > 0) {
			previousLinks = getExternalLinksProblem(c, problemId, false);
		}
		else {
			throw new UnsupportedOperationException("areaId=0, sectorId=0, problemId=0");
		}
		var toRemove = previousLinks.stream()
				.filter(l -> newLinks == null || newLinks.stream().filter(x -> x.id() == l.id()).findAny().isEmpty())
				.toList();
		if (!toRemove.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM external_link WHERE id=?")) {
				for (var link : toRemove) {
					ps.setInt(1, link.id());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		if (newLinks != null) {
			var newLinksUpdate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() != 0)
					.toList();
			var newLinksCreate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() == 0)
					.toList();
			if (!newLinksUpdate.isEmpty()) {
				// Updating existing links
				try (PreparedStatement ps = c.prepareStatement("UPDATE external_link SET url=?, title=? WHERE id=?")) {
					for (var l : newLinksUpdate) {
						ps.setString(1, l.url());
						ps.setString(2, l.title());
						ps.setInt(3, l.id());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			if (!newLinksCreate.isEmpty()) {
				// Insert new links
				for (var l : newLinksCreate) {
					try (PreparedStatement ps = c.prepareStatement("INSERT INTO external_link (url, title) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
						ps.setString(1, l.url());
						ps.setString(2, l.title());
						ps.executeUpdate();
						try (ResultSet rst = ps.getGeneratedKeys()) {
							if (rst != null && rst.next()) {
								int externalLinkId = rst.getInt(1);
								if (areaId > 0) {
									try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO external_link_area (external_link_id, area_id) VALUES (?, ?)")) {
										ps2.setInt(1, externalLinkId);
										ps2.setInt(2, areaId);
										ps2.execute();
									}
								}
								else if (sectorId > 0) {
									try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO external_link_sector (external_link_id, sector_id) VALUES (?, ?)")) {
										ps2.setInt(1, externalLinkId);
										ps2.setInt(2, sectorId);
										ps2.execute();
									}
								}
								else if (problemId > 0) {
									try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO external_link_problem (external_link_id, problem_id) VALUES (?, ?)")) {
										ps2.setInt(1, externalLinkId);
										ps2.setInt(2, problemId);
										ps2.execute();
									}
								}
								else {
									throw new UnsupportedOperationException("areaId=0, sectorId=0, problemId=0");
								}
							}
						}
					}
				}
			}
		}
	}

	private void upsertTickRepeats(Connection c, int idTick, List<TickRepeat> repeats) throws SQLException {
		// Deleted removed ascents
		String repeatIdsToKeep = repeats == null? null : repeats.stream().filter(x -> x.id() > 0).map(TickRepeat::id).map(String::valueOf).collect(Collectors.joining(","));
		String sqlStr = Strings.isNullOrEmpty(repeatIdsToKeep) ? "DELETE FROM tick_repeat WHERE tick_id=?" :
			"DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (" + repeatIdsToKeep + ")";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, idTick);
			ps.execute();
		}
		// Upsert repeats
		if (repeats != null && !repeats.isEmpty()) {
			for (TickRepeat r : repeats) {
				final LocalDate dt = Strings.isNullOrEmpty(r.date())? null : LocalDate.parse(r.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				if (r.id() > 0) {
					try (PreparedStatement ps = c.prepareStatement("UPDATE tick_repeat SET date=?, comment=? WHERE id=?")) {
						ps.setObject(1, dt);
						ps.setString(2, GlobalFunctions.stripString(r.comment()));
						ps.setInt(3, r.id());
						int res = ps.executeUpdate();
						if (res != 1) {
							throw new SQLException("Invalid repeat=" + r);
						}
					}
				}
				else {
					try (PreparedStatement ps = c.prepareStatement("INSERT INTO tick_repeat (tick_id, date, comment) VALUES (?, ?, ?)")) {
						ps.setInt(1, idTick);
						ps.setObject(2, dt);
						ps.setString(3, GlobalFunctions.stripString(r.comment()));
						ps.execute();
					}
				}
			}
		}
	}
}
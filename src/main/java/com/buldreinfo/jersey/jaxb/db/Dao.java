package com.buldreinfo.jersey.jaxb.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.Auth0Profile;
import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.helpers.HitsFormatter;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.io.VideoHelper;
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
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageFirstAscent;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageLastComment;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageNewestMedia;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRecentAscent;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageStats;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
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
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileGradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileIdentity;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileKpis;
import com.buldreinfo.jersey.jaxb.model.ProfileAscent;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoArea;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoProblem;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoSector;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Region;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.buldreinfo.jersey.jaxb.model.SectorProblemOrder;
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
import com.buldreinfo.jersey.jaxb.model.TopRank;
import com.buldreinfo.jersey.jaxb.model.TopUser;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserRegion;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.NormalizedVertex;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Dao {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String ACTIVITY_TYPE_TICK_REPEAT = "TICK_REPEAT";
	private static final long MAX_IMAGE_UPLOAD_BYTES = 25L * 1024L * 1024L;
	private static final long MAX_VIDEO_UPLOAD_BYTES = 1024L * 1024L * 1024L;
	private static Logger logger = LogManager.getLogger();
	private final Gson gson = new Gson();

	public Dao() {
	}

	public void addProblemMedia(Connection c, Optional<Integer> authUserId, Problem p, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		for (NewMedia m : p.getNewMedia()) {
			final int idSector = 0;
			final int idArea = 0;
			final int idGuestbook = 0;
			final int idUserAvatar = 0;
			addNewMedia(c, authUserId, p.getId(), m.pitch(), m.trivia(), idSector, idArea, idGuestbook, idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
		}
		fillActivity(c, p.getId());
	}

	public void deleteMedia(Connection c, Setup setup, Optional<Integer> authUserId, int idMedia) throws SQLException {
		ensureAdminOrMediaUpdatedByMe(c, setup, authUserId, idMedia);
		List<Integer> idProblems = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT problem_id FROM media_problem WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idProblems.add(rst.getInt("problem_id"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, idMedia);
			ps.execute();
		}

		for (int idProblem : idProblems) {
			fillActivity(c, idProblem);
		}
	}

	public void deleteMediaAnalysis(Connection c, int idMedia) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_label WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_object WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_analysis WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		logger.debug("Deleted existing AI analysis for idMedia={}", idMedia);
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

	public void ensureUserExists(Connection c, int userId) throws SQLException {
		Preconditions.checkArgument(userId > 0, "Invalid userId=%s", userId);
		boolean exists = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT id FROM user WHERE id=?")) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				exists = rst.next();
			}
		}
		if (!exists) {
			throw new NoSuchElementException("Could not find user with id=" + userId);
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
		List<Integer> faUserIds = new ArrayList<>();
		boolean hasFa = false;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT p.fa_date, p.last_updated, f.user_id
				FROM problem p
				JOIN grade g ON p.grade_id=g.id
				LEFT JOIN fa f ON p.id=f.problem_id
				WHERE p.id=?
				  AND (g.grade!='n/a' OR f.user_id IS NOT NULL)
				""")) {
			ps.setInt(1, idProblem);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (!hasFa) {
						hasFa = true;
						LocalDate faDate = rst.getObject("fa_date", LocalDate.class);
						LocalDateTime lastUpdated = rst.getObject("last_updated", LocalDateTime.class);
						if (faDate != null && lastUpdated != null) {
							problemActivityTimestamp = faDate.atTime(lastUpdated.getHour(), lastUpdated.getMinute(), lastUpdated.getSecond());
						}
						else if (faDate != null) {
							problemActivityTimestamp = faDate.atStartOfDay();
						}
					}
					int faUserId = rst.getInt("user_id");
					if (faUserId > 0) {
						faUserIds.add(faUserId);
					}
				}
			}
		}
		try (PreparedStatement psAddActivity = c.prepareStatement("INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id, tick_repeat_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
			if (hasFa) {
				psAddActivity.setObject(1, problemActivityTimestamp != null? problemActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
				psAddActivity.setString(2, ACTIVITY_TYPE_FA);
				psAddActivity.setInt(3, idProblem);
				psAddActivity.setNull(4, Types.INTEGER);
				psAddActivity.setNull(5, Types.INTEGER);
				psAddActivity.setNull(6, Types.INTEGER);
				psAddActivity.setNull(7, Types.INTEGER);
				psAddActivity.addBatch();
			}

			/**
			 * Media
			 */
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT m.id, m.date_created
					FROM media_problem mp
					JOIN media m ON mp.media_id = m.id
					WHERE mp.problem_id = ? AND m.deleted_timestamp IS NULL
					ORDER BY m.date_created ASC
					""")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					record MediaItem(int id) {}
					List<MediaItem> buffer = new ArrayList<>();
					LocalDateTime groupAnchor = problemActivityTimestamp; 
					LocalDateTime latestInGroup = problemActivityTimestamp;
					while (rst.next()) {
						int id = rst.getInt("id");
						LocalDateTime current = rst.getObject("date_created", LocalDateTime.class);
						boolean inFA = groupAnchor != null && groupAnchor == problemActivityTimestamp && current != null && Math.abs(ChronoUnit.DAYS.between(groupAnchor, current)) <= 7;
						boolean inRolling = groupAnchor != null && groupAnchor != problemActivityTimestamp && current != null && Math.abs(ChronoUnit.HOURS.between(groupAnchor, current)) <= 24;
						if (groupAnchor != null && !inFA && !inRolling) {
							for (var item : buffer) {
								psAddActivity.setObject(1, latestInGroup != null ? latestInGroup : LocalDate.EPOCH.atStartOfDay());
								psAddActivity.setString(2, ACTIVITY_TYPE_MEDIA);
								psAddActivity.setInt(3, idProblem);
								psAddActivity.setInt(4, item.id());
								psAddActivity.setNull(5, Types.INTEGER);
								psAddActivity.setNull(6, Types.INTEGER);
								psAddActivity.setNull(7, Types.INTEGER);
								psAddActivity.addBatch();
							}
							buffer.clear();
							groupAnchor = current; 
						}
						if (groupAnchor == null) {
							groupAnchor = current;
						}
						latestInGroup = (groupAnchor == problemActivityTimestamp) ? problemActivityTimestamp : current;
						buffer.add(new MediaItem(id));
					}
					for (var item : buffer) {
						psAddActivity.setObject(1, latestInGroup != null ? latestInGroup : LocalDate.EPOCH.atStartOfDay());
						psAddActivity.setString(2, ACTIVITY_TYPE_MEDIA);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setInt(4, item.id());
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
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT t.user_id, t.date, t.created,
					       ROW_NUMBER() OVER (PARTITION BY DAY(t.created) ORDER BY t.created) ix_on_created_date
					FROM tick t
					WHERE t.problem_id=?
					ORDER BY t.date, t.created
					""")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int userId = rst.getInt("user_id");
						LocalDateTime tickActivityTimestamp = null;
						if (faUserIds.contains(userId) && problemActivityTimestamp != null) {
							tickActivityTimestamp = problemActivityTimestamp;
						}
						else {
							LocalDate tickDate = rst.getObject("date", LocalDate.class);
							LocalDateTime tickCreated = rst.getObject("created", LocalDateTime.class);
							if (tickDate != null && tickCreated != null) {
								if (tickCreated.toLocalDate().isAfter(tickDate)) {
									// Tick created on different date, use end of day in activity order
									int ixOnCreatedDate = rst.getInt("ix_on_created_date");
									tickActivityTimestamp = tickDate.atTime(23, 59, Math.min(ixOnCreatedDate, 59));
								}
								else {
									// Tick created on same date as FA, use HHMMSS in activity order
									tickActivityTimestamp = tickDate.atTime(tickCreated.getHour(), tickCreated.getMinute(), tickCreated.getSecond());
								}
							}
							else if (tickDate != null) {
								tickActivityTimestamp = tickDate.atStartOfDay();
							}
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
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT r.id, t.user_id, r.date, r.created
					FROM tick t
					JOIN tick_repeat r ON t.id=r.tick_id
					WHERE t.problem_id=?
					ORDER BY r.tick_id, r.date, r.id
					""")) {
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
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT g.id, g.post_time
					FROM guestbook g
					WHERE g.problem_id=?
					ORDER BY g.post_time
					""")) {
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

	public List<Activity> getActivity(Connection c, Optional<Integer> authUserId, Setup setup, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media, int offset) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Activity> res = new ArrayList<>();
		final Set<Integer> faIds = new HashSet<>();
		final Set<Integer> tickIds = new HashSet<>();
		final Set<Integer> repeatIds = new HashSet<>();
		final Set<Integer> mediaIds = new HashSet<>();
		final Set<Integer> gbIds = new HashSet<>();
		boolean showAllTime = offset > 0 ||lowerGrade > 0 || !fa || !comments || !ticks || !media || idArea > 0 || idSector > 0;
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id, ? show_all_time,
				         ? hide_fa, ? hide_guestbook, ? hide_ticks, ? hide_media, ? min_grade_weight,
				         ? filter_area_id, ? filter_sector_id
				),
				x AS (
				  SELECT a1.id, a1.type, a1.activity_timestamp, a1.problem_id 
				  FROM activity a1
				  JOIN req ON 1=1
				  JOIN problem p1 ON a1.problem_id = p1.id
				  JOIN sector s1 ON p1.sector_id = s1.id
				  JOIN area ar1 ON s1.area_id = ar1.id
				  WHERE ar1.region_id IN (
				      SELECT r.id FROM region r 
				      JOIN region_type rt ON r.id = rt.region_id 
				      LEFT JOIN user_region ur ON (r.id = ur.region_id AND ur.user_id = req.auth_user_id)
				      WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				        AND (r.id = req.region_id OR ur.user_id IS NOT NULL)
				  )
				    AND (req.show_all_time = 1 OR a1.activity_timestamp > DATE_SUB(NOW(), INTERVAL 2 YEAR))
				    AND (req.hide_fa = 0 OR a1.type != 'FA')
				    AND (req.hide_guestbook = 0 OR a1.type != 'GUESTBOOK')
				    AND (req.hide_ticks = 0 OR a1.type NOT IN ('TICK','TICK_REPEAT'))
				    AND (req.hide_media = 0 OR a1.type != 'MEDIA')
				    AND (req.min_grade_weight = 0 OR p1.grade_id IN (SELECT id FROM grade WHERE id >= req.min_grade_weight))
				    AND (req.filter_area_id = 0 OR s1.area_id = req.filter_area_id)
				    AND (req.filter_sector_id = 0 OR s1.id = req.filter_sector_id)
				  ORDER BY a1.activity_timestamp DESC, a1.problem_id DESC
				  LIMIT 50 OFFSET ?
				)
				SELECT 
				    x.activity_timestamp, 
				    a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, 
				    s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, 
				    x.problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, 
				    ty.subtype problem_subtype,
				    g.grade,
				    GROUP_CONCAT(DISTINCT concat(x.id,'-',x.type) SEPARATOR ',') activities 
				FROM x
				JOIN req ON 1=1
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY 
				    x.activity_timestamp, a.id, s.id, x.problem_id, p.name, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, x.problem_id DESC
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 1;
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, setup.idRegion());
			ps.setBoolean(ix++, showAllTime);
			ps.setBoolean(ix++, !fa);
			ps.setBoolean(ix++, !comments);
			ps.setBoolean(ix++, !ticks);
			ps.setBoolean(ix++, !media);
			ps.setInt(ix++, lowerGrade);
			ps.setInt(ix++, idArea);
			ps.setInt(ix++, idSector);
			ps.setInt(ix++, offset);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					LocalDateTime ts = rst.getObject("activity_timestamp", LocalDateTime.class);
					Set<Integer> currentActivityIds = new HashSet<>();
					String raw = rst.getString("activities");
					if (raw != null) {
						for (String entry : raw.split(",")) {
							String[] parts = entry.split("-");
							int id = Integer.parseInt(parts[0]);
							String type = parts[1];
							currentActivityIds.add(id);
							switch (type) {
							case "FA" -> faIds.add(id);
							case "TICK" -> tickIds.add(id);
							case "TICK_REPEAT" -> repeatIds.add(id);
							case "GUESTBOOK" -> gbIds.add(id);
							case "MEDIA" -> mediaIds.add(id);
							}
						}
					}
					res.add(new Activity(currentActivityIds, TimeAgo.getTimeAgo(ts.toLocalDate()), 
							rst.getInt("area_id"), rst.getString("area_name"), rst.getBoolean("area_locked_admin"), rst.getBoolean("area_locked_superadmin"),
							rst.getInt("sector_id"), rst.getString("sector_name"), rst.getBoolean("sector_locked_admin"), rst.getBoolean("sector_locked_superadmin"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), rst.getString("problem_subtype"), rst.getString("grade")));
				}
			}
		}
		Map<Integer, Activity> activityLookup = new HashMap<>();
		for (Activity act : res) {
			for (Integer id : act.getActivityIds()) {
				activityLookup.put(id, act);
			}
		}
		if (!tickIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
					       t.comment description, t.stars, g.grade
					FROM activity a
					JOIN tick t ON a.problem_id=t.problem_id
					LEFT JOIN grade g ON t.grade_id=g.id
					JOIN user u ON t.user_id=u.id AND a.user_id=u.id
					LEFT JOIN media m ON u.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE a.id IN (%s)
					""".formatted(Joiner.on(",").join(tickIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						Activity a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							String name = rst.getString("name");
							String grade = rst.getString("grade");
							if (grade == null) {
								grade = GradeConverter.NO_PERSONAL_GRADE;
							}
							a.setTick(false, userId, name, rst.getString("description"), rst.getInt("stars"), grade);
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!repeatIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
					       r.comment description, t.stars, g.grade
								FROM activity a
								JOIN user u ON a.user_id=u.id
								JOIN tick t ON a.problem_id=t.problem_id AND u.id=t.user_id
								LEFT JOIN grade g ON t.grade_id=g.id
								JOIN tick_repeat r ON a.tick_repeat_id=r.id AND t.id=r.tick_id
								LEFT JOIN media m ON u.media_id=m.id
								LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
								WHERE a.id IN (%s)
								""".formatted(Joiner.on(",").join(repeatIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						Activity a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							String name = rst.getString("name");
							String grade = rst.getString("grade");
							if (grade == null) {
								grade = GradeConverter.NO_PERSONAL_GRADE;
							}
							a.setTick(true, userId, name, rst.getString("description"), rst.getInt("stars"), grade);
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!gbIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       ma.id avatar_media_id, UNIX_TIMESTAMP(ma.updated_at) avatar_version_stamp, mama.focus_x avatar_focus_x, mama.focus_y avatar_focus_y, mama.primary_color_hex avatar_primary_color_hex,
					       g.message,
					       mg.media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
					FROM activity a
					JOIN guestbook g ON a.guestbook_id=g.id
					JOIN user u ON g.user_id=u.id
					LEFT JOIN media ma ON u.media_id=ma.id
					LEFT JOIN media_ml_analysis mama ON ma.id=mama.media_id
					LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id
					LEFT JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0)
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE a.id IN (%s)
					""".formatted(Joiner.on(",").join(gbIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						Activity a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							String name = rst.getString("name");
							a.setGuestbook(userId, name, rst.getString("message"));
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.addMedia(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), false, null);
							}
							int avatarMediaId = rst.getInt("avatar_media_id");
							if (avatarMediaId > 0) {
								long avatarMediaVersionStamp = rst.getLong("avatar_version_stamp");
								int avatarFocusX = rst.getInt("avatar_focus_x");
								int avatarFocusY = rst.getInt("avatar_focus_y");
								String avatarPrimaryColorHex = rst.getString("avatar_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(avatarMediaId, avatarMediaVersionStamp, avatarFocusX, avatarFocusY, avatarPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!faIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id, p.description, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
					FROM activity a
					JOIN problem p ON a.problem_id=p.id
					LEFT JOIN fa ON p.id=fa.problem_id
					LEFT JOIN user u ON fa.user_id=u.id
					LEFT JOIN media m ON u.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE a.id IN (%s)
					""".formatted(Joiner.on(",").join(faIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						Activity a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							String description = rst.getString("description");
							if (description != null) {
								if (a.getDescription() == null) {
									a.setDescription(description);
								}
								else if (!a.getDescription().contains(description)) {
									a.setDescription(a.getDescription() + " (" + description + ")");
								}
							}
							int userId = rst.getInt("user_id");
							String name = rst.getString("name");
							int mediaId = rst.getInt("media_id");
							MediaIdentity mediaIdentity = null;
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
							a.addUser(userId, name, mediaIdentity);
						}
					}
				}
			}
			if (!media) {
				Set<Integer> problemIds = res.stream()
						.filter(a -> a.getActivityIds().stream().anyMatch(faIds::contains))
						.map(Activity::getProblemId)
						.collect(Collectors.toSet());
				if (!problemIds.isEmpty()) {
					try (PreparedStatement ps = c.prepareStatement("""
							SELECT mp.problem_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, 
							       mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.embed_url
							FROM media_problem mp
							JOIN media m ON mp.media_id = m.id
							LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
							WHERE mp.problem_id IN (%s) AND m.deleted_user_id IS NULL
							""".formatted(Joiner.on(",").join(problemIds)))) {
						try (ResultSet rst = ps.executeQuery()) {
							while (rst.next()) {
								int pId = rst.getInt("problem_id");
								var activities = res.stream()
										.filter(act -> act.getProblemId() == pId && act.getActivityIds().stream().anyMatch(faIds::contains))
										.toList();
								for (var a : activities) {
									var mediaIdentity = new MediaIdentity(rst.getInt("media_id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
									a.addMedia(mediaIdentity, rst.getBoolean("is_movie"), rst.getString("embed_url"));
								}
							}
						}
					}
				}
			}
		}
		if (!mediaIds.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT a.id,
						   u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
					       m.is_movie, m.embed_url,
					       COALESCE(m_photographer.id, m_creator.id) photographer_media_id, UNIX_TIMESTAMP(COALESCE(m_photographer.updated_at,m_creator.updated_at)) photographer_version_stamp
					FROM activity a
					JOIN media m ON a.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					JOIN media_problem mp ON m.id=mp.media_id AND a.problem_id=mp.problem_id
					LEFT JOIN user u ON m.photographer_user_id=u.id
					LEFT JOIN media m_photographer ON u.media_id=m_photographer.id
					LEFT JOIN user u_creator ON m.uploader_user_id=u_creator.id
					LEFT JOIN media m_creator ON u_creator.media_id=m_creator.id
					WHERE a.id IN (%s)
					""".formatted(Joiner.on(",").join(mediaIds)))) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						Activity a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							var mediaIdentity = new MediaIdentity(rst.getInt("media_id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
							a.addMedia(mediaIdentity, rst.getBoolean("is_movie"), rst.getString("embed_url"));
							if (a.getUsers() == null || a.getUsers().isEmpty()) {
								// Don't append activity thumbnail if this is a new problem, only show FA users
								a.appendActivityThumbnail(new MediaIdentity(rst.getInt("photographer_media_id"), rst.getLong("photographer_version_stamp"), 0, 0, null), rst.getInt("user_id"), rst.getString("name"));
							}
						}
					}
				}
			}
		}
		logger.debug("getActivity(offset={}) - res.size()={}, duration={}", offset, res.size(), stopwatch);
		return res;
	}

	public List<Administrator> getAdministrators(Connection c, Setup setup) throws SQLException {
		List<Administrator> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
				       CASE WHEN u.email_visible_to_all=1 THEN (SELECT GROUP_CONCAT(DISTINCT e.email ORDER BY e.email SEPARATOR ';') FROM user_email e WHERE e.user_id=u.id AND e.email NOT LIKE '%@missing-email.com') END emails,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       DATE_FORMAT(l_agg.last_login_raw, '%Y.%m.%d') last_login
				FROM (SELECT l.user_id, MAX(l.when) last_login_raw
				      FROM user_login l
				      JOIN user_region ur ON l.user_id=ur.user_id AND l.region_id=ur.region_id
				      WHERE l.region_id=?
				        AND (ur.admin_write=1 OR ur.superadmin_write=1)
				      GROUP BY user_id
				) l_agg
				JOIN user u ON u.id=l_agg.user_id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				ORDER BY name
				""")) {
			ps.setInt(1, setup.idRegion());
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
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long versionStamp = rst.getLong("media_version_stamp");
						int focusX = rst.getInt("media_focus_x");
						int focusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, versionStamp, focusX, focusY, mediaPrimaryColorHex);
					}
					String lastLogin = rst.getString("last_login");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new Administrator(userId, name, emails, mediaIdentity, timeAgo));
				}
			}
		}
		return res;
	}

	public Area getArea(Connection c, Setup s, Optional<Integer> authUserId, int reqId, boolean shouldUpdateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (shouldUpdateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Area a = null;
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
					SELECT ? region_id, ? auth_user_id, ? area_id
				)
				SELECT r.name region_name, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				FROM req
				JOIN area a ON req.area_id=a.id
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN coordinates c ON a.coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				GROUP BY r.name, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.name, a.sun_from_hour, a.sun_to_hour, a.description,
				         c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				""")) {
			ps.setInt(1, s.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
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
					a = new Area(null, regionName, reqId, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, -1, -1, media, triviaMedia, null, externalLinks, pageViews);
				}
			}
		}
		if (a == null) {
			// Area not found, see if it's visible on a different domain
			try {
				Redirect res = getCanonicalUrl(c, reqId, 0, 0);
				if (!Strings.isNullOrEmpty(res.redirectUrl())) {
					return new Area(res.redirectUrl(), null, -1, false, false, false, false, null, null, false, 0, 0, null, null, null, 0, 0, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
				// Not found on other domains either
			}
		}
		if (a == null) {
			throw new NoSuchElementException("Could not find area with id=" + reqId);
		}
		Map<Integer, Area.AreaSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				  SELECT ? auth_user_id, ? area_id
				),
				ranked_media AS (
				  SELECT s.id sector_id,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         ROW_NUMBER() OVER (PARTITION BY p.sector_id ORDER BY m.id DESC) rn
				  FROM req
				  JOIN area a ON req.area_id=a.id
				  JOIN sector s ON a.id=s.area_id
				  JOIN problem p ON s.id=p.sector_id
				  JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
				  JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				  WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				)
				SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual,
				       rm.media_id, rm.media_version_stamp, rm.media_focus_x, rm.media_focus_y, rm.media_primary_color_hex
				FROM req
				JOIN area a ON a.id=req.area_id
				JOIN sector s ON a.id=s.area_id
				LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				LEFT JOIN ranked_media rm ON s.id=rm.sector_id AND rm.rn=1
				WHERE is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				ORDER BY s.sorting, s.name
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
					MediaIdentity mediaIdentity = null;
					int mediaId = rst.getInt("media_id");
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					else {
						boolean inherited = false;
						boolean showHiddenMedia = true; // Show everything to ensure image in area overview
						List<Media> x = getMediaSector(c, s, authUserId, id, 0, inherited, 0, 0, 0, showHiddenMedia);
						if (!x.isEmpty()) {
							mediaIdentity = x.getFirst().identity();
						}
					}
					Area.AreaSector as = a.addSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, wallDirectionCalculated, wallDirectionManual, mediaIdentity);
					sectorLookup.put(id, as);
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Add problems
			var sectorProblems = getSectorProblems(c, s, authUserId, reqId, 0);
			for (int sectorId : sectorProblems.keySet()) {
				sectorLookup.get(sectorId).getProblems().addAll(sectorProblems.get(sectorId));
			}
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(c, sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idSectorOutline.get(idSector));
				sectorLookup.get(idSector).setOutline(outline);
			}
			// Fill sector approaches
			getSectorSlopes(c, true, sectorLookup.keySet()).entrySet().forEach(e -> sectorLookup.get(e.getKey().intValue()).setApproach(e.getValue()));			
			// Fill sector descents
			getSectorSlopes(c, false, sectorLookup.keySet()).entrySet().forEach(e -> sectorLookup.get(e.getKey().intValue()).setDescent(e.getValue()));
			// Add simplified grade counts
			loadSimplifiedGradeCounts(c, authUserId, reqId, sectorLookup);
		}
		a.orderSectors();
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return a;
	}

	public Collection<Area> getAreaList(Connection c, Optional<Integer> authUserId, int reqIdRegion) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Area> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT r.name region_name,
				       a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				       COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits
				FROM area a
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN coordinates c ON a.coordinates_id=c.id
				LEFT JOIN sector s ON a.id=s.area_id
				LEFT JOIN problem p ON s.id=p.sector_id
				LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				GROUP BY r.name, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				         c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				ORDER BY r.name, replace(replace(replace(lower(a.name),'æ','zx'),'ø','zy'),'å','zz')
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
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
					res.add(new Area(null, regionName, id, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, numSectors, numProblems, null, null, null, null, pageViews));
				}
			}
		}
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public synchronized Optional<Integer> getAuthUserId(Connection c, Auth0Profile profile) throws SQLException {
		Optional<Integer> authUserId = Optional.empty();
		boolean hasAvatar = false;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT e.user_id, CASE WHEN m.id IS NOT NULL THEN 1 ELSE 0 END has_avatar
				FROM user_email e
				JOIN user u ON e.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				WHERE lower(e.email)=?
				""")) {
			ps.setString(1, profile.email().toLowerCase());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					authUserId = Optional.of(rst.getInt("user_id"));
					hasAvatar = rst.getBoolean("has_avatar");
				}
			}
		}
		if (authUserId.isEmpty()) {
			authUserId = Optional.of(addUser(c, profile.email(), profile.firstname(), profile.lastname()));
		}
		final Optional<Integer> finalUserId = authUserId;
		if (!hasAvatar && profile.picture() != null) {
			try {
				saveUserAvatar(c, finalUserId, () -> {
					try {
						return URI.create(profile.picture()).toURL().openStream();
					} catch (java.io.IOException e) {
						logger.error(e.getMessage(), e);
						throw new java.io.UncheckedIOException(e);
					}
				});
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
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
		if (res == null) {
			throw new NoSuchElementException("Could not find canonical url for idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		}
		return res;
	}

	public Collection<GradeDistribution> getContentGraph(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN region_type rt ON req.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				),
				x AS (
				  SELECT g.label_major g_base, r.id region_id, r.name region, COALESCE(ty.subtype,'Boulder') t, COUNT(p.id) num
				  FROM req
				  JOIN region r ON 1=1
				  JOIN region_type rt ON r.id = rt.region_id
				  JOIN area a ON r.id = a.region_id
				  JOIN sector s ON a.id = s.area_id
				  JOIN problem p ON s.id = p.sector_id
				  JOIN type ty ON p.type_id = ty.id
				  JOIN grade g ON p.consensus_grade_id = g.id
				  JOIN target_systems ts ON g.grade_system_id = ts.grade_system_id
				  LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
				  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				    AND (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				  GROUP BY r.id, r.name, g.label_major, ty.subtype
				)
				SELECT g.label_major grade, clr.hex_code color, x.region_id, x.region, x.t, COALESCE(x.num, 0) num
				FROM req
				JOIN target_systems ts ON 1=1
				JOIN (
				  SELECT label_major, grade_system_id, grade_color_id, MIN(weight) sort 
				  FROM grade GROUP BY label_major, grade_system_id, grade_color_id
				) g ON g.grade_system_id = ts.grade_system_id
				JOIN grade_color clr ON g.grade_color_id = clr.id
				LEFT JOIN x ON g.label_major = x.g_base
				ORDER BY g.sort, x.region, x.t
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String label = rst.getString("grade");
					String color = rst.getString("color");
					GradeDistribution dist = res.computeIfAbsent(label, k -> new GradeDistribution(k, color));
					int regionId = rst.getInt("region_id");
					if (!rst.wasNull()) {
						dist.addSector(regionId, rst.getString("region"), rst.getString("t"), rst.getInt("num"));
					}
				}
			}
		}
		return res.values();
	}

	public Collection<DangerousArea> getDangerous(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Map<Integer, DangerousArea> areasLookup = new LinkedHashMap<>();
		Map<Integer, DangerousSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour,
				       s.id sector_id, s.name sector_name, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour,
				       p.id problem_id, p.broken problem_broken, p.nr problem_nr, gr.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, DATE_FORMAT(g.post_time,'%Y.%m.%d') post_time, g.message
				FROM area a
				JOIN region_type rt ON a.region_id=rt.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN grade gr ON p.grade_id=gr.id
				JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)
				JOIN user u ON g.user_id=u.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour,
				         s.id, s.name, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, s.sun_from_hour, s.sun_to_hour,
				         p.id, p.broken, p.nr, gr.grade, p.name, p.locked_admin, p.locked_superadmin,
				         u.firstname, u.lastname, g.post_time, g.message
				ORDER BY a.name, s.name, p.nr
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					DangerousArea a = areasLookup.get(areaId);
					if (a == null) {
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new DangerousArea(areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour, new ArrayList<>());
						areasLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					DangerousSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorName = rst.getString("sector_name");
						CompassDirection sectorWallDirectionCalculated = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
						int sectorSunToHour = rst.getInt("sector_sun_to_hour");
						s = new DangerousSector(sectorId, sectorName, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin, sectorSunFromHour, sectorSunToHour, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("problem_id");
					String broken = rst.getString("problem_broken");
					int nr = rst.getInt("problem_nr");
					String grade = rst.getString("problem_grade");
					boolean lockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String name = rst.getString("problem_name");
					String postBy = rst.getString("name");
					String postWhen = rst.getString("post_time");
					String postTxt = rst.getString("message");
					s.problems().add(new DangerousProblem(id, broken, lockedAdmin, lockedSuperadmin, nr, name, grade, postBy, postWhen, postTxt));
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

	public List<FrontpageFirstAscent> getFrontpageActivityFirstAscents(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		final List<FrontpageFirstAscent> res = new ArrayList<>();
		String sqlStr = """
				WITH x AS (
				    SELECT p1.id AS problem_id, p1.fa_date AS activity_timestamp
				    FROM problem p1
				    JOIN sector s1 ON p1.sector_id = s1.id
				    JOIN area ar1 ON s1.area_id = ar1.id
				    WHERE p1.fa_date IS NOT NULL
				      AND ar1.region_id IN (
				        SELECT r.id FROM region r 
				        JOIN region_type rt ON r.id = rt.region_id 
				        LEFT JOIN user_region ur ON (r.id = ur.region_id AND ur.user_id = ?)
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = ?)
				          AND (r.id = ? OR ur.user_id IS NOT NULL)
				    )
				    ORDER BY p1.fa_date DESC, p1.id DESC LIMIT 8
				)
				SELECT 
				    x.activity_timestamp, a.id area_id, a.name area_name,
				    p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, ty.subtype problem_subtype,
				    g.grade grade,
				    GROUP_CONCAT(DISTINCT concat(u.id,':',u.firstname,' ',COALESCE(u.lastname,''),':',COALESCE(m.id,0),':',COALESCE(UNIX_TIMESTAMP(m.updated_at),0),':',COALESCE(mma.focus_x,0),':',COALESCE(mma.focus_y,0)) SEPARATOR '|') user_data
				FROM x
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				LEFT JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN fa ON p.id = fa.problem_id
				LEFT JOIN user u ON fa.user_id = u.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = ?)
				WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY x.activity_timestamp, a.id, a.name, p.id, p.name, p.locked_admin, p.locked_superadmin, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, p.id DESC
				         """;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 1;
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, setup.idRegion());
			ps.setInt(ix++, setup.idRegion());
			ps.setInt(ix++, authUserId.orElse(0));
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					LocalDateTime ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					String grade = rst.getString("grade");
					List<User> users = new ArrayList<>();
					String rawUsers = rst.getString("user_data");
					if (rawUsers != null) {
						for (String userRecord : rawUsers.split("\\|")) {
							String[] p = userRecord.split(":");
							int mediaId = Integer.parseInt(p[2]);
							MediaIdentity mi = mediaId > 0 ? new MediaIdentity(mediaId, Long.parseLong(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]), p.length > 6 ? p[6] : null) : null;
							users.add(new User(Integer.parseInt(p[0]), p[1], mi));
						}
					}
					res.add(new FrontpageFirstAscent(TimeAgo.getTimeAgo(ts.toLocalDate()), 
							rst.getInt("area_id"), rst.getString("area_name"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), rst.getString("problem_subtype"), grade, users));
				}
			}
		}
		return res;
	}

	public List<FrontpageLastComment> getFrontpageLastComments(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		final List<FrontpageLastComment> res = new ArrayList<>();
		String sqlStr = """
				SELECT g.post_time AS activity_timestamp, a.id area_id, a.name area_name,
				       p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       g.message, u.id user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) user_name,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM guestbook g
				JOIN user u ON g.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN problem p ON g.problem_id=p.id
				JOIN sector s ON p.sector_id=s.id 
				JOIN area a ON s.area_id=a.id 
				LEFT JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)
				WHERE a.region_id IN (
				    SELECT r.id FROM region r 
				    JOIN region_type rt ON r.id=rt.region_id 
				    LEFT JOIN user_region ur2 ON (r.id=ur2.region_id AND ur2.user_id=?)
				    WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				      AND (r.id=? OR ur2.user_id IS NOT NULL)
				)
				AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				ORDER BY g.id DESC LIMIT 4
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 1;
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, setup.idRegion());
			ps.setInt(ix++, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					LocalDateTime ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					int mediaId = rst.getInt("media_id");
					MediaIdentity mi = mediaId > 0 ? new MediaIdentity(mediaId, rst.getLong("media_version_stamp"), rst.getInt("media_focus_x"), rst.getInt("media_focus_y"), rst.getString("media_primary_color_hex")) : null;
					User user = new User(rst.getInt("user_id"), rst.getString("user_name"), mi);
					res.add(new FrontpageLastComment(TimeAgo.getTimeAgo(ts.toLocalDate()), rst.getInt("area_id"), rst.getString("area_name"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), user, rst.getString("message")));
				}
			}
		}
		return res;
	}

	public List<FrontpageRecentAscent> getFrontpageNewestAscents(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		final List<FrontpageRecentAscent> res = new ArrayList<>();
		String sqlStr = """
				SELECT t.date AS activity_timestamp, t.id AS tick_id, a.id area_id, a.name area_name,
				       p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, ty.subtype problem_subtype,
				       g.grade tick_grade, u.id user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) user_name,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM tick t
				JOIN problem p ON t.problem_id=p.id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN grade g ON t.grade_id=g.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN type ty ON p.type_id=ty.id 
				JOIN sector s ON p.sector_id=s.id 
				JOIN area a ON s.area_id=a.id 
				LEFT JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)
				WHERE a.region_id IN (
				    SELECT r.id FROM region r 
				    JOIN region_type rt ON r.id=rt.region_id 
				    LEFT JOIN user_region ur2 ON (r.id=ur2.region_id AND ur2.user_id=?)
				    WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				      AND (r.id=? OR ur2.user_id IS NOT NULL)
				)
				AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				ORDER BY t.date DESC, t.id DESC LIMIT 8
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 1;
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, setup.idRegion());
			ps.setInt(ix++, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					LocalDateTime ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					String tickGrade = rst.getString("tick_grade");
					if (tickGrade == null) {
						tickGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					boolean repeat = false; 
					int mediaId = rst.getInt("media_id");
					MediaIdentity mi = mediaId > 0 ? new MediaIdentity(mediaId, rst.getLong("media_version_stamp"), rst.getInt("media_focus_x"), rst.getInt("media_focus_y"), rst.getString("media_primary_color_hex")) : null;
					User user = new User(rst.getInt("user_id"), rst.getString("user_name"), mi);
					res.add(new FrontpageRecentAscent(TimeAgo.getTimeAgo(ts.toLocalDate()),  rst.getInt("area_id"), rst.getString("area_name"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), rst.getString("problem_subtype"), tickGrade, user, repeat));
				}
			}
		}
		return res;
	}

	public List<FrontpageNewestMedia> getFrontpageNewestMedia(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		final List<FrontpageNewestMedia> res = new ArrayList<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id
				),
				m_list AS (
				  SELECT m.id AS media_id, mp.problem_id
				  FROM req, media m
				  JOIN media_problem mp ON m.id = mp.media_id AND mp.trivia = 0
				  JOIN problem p1 ON mp.problem_id = p1.id
				  JOIN sector s1 ON p1.sector_id = s1.id
				  JOIN area ar1 ON s1.area_id = ar1.id
				  WHERE m.deleted_user_id IS NULL
				    AND ar1.region_id IN (
				        SELECT r.id FROM region r 
				        JOIN region_type rt ON r.id = rt.region_id 
				        LEFT JOIN user_region ur ON (r.id = ur.region_id AND ur.user_id = req.auth_user_id)
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				          AND (r.id = req.region_id OR ur.user_id IS NOT NULL)
				    )
				  ORDER BY m.id DESC
				  LIMIT 12
				)
				SELECT 
				    ml.media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie,
				    p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				    g.grade
				FROM m_list ml
				JOIN req ON 1=1
				JOIN media m ON ml.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN problem p ON ml.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				LEFT JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				ORDER BY ml.media_id DESC
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 1;
			ps.setInt(ix++, authUserId.orElse(0));
			ps.setInt(ix++, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					MediaIdentity mi = new MediaIdentity(rst.getInt("media_id"), rst.getLong("media_version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					res.add(new FrontpageNewestMedia(mi, rst.getBoolean("is_movie"), rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), rst.getString("problem_name"), rst.getString("grade")));
				}
			}
		}
		return res;
	}

	public List<FrontpageRandomMedia> getFrontpageRandomMedia(Connection c, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<FrontpageRandomMedia> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				WITH random_id AS (
				    SELECT id FROM (
				        SELECT m_sub.id, 
				               AVG(t_sub.stars) as sub_avg,
				               mma_sub.is_action_shot,
				               ROW_NUMBER() OVER (ORDER BY 
				                   IFNULL(mma_sub.is_action_shot, 0) DESC, 
				                   (AVG(t_sub.stars) >= 2) DESC, 
				                   RAND()
				               ) as random_rank
				        FROM media m_sub
				        JOIN media_problem mp_sub ON m_sub.id=mp_sub.media_id
				        JOIN problem p_sub ON mp_sub.problem_id=p_sub.id
				        JOIN sector s_sub ON p_sub.sector_id=s_sub.id
				        JOIN area a_sub ON s_sub.area_id=a_sub.id
				        JOIN region r_sub ON a_sub.region_id=r_sub.id
				        LEFT JOIN tick t_sub ON p_sub.id = t_sub.problem_id
				        LEFT JOIN media_ml_analysis mma_sub ON m_sub.id = mma_sub.media_id
				        WHERE r_sub.id=?
				          AND m_sub.deleted_user_id IS NULL
				          AND a_sub.trash IS NULL AND s_sub.trash IS NULL AND p_sub.trash IS NULL
				          AND a_sub.access_closed IS NULL AND s_sub.access_closed IS NULL
				          AND m_sub.is_movie=0
				          AND mp_sub.trivia=0
				          AND p_sub.locked_admin=0 AND p_sub.locked_superadmin=0
				          AND s_sub.locked_admin=0 AND s_sub.locked_superadmin=0
				          AND a_sub.locked_admin=0 AND a_sub.locked_superadmin=0
				        GROUP BY m_sub.id
				    ) ranked_pool
				    WHERE (sub_avg>=2 AND is_action_shot=1) OR random_rank<=500
				    ORDER BY RAND()
				    LIMIT 20
				)
				SELECT m.id id_media, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.width, m.height, 
				       a.id id_area, a.name area, 
				       s.id id_sector, s.name sector, 
				       p.id id_problem, p.name problem,
				       g.grade grade,

				       IF(u.id IS NULL, NULL, 
				          CONCAT('{"id":', u.id, 
				                 ',"name":"', REPLACE(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))), '"', '\\"'), 
				                 '",', IF(ma.id IS NULL, '"mediaIdentity":null', 
				                          CONCAT('"mediaIdentity":{"id":', ma.id, 
				                                 ',"versionStamp":', COALESCE(UNIX_TIMESTAMP(ma.updated_at), 0), 
				                                 ',"focusX":', COALESCE(mma_u.focus_x, 0), 
				                                 ',"focusY":', COALESCE(mma_u.focus_y, 0), 
				                                 ',"primaryColorHex":"', COALESCE(mma_u.primary_color_hex, ''), '"}')
				                       ), '}')
				       ) photographer,

				       GROUP_CONCAT(DISTINCT 
				           IF(u2.id IS NULL, NULL, 
				              CONCAT('{"id":', u2.id, 
				                     ',"name":"', REPLACE(TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname, ''))), '"', '\\"'), 
				                     '",', IF(ma2.id IS NULL, '"mediaIdentity":null',
				                              CONCAT('"mediaIdentity":{"id":', ma2.id, 
				                                     ',"versionStamp":', COALESCE(UNIX_TIMESTAMP(ma2.updated_at), 0), 
				                                     ',"focusX":', COALESCE(mma_u2.focus_x, 0), 
				                                     ',"focusY":', COALESCE(mma_u2.focus_y, 0), 
				                                     ',"primaryColorHex":"', COALESCE(mma_u2.primary_color_hex, ''), '"}')
				                           ), '}')
				           ) SEPARATOR ', '
				       ) tagged
				FROM random_id rid
				JOIN media m ON rid.id = m.id
				JOIN media_problem mp ON m.id = mp.media_id AND mp.trivia = 0
				JOIN problem p ON mp.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				LEFT JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user u ON m.photographer_user_id = u.id AND u.id != 1049 
				LEFT JOIN media ma ON u.media_id = ma.id
				LEFT JOIN media_ml_analysis mma_u ON ma.id = mma_u.media_id
				LEFT JOIN media_user mu ON m.id = mu.media_id AND mu.user_id != 1049 
				LEFT JOIN user u2 ON mu.user_id = u2.id
				LEFT JOIN media ma2 ON u2.media_id = ma2.id
				LEFT JOIN media_ml_analysis mma_u2 ON ma2.id = mma_u2.media_id
				GROUP BY m.id, m.updated_at, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname, u.id, ma.id, ma.updated_at,
				         mma.focus_x, mma.focus_y, mma.primary_color_hex, mma_u.focus_x, mma_u.focus_y, g.grade
				         """)) {
			ps.setInt(1, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id_media");
					long versionStamp = rst.getLong("version_stamp");
					int focusX = rst.getInt("focus_x");
					int focusY = rst.getInt("focus_y");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int idArea = rst.getInt("id_area");
					String area = rst.getString("area");
					int idSector = rst.getInt("id_sector");
					String sector = rst.getString("sector");
					int idProblem = rst.getInt("id_problem");
					String problem = rst.getString("problem");
					String grade = rst.getString("grade");
					String photographerJson = rst.getString("photographer");
					String taggedJson = rst.getString("tagged");
					String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
					MediaIdentity identity = new MediaIdentity(idMedia, versionStamp, focusX, focusY, mediaPrimaryColorHex);
					User photographer = photographerJson == null? null : gson.fromJson(photographerJson, User.class);
					List<User> tagged = taggedJson == null? null : gson.fromJson("[" + taggedJson + "]", new TypeToken<List<User>>(){});
					res.add(new FrontpageRandomMedia(identity, width, height, idArea, area, idSector, sector, idProblem, problem, grade, photographer, tagged));
				}
			}
		}
		logger.debug("getFrontpageRandomMedia(setup={}) - res.size()={}, duration={}", setup, res.size(), stopwatch);
		return res;
	}

	public FrontpageStats getFrontpageStats(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		FrontpageStats res = null;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT COUNT(DISTINCT a.id) areas,
				       COUNT(DISTINCT p.id) problems,
				       COUNT(DISTINCT t.id) ticks
				FROM region_type rt
				JOIN region r ON rt.region_id=r.id
				LEFT JOIN area a ON r.id=a.region_id AND a.trash IS NULL
				LEFT JOIN sector s ON a.id=s.area_id AND s.trash IS NULL
				LEFT JOIN problem p ON s.id=p.sector_id AND p.trash IS NULL
				LEFT JOIN tick t ON p.id=t.problem_id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?
				WHERE rt.type_id IN (SELECT x.type_id FROM region_type x WHERE x.region_id=?)
				  AND (a.region_id=? OR ur.user_id IS NOT NULL)
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areas = rst.getInt("areas");
					int problems = rst.getInt("problems");
					int ticks = rst.getInt("ticks");
					res = new FrontpageStats(areas, problems, ticks);
				}
			}
		}
		logger.debug("getFrontpageStats(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public Collection<GradeDistribution> getGradeDistribution(Connection c, Optional<Integer> authUserId, int optionalAreaId, int optionalSectorId) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? sector_id, ? area_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN area a ON a.id = COALESCE(NULLIF(req.area_id, 0), (SELECT area_id FROM sector WHERE id = req.sector_id))
				  JOIN region_type rt ON a.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				),
				x AS (
				  SELECT g.label_major g_base, s.sorting, s.id sector_id, s.name sector, COALESCE(ty.subtype,'Boulder') t, COUNT(p.id) num
				  FROM req
				  JOIN sector s ON (CASE WHEN req.sector_id != 0 THEN s.id = req.sector_id ELSE s.area_id = req.area_id END)
				  JOIN area a ON s.area_id = a.id
				  JOIN problem p ON s.id = p.sector_id 
				  JOIN type ty ON p.type_id = ty.id 
				  JOIN grade g ON p.consensus_grade_id = g.id
				  JOIN target_systems ts ON g.grade_system_id = ts.grade_system_id
				  LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id 
				  WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				  GROUP BY s.sorting, s.id, s.name, g.label_major, ty.subtype
				)
				SELECT g.label_major grade, clr.hex_code color, x.sector_id, x.sector, x.t, COALESCE(x.num, 0) num
				FROM req
				JOIN target_systems ts ON 1=1
				JOIN (
				  SELECT label_major, grade_system_id, grade_color_id, MIN(weight) sort 
				  FROM grade GROUP BY label_major, grade_system_id, grade_color_id
				) g ON g.grade_system_id = ts.grade_system_id
				JOIN grade_color clr ON g.grade_color_id = clr.id
				LEFT JOIN x ON g.label_major = x.g_base
				ORDER BY g.sort, x.sorting, x.sector, x.t
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, optionalSectorId);
			ps.setInt(3, optionalAreaId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String label = rst.getString("grade");
					String color = rst.getString("color");
					GradeDistribution dist = res.computeIfAbsent(label, k -> new GradeDistribution(k, color));
					int sectorId = rst.getInt("sector_id");
					if (!rst.wasNull()) {
						dist.addSector(sectorId, rst.getString("sector"), rst.getString("t"), rst.getInt("num"));
					}
				}
			}
		}
		return res.values();
	}

	public Media getMedia(Connection c, Optional<Integer> authUserId, int id) throws SQLException {
		String sql = """
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, m.width, m.height, m.is_movie, m.embed_url,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 
				       TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
				       GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged
				FROM media m
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN user c ON m.photographer_user_id = c.id
				LEFT JOIN media_user mu ON m.id = mu.media_id
				LEFT JOIN user u ON mu.user_id = u.id
				WHERE m.id = ?
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname
				""";
		Media res = null;
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				if (rst.next()) {
					MediaIdentity identity = new MediaIdentity(
							rst.getInt("id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), 
							rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
							);
					MediaMetadata metadata = MediaMetadata.from(
							rst.getString("date_created"), rst.getString("date_taken"), 
							rst.getString("capturer"), rst.getString("tagged"), 
							rst.getString("description"), null
							);
					res = new Media(
							identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), 
							0, false, rst.getInt("width"), rst.getInt("height"), 
							rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, 
									metadata, rst.getString("embed_url"), false, 0, 0, 0, null
							);
				}
			}
		}
		if (res == null) {
			throw new NoSuchElementException("Could not find media with id=" + id);
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, List.of(id));
		return res.withMediaSvgs(svgMap.getOrDefault(id, List.of()));
	}

	public List<PermissionUser> getPermissions(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		ensureSuperadminWriteRegion(c, setup, authUserId);
		List<PermissionUser> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT x.id, x.name,
				       x.media_id, x.media_version_stamp, x.media_focus_x, x.media_focus_y, x.media_primary_color_hex,
				       DATE_FORMAT(MAX(x.last_login),'%Y.%m.%d') last_login, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write
				FROM (
				  SELECT u.id, 
				         TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) name,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         l_agg.last_login,
				         ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write
				  FROM (SELECT user_id, MAX(`when`) last_login
				        FROM user_login
				        WHERE region_id=?
				        GROUP BY user_id
				  ) l_agg
				  JOIN user u ON u.id=l_agg.user_id
				  LEFT JOIN media m ON u.media_id=m.id
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  LEFT JOIN user_region ur ON u.id=ur.user_id AND ur.region_id=?

				  UNION

				  SELECT u.id, 
				         TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) name,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         l_agg.last_login,
				         ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write
				  FROM user_region ur
				  JOIN user u ON ur.user_id=u.id
				  LEFT JOIN media m ON u.media_id=m.id
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  JOIN (SELECT user_id, MAX(`when`) last_login
				        FROM user_login
				        WHERE region_id=?
				        GROUP BY user_id
				  ) l_agg ON u.id=l_agg.user_id
				  WHERE ur.region_id=?
				) x
				GROUP BY x.id, x.name, x.media_id, x.media_version_stamp, x.media_focus_x, x.media_focus_y, x.media_primary_color_hex, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write
				ORDER BY COALESCE(x.superadmin_write,0) DESC, COALESCE(x.superadmin_read,0) DESC, COALESCE(x.admin_write,0) DESC, COALESCE(x.admin_read,0) DESC, x.name
				         """)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			ps.setInt(4, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					String lastLogin = rst.getString("last_login");
					boolean adminRead = rst.getBoolean("admin_read");
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superadminRead = rst.getBoolean("superadmin_read");
					boolean superadminWrite = rst.getBoolean("superadmin_write");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new PermissionUser(userId, name, mediaIdentity, timeAgo, adminRead, adminWrite, superadminRead, superadminWrite, authUserId.orElse(0)==userId));
				}
			}
		}
		return res;
	}

	public Problem getProblem(Connection c, Optional<Integer> authUserId, Setup s, int reqId, boolean showHiddenMedia, boolean shouldUpdateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (shouldUpdateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		List<Integer> todoIdProblems = new ArrayList<>();
		if (authUserId.isPresent()) {
			ProfileTodo todo = getProfileTodo(c, authUserId, s, authUserId.orElseThrow());
			if (todo != null) {
				for (ProfileTodoArea ta : todo.areas()) {
					for (ProfileTodoSector ts : ta.sectors()) {
						for (ProfileTodoProblem tp : ts.problems()) {
							todoIdProblems.add(tp.id());
						}
					}
				}
			}
		}
		Problem p = null;
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id, ? problem_id
				),
				stars_count AS (
				    SELECT 
				        p_sub.id AS pid,
				        COUNT(DISTINCT t_sub.id) AS num_ticks,
				        ROUND(ROUND(AVG(NULLIF(t_sub.stars, -1)) * 2) / 2, 1) AS stars
				    FROM req
				    JOIN problem p_sub ON p_sub.id = req.problem_id
				    LEFT JOIN tick t_sub ON p_sub.id = t_sub.problem_id
				    GROUP BY p_sub.id
				)
				SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, 
				       s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, s.access_info sector_access_info, s.access_closed sector_access_closed, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour, 
				       sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, 
				       s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, 
				       p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,
				       gf.grade grade, go.grade original_grade,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				       GROUP_CONCAT(DISTINCT 
				           IF(u.id IS NULL, NULL, 
				              CONCAT('{"id":', u.id, 
				                     ',"name":"', REPLACE(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '"', '\\"'), 
				                     '",', IF(m.id IS NULL, '"mediaIdentity":null', 
				                              CONCAT('"mediaIdentity":{"id":', m.id, 
				                                     ',"versionStamp":', COALESCE(UNIX_TIMESTAMP(m.updated_at), 0), 
				                                     ',"focusX":', COALESCE(mma.focus_x, 0), 
				                                     ',"focusY":', COALESCE(mma.focus_y, 0), '}')
				                           ), '}')
				           ) ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,
				       p.length_meter,
				       sc_data.num_ticks, sc_data.stars,
				       MAX(CASE WHEN (t.user_id = req.auth_user_id OR u.id = req.auth_user_id) THEN 1 END) ticked, 
				       ty.id type_id, ty.type, ty.subtype,
				       p.trivia, p.starting_altitude, p.aspect, p.descent
				FROM req
				JOIN problem p ON p.id = req.problem_id
				JOIN stars_count sc_data ON p.id = sc_data.pid
				JOIN type ty ON p.type_id = ty.id
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				JOIN region r ON a.region_id = r.id
				JOIN region_type rt ON r.id = rt.region_id
				LEFT JOIN grade gf ON p.consensus_grade_id = gf.id
				LEFT JOIN grade go ON p.grade_id = go.id
				LEFT JOIN coordinates sc ON s.parking_coordinates_id = sc.id
				LEFT JOIN coordinates c ON p.coordinates_id = c.id
				LEFT JOIN fa f ON p.id = f.problem_id
				LEFT JOIN user u ON f.user_id = u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN tick t ON p.id=t.problem_id
				LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY a.id, s.id, p.id, sc.id, c.id, ty.id, gf.grade, go.grade, sc_data.num_ticks, sc_data.stars
				ORDER BY p.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, s.idRegion());
			ps.setInt(3, reqId);
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
					int id = rst.getInt("id");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					String grade = rst.getString("grade");
					String originalGrade = rst.getString("original_grade");
					String faDate = rst.getString("fa_date");
					String faDateHr = rst.getString("fa_date_hr");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String faStr = rst.getString("fa");
					List<User> fa = Strings.isNullOrEmpty(faStr) ? null : gson.fromJson("[" + faStr + "]", new TypeToken<List<User>>(){});
					int lengthMeter = rst.getInt("length_meter");
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
					String descent = rst.getString("descent");

					SectorProblem neighbourPrev = null;
					SectorProblem neighbourNext = null;
					List<SectorProblem> problems = Lists.newArrayList(getSectorProblems(c, s, authUserId, 0, sectorId).get(sectorId));
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
							id, broken, false, lockedAdmin, lockedSuperadmin, nr, name, rock, comment,
							grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates,
							media, numTicks, stars, ticked, null, t, todoIdProblems.contains(id), externalLinks, pageViews,
							trivia, triviaMedia, startingAltitude, aspect, descent);
				}
			}
		}
		if (p == null) {
			// Poblem not found, see if it's visible on a different domain
			try {
				Redirect res = getCanonicalUrl(c, 0, 0, reqId);
				if (!Strings.isNullOrEmpty(res.redirectUrl())) {
					return new Problem(res.redirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, 0, 0, null, null, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, 0, null, null, 0, 0, false, null, null, false, null, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
				// Not found on other domains either
			}
		}

		if (p == null) {
			throw new NoSuchElementException("Could not find problem with id=" + reqId);
		}
		// Ascents
		Map<Integer, ProblemTick> tickLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT t.id id_tick, u.id id_user,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       CAST(t.date AS char) date, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, t.comment, t.stars, g.grade
				FROM tick t
				LEFT JOIN grade g ON t.grade_id=g.id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE t.problem_id=?
				ORDER BY t.date DESC, t.id DESC
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id_tick");
					int idUser = rst.getInt("id_user");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					String date = rst.getString("date");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					double stars = rst.getDouble("stars");
					String grade = rst.getString("grade");
					boolean noPersonalGrade = grade == null;
					boolean writable = idUser == authUserId.orElse(0);
					ProblemTick t = p.addTick(id, idUser, mediaIdentity, date, name, grade, noPersonalGrade, comment, stars, writable);
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
				SELECT u.id,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name
				FROM todo t
				JOIN user u ON t.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE t.problem_id=?
				ORDER BY u.firstname, u.lastname
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idUser = rst.getInt("id");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					String name = rst.getString("name");
					p.addTodo(idUser, mediaIdentity, name);
				}
			}
		}
		// Comments
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT g.id, CAST(g.post_time AS char) date, u.id user_id,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, g.message, g.danger, g.resolved
				FROM guestbook g
				JOIN user u ON g.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE g.problem_id=?
				ORDER BY g.post_time DESC
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String date = rst.getString("date");
					int idUser = rst.getInt("user_id");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					String name = rst.getString("name");
					String message = rst.getString("message");
					boolean danger = rst.getBoolean("danger");
					boolean resolved = rst.getBoolean("resolved");
					List<Media> media = getMediaGuestbook(c, authUserId, id);
					p.addComment(id, date, idUser, mediaIdentity, name, message, danger, resolved, media);
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
				SELECT ps.id, ps.nr, ps.description, g.grade
				FROM problem_section ps
				JOIN grade g ON ps.grade_id=g.id
				WHERE ps.problem_id=?
				ORDER BY ps.nr
				""")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					String grade = rst.getString("grade");
					List<Media> sectionMedia = null;
					if (p.getMedia() != null) {
						sectionMedia = p.getMedia()
								.stream()
								.filter(x -> x.pitch() == nr)
								.toList();
						p.getMedia().removeAll(sectionMedia);
					}
					p.addSection(id, nr, description, grade, sectionMedia);
				}
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT DATE_FORMAT(a.aid_date,'%Y-%m-%d') aid_date, DATE_FORMAT(a.aid_date,'%d/%m-%y') aid_date_hr, a.aid_description, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
					FROM fa_aid a
					LEFT JOIN fa_aid_user au ON a.problem_id=au.problem_id
					LEFT JOIN user u ON au.user_id=u.id
					LEFT JOIN media m ON u.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
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
							int mediaId = rst.getInt("media_id");
							MediaIdentity mediaIdentity = null;
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
							}
							User user = User.from(userId, userName, mediaIdentity);
							faAid.users().add(user);
						}
					}
				}
			}
		}
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}) - duration={} - p={}", authUserId, s.idRegion(), reqId, stopwatch, p);
		return p;
	}

	public List<ProfileAscent> getProfileAscents(Connection c, Optional<Integer> authUserId, Setup setup, int reqId) throws SQLException {
		List<ProfileAscent> res = new ArrayList<>();
		Map<Integer, ProfileAscent> idProblemTickMap = new HashMap<>();
		// Tick
		String sqlStr = """
				SELECT r.name region_name,
				       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id id_tick, 0 id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches,
				       p.id id_problem, p.locked_admin, p.locked_superadmin, p.name,
				       CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment,
				       DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date,
				       DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr,
				       CASE WHEN t.id IS NULL THEN -1 ELSE t.stars END stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa,
				       (CASE WHEN t.id IS NOT NULL AND gt.grade IS NOT NULL THEN gt.weight ELSE g.weight END) grade_weight,
				       (CASE WHEN t.id IS NOT NULL AND gt.grade IS NOT NULL THEN gt.grade ELSE g.grade END) grade,
				       CASE WHEN t.id IS NOT NULL AND gt.id IS NULL THEN 1 ELSE 0 END no_personal_grade
				FROM problem p
				JOIN grade g ON p.grade_id=g.id
				JOIN type ty ON p.type_id=ty.id
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN problem_section ps ON p.id=ps.problem_id
				LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?
				LEFT JOIN grade gt ON t.grade_id=gt.id
				LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)
				WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL)
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, g.grade, gt.grade
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					int areaId = rst.getInt("area_id");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
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
					int gradeWeight = rst.getInt("grade_weight");
					String grade = rst.getString("grade");
					boolean noPersonalGrade = rst.getBoolean("no_personal_grade");
					ProfileAscent tick = new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, grade, gradeWeight, noPersonalGrade);
					res.add(tick);
					idProblemTickMap.put(idProblem, tick);
				}
			}
		}
		// Tick_repeat
		sqlStr = """
				SELECT r.name region_name,
				       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id id_tick, tr.id id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches,
				       p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, tr.comment,
				       DATE_FORMAT(tr.date,'%Y-%m-%d') date, DATE_FORMAT(tr.date,'%d/%m-%y') date_hr, t.stars, 0 fa, g.weight grade_weight, g.grade
				FROM problem p
				JOIN type ty ON p.type_id=ty.id
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				JOIN tick t ON p.id=t.problem_id AND t.user_id=?
				LEFT JOIN grade g ON t.grade_id=g.id
				JOIN tick_repeat tr ON t.id=tr.tick_id
				LEFT JOIN problem_section ps ON p.id=ps.problem_id
				LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY s.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, tr.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, tr.comment, tr.date, t.stars, g.weight, g.grade
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					int areaId = rst.getInt("area_id");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
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
					int gradeWeight = rst.getInt("grade_weight");
					String grade = rst.getString("grade");
					boolean noPersonalGrade = grade == null;
					res.add(new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, grade, gradeWeight, noPersonalGrade));
				}
			}
		}
		// First aid ascent
		if (!setup.isBouldering()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT r.name region_name,
					       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
					       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, COUNT(DISTINCT ps.id) num_pitches,
					       p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description,
					       DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr
					FROM problem p
					JOIN sector s ON p.sector_id=s.id
					JOIN area a ON s.area_id=a.id
					JOIN region r ON a.region_id=r.id
					JOIN region_type rt ON r.id=rt.region_id
					JOIN fa_aid aid ON p.id=aid.problem_id
					JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?)
					LEFT JOIN problem_section ps ON p.id=ps.problem_id
					LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
					WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
					GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date
					""")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId.orElse(0));
				ps.setInt(3, setup.idRegion());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String regionName = rst.getString("region_name");
						int areaId = rst.getInt("area_id");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int sectorId = rst.getInt("sector_id");
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
						String grade = "n/a";
						int gradeWeight = 0;
						boolean noPersonalGrade = false;
						Optional<ProfileAscent> optTick = res.stream()
								.filter(x -> x.getIdProblem() == idProblem)
								.findAny();
						if (optTick.isPresent()) {
							// User has ticked route, update this (don't add an extra First Ascent (AID))
							ProfileAscent tick = optTick.get();
							tick.setFa(true);
							if (tick.getDate() == null && date != null) {
								tick.setDate(date);
							}
							if (tick.getDateHr() == null && dateHr != null) {
								tick.setDateHr(dateHr);
							}
						}
						else {
							ProfileAscent tick = new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, 0, 0, "Aid", numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, 0, true, grade, gradeWeight, noPersonalGrade);
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
		res.sort((t1, t2) -> -ComparisonChain
				.start()
				.compare(Strings.nullToEmpty(t1.getDate()), Strings.nullToEmpty(t2.getDate()))
				.compare(t1.getId(), t2.getId())
				.compare(t1.getIdProblem(), t2.getIdProblem())
				.result());
		for (int i = 0; i < res.size(); i++) {
			res.get(i).setNum(i);
		}
		return res;
	}

	public List<ProfileGradeDistribution> getProfileGradeDistribution(Connection c, Setup setup, int userId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<ProfileGradeDistribution> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				    SELECT ? region_id, ? auth_user_id
				),
				target_types AS (
				    SELECT rt.type_id 
				    FROM region_type rt
				    JOIN req ON rt.region_id = req.region_id
				)
				SELECT v.grade, v.color, SUM(v.is_fa) num_fa, SUM(v.is_tick) num_tick, SUM(v.is_repeat) num_repeat
				FROM (SELECT g.grade, clr.hex_code color, g.weight, 1 is_fa, 0 is_tick, 0 is_repeat
					  FROM req
				      JOIN fa f ON f.user_id=req.auth_user_id
				      JOIN problem p ON f.problem_id=p.id
				      JOIN target_types tt ON p.type_id=tt.type_id
				      JOIN grade g ON p.grade_id=g.id
				      JOIN grade_color clr ON g.grade_color_id=clr.id

				      UNION ALL

				      SELECT g.grade, clr.hex_code color, g.weight, 0 is_fa, 1 is_tick, 0 is_repeat
				      FROM req
				      JOIN tick t ON t.user_id=req.auth_user_id
				      JOIN problem p ON t.problem_id=p.id
				      JOIN target_types tt ON p.type_id=tt.type_id
				      JOIN grade g ON COALESCE(t.grade_id,p.consensus_grade_id)=g.id
				      JOIN grade_color clr ON g.grade_color_id=clr.id
				        AND NOT EXISTS (SELECT 1 FROM fa f2 WHERE f2.user_id=req.auth_user_id AND f2.problem_id=t.problem_id)

				      UNION ALL

				      SELECT g.grade, clr.hex_code color, g.weight, 0 is_fa, 0 is_tick, 1 is_repeat
				      FROM req
				      JOIN tick t ON req.auth_user_id=t.user_id
				      JOIN tick_repeat tr ON t.id=tr.tick_id
				      JOIN problem p ON t.problem_id=p.id
				      JOIN target_types tt ON p.type_id=tt.type_id
				      JOIN grade g ON COALESCE(t.grade_id,p.consensus_grade_id)=g.id
				      JOIN grade_color clr ON g.grade_color_id=clr.id
				) v
				GROUP BY v.grade, v.color, v.weight
				ORDER BY v.weight DESC
				""")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					String color = rst.getString("color");
					int fa = rst.getInt("num_fa");
					int tick = rst.getInt("num_tick");
					int repeat = rst.getInt("num_repeat");
					res.add(new ProfileGradeDistribution(grade, color, fa, tick, repeat));
				}
			}
		}
		logger.debug("getProfileGradeDistribution(userId={}) - res.size()={}, duration={}", userId, res.size(), stopwatch);
		return res;
	}

	public ProfileIdentity getProfileIdentity(Connection c, Setup setup, int userId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT u.firstname, u.lastname, u.email_visible_to_all, u.theme_preference,
				       m.id media_id,  UNIX_TIMESTAMP(m.updated_at) media_version_stamp,  mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       e.emails, l.last_login
				FROM user u
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				LEFT JOIN LATERAL (
				    SELECT GROUP_CONCAT(DISTINCT email ORDER BY email SEPARATOR ';') emails
				    FROM user_email
				    WHERE user_id=u.id
				      AND email NOT LIKE '%@missing-email.com'
				      AND u.email_visible_to_all=1
				) e ON TRUE
				LEFT JOIN LATERAL (
				    SELECT `when` last_login
				    FROM user_login
				    WHERE user_id=u.id
				    ORDER BY `when` DESC
				    LIMIT 1
				) l ON TRUE
				WHERE u.id=?
				""")) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String firstname = rst.getString("firstname");
					String lastname = rst.getString("lastname");
					boolean emailVisibleToAll = rst.getBoolean("email_visible_to_all");
					String themePreference = rst.getString("theme_preference");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					String emailsStr = rst.getString("emails");
					List<String> emails = Strings.isNullOrEmpty(emailsStr) ? null : Splitter.on(';')
							.trimResults()
							.omitEmptyStrings()
							.splitToList(emailsStr);
					List<UserRegion> userRegions = getUserRegion(c, setup, userId);
					LocalDateTime lastLogin = rst.getObject("last_login", LocalDateTime.class);
					String lastActivity = lastLogin == null ? null : TimeAgo.getTimeAgo(lastLogin.toLocalDate());
					var res = new ProfileIdentity(userId, firstname, lastname, emailVisibleToAll, themePreference, mediaIdentity, emails, userRegions, lastActivity);
					logger.debug("getProfileIdentity(userId={}) - res={}, duration={}", userId, res, stopwatch);
					return res;
				}
			}
		}
		throw new NoSuchElementException("Could not find user with id=" + userId);
	}

	public ProfileKpis getProfileKpis(Connection c, int userId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		ProfileKpis res = null;
		String sqlStr = """
				WITH req AS (
				    SELECT ? user_id
				),
				valid_media AS (
				    SELECT m.id, m.is_movie, m.photographer_user_id
				    FROM media m
				    WHERE m.deleted_user_id IS NULL
				      AND (
				        EXISTS (SELECT 1 FROM media_area ma 
				                JOIN area a ON ma.area_id = a.id 
				                JOIN region_type rt ON a.region_id = rt.region_id 
				                WHERE ma.media_id = m.id)
				        OR EXISTS (SELECT 1 FROM media_sector ms 
				                   JOIN sector s ON ms.sector_id = s.id 
				                   JOIN area a ON s.area_id = a.id
				                   JOIN region_type rt ON a.region_id = rt.region_id
				                   WHERE ms.media_id = m.id)
				        OR EXISTS (SELECT 1 FROM media_problem mp 
				                   JOIN problem p ON mp.problem_id = p.id 
				                   JOIN sector s ON p.sector_id = s.id
				                   JOIN area a ON s.area_id = a.id
				                   JOIN region_type rt ON a.region_id = rt.region_id
				                   WHERE mp.media_id = m.id)
				      )
				)
				SELECT 
				    COUNT(DISTINCT CASE WHEN vm.photographer_user_id = req.user_id AND vm.is_movie = 0 THEN vm.id END) as created_img,
				    COUNT(DISTINCT CASE WHEN vm.photographer_user_id = req.user_id AND vm.is_movie = 1 THEN vm.id END) as created_vid,
				    COUNT(DISTINCT CASE WHEN mu.user_id = req.user_id AND vm.is_movie = 0 THEN vm.id END) as tagged_img,
				    COUNT(DISTINCT CASE WHEN mu.user_id = req.user_id AND vm.is_movie = 1 THEN vm.id END) as tagged_vid
				FROM req
				CROSS JOIN valid_media vm
				LEFT JOIN media_user mu ON vm.id = mu.media_id AND mu.user_id = req.user_id
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				if (rst.next()) {
					res = new ProfileKpis(rst.getInt("created_img"), rst.getInt("created_vid"), rst.getInt("tagged_img"), rst.getInt("tagged_vid"));
				}
			}
		}
		logger.debug("getProfileKpis(userId={}) - res={}, duration={}", userId, res, stopwatch);
		return res;
	}

	public List<Media> getProfileMediaCapturedArea(Connection c, Optional<Integer> authUserId, int reqId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		String sqlStr = """
				WITH req AS (
				    SELECT ? photographer_user_id, ? auth_user_id
				)
				SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged,
				       m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, MAX(a.name) location,
				       m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created,
				       DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
				       CONCAT(MAX(r.url),'/area/',MAX(a.id)) url
				FROM req
				JOIN media m ON m.photographer_user_id = req.photographer_user_id AND m.deleted_user_id IS NULL
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN user c ON m.photographer_user_id = c.id
				JOIN media_area ma ON m.id = ma.media_id
				JOIN area a ON ma.area_id = a.id
				JOIN region r ON a.region_id = r.id
				LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
				LEFT JOIN media_user mu ON m.id = mu.media_id
				LEFT JOIN user u ON mu.user_id = u.id
				WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname
				ORDER BY m.id DESC
				""";
		List<Media> initialList = new ArrayList<>();
		List<Integer> mediaIds = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, currentAuthUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					mediaIds.add(idMedia);

					MediaIdentity identity = new MediaIdentity(
							idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), 
							rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
							);

					MediaMetadata metadata = MediaMetadata.from(
							rst.getString("date_created"), rst.getString("date_taken"), 
							rst.getString("capturer"), rst.getString("tagged"), 
							rst.getString("description"), rst.getString("location")
							);

					initialList.add(new Media(
							identity, rst.getInt("uploader_user_id") == currentAuthUserId, 
							0, false, rst.getInt("width"), rst.getInt("height"), 
							rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, 
									metadata, rst.getString("embed_url"), false, 0, 0, 0, rst.getString("url")
							));
				}
			}
		}
		if (initialList.isEmpty()) {
			return initialList;
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, mediaIds);
		List<Media> res = initialList.stream()
				.map(m -> m.withMediaSvgs(svgMap.getOrDefault(m.identity().id(), List.of())))
				.toList();

		logger.debug("getProfileMediaCapturedArea(reqId={}) - res.size()={}, duration={}", reqId, res.size(), stopwatch);
		return res;
	}

	public List<Media> getProfileMediaCapturedSector(Connection c, Optional<Integer> authUserId, int reqId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		String sqlStr = """
				WITH req AS (
				    SELECT ? photographer_user_id, ? auth_user_id
				)
				SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged,
				       m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, CONCAT(MAX(s.name),' (',MAX(a.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
				       0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
				       CONCAT(MAX(r.url),'/sector/',MAX(s.id)) url
				FROM req
				JOIN media m ON m.photographer_user_id = req.photographer_user_id AND m.deleted_user_id IS NULL
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN user c ON m.photographer_user_id = c.id
				JOIN media_sector ms ON m.id = ms.media_id
				JOIN sector s ON ms.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				JOIN region r ON a.region_id = r.id
				LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
				LEFT JOIN media_user mu ON m.id = mu.media_id
				LEFT JOIN user u ON mu.user_id = u.id
				WHERE is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname
				ORDER BY m.id DESC
				""";
		List<Media> initialList = new ArrayList<>();
		List<Integer> mediaIds = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, currentAuthUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					mediaIds.add(idMedia);
					MediaIdentity identity = new MediaIdentity(
							idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), 
							rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
							);
					MediaMetadata metadata = MediaMetadata.from(
							rst.getString("date_created"), rst.getString("date_taken"), 
							rst.getString("capturer"), rst.getString("tagged"), 
							rst.getString("description"), rst.getString("location")
							);
					initialList.add(new Media(
							identity, rst.getInt("uploader_user_id") == currentAuthUserId, 
							0, false, rst.getInt("width"), rst.getInt("height"), 
							rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, 
									metadata, rst.getString("embed_url"), false, 0, 0, 0, rst.getString("url")
							));
				}
			}
		}
		if (initialList.isEmpty()) {
			return initialList;
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, mediaIds);
		List<Media> res = initialList.stream()
				.map(m -> m.withMediaSvgs(svgMap.getOrDefault(m.identity().id(), List.of())))
				.toList();

		logger.debug("getProfileMediaCapturedSector(reqId={}) - res.size()={}, duration={}", reqId, res.size(), stopwatch);
		return res;
	}

	public List<Media> getProfileMediaProblem(Connection c, Optional<Integer> authUserId, int reqId, boolean captured) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		String sqlStr;
		if (captured) {
			sqlStr = """
					WITH req AS (
					    SELECT ? photographer_user_id, ? auth_user_id
					)
					SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged,
					       m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
					       CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url,
					       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, MAX(mp.pitch) pitch, 0 t,
					       TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
					       CONCAT(MAX(r.url),'/problem/',MAX(p.id)) url
					FROM req
					JOIN media m ON m.photographer_user_id = req.photographer_user_id AND m.deleted_user_id IS NULL
					LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
					JOIN user c ON m.photographer_user_id = c.id
					JOIN media_problem mp ON m.id = mp.media_id
					JOIN problem p ON mp.problem_id = p.id
					JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id
					JOIN region r ON a.region_id = r.id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
					LEFT JOIN media_user mu ON m.id = mu.media_id
					LEFT JOIN user u ON mu.user_id = u.id
					WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
					GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname
					ORDER BY m.id DESC
					""";
		} else {
			sqlStr = """
					WITH req AS (
					    SELECT ? tagged_user_id, ? auth_user_id
					)
					SELECT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) tagged,
					       m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
					       CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location,
					       m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created,
					       DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, 0 t,
					       TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
					       CONCAT(MAX(r.url),'/problem/',MAX(p.id)) url
					FROM req
					JOIN user u ON u.id = req.tagged_user_id
					JOIN media_user mu ON u.id = mu.user_id
					JOIN media m ON mu.media_id = m.id AND m.deleted_user_id IS NULL
					LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
					JOIN user c ON m.photographer_user_id = c.id
					JOIN media_problem mp ON m.id = mp.media_id
					JOIN problem p ON mp.problem_id = p.id
					JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id
					JOIN region r ON a.region_id = r.id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
					WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
					GROUP BY u.firstname, u.lastname, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, mp.pitch, c.firstname, c.lastname
					ORDER BY m.id DESC
					""";
		}
		List<Media> initialList = new ArrayList<>();
		List<Integer> mediaIds = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, currentAuthUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					mediaIds.add(idMedia);
					MediaIdentity identity = new MediaIdentity(
							idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), 
							rst.getInt("focus_y"), rst.getString("media_primary_color_hex")
							);
					MediaMetadata metadata = MediaMetadata.from(
							rst.getString("date_created"), rst.getString("date_taken"), 
							rst.getString("capturer"), rst.getString("tagged"), 
							rst.getString("description"), rst.getString("location")
							);
					initialList.add(new Media(
							identity, rst.getInt("uploader_user_id") == currentAuthUserId, 
							rst.getInt("pitch"), false, rst.getInt("width"), rst.getInt("height"), 
							rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, 
									metadata, rst.getString("embed_url"), false, 0, 0, 0, rst.getString("url")
							));
				}
			}
		}
		if (initialList.isEmpty()) {
			return initialList;
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, mediaIds);
		List<Media> res = initialList.stream()
				.map(m -> m.withMediaSvgs(svgMap.get(m.identity().id())))
				.toList();
		logger.debug("getProfileMediaProblem(reqId={}, captured={}) - res.size()={}, duration={}", reqId, captured, res.size(), stopwatch);
		return res;
	}

	public ProfileTodo getProfileTodo(Connection c, Optional<Integer> authUserId, Setup setup, int userId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		ProfileTodo res = new ProfileTodo(new ArrayList<>());
		Map<Integer, ProfileTodoArea> areaLookup = new HashMap<>();
		Map<Integer, ProfileTodoSector> sectorLookup = new HashMap<>();
		String sql = """
				WITH req AS (
				    SELECT ? user_id, ? auth_user_id, ? region_id
				)
				SELECT a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, g.grade problem_grade, 
				       p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       COALESCE(pc.id, oc.id, sc.id, ac.id) coord_id,
				       COALESCE(pc.latitude, oc.latitude, sc.latitude, ac.latitude) lat,
				       COALESCE(pc.longitude, oc.longitude, sc.longitude, ac.longitude) lon,
				       COALESCE(pc.elevation, oc.elevation, sc.elevation, ac.elevation) ele,
				       COALESCE(pc.elevation_source, oc.elevation_source, sc.elevation_source, ac.elevation_source) ele_src,
				       (SELECT GROUP_CONCAT(CONCAT(u_other.id, ':', u_other.firstname, ' ', COALESCE(u_other.lastname, '')) SEPARATOR '|')
				        FROM todo t_other
				        JOIN user u_other ON t_other.user_id = u_other.id
				        CROSS JOIN req
				        WHERE t_other.problem_id = p.id AND t_other.user_id != req.user_id) AS partners_raw
				FROM req
				JOIN todo t ON t.user_id = req.user_id
				JOIN problem p ON t.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				JOIN region_type rt ON a.region_id = rt.region_id
				JOIN grade g ON p.grade_id = g.id
				LEFT JOIN coordinates ac ON a.coordinates_id = ac.id
				LEFT JOIN coordinates sc ON s.parking_coordinates_id = sc.id
				LEFT JOIN coordinates pc ON p.coordinates_id = pc.id
				LEFT JOIN sector_outline so ON s.id = so.sector_id AND so.sorting = 1
				LEFT JOIN coordinates oc ON so.coordinates_id = oc.id
				LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				  AND (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY p.id, t.id
				ORDER BY a.name, s.name, p.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, userId);
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					ProfileTodoArea a = areaLookup.computeIfAbsent(areaId, id -> {
						try {
							ProfileTodoArea newArea = new ProfileTodoArea(id, rst.getString("area_name"), 
									rst.getBoolean("area_locked_admin"), rst.getBoolean("area_locked_superadmin"), new ArrayList<>());
							res.areas().add(newArea);
							return newArea;
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
					});
					int sectorId = rst.getInt("sector_id");
					ProfileTodoSector s = sectorLookup.computeIfAbsent(sectorId, id -> {
						try {
							ProfileTodoSector newSector = new ProfileTodoSector(id, rst.getString("sector_name"), 
									rst.getBoolean("sector_locked_admin"), rst.getBoolean("sector_locked_superadmin"), new ArrayList<>());
							a.sectors().add(newSector);
							return newSector;
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
					});
					Coordinates coords = null;
					int coordId = rst.getInt("coord_id");
					if (!rst.wasNull() && coordId > 0) {
						coords = new Coordinates(coordId, rst.getDouble("lat"), rst.getDouble("lon"), 
								rst.getDouble("ele"), rst.getString("ele_src"));
					}
					List<User> partners = new ArrayList<>();
					String rawPartners = rst.getString("partners_raw");
					if (rawPartners != null && !rawPartners.isEmpty()) {
						for (String part : rawPartners.split("\\|")) {
							String[] bits = part.split(":", 2);
							if (bits.length == 2) {
								partners.add(User.from(Integer.parseInt(bits[0]), bits[1]));
							}
						}
					}
					ProfileTodoProblem p = new ProfileTodoProblem(
							rst.getInt("todo_id"), rst.getInt("problem_id"), 
							rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"),
							rst.getInt("problem_nr"), rst.getString("problem_name"), 
							rst.getString("problem_grade"), coords, partners
							);
					s.problems().add(p);
				}
			}
		}
		res.areas().sort(Comparator.comparing(ProfileTodoArea::name));
		logger.debug("getProfileTodo(id={}) - duration={}", userId, stopwatch);
		return res;
	}

	public List<Region> getRegions(Connection c, int currIdRegion) throws SQLException {
		Map<Integer, Region> regionLookup = new LinkedHashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id region_id, t.group, r.name, r.url FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String group = rst.getString("group");
					String name = rst.getString("name");
					String url = rst.getString("url");
					boolean active = idRegion == currIdRegion;
					regionLookup.put(idRegion, new Region(group, name, url, active, new ArrayList<>()));
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

	public List<Search> getSearch(Connection c, Optional<Integer> authUserId, Setup setup, String search) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		String quotedSearch = Pattern.quote(search); // Quote the literal search string to escape special characters like '('
		String searchRegexPattern = "(^|\\W)" + quotedSearch;
		List<Search> areas = new ArrayList<>();
		List<Search> externalAreas = new ArrayList<>();
		List<Search> sectors = new ArrayList<>();
		List<Search> problems = new ArrayList<>();
		List<Search> users = new ArrayList<>();
		Set<Integer> areaIdsVisible = new HashSet<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id, ? search_regex
				),
				ranked_area_media AS (
				   SELECT ma.area_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY ma.area_id ORDER BY m.is_movie, ma.sorting, m.id DESC) rn
				   FROM media_area ma
				   JOIN media m ON ma.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				),
				ranked_sector_media AS (
				   SELECT ms.sector_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY ms.sector_id ORDER BY m.is_movie, ms.sorting, m.id DESC) rn
				   FROM media_sector ms
				   JOIN media m ON ms.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				),
				ranked_problem_media AS (
				   SELECT mp.problem_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY mp.problem_id ORDER BY m.is_movie, mp.sorting, m.id DESC) rn
				   FROM media_problem mp
				   JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				   WHERE mp.trivia=0
				)
				(SELECT 'AREA' result_type, a.id, a.name title, NULL sub_title, r.name breadcrumb, 
				        ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex,
				        a.hits, NULL external_url,
				        a.locked_admin, a.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE REGEXP_LIKE(a.name, req.search_regex, 'i')
				   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				 GROUP BY a.id, r.name, ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex, a.hits, a.locked_admin, a.locked_superadmin
				 ORDER BY a.hits DESC, a.name LIMIT 8)

				UNION ALL

				(SELECT 'EXTERNAL' result_type, a_ext.id, a_ext.name title, NULL sub_title, r_ext.name breadcrumb, 
				        ma_ext.media_id, ma_ext.media_version_stamp, ma_ext.media_focus_x, ma_ext.media_focus_y, ma_ext.media_primary_color_hex,
				        a_ext.hits, CONCAT(r_ext.url, '/area/', a_ext.id) external_url,
				        a_ext.locked_admin, a_ext.locked_superadmin
				 FROM req
				 JOIN region_type rt ON rt.region_id=req.region_id
				 JOIN region_type rt_ext ON rt.type_id=rt_ext.type_id
				 JOIN region r_ext ON rt_ext.region_id=r_ext.id AND r_ext.id != req.region_id
				 JOIN area a_ext ON r_ext.id=a_ext.region_id AND a_ext.locked_admin=0 AND a_ext.locked_superadmin=0
				             LEFT JOIN ranked_area_media ma_ext ON a_ext.id=ma_ext.area_id AND ma_ext.rn=1
				 LEFT JOIN user_region ur_check ON r_ext.id=ur_check.region_id AND ur_check.user_id=req.auth_user_id
				 WHERE ur_check.region_id IS NULL
				   AND REGEXP_LIKE(a_ext.name, req.search_regex, 'i')
				 GROUP BY a_ext.id, r_ext.name, r_ext.url, ma_ext.media_id, ma_ext.media_version_stamp, ma_ext.media_focus_x, ma_ext.media_focus_y, ma_ext.media_primary_color_hex, a_ext.hits, a_ext.locked_admin, a_ext.locked_superadmin
				 ORDER BY a_ext.hits DESC, a_ext.name LIMIT 3)

				UNION ALL

				(SELECT 'SECTOR' result_type, s.id, s.name title, NULL sub_title, a.name breadcrumb,
				        COALESCE(ms.media_id,ma.media_id) media_id, COALESCE(ms.media_version_stamp,ma.media_version_stamp) media_version_stamp, COALESCE(ms.media_focus_x,ma.media_focus_x) media_focus_x, COALESCE(ms.media_focus_y,ma.media_focus_y) media_focus_y, COALESCE(ms.media_primary_color_hex,ma.media_primary_color_hex) media_primary_color_hex,
				        s.hits, NULL external_url,
				        s.locked_admin, s.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 JOIN sector s ON a.id=s.area_id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_sector_media ms ON s.id=ms.sector_id AND ms.rn=1
				             LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE REGEXP_LIKE(s.name, req.search_regex, 'i')
				   AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				 GROUP BY s.id, a.name, s.hits, s.locked_admin, s.locked_superadmin,
				                      ms.media_id, ms.media_version_stamp, ms.media_focus_x, ms.media_focus_y, ms.media_primary_color_hex,
				                      ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex
				 ORDER BY s.hits DESC, a.name, s.name LIMIT 8)

				UNION ALL

				(SELECT 'PROBLEM' result_type, p.id, p.name title, g.grade sub_title, CONCAT(a.name, ' / ', s.name, CASE WHEN p.rock IS NOT NULL THEN CONCAT(' (', p.rock,')') ELSE '' END) breadcrumb,
				        COALESCE(mp.media_id,ms.media_id,ma.media_id) media_id, COALESCE(mp.media_version_stamp,ms.media_version_stamp,ma.media_version_stamp) media_version_stamp, COALESCE(mp.media_focus_x,ms.media_focus_x,ma.media_focus_x) media_focus_x, COALESCE(mp.media_focus_y,ms.media_focus_y,ma.media_focus_y) media_focus_y, COALESCE(mp.media_primary_color_hex,ms.media_primary_color_hex,ma.media_primary_color_hex) media_primary_color_hex,
				        p.hits, NULL external_url,
				        p.locked_admin, p.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 JOIN sector s ON a.id=s.area_id
				 JOIN problem p ON s.id=p.sector_id
				 LEFT JOIN grade g ON p.consensus_grade_id = g.id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_problem_media mp ON p.id=mp.problem_id AND mp.rn=1
				             LEFT JOIN ranked_sector_media ms ON s.id=ms.sector_id AND ms.rn=1
				             LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE (REGEXP_LIKE(p.name, req.search_regex, 'i') OR REGEXP_LIKE(p.rock, req.search_regex, 'i'))
				   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				 GROUP BY p.id, a.name, s.name, g.grade, p.hits, p.locked_admin, p.locked_superadmin,
				                      mp.media_id, mp.media_version_stamp, mp.media_focus_x, mp.media_focus_y, mp.media_primary_color_hex,
				                      ms.media_id, ms.media_version_stamp, ms.media_focus_x, ms.media_focus_y, ms.media_primary_color_hex,
				                      ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex
				 ORDER BY p.hits DESC, p.name LIMIT 8)

				UNION ALL

				(SELECT 'USER' result_type, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) title, NULL sub_title, NULL breadcrumb,
				        m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_y, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				        0 hits, NULL external_url,
				        0 locked_admin, 0 locked_superadmin
				 FROM req
				 JOIN user u ON REGEXP_LIKE(CONCAT(' ', u.firstname, ' ', COALESCE(u.lastname,'')), req.search_regex, 'i')
				 LEFT JOIN media m ON u.media_id=m.id
				 LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				 ORDER BY TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) LIMIT 8)
				 		""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setString(3, searchRegexPattern);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String type = rst.getString("result_type");
					int id = rst.getInt("id");
					String title = rst.getString("title");
					String subTitle = rst.getString("sub_title");
					String breadcrumb = rst.getString("breadcrumb");
					long hits = rst.getLong("hits");
					String pageViews = HitsFormatter.formatHits(hits);
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					switch (type) {
					case "AREA" -> {
						areaIdsVisible.add(id);
						areas.add(new Search(title, subTitle, breadcrumb, "/area/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));

					}
					case "EXTERNAL" -> {
						externalAreas.add(new Search(title, subTitle, breadcrumb, null, rst.getString("external_url"), null, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "SECTOR" -> {
						sectors.add(new Search(title, subTitle, breadcrumb, "/sector/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "PROBLEM" -> {
						problems.add(new Search(title, subTitle, breadcrumb, "/problem/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "USER" -> {
						users.add(new Search(title, null, null, "/user/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					default -> throw new IllegalArgumentException("Invalid type: " + type);
					}
				}
			}
		}
		// Truncate logic
		while (areas.size() + sectors.size() + problems.size() + users.size() > 10) {
			if (problems.size() > 5) {
				problems.removeLast();
			}
			else if (areas.size() > 2) {
				areas.removeLast();
			}
			else if (sectors.size() > 2) {
				sectors.removeLast();
			}
			else if (users.size() > 1) {
				users.removeLast();
			}
		}
		// Filter External Areas
		List<Search> filteredExternal = externalAreas.stream()
				.filter(ea -> {
					try {
						String url = ea.externalUrl();
						int extId = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1));
						return !areaIdsVisible.contains(extId);
					} catch (Exception _) {
						return true;
					}
				}).toList();
		// Assemble and Final Sort
		List<Search> res = new ArrayList<>();
		res.addAll(areas);
		res.addAll(sectors);
		res.addAll(problems);
		res.sort((r1, r2) -> Long.compare(r2.hits(), r1.hits()));
		res.addAll(users);
		res.addAll(filteredExternal);
		logger.debug("getSearch(search={}) - res.size()={}, duration={}", search, res.size(), stopwatch);
		return res;
	}

	public Sector getSector(Connection c, Optional<Integer> authUserId, boolean orderByGrade, Setup setup, int reqId, boolean shouldUpdateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (shouldUpdateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE sector SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Sector s = null;
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
					SELECT ? region_id, ? auth_user_id, ? sector_id
				)
				SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, a.name area_name, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits
				FROM req
				JOIN sector s ON req.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				GROUP BY a.id, a.locked_admin, a.locked_superadmin, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits
				""")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, reqId);
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
					s = new Sector(null, orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, areaName, reqId, false, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, sectorOutline, wallDirectionCalculated, wallDirectionManual, sectorApproach, sectorDescent, media, triviaMedia, null, externalLinks, pageViews);
				}
			}
		}
		if (s == null) {
			// Sector not found, see if it's visible on a different domain
			try {
				Redirect res = getCanonicalUrl(c, 0, reqId, 0);
				if (!Strings.isNullOrEmpty(res.redirectUrl())) {
					return new Sector(res.redirectUrl(), false, 0, false, false, null, null, false, 0, 0, null, 0, false, false, false, null, null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
				// Not found on other domains either
			}
		}

		if (s == null) {
			throw new NoSuchElementException("Could not find sector with id=" + reqId);
		}
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
		for (SectorProblem sp : getSectorProblems(c, setup, authUserId, 0, reqId).get(reqId)) {
			s.addProblem(sp);
		}
		if (!s.getProblems().isEmpty() && orderByGrade) {
			Collections.sort(s.getProblems(), Comparator.comparing(SectorProblem::gradeWeight).reversed());
		}
		logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, stopwatch);
		return s;
	}

	public List<Setup> getSetups(Connection c) throws SQLException {
		List<Setup> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT r.id id_region, r.title, r.description, REPLACE(REPLACE(r.url,'https://',''),'http://','') domain, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				FROM region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN type t ON rt.type_id=t.id
				JOIN type_grade_system tgs ON t.id=tgs.type_id
				GROUP BY r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				ORDER BY r.id
				""")) {
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
					int gradeSystemId = rst.getInt("grade_system_id");
					List<CompassDirection> compassDirections = getCompassDirections(c);
					GradeConverter gradeConverter = new GradeConverter(getGrades(c, gradeSystemId));
					res.add(Setup.newBuilder(domain, group)
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
		urls.add(setup.url() + "/activity");
		urls.add(setup.url() + "/areas");
		urls.add(setup.url() + "/donate");
		urls.add(setup.url() + "/dangerous");
		urls.add(setup.url() + "/gpl-3.0.txt");
		urls.add(setup.url() + "/graph");
		urls.add(setup.url() + "/privacy-policy");
		urls.add(setup.url() + "/problems");
		urls.add(setup.url() + "/regions/bouldering");
		urls.add(setup.url() + "/regions/climbing");
		urls.add(setup.url() + "/regions/ice");
		urls.add(setup.url() + "/webcams");
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

	public Ticks getTicks(Connection c, Optional<Integer> authUserId, Setup setup, int page) throws SQLException {
		final int take = 200;
		int skip = (page - 1) * take;
		String sqlStr = """
				WITH req AS (
					SELECT ? region_id, ? auth_user_id
				)
				SELECT a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       p.id problem_id, g.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name,
				       COUNT(*) OVER() as total_count
				FROM req
				JOIN region r ON 1=1
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN tick t ON p.id=t.problem_id
				LEFT JOIN grade g ON t.grade_id=g.id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY a.id, s.id, p.id, t.id, u.id, t.date, p.name, u.firstname, u.lastname
				ORDER BY t.date DESC, p.name, name
				LIMIT ? OFFSET ?
				""";
		List<PublicAscent> ticks = new ArrayList<>();
		int totalCount = 0;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, take);
			ps.setInt(4, skip);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (totalCount == 0) {
						totalCount = rst.getInt("total_count");
					}
					int areaId = rst.getInt("area_id");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					String problemGrade = rst.getString("problem_grade");
					if (problemGrade == null) {
						problemGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(
							areaId, areaName, areaLockedAdmin, areaLockedSuperadmin,
							sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin,
							problemId, problemGrade,
							problemName, problemLockedAdmin, problemLockedSuperadmin, date, name
							));
				}
			}
		}
		int numPages = (int) Math.ceil((double) totalCount / take);
		return new Ticks(ticks, page, numPages);
	}

	public Toc getToc(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, TocRegion> regionLookup = new LinkedHashMap<>();
		Map<Integer, TocArea> areaLookup = new HashMap<>();
		Map<Integer, TocSector> sectorLookup = new HashMap<>();
		int numProblems = 0;
		String sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				),
				p_stars AS (
				    SELECT 
				        p_sub.id AS pid,
				        COUNT(DISTINCT t_sub.id) AS num_ticks,
				        ROUND(ROUND(AVG(NULLIF(t_sub.stars, -1)) * 2) / 2, 1) AS stars
				    FROM req
				    JOIN problem p_sub ON 1=1
				    LEFT JOIN tick t_sub ON p_sub.id = t_sub.problem_id
				    GROUP BY p_sub.id
				)
				SELECT 
				    r.id region_id, r.name region_name, a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, 
				    ac.id area_coordinates_id, ac.latitude area_latitude, ac.longitude area_longitude, ac.elevation area_elevation, ac.elevation_source area_elevation_source, 
				    a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour,
				    s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.sorting sector_sorting, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour, 
				    sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, 
				    s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, 
				    s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				    p.id, CONCAT(r.url,'/problem/',p.id) url, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, p.length_meter, REGEXP_SUBSTR(p.starting_altitude,'[0-9]+') starting_altitude,
				    c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				    gf.grade,
				    GROUP_CONCAT(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa, 
				    YEAR(p.fa_date) fa_year,
				    ps_data.num_ticks, ps_data.stars, 
				    MAX(CASE WHEN (t.user_id = req.auth_user_id OR u.id = req.auth_user_id) THEN 1 END) ticked,
				    CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END todo,
				    ty.id type_id, ty.type, ty.subtype, COUNT(DISTINCT ps.id) num_pitches 
				FROM req
				JOIN area a ON 1=1
				JOIN region r ON a.region_id = r.id
				JOIN region_type rt ON r.id = rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				JOIN sector s ON a.id = s.area_id
				JOIN problem p ON s.id = p.sector_id AND rt.type_id = p.type_id
				JOIN p_stars ps_data ON p.id = ps_data.pid
				JOIN type ty ON p.type_id = ty.id
				LEFT JOIN grade gf ON p.consensus_grade_id = gf.id
				LEFT JOIN coordinates ac ON a.coordinates_id = ac.id
				LEFT JOIN coordinates sc ON s.parking_coordinates_id = sc.id
				LEFT JOIN coordinates c ON p.coordinates_id = c.id
				LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				LEFT JOIN fa f ON p.id = f.problem_id
				LEFT JOIN user u ON f.user_id = u.id
				LEFT JOIN tick t ON p.id = t.problem_id
				LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = req.auth_user_id
				LEFT JOIN problem_section ps ON p.id = ps.problem_id 
				WHERE (a.region_id = req.region_id OR ur.user_id IS NOT NULL) 
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1 
				GROUP BY p.id, r.id, a.id, s.id, gf.grade, todo.id, ty.id, ps_data.num_ticks, ps_data.stars
				ORDER BY r.name, a.name, s.sorting, s.name, p.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
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
					int lengthMeter = rst.getInt("length_meter");
					int startingAltitude = rst.getInt("starting_altitude");
					int faYear = rst.getInt("fa_year");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					String grade = rst.getString("grade");
					String fa = rst.getString("fa");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean todo = rst.getBoolean("todo");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int numPitches = rst.getInt("num_pitches");
					TocProblem p = new TocProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, description, lengthMeter, startingAltitude, coordinates, grade, fa, faYear, numTicks, stars, ticked, todo, t, numPitches);
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
				SELECT r.name region_name, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name problem_name, ps.nr pitch, g.grade, ps.description
				FROM area a INNER JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)
				JOIN problem_section ps ON p.id=ps.problem_id
				JOIN grade g ON ps.grade_id=g.id
				JOIN type ty ON p.type_id=ty.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY r.name, r.url, p.id, a.name, s.name, p.name, ps.nr, g.grade, ps.description
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
					String grade = rst.getString("grade");
					String description = rst.getString("description");
					res.add(new TocPitch(regionName, url, areaName, sectorName, problemName, pitch, grade, description));
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
		String sqlStr = """
				SELECT s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, g.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) user_name
				FROM region r
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN grade g ON p.grade_id=g.id
				JOIN todo t ON p.id=t.problem_id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?
				WHERE %s
				AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				ORDER BY a.name, s.sorting, s.name, p.nr, u.firstname, u.lastname
				""".formatted(condition);
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
						String problemGrade = rst.getString("problem_grade");
						boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
						boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
						p = new TodoProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, problemGrade, new ArrayList<>());
						s.problems().add(p);
						problemLookup.put(problemId, p);
					}
					// Partner
					int userId = rst.getInt("user_id");
					String userName = rst.getString("user_name");
					p.partners().add(User.from(userId, userName));
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idArea={}, idSector)={}) - res={}", authUserId, setup.idRegion(), idArea, idSector, res);
		return res;
	}

	public Top getTop(Connection c, Optional<Integer> authUserId, int areaId, int sectorId) throws SQLException {
		String columnCondition = (sectorId > 0) ? "s.id" : "a.id";
		int filterId = (sectorId > 0) ? sectorId : areaId;
		String sqlStr = """
				WITH total_problems AS (
				    SELECT COUNT(DISTINCT p.id) AS sum
				    FROM area a
				    JOIN sector s ON a.id = s.area_id
				    JOIN problem p ON s.id = p.sector_id AND p.broken IS NULL
				    JOIN grade g ON p.grade_id = g.id
				    LEFT JOIN fa f ON p.id = f.problem_id
				    LEFT JOIN tick t ON p.id = t.problem_id
				    LEFT JOIN fa_aid_user aid ON p.id = aid.problem_id
				    WHERE %1$s = ? 
				      AND (g.grade != 'n/a' OR aid.user_id IS NOT NULL OR f.user_id IS NOT NULL OR t.id IS NOT NULL)
				),
				user_completions AS (
				    SELECT f.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN fa f ON p.id = f.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND f.user_id IS NOT NULL

				    UNION

				    SELECT t.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN tick t ON p.id = t.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND t.user_id IS NOT NULL

				    UNION

				    SELECT aid.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN fa_aid_user aid ON p.id = aid.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND aid.user_id IS NOT NULL
				)
				SELECT 
				    u.id AS user_id,
				    TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) AS name,
				    m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				    ROUND((COUNT(DISTINCT uc.problem_id) / NULLIF(tp.sum, 0)) * 100, 2) AS percentage
				FROM user_completions uc
				JOIN user u ON uc.user_id = u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				CROSS JOIN total_problems tp
				GROUP BY u.id, u.firstname, u.lastname, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, tp.sum
				ORDER BY percentage DESC, name ASC
				""".formatted(columnCondition);
		Map<Double, TopRank> topByPercentage = new LinkedHashMap<>();
		Set<Integer> uniqueUserIds = new HashSet<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, filterId);
			ps.setInt(2, filterId);
			ps.setInt(3, filterId);
			ps.setInt(4, filterId);
			try (ResultSet rst = ps.executeQuery()) {
				double prevPercentage = -1.0;
				int rank = 0;
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					uniqueUserIds.add(userId);
					String name = rst.getString("name");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					double percentage = rst.getDouble("percentage");
					if (prevPercentage != percentage) {
						rank++;
					}
					prevPercentage = percentage;
					boolean mine = authUserId.orElse(0) == userId;
					var top = topByPercentage.get(percentage);
					if (top == null) {
						top = new TopRank(rank, percentage, new ArrayList<>());
						topByPercentage.put(percentage, top);
					}
					top.users().add(new TopUser(userId, name, mediaIdentity, mine));
				}
			}
		}
		var rows = topByPercentage.values();
		return new Top(rows, uniqueUserIds.size());
	}

	public List<Trash> getTrash(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		ensureAdminWriteRegion(c, setup, authUserId);
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

	public List<UserRegion> getUserRegion(Connection c, Setup setup, int userId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<UserRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				    SELECT ? region_id, ? user_id
				),
				target_types AS (
				    SELECT rt.type_id 
				    FROM region_type rt 
				    JOIN req ON rt.region_id = req.region_id
				),
				user_activity AS (
				    SELECT DISTINCT region_id FROM (
				        SELECT a.region_id 
				        FROM fa f
				        JOIN problem p ON f.problem_id = p.id
				        JOIN sector s ON p.sector_id = s.id
				        JOIN area a ON s.area_id = a.id
				        JOIN req ON f.user_id = req.user_id

				        UNION ALL

				        SELECT a.region_id 
				        FROM tick t
				        JOIN problem p ON t.problem_id = p.id
				        JOIN sector s ON p.sector_id = s.id
				        JOIN area a ON s.area_id = a.id
				        JOIN req ON t.user_id = req.user_id
				    ) acts
				)
				SELECT r.id, r.name,
				       MAX(CASE WHEN r.id = req.region_id OR ur.admin_read = 1 OR ur.admin_write = 1 OR ur.superadmin_read = 1 OR ur.superadmin_write = 1 THEN 1 ELSE 0 END) AS read_only,
				       MAX(ur.region_visible) AS region_visible,
				       MAX(CASE 
				           WHEN ur.superadmin_write = 1 THEN 'Superadmin' 
				           WHEN ur.superadmin_read = 1 THEN 'Superadmin (read)' 
				           WHEN ur.admin_read = 1 THEN 'Admin (read)' 
				           WHEN ur.admin_write = 1 THEN 'Admin' 
				       END) AS role,
				       MAX(CASE WHEN ua.region_id IS NOT NULL THEN 1 ELSE 0 END) AS activity
				FROM req
				JOIN region r ON 1=1
				JOIN region_type rt ON r.id = rt.region_id
				LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.user_id
				LEFT JOIN user_activity ua ON r.id = ua.region_id
				WHERE rt.type_id IN (SELECT type_id FROM target_types)
				GROUP BY r.id, r.name 
				ORDER BY r.name
				""")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String role = rst.getString("role");
					boolean readOnly = rst.getBoolean("read_only");
					boolean enabled = readOnly || rst.getBoolean("region_visible");
					boolean activity = rst.getBoolean("activity");
					res.add(new UserRegion(id, name, role, enabled, readOnly, activity));
				}
			}
		}
		logger.debug("getUserRegion(userId={}) - res.size()={}, duration={}", userId, res.size(), stopwatch);
		return res;
	}

	public List<User> getUserSearch(Connection c, Optional<Integer> authUserId, String value) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in...");
		List<User> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(value)) {
			String searchRegexPattern = "(^|\\W)" + Pattern.quote(value);
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name
					FROM user u
					WHERE regexp_like(TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))),?,'i')
					ORDER BY u.firstname, u.lastname
					""")) {
				ps.setString(1, searchRegexPattern);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						String name = rst.getString("name");
						res.add(User.from(id, name));
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
		int userId = authUserId.orElseThrow();
		try (var workbook = new ExcelWorkbook(); var os = new ByteArrayOutputStream()) {
			Map<String, ExcelSheet> sheets = new HashMap<>();

			// 1. PRIMARY TICKS & FAs
			String sqlTicks = """
					         SELECT 
					    ty.type, pt.subtype, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    IF(t.id IS NOT NULL, t.comment, p.description) AS comment,
					    DATE_FORMAT(IF(t.date IS NULL AND f.user_id IS NOT NULL, p.fa_date, t.date), '%Y-%m-%d') AS date,
					    t.stars, IF(f.user_id IS NOT NULL, 1, 0) AS fa, g.grade,
					    COUNT(DISTINCT ps.id) AS num_pitches
					FROM problem p
					JOIN type pt ON p.type_id = pt.id
					JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id
					JOIN region r ON a.region_id = r.id
					JOIN region_type rt ON r.id = rt.region_id AND rt.type_id = p.type_id
					JOIN type ty ON rt.type_id = ty.id
					JOIN type_grade_system tgs ON p.type_id = tgs.type_id
					LEFT JOIN problem_section ps ON p.id = ps.problem_id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					LEFT JOIN tick t ON p.id = t.problem_id AND t.user_id = ?
					LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = ?
					LEFT JOIN grade g ON g.grade_system_id = tgs.grade_system_id 
					    AND g.id = IF(t.id IS NOT NULL, t.grade_id, p.grade_id)
					WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL)
					  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
					GROUP BY p.id, t.id, ty.type, pt.subtype, r.url, a.name, s.name, p.name, t.comment, p.description, t.date, p.fa_date, t.stars, f.user_id, g.grade
					ORDER BY ty.type, a.name, s.name, p.name
					         """;
			try (var ps = c.prepareStatement(sqlTicks)) {
				ps.setInt(1, userId);
				ps.setInt(2, userId);
				ps.setInt(3, userId);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						writeRow(workbook, sheets, rst, rst.getString("type"));
					}
				}
			}

			// 2. REPEAT TICKS
			String sqlRepeats = """
					         SELECT 
					    CONCAT(ty.type, ' (repeats)') AS type, pt.subtype, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    tr.comment, DATE_FORMAT(tr.date, '%Y-%m-%d') AS date, t.stars, 0 AS fa, g.grade,
					    COUNT(DISTINCT ps.id) AS num_pitches
					FROM problem p
					JOIN type pt ON p.type_id = pt.id
					JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id
					JOIN region r ON a.region_id = r.id
					JOIN region_type rt ON r.id = rt.region_id AND rt.type_id = p.type_id
					JOIN type ty ON rt.type_id = ty.id
					JOIN type_grade_system tgs ON p.type_id = tgs.type_id
					JOIN tick t ON p.id = t.problem_id AND t.user_id = ?
					JOIN tick_repeat tr ON t.id = tr.tick_id
					LEFT JOIN problem_section ps ON p.id = ps.problem_id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					LEFT JOIN grade g ON g.grade_system_id = tgs.grade_system_id AND g.id = t.grade_id
					WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
					GROUP BY tr.id, p.id, ty.type, pt.subtype, r.url, a.name, s.name, p.name, tr.comment, tr.date, t.stars, g.grade
					ORDER BY ty.type, a.name, s.name, p.name, tr.date
					         """;
			try (var ps = c.prepareStatement(sqlRepeats)) {
				ps.setInt(1, userId);
				ps.setInt(2, userId);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						writeRow(workbook, sheets, rst, rst.getString("type"));
					}
				}
			}

			// 3. FIRST AID ASCENTS
			String sqlAid = """
					         SELECT 
					    p.id AS problem_id, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    aid.aid_description AS comment, DATE_FORMAT(aid.aid_date, '%Y-%m-%d') AS date
					FROM problem p
					JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id
					JOIN region r ON a.region_id = r.id
					JOIN fa_aid aid ON p.id = aid.problem_id
					JOIN fa_aid_user aid_u ON p.id = aid_u.problem_id AND aid_u.user_id = ?
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
					GROUP BY p.id, r.url, a.name, s.name, p.name, aid.aid_description, aid.aid_date
					ORDER BY a.name, s.name, p.name
					         """;
			try (var ps = c.prepareStatement(sqlAid)) {
				ps.setInt(1, userId);
				ps.setInt(2, userId);
				try (var rst = ps.executeQuery()) {
					ExcelSheet aidSheet = null;
					while (rst.next()) {
						if (aidSheet == null) aidSheet = workbook.addSheet("First_AID_Ascent");
						aidSheet.incrementRow();
						aidSheet.writeString("AREA", rst.getString("area_name"));
						aidSheet.writeString("SECTOR", rst.getString("sector_name"));
						aidSheet.writeString("NAME", rst.getString("name"));
						aidSheet.writeDate("DATE", rst.getObject("date", LocalDate.class));
						aidSheet.writeString("DESCRIPTION", rst.getString("comment"));
						aidSheet.writeHyperlink("URL", rst.getString("url") + "/problem/" + rst.getInt("problem_id"));
					}
					if (aidSheet != null) aidSheet.close();
				}
			}

			sheets.values().forEach(ExcelSheet::close);
			workbook.write(os);
			return os.toByteArray();
		}
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
					idMediaList.addFirst(id); // Move from end to start
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

	public void rotateMedia(Connection c, Setup setup, Optional<Integer> authUserId, int idMedia, int degrees) throws IOException, SQLException, InterruptedException {
		ensureAdminOrMediaUpdatedByMe(c, setup, authUserId, idMedia);
		Rotation r = switch (degrees) {
		case 90 -> Rotation.CW_90;
		case 180 -> Rotation.CW_180;
		case 270 -> Rotation.CW_270;
		default -> throw new IllegalArgumentException("Cannot rotate image " + degrees + " degrees (legal degrees = 90, 180, 270)");
		};
		ImageHelper.rotateImage(this, c, idMedia, r);
	}

	public void saveMediaAnalysis(Connection c, int mediaId, int imageWidth, int imageHeight, String hexColor, List<EntityAnnotation> labels, List<LocalizedObjectAnnotation> objects, boolean failed) throws SQLException {
		Preconditions.checkArgument(mediaId > 0, "Media id required");
		boolean hasPersonObject = objects != null && objects.stream().anyMatch(obj -> obj.getName().equalsIgnoreCase("Person"));

		int focusX = 0;
		int focusY = 0;

		if (hasPersonObject) {
			var climber = objects.stream()
					.filter(obj -> obj.getName().equalsIgnoreCase("Person"))
					.min(Comparator.comparing(obj -> obj.getBoundingPoly().getNormalizedVertices(0).getY()))
					.orElse(null);

			if (climber != null) {
				List<NormalizedVertex> v = climber.getBoundingPoly().getNormalizedVerticesList();
				if (v.size() >= 3) {
					float xMin = v.get(0).getX();
					float yMin = v.get(0).getY();
					float xMax = v.get(2).getX();
					float yMax = v.get(2).getY();
					float personHeight = yMax - yMin;

					focusX = Math.round(((xMin + xMax) / 2) * 100);
					if (imageHeight > imageWidth) {
						if (yMax > 0.80f && personHeight < 0.60f) {
							focusY = Math.round(yMax * 100);
						} else {
							focusY = Math.round((yMin + personHeight * 0.85f) * 100);
						}
					} else {
						focusY = Math.round(((yMin + yMax) / 2) * 100);
					}
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_analysis (media_id, primary_color_hex, focus_x, focus_y, is_action_shot, failed) VALUES (?, ?, ?, ?, ?, ?)")) {
			ps.setInt(1, mediaId);
			ps.setString(2, hexColor);
			ps.setInt(3, focusX);
			ps.setInt(4, focusY);
			ps.setBoolean(5, hasPersonObject);
			ps.setBoolean(6, failed);
			ps.execute();
		}

		if (!failed) {
			if (labels != null && !labels.isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_label (media_id, description, score) VALUES (?, ?, ?)")) {
					for (EntityAnnotation l : labels) {
						ps.setInt(1, mediaId);
						ps.setString(2, l.getDescription());
						ps.setFloat(3, l.getScore());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			if (objects != null && !objects.isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_object (media_id, name, score, x_min, y_min, x_max, y_max) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
					for (LocalizedObjectAnnotation obj : objects) {
						List<NormalizedVertex> v = obj.getBoundingPoly().getNormalizedVerticesList();
						if (v.size() >= 3) {
							ps.setInt(1, mediaId);
							ps.setString(2, obj.getName());
							ps.setFloat(3, obj.getScore());
							ps.setFloat(4, v.get(0).getX());
							ps.setFloat(5, v.get(0).getY());
							ps.setFloat(6, v.get(2).getX());
							ps.setFloat(7, v.get(2).getY());
							ps.addBatch();
						}
					}
					ps.executeBatch();
				}
			}
		}
	}

	public void saveUserAvatar(Connection c, Optional<Integer> authUserId, Supplier<InputStream> inputStreamSupplier) throws SQLException, IOException, InterruptedException {
		String name = UUID.randomUUID().toString();
		var m = new NewMedia(name, null, null, 0, false, null, null, null, 0l);
		final int pitch = 0;
		final int idProblem = 0;
		final int idSector = 0;
		final int idArea = 0;
		final int idGuestbook = 0;
		addNewMedia(c, authUserId, idProblem, pitch, false, idSector, idArea, idGuestbook, authUserId.get().intValue(), m, inputStreamSupplier);
	}

	public Redirect setArea(Connection c, Setup s, Optional<Integer> authUserId, Area a, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkArgument(s.idRegion() > 0, "Insufficient credentials");
		ensureAdminWriteRegion(c, s, authUserId);
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
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO area (region_id, name, description, coordinates_id, locked_admin, locked_superadmin, for_developers, access_info, access_closed, no_dogs_allowed, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, s.idRegion());
				ps.setString(2, GlobalFunctions.stripString(a.getName()));
				ps.setString(3, GlobalFunctions.stripString(a.getComment()));
				setNullablePositiveInteger(ps, 4, a.getCoordinates() == null? 0 : a.getCoordinates().getId());
				ps.setBoolean(5, isLockedAdmin);
				ps.setBoolean(6, a.isLockedSuperadmin());
				ps.setBoolean(7, a.isForDevelopers());
				ps.setString(8, GlobalFunctions.stripString(a.getAccessInfo()));
				ps.setString(9, GlobalFunctions.stripString(a.getAccessClosed()));
				ps.setBoolean(10, a.isNoDogsAllowed());
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
				final int idUserAvatar = 0;
				addNewMedia(c, authUserId, idProblem, pitch, m.trivia(), idSector, idArea, idGuestbook, idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
			}
		}
		upsertExternalLinks(c, a.getExternalLinks(), idArea, 0, 0);
		if (a.isTrash()) {
			return Redirect.fromRoot();
		}
		return Redirect.fromIdArea(idArea);
	}

	public void setMediaMetadata(Connection c, int idMedia, int width, int height, LocalDateTime dateTaken) throws SQLException {
		String sqlStr = dateTaken == null ?
				"UPDATE media SET width=?, height=? WHERE id=?" :
					"UPDATE media SET date_taken=?, width=?, height=? WHERE id=?";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 0;
			if (dateTaken != null) {
				ps.setObject(++ix, dateTaken);
			}
			ps.setInt(++ix, width);
			ps.setInt(++ix, height);
			ps.setInt(++ix, idMedia);
			ps.executeUpdate();
		}
		logger.debug("setMediaMetadata(idMedia={}, width={}, height={}, dateTaken={}) - success", idMedia, width, height, dateTaken);
	}

	public Redirect setProblem(Connection c, Optional<Integer> authUserId, Setup s, Problem p, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		final boolean orderByGrade = s.isBouldering();
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
		int gradeId = s.gradeConverter().getIdGradeFromGrade(p.getOriginalGrade());
		if (p.getId() > 0) {
			try (PreparedStatement ps = c.prepareStatement("""
					UPDATE problem p
					JOIN sector s ON p.sector_id=s.id
					JOIN area a ON s.area_id=a.id
					JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)
					SET p.name=?, p.rock=?, p.description=?, p.grade_id=?, p.fa_date=?, p.coordinates_id=?, p.broken=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, length_meter=?, descent=?, p.trash=CASE WHEN ? THEN NOW() ELSE NULL END, p.trash_by=?, p.last_updated=now()
					WHERE p.id=?
					""")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setString(2, GlobalFunctions.stripString(p.getName()));
				ps.setString(3, GlobalFunctions.stripString(p.getRock()));
				ps.setString(4, GlobalFunctions.stripString(p.getComment()));
				ps.setInt(5, gradeId);
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
				setNullablePositiveInteger(ps, 16, p.getLengthMeter());
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
			updateProblemConsensusGrade(c, idProblem);
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem (sector_id, name, rock, description, grade_id, consensus_grade_id, fa_date, coordinates_id, broken, locked_admin, locked_superadmin, nr, type_id, trivia, starting_altitude, aspect, length_meter, descent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, p.getSectorId());
				ps.setString(2, GlobalFunctions.stripString(p.getName()));
				ps.setString(3, GlobalFunctions.stripString(p.getRock()));
				ps.setString(4, GlobalFunctions.stripString(p.getComment()));
				ps.setInt(5, gradeId);
				ps.setInt(6, gradeId);
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
				setNullablePositiveInteger(ps, 17, p.getLengthMeter());
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
				final int idUserAvatar = 0;
				addNewMedia(c, authUserId, idProblem, m.pitch(), m.trivia(), idSector, idArea, idGuestbook, idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
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
					int idUser = addUser(c, null, x.name(), null);
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
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade_id) VALUES (?, ?, ?, ?)")) {
				for (ProblemSection section : p.getSections()) {
					ps.setInt(1, idProblem);
					ps.setInt(2, section.nr());
					ps.setString(3, GlobalFunctions.stripString(section.description()));
					setNullablePositiveInteger(ps, 4, s.gradeConverter().getIdGradeFromGrade(section.grade()));
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
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
								idUser = addUser(c, null, u.name(), null);
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

	public void setProfile(Connection c, Optional<Integer> authUserId, ProfileIdentity profile, FormDataMultiPart multiPart) throws SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId.orElse(0) == profile.id(), "Wrong input");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.firstname()), "Firstname cannot be null");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.lastname()), "Lastname cannot be null");
		String theme = (profile.themePreference() != null && (profile.themePreference().equals("light") || profile.themePreference().equals("dark"))) ? profile.themePreference() : null;
		try (PreparedStatement ps = c.prepareStatement("UPDATE user SET firstname=?, lastname=?, email_visible_to_all=?, theme_preference=COALESCE(theme_preference, ?) WHERE id=?")) {
			ps.setString(1, profile.firstname());
			ps.setString(2, profile.lastname());
			ps.setBoolean(3, profile.emailVisibleToAll());
			if (theme != null) {
				ps.setString(4, theme);
			} else {
				ps.setNull(4, Types.VARCHAR);
			}
			ps.setInt(5, authUserId.orElseThrow());
			ps.execute();
		}
		var avatar = multiPart.getField("avatar");
		if (avatar != null) {
			saveUserAvatar(c, authUserId, () -> avatar.getValueAs(InputStream.class));
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
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO sector (area_id, name, description, access_info, access_closed, parking_coordinates_id, locked_admin, locked_superadmin, compass_direction_id_calculated, compass_direction_id_manual, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, s.getAreaId());
				ps.setString(2, s.getName());
				ps.setString(3, GlobalFunctions.stripString(s.getComment()));
				ps.setString(4, GlobalFunctions.stripString(s.getAccessInfo()));
				ps.setString(5, GlobalFunctions.stripString(s.getAccessClosed()));
				setNullablePositiveInteger(ps, 6, s.getParking() == null? 0 : s.getParking().getId());
				ps.setBoolean(7, isLockedAdmin);
				ps.setBoolean(8, s.isLockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.getOutline());
				setNullablePositiveInteger(ps, 9, calculatedWallDirection != null? calculatedWallDirection.id() : 0);
				setNullablePositiveInteger(ps, 10, s.getWallDirectionManual() != null? s.getWallDirectionManual().id() : 0);
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
			List<Integer> ids = s.getOutline().stream().map(Coordinates::getId).toList();
			String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
			String sqlStr = "DELETE FROM sector_outline WHERE sector_id=? AND coordinates_id NOT IN (" + placeholders + ")";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				for (int i = 0; i < ids.size(); i++) {
					ps.setInt(i + 2, ids.get(i));
				}
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
			List<Integer> ids = s.getApproach().coordinates().stream().map(Coordinates::getId).toList();
			String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
			String sqlStr = "DELETE FROM sector_approach WHERE sector_id=? AND coordinates_id NOT IN (" + placeholders + ")";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				for (int i = 0; i < ids.size(); i++) {
					ps.setInt(i + 2, ids.get(i));
				}
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
			List<Integer> ids = s.getDescent().coordinates().stream().map(Coordinates::getId).toList();
			String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
			String sqlStr = "DELETE FROM sector_descent WHERE sector_id=? AND coordinates_id NOT IN (" + placeholders + ")";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idSector);
				for (int i = 0; i < ids.size(); i++) {
					ps.setInt(i + 2, ids.get(i));
				}
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
				final int idUserAvatar = 0;
				addNewMedia(c, authUserId, idProblem, pitch, m.trivia(), idSector, idArea, idGuestbook, idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
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

	public void setThemePreference(Connection c, Optional<Integer> authUserId, String themePreference) throws SQLException {
		Preconditions.checkArgument(themePreference != null && (themePreference.equals("light") || themePreference.equals("dark")), "themePreference must be 'light' or 'dark'");
		try (PreparedStatement ps = c.prepareStatement("UPDATE user SET theme_preference=? WHERE id=?")) {
			ps.setString(1, themePreference);
			ps.setInt(2, authUserId.orElseThrow());
			ps.execute();
		}
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
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade_id, comment, stars) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, t.idProblem());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setObject(3, dt);
				setNullablePositiveInteger(ps, 4, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
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
			try (PreparedStatement ps = c.prepareStatement("UPDATE tick SET date=?, grade_id=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setObject(1, dt);
				setNullablePositiveInteger(ps, 2, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
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
		updateProblemConsensusGrade(c, t.idProblem());
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
		ensureSuperadminWriteRegion(c, setup, authUserId);
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
								final int idUserAvatar = 0;
								addNewMedia(c, authUserId, idProblem, 0, m.trivia(), idSector, idArea, co.id(), idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
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
			Objects.requireNonNull(GlobalFunctions.stripString(co.comment()));
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
								final int idUserAvatar = 0;
								addNewMedia(c, authUserId, idProblem, 0, m.trivia(), idSector, idArea, idGuestbook, idUserAvatar, m, () -> multiPart.getField(m.name()).getValueAs(InputStream.class));
							}
						}
					}
				}
			}
		}
		fillActivity(c, co.idProblem());
	}

	public void upsertMediaSvg(Connection c, Optional<Integer> authUserId, Setup setup, Media m) throws SQLException {
		ensureAdminWriteRegion(c, setup, authUserId);
		// Clear existing
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_svg WHERE media_id=?")) {
			ps.setInt(1, m.identity().id());
			ps.execute();
		}
		// Insert
		for (MediaSvgElement element : m.mediaSvgs()) {
			if (element.t().equals(MediaSvgElementType.PATH)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")) {
					ps.setInt(1, m.identity().id());
					ps.setString(2, element.path());
					ps.execute();
				}
			}
			else if (element.t().equals(MediaSvgElementType.RAPPEL_BOLTED) || element.t().equals(MediaSvgElementType.RAPPEL_NOT_BOLTED)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, m.identity().id());
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

	public void upsertPermissionUser(Connection c, Setup setup, Optional<Integer> authUserId, PermissionUser u) throws SQLException {
		ensureSuperadminWriteRegion(c, setup, authUserId);
		// Upsert
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?")) {
			ps.setInt(1, u.userId());
			ps.setInt(2, setup.idRegion());
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

	private int addNewMedia(Connection c, Optional<Integer> authUserId, int idProblem, int pitch, boolean trivia, int idSector, int idArea, int idGuestbook, int idUserAvatar, NewMedia m, Supplier<InputStream> inputStreamSupplier) throws SQLException, IOException, InterruptedException {
		logger.debug("addNewMedia(authUserId={}, idProblem={}, pitch={}, trivia={}, idSector={}, idArea={}, idGuestbook={}, idUserAvatar={}, m={}) initialized", authUserId, idProblem, pitch, trivia, idSector, idArea, idGuestbook, idUserAvatar, m);
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		final boolean isEmbed = !Strings.isNullOrEmpty(m.embedVideoUrl());
		final StorageType storageType = isEmbed ? StorageType.MP4 : StorageType.fromFilename(m.name()).orElseThrow(() -> new IllegalArgumentException("Unsupported file extension for " + m.name()));
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0 && idGuestbook == 0 && idUserAvatar == 0)
				|| (idProblem == 0 && idSector > 0 && idArea == 0 && idGuestbook == 0 && idUserAvatar == 0)
				|| (idProblem == 0 && idSector == 0 && idArea > 0 && idGuestbook == 0 && idUserAvatar == 0)
				|| (idProblem == 0 && idSector == 0 && idArea == 0 && idGuestbook > 0 && idUserAvatar == 0)
				|| (idProblem == 0 && idSector == 0 && idArea == 0 && idGuestbook == 0 && idUserAvatar > 0 && !storageType.isMovie()));
		int idMedia = -1;
		boolean alreadyExistsInDb = false;
		if (isEmbed) {
			// Embed video url
			Objects.requireNonNull(m.embedThumbnailUrl(), "embedThumbnailUrl required");
			Objects.requireNonNull(m.embedVideoUrl(), "embedVideoUrl required");
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
		}

		/**
		 * DB
		 */
		if (!alreadyExistsInDb) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, embed_url) VALUES (?, ?, ?, ?, NOW(), ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setBoolean(1, storageType.isMovie());
				ps.setString(2, storageType.getExtension());
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
		}
		else if (idSector > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_sector (media_id, sector_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idSector);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		}
		else if (idArea > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_area (media_id, area_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idArea);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		}
		else if (idGuestbook > 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idGuestbook);
				ps.execute();
			}
		}
		else if (idUserAvatar > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE user SET media_id=? WHERE id=?")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idUserAvatar);
				ps.execute();
			}
		}
		else {
			throw new RuntimeException("Server error");
		}
		if (!alreadyExistsInDb) {
			if (m.inPhoto() != null && !m.inPhoto().isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
					for (User u : m.inPhoto()) {
						ps.setInt(1, idMedia);
						ps.setInt(2, getExistingOrInsertUser(c, u.name()));
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			if (storageType.isMovie() && !isEmbed) {
				Path tempFile = Files.createTempFile("Temp_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis(), ".tmp");
				try {
					try (InputStream is = inputStreamSupplier.get()) {
						copyWithLimit(is, tempFile, MAX_VIDEO_UPLOAD_BYTES);
					}
					StorageManager.getInstance().uploadFile(S3KeyGenerator.getOriginalMp4(idMedia), tempFile, StorageType.MP4); // Save with mime type mp4, the input might have been mov
					final int id = idMedia;
					Server.runAsync(() -> {
						try {
							VideoHelper.processVideo(id);
						} catch (Exception e) {
							logger.error("Failed to run async video processing for id=" + id, e);
						}
					});
				} finally {
					Files.deleteIfExists(tempFile);
				}
			}
			else if (isEmbed) {
				ImageHelper.saveImageFromEmbedVideo(this, c, idMedia, m.embedVideoUrl());
			}
			else {
				try (InputStream is = inputStreamSupplier.get()) {
					byte[] bytes = readBytesWithLimit(is, MAX_IMAGE_UPLOAD_BYTES);
					ImageHelper.saveImage(this, c, idMedia, bytes);
				}
			}
		}
		return idMedia;
	}

	private int addUser(Connection c, String email, String firstname, String lastname) throws SQLException {
		int id = -1;
		try (PreparedStatement ps = c.prepareStatement("INSERT INTO user (firstname, lastname) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, firstname);
			ps.setString(2, lastname);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					id = rst.getInt(1);
					logger.debug("addUser(email={}, firstname={}, lastname={}) - getInt(1)={}", email, firstname, lastname, id);
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
		return id;
	}

	private void copyWithLimit(InputStream is, Path targetPath, long maxBytes) throws IOException {
		try (OutputStream os = Files.newOutputStream(targetPath)) {
			byte[] buffer = new byte[16 * 1024];
			long total = 0;
			int read;
			while ((read = is.read(buffer)) != -1) {
				total += read;
				Preconditions.checkArgument(total <= maxBytes, "File too large (max %s bytes)", maxBytes);
				os.write(buffer, 0, read);
			}
		}
	}

	private boolean ensureAdminOrMediaUpdatedByMe(Connection c, Setup setup, Optional<Integer> authUserId, int idMedia) throws SQLException {
		Media m = getMedia(c, authUserId, idMedia);
		if (!m.uploadedByMe()) {
			ensureAdminWriteRegion(c, setup, authUserId);
		}
		return true;
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

	private void ensureAdminWriteRegion(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureSuperadminWriteRegion(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
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
		int usId = addUser(c, null, name, null);
		Preconditions.checkArgument(usId > 0);
		return usId;
	}

	private List<ExternalLink> getExternalLinksArea(Connection c, int areaId, boolean inherited) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
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
		logger.debug("getExternalLinksArea(sectorId={}, areaId={}) - res.size()={}, duration={}", areaId, inherited, res.size(), stopwatch);
		return res;
	}

	private List<ExternalLink> getExternalLinksProblem(Connection c, int problemId, boolean inherited) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
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
		logger.debug("getExternalLinksProblem(sectorId={}, problemId={}) - res.size()={}, duration={}", problemId, inherited, res.size(), stopwatch);
		return res;
	}

	private List<ExternalLink> getExternalLinksSector(Connection c, int sectorId, boolean inherited) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
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
		logger.debug("getExternalLinksSector(sectorId={}, inherited={}) - res.size()={}, duration={}", sectorId, inherited, res.size(), stopwatch);
		return res;
	}

	private Map<Integer, String> getFaAidNamesOnSectors(Connection c, int optAreaId, int optSectorId) throws SQLException {
		Preconditions.checkArgument((optAreaId == 0 && optSectorId > 0) || (optAreaId > 0 && optSectorId == 0));
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, String> res = new HashMap<>();
		String sql = """
				WITH req AS (
				    SELECT ? area_id, ? sector_id
				)
				SELECT p.id,
				       GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) 
				       ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa
				FROM req
				JOIN sector s ON (req.area_id > 0 AND s.area_id = req.area_id) OR (req.sector_id > 0 AND s.id = req.sector_id)
				JOIN problem p ON s.id = p.sector_id
				JOIN fa_aid_user a ON p.id = a.problem_id
				JOIN user u ON a.user_id = u.id
				GROUP BY p.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, optAreaId);
			ps.setInt(2, optSectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.put(rst.getInt("id"), rst.getString("fa"));
				}
			}
		}
		logger.debug("getFaAidNamesOnSectors(optAreaId={}, optSectorId={}) - res.size()={}, duration={}", optAreaId, optSectorId, res.size(), stopwatch);
		return res;
	}

	private List<Grade> getGrades(Connection c, int gradeSystemId) throws SQLException {
		List<Grade> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT g.id, g.grade, g.label_compact, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				WHERE g.grade_system_id=?
				ORDER BY g.weight
				""")) {
			ps.setInt(1, gradeSystemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String grade = rst.getString("grade");
					String labelCompact = rst.getString("label_compact");
					String color = rst.getString("color");
					res.add(new Grade(id, grade, labelCompact, color));
				}
			}
		}
		return res;
	}

	private List<Media> getMediaArea(Connection c, Optional<Integer> authUserId, int id, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem) throws SQLException {
		List<Media> initialList = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp, m.description, a.name location, ma.trivia, m.width, m.height, m.is_movie, m.embed_url,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
				       GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged
				FROM media m
				JOIN media_area ma ON m.id=ma.media_id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN area a ON ma.area_id=a.id
				JOIN user c ON m.photographer_user_id=c.id
				LEFT JOIN media_user mu ON m.id=mu.media_id
				LEFT JOIN user u ON mu.user_id=u.id
				WHERE m.deleted_user_id IS NULL
				  AND ma.area_id=?
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ma.trivia, m.description, a.name, m.width, m.height, m.is_movie, m.embed_url, ma.sorting, m.date_created, m.date_taken, c.firstname, c.lastname
				ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id
				""")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; // Don't inherit trivia image
					}
					int idMedia = rst.getInt("id");
					ids.add(idMedia);
					MediaIdentity identity = new MediaIdentity(idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					MediaMetadata metadata = MediaMetadata.from(rst.getString("date_created"), rst.getString("date_taken"), rst.getString("capturer"), rst.getString("tagged"), rst.getString("description"), rst.getString("location"));
					initialList.add(new Media(identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), 0, trivia, rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, metadata, rst.getString("embed_url"), inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, null));
				}
			}
		}
		if (initialList.isEmpty()) {
			return initialList;
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, ids);
		return initialList.stream().map(m -> m.withMediaSvgs(svgMap.get(m.identity().id()))).toList();
	}

	private List<Media> getMediaGuestbook(Connection c, Optional<Integer> authUserId, int guestbookId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> initialList = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
				       TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged
				FROM guestbook g
				JOIN media_guestbook mg ON g.id=mg.guestbook_id
				JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL)
				JOIN problem p ON g.problem_id=p.id
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN user c ON m.photographer_user_id=c.id
				LEFT JOIN media_user mu ON m.id=mu.media_id
				LEFT JOIN user u ON mu.user_id=u.id
				WHERE g.id=?
				GROUP BY m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.uploader_user_id, m.updated_at, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname
				ORDER BY m.is_movie, m.embed_url, m.id
				""")) {
			ps.setInt(1, guestbookId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					ids.add(idMedia);
					MediaIdentity identity = new MediaIdentity(idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					MediaMetadata metadata = MediaMetadata.from(rst.getString("date_created"), rst.getString("date_taken"), rst.getString("capturer"), rst.getString("tagged"), rst.getString("description"), rst.getString("location"));
					initialList.add(new Media(identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), 0, false, rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie") ? 2 : 1, null, null, 0, null, metadata, rst.getString("embed_url"), false, 0, 0, 0, null));
				}
			}
		}
		if (initialList.isEmpty()) {
			return initialList;
		}
		Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, ids);
		var res = initialList.stream().map(m -> m.withMediaSvgs(svgMap.get(m.identity().id()))).toList();
		logger.debug("getMediaGuestbook(guestbookId={}) - res.size={}, duration={}", guestbookId, res.size(), stopwatch);
		return res;
	}

	private List<Media> getMediaProblem(Connection c, Setup s, Optional<Integer> authUserId, int areaId, int sectorId, int problemId, boolean showHiddenMedia) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> media = getMediaSector(c, s, authUserId, sectorId, problemId, true, areaId, 0, problemId, showHiddenMedia);
		List<Media> pMediaList = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       CONCAT(p.name,' (',a.name,'/',s.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, mp.trivia, ROUND(mp.milliseconds/1000) t,
				       TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged
				FROM problem p
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN media_problem mp ON p.id=mp.problem_id
				JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL)
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN user c ON m.photographer_user_id=c.id
				LEFT JOIN media_user mu ON m.id=mu.media_id
				LEFT JOIN user u ON mu.user_id=u.id
				WHERE p.id=?
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, p.name, s.name, a.name, m.description, m.width, m.height, m.is_movie, m.embed_url, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.trivia, mp.milliseconds, c.firstname, c.lastname
				ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id
				""")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					ids.add(idMedia);
					String embedUrl = rst.getString("embed_url");
					String t = rst.getString("t");
					if (embedUrl != null) {
						long seconds = Long.parseLong(t);
						if (seconds > 0) {
							if (embedUrl.contains("youtu")) embedUrl += "?start=" + seconds;
							else embedUrl += "#t=" + seconds + "s";
						}
					}
					MediaIdentity identity = new MediaIdentity(idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					MediaMetadata metadata = MediaMetadata.from(rst.getString("date_created"), rst.getString("date_taken"), rst.getString("capturer"), rst.getString("tagged"), rst.getString("description"), rst.getString("location"));
					pMediaList.add(new Media(identity, rst.getInt("uploader_user_id") == authUserId.orElse(0), rst.getInt("pitch"), rst.getBoolean("trivia"), rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie") ? 2 : 1, t, null, problemId, null, metadata, embedUrl, false, 0, sectorId, 0, null));
				}
			}
		}
		if (!pMediaList.isEmpty()) {
			Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, ids);
			Map<Integer, List<Svg>> svgsMap = getSvgs(c, authUserId, ids);
			for (Media m : pMediaList) {
				List<Svg> svgs = svgsMap.get(m.identity().id());
				if (media == null) {
					media = new ArrayList<>();
				}
				media.add(m.withMediaSvgs(svgMap.get(m.identity().id())).withSvgs(svgs, (svgs == null || svgs.isEmpty() ? areaId : 0)));
			}
		}
		if (media != null && media.isEmpty()) {
			media = null;
		}
		logger.debug("getMediaProblem(areaId={}, sectorId={}, problemId={}, showHiddenMedia={}) - media.size()={}, duration={}", areaId, sectorId, problemId, showHiddenMedia, media == null ? 0 : media.size(), stopwatch);
		return media;
	}

	private List<Media> getMediaSector(Connection c, Setup s, Optional<Integer> authUserId, int idSector, int optionalIdProblem, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem, boolean showHiddenMedia) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> initialList = new ArrayList<>();
		List<Integer> ids = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp,
				       ms.trivia, CONCAT(s.name,' (',a.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer,
				       GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged
				FROM sector s
				JOIN area a ON a.id=s.area_id
				JOIN media_sector ms ON s.id=ms.sector_id
				JOIN media m ON ms.media_id=m.id AND m.deleted_user_id IS NULL
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN user c ON m.photographer_user_id=c.id
				LEFT JOIN media_user mu ON m.id=mu.media_id
				LEFT JOIN user u ON mu.user_id=u.id
				WHERE s.id=?
				GROUP BY m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ms.trivia, m.description, s.name, a.name, m.width, m.height, m.is_movie, m.embed_url, ms.sorting, m.date_created, m.date_taken, c.firstname, c.lastname
				ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id
				""")) {
			ps.setInt(1, idSector);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) continue; // Don't inherit trivia image
					int idMedia = rst.getInt("id");
					ids.add(idMedia);
					MediaIdentity identity = new MediaIdentity(idMedia, rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					MediaMetadata metadata = MediaMetadata.from(rst.getString("date_created"), rst.getString("date_taken"), rst.getString("capturer"), rst.getString("tagged"), rst.getString("description"), rst.getString("location"));
					initialList.add(new Media(identity, rst.getInt("uploader_user_id") == currentAuthUserId, 0, trivia, rst.getInt("width"), rst.getInt("height"), rst.getBoolean("is_movie") ? 2 : 1, null, null, optionalIdProblem, null, metadata, rst.getString("embed_url"), inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, null));
				}
			}
		}
		List<Media> allMedia = new ArrayList<>();
		if (!initialList.isEmpty()) {
			Map<Integer, List<MediaSvgElement>> svgMap = getMediaSvgElements(c, ids);
			Map<Integer, List<Svg>> svgsMap = getSvgs(c, authUserId, ids);
			Set<Media> mediaWithRequestedTopoLine = new HashSet<>();
			for (Media m : initialList) {
				List<Svg> svgs = svgsMap.get(m.identity().id());
				Media fullMedia = m.withMediaSvgs(svgMap.get(m.identity().id()))
						.withSvgs(svgs, (svgs == null || svgs.isEmpty() ? enableMoveToIdArea : 0));
				// Update logic based on svgs
				fullMedia = new Media(fullMedia.identity(), fullMedia.uploadedByMe(), fullMedia.pitch(), fullMedia.trivia(), fullMedia.width(), fullMedia.height(), fullMedia.idType(), fullMedia.t(), fullMedia.mediaSvgs(), fullMedia.svgProblemId(), fullMedia.svgs(), fullMedia.mediaMetadata(), fullMedia.embedUrl(), fullMedia.inherited(), fullMedia.enableMoveToIdArea(), fullMedia.enableMoveToIdSector(), (svgs == null || svgs.stream().filter(x -> x.problemId() != enableMoveToIdProblem).findAny().isEmpty() ? enableMoveToIdProblem : 0), null);
				if (optionalIdProblem != 0 && svgs != null && svgs.stream().anyMatch(svg -> svg.problemId() == optionalIdProblem)) {
					mediaWithRequestedTopoLine.add(fullMedia);
				}
				allMedia.add(fullMedia);
			}
			// Figure out what to actually return
			if (!showHiddenMedia && !mediaWithRequestedTopoLine.isEmpty()) {
				// Only images without topo lines or images with topo lines for this problem
				allMedia = allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty() || mediaWithRequestedTopoLine.contains(m)).collect(Collectors.toList());
			}
			else if (!showHiddenMedia && s.isBouldering() && optionalIdProblem != 0) {
				// In bouldering we don't want to show all rocks with lines if this one does not have a line
				allMedia = allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty()).collect(Collectors.toList());
			}
		}
		logger.debug("getMediaSector(idSector={}, optionalIdProblem={}, inherited={}, enableMoveToIdArea={}, enableMoveIdSector={}, enableMoveIdProblem={}, showHiddenMedia={}) - allMedia.size()={}, duration={}", idSector, optionalIdProblem, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, showHiddenMedia, allMedia.size(), stopwatch);
		return allMedia;
	}

	private Map<Integer, List<MediaSvgElement>> getMediaSvgElements(Connection c, Collection<Integer> mediaIds) throws SQLException {
		if (mediaIds == null || mediaIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, List<MediaSvgElement>> res = new HashMap<>();
		String inClause = Joiner.on(",").join(mediaIds);
		String sql = "SELECT ms.media_id, ms.id, ms.path, ms.rappel_x, ms.rappel_y, ms.rappel_bolted FROM media_svg ms WHERE ms.media_id IN (" + inClause + ")";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int mediaId = rst.getInt("media_id");
					int id = rst.getInt("id");
					String path = rst.getString("path");
					MediaSvgElement element = (path != null) 
							? MediaSvgElement.fromPath(id, path) 
									: MediaSvgElement.fromRappel(id, rst.getInt("rappel_x"), rst.getInt("rappel_y"), rst.getBoolean("rappel_bolted"));
					res.computeIfAbsent(mediaId, _ -> new ArrayList<>()).add(element);
				}
			}
		}
		logger.debug("getMediaSvgElements(mediaIds.size()={}) - res.size={}, duration={}", mediaIds.size(), res.size(), stopwatch);
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
		Stopwatch stopwatch = Stopwatch.createStarted();
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
		logger.debug("getSectorOutlines(idSectors.size()={}) - res.size()={}, duration={}", idSectors.size(), res.size(), stopwatch);
		return res;
	}

	private Multimap<Integer, SectorProblem> getSectorProblems(Connection c, Setup setup, Optional<Integer> authUserId, int optAreaId, int optSectorId) throws SQLException {
		Preconditions.checkArgument((optAreaId == 0 && optSectorId > 0) || optAreaId > 0 && optSectorId == 0);
		Stopwatch stopwatch = Stopwatch.createStarted();
		Multimap<Integer, SectorProblem> res = LinkedListMultimap.create();
		Map<Integer, String> problemIdFirstAidAscentLookup = null;
		if (!setup.isBouldering()) {
			problemIdFirstAidAscentLookup = getFaAidNamesOnSectors(c, optAreaId, optSectorId);
		}
		String sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? area_id, ? sector_id
				),
				filtered_problems AS (
				    SELECT p.*, ur.admin_read, ur.superadmin_read
				    FROM problem p
				    CROSS JOIN req
				    JOIN sector s ON s.id = p.sector_id
				    JOIN area a ON s.area_id = a.id
				    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				    WHERE ((req.area_id > 0 AND a.id = req.area_id) OR (req.sector_id > 0 AND p.sector_id = req.sector_id))
				      AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				),
				fa_agg AS (
				    SELECT f.problem_id,
				           GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_names,
				           MAX(CASE WHEN u.id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_is_fa
				    FROM fa f
				    JOIN user u ON f.user_id = u.id
				    WHERE f.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY f.problem_id
				),
				tick_agg AS (
				    SELECT t.problem_id,
				           COUNT(t.id) AS total_ticks,
				           ROUND(AVG(NULLIF(t.stars, -1)), 1) AS avg_stars,
				           MAX(CASE WHEN t.user_id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_ticked
				    FROM tick t
				    WHERE t.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY t.problem_id
				),
				media_agg AS (
				    SELECT mp.problem_id,
				           COUNT(DISTINCT CASE WHEN m.is_movie = 0 THEN m.id END) AS num_images,
				           COUNT(DISTINCT CASE WHEN m.is_movie = 1 THEN m.id END) AS num_movies
				    FROM media_problem mp
				    JOIN media m ON mp.media_id = m.id
				    WHERE mp.trivia = 0 AND m.deleted_user_id IS NULL
				    AND mp.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY mp.problem_id
				)
				SELECT p.sector_id, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.fa_date,
				       fa.fa_names AS fa,
				       ty.id AS type_id, ty.type, ty.subtype,
				       g.weight AS problem_grade_weight, g.grade AS problem_grade,
				       COALESCE(t.total_ticks, 0) AS total_ticks,
				       COALESCE(t.avg_stars, 0) AS stars,
				       GREATEST(COALESCE(t.user_ticked, 0), COALESCE(fa.user_is_fa, 0)) AS ticked,
				       CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END AS todo,
				       gb.danger,
				       p.length_meter,
				       co.id AS coordinates_id, co.latitude, co.longitude, co.elevation, co.elevation_source,
				       (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p.id) AS num_pitches,
				       COALESCE(m.num_images, 0) AS num_images,
				       COALESCE(m.num_movies, 0) AS num_movies,
				       CASE WHEN EXISTS (SELECT 1 FROM svg WHERE svg.problem_id = p.id) THEN 1 ELSE 0 END AS has_topo
				FROM filtered_problems p
				JOIN grade g ON p.consensus_grade_id = g.id
				JOIN type ty ON p.type_id = ty.id
				LEFT JOIN coordinates co ON p.coordinates_id = co.id
				LEFT JOIN fa_agg fa ON p.id = fa.problem_id
				LEFT JOIN tick_agg t ON p.id = t.problem_id
				LEFT JOIN media_agg m ON p.id = m.problem_id
				LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = (SELECT auth_user_id FROM req)
				LEFT JOIN LATERAL (
				    SELECT danger 
				    FROM guestbook 
				    WHERE problem_id = p.id 
				    AND (danger = 1 OR resolved = 1)
				    ORDER BY id DESC
				    LIMIT 1
				) gb ON TRUE
				ORDER BY p.nr
				         """;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, optAreaId);
			ps.setInt(3, optSectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					int id = rst.getInt("id");
					String fa = rst.getString("fa");
					if (problemIdFirstAidAscentLookup != null && problemIdFirstAidAscentLookup.containsKey(id)) {
						String faAid = "FA: " + problemIdFirstAidAscentLookup.get(id);
						fa = (fa == null) ? faAid : faAid + ". FFA: " + fa;
					}
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					LocalDate faDate = rst.getObject("fa_date", LocalDate.class);
					String faDateStr = faDate == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format(faDate);
					var p = new SectorProblem(id, rst.getString("broken"), rst.getBoolean("locked_admin"), rst.getBoolean("locked_superadmin"),
							rst.getInt("nr"), rst.getString("name"), rst.getString("rock"), rst.getString("description"),
							rst.getInt("problem_grade_weight"), rst.getString("problem_grade"), fa, faDateStr, rst.getInt("length_meter"),
							rst.getInt("num_pitches"), rst.getInt("num_images") > 0, rst.getInt("num_movies") > 0, rst.getBoolean("has_topo"),
							coordinates, rst.getInt("total_ticks"), rst.getDouble("stars"), rst.getBoolean("ticked"), rst.getBoolean("todo"),
							new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype")), rst.getBoolean("danger")
							);
					res.put(sectorId, p);
				}
			}
		}
		logger.debug("getSectorProblems(optAreaId={}, optSectorId={}) - res.size()={}, duration={}", optAreaId, optSectorId, res.size(), stopwatch);
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

	private Map<Integer, List<Svg>> getSvgs(Connection c, Optional<Integer> authUserId, Collection<Integer> mediaIds) throws SQLException {
		if (mediaIds == null || mediaIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, List<Svg>> res = new HashMap<>();
		String inClause = Joiner.on(",").join(mediaIds);
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id
				)
				SELECT 
				    svg.media_id,
				    p.id problem_id, 
				    p.name problem_name,
				    g.grade problem_grade, 
				    g.weight problem_grade_weight, 
				    clr.hex_code problem_grade_color, 
				    ty.subtype problem_subtype, 
				    p.nr, 
				    ps.nr pitch, 
				    g_sect.grade problem_section_grade, 
				    clr_sect.hex_code problem_section_grade_color,
				    svg.id, 
				    svg.path, 
				    svg.has_anchor, 
				    svg.texts, 
				    svg.anchors, 
				    svg.trad_belay_stations, 
				    CASE WHEN p.type_id IN (1,2) THEN 1 ELSE 0 END prim,
				    MAX(CASE WHEN tk.user_id = req.auth_user_id OR fa.user_id IS NOT NULL THEN 1 ELSE 0 END) is_ticked, 
				    CASE WHEN t2.id IS NOT NULL THEN 1 ELSE 0 END is_todo, 
				    IFNULL(danger.danger, 0) is_dangerous
				FROM req
				JOIN svg ON svg.media_id IN (%s)
				JOIN problem p ON svg.problem_id = p.id
				JOIN grade g ON p.consensus_grade_id = g.id
				JOIN grade_color clr ON g.grade_color_id = clr.id
				JOIN type ty ON p.type_id = ty.id
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				LEFT JOIN fa ON p.id = fa.problem_id AND fa.user_id = req.auth_user_id
				LEFT JOIN problem_section ps ON ps.problem_id = p.id AND ps.nr = svg.pitch
				LEFT JOIN grade g_sect ON ps.grade_id = g_sect.id
				LEFT JOIN grade_color clr_sect ON g_sect.grade_color_id = clr_sect.id
				LEFT JOIN tick tk ON p.id = tk.problem_id
				LEFT JOIN todo t2 ON p.id = t2.problem_id AND t2.user_id = req.auth_user_id
				LEFT JOIN (
				    SELECT problem_id, danger 
				    FROM guestbook 
				    WHERE (danger = 1 OR resolved = 1) 
				    AND id IN (SELECT MAX(id) FROM guestbook WHERE (danger = 1 OR resolved = 1) GROUP BY problem_id)
				) danger ON p.id = danger.problem_id
				LEFT JOIN user_region ur ON ur.user_id = req.auth_user_id AND ur.region_id = a.region_id
				WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY 
				    svg.media_id, p.id, p.name, g.grade, g.weight, clr.hex_code, ty.subtype, p.nr, ps.id, ps.nr, 
				    g_sect.grade, clr_sect.hex_code, svg.id, svg.path, svg.has_anchor, 
				    svg.texts, svg.anchors, svg.trad_belay_stations, t2.id, danger.danger
				ORDER BY svg.media_id, p.nr
				""".formatted(inClause);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int mediaId = rst.getInt("media_id");
					int pitch = rst.getInt("pitch");

					Svg svg = new Svg(
							false, 
							rst.getInt("id"),
							rst.getInt("problem_id"), 
							rst.getString("problem_name"), 
							(pitch == 0) ? rst.getString("problem_grade") : rst.getString("problem_section_grade"), 
									(pitch == 0) ? rst.getString("problem_grade_color") : rst.getString("problem_section_grade_color"), 
											rst.getString("problem_subtype"), 
											rst.getInt("nr"), 
											pitch,
											rst.getString("path"), 
											rst.getBoolean("has_anchor"), 
											rst.getString("texts"), 
											rst.getString("anchors"), 
											rst.getString("trad_belay_stations"), 
											rst.getBoolean("prim"), 
											rst.getBoolean("is_ticked"), 
											rst.getBoolean("is_todo"), 
											rst.getBoolean("is_dangerous")
							);
					res.computeIfAbsent(mediaId, _ -> new ArrayList<>()).add(svg);
				}
			}
		}
		logger.debug("getSvgs(mediaIds.size={}) - res.size={}, duration={}", mediaIds.size(), res.size(), stopwatch);
		return res;
	}

	private void loadSimplifiedGradeCounts(Connection c, Optional<Integer> authUserId, int areaId, Map<Integer, Area.AreaSector> sectorLookup) throws SQLException {
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? area_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN area a ON a.id = req.area_id
				  JOIN region_type rt ON a.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				),
				all_labels AS (
				  SELECT 
				    g.label_compact, 
				    g.grade_system_id, 
				    clr.hex_code, 
				    MIN(g.weight) as sort_weight
				  FROM grade g
				  JOIN target_systems ts ON g.grade_system_id = ts.grade_system_id
				  JOIN grade_color clr ON g.grade_color_id = clr.id
				  GROUP BY g.label_compact, g.grade_system_id, clr.hex_code
				)
				SELECT 
				    s.id as sector_id, 
				    al.label_compact, 
				    al.hex_code as color, 
				    COUNT(p.id) as num
				FROM req
				JOIN sector s ON s.area_id = req.area_id
				CROSS JOIN all_labels al
				LEFT JOIN user_region ur ON ur.user_id = req.auth_user_id AND ur.region_id = (SELECT region_id FROM area WHERE id = req.area_id)
				LEFT JOIN problem p ON s.id = p.sector_id 
				    AND EXISTS (
				        SELECT 1 FROM grade g_p 
				        WHERE p.consensus_grade_id = g_p.id 
				        AND g_p.label_compact = al.label_compact 
				        AND g_p.grade_system_id = al.grade_system_id
				    )
				    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				GROUP BY s.id, al.label_compact, al.hex_code, al.sort_weight
				ORDER BY s.id, al.sort_weight
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, areaId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					Area.AreaSector sector = sectorLookup.get(rst.getInt("sector_id"));
					if (sector != null) {
						if (sector.getGradeCounts() == null) sector.setGradeCounts(new ArrayList<>());
						sector.getGradeCounts().add(new Area.GradeCount(
								rst.getString("label_compact"), 
								rst.getString("color"), 
								rst.getInt("num")
								));
					}
				}
			}
		}
	}

	private byte[] readBytesWithLimit(InputStream is, long maxBytes) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[16 * 1024];
			long total = 0;
			int read;
			while ((read = is.read(buffer)) != -1) {
				total += read;
				Preconditions.checkArgument(total <= maxBytes, "File too large (max %s bytes)", maxBytes);
				os.write(buffer, 0, read);
			}
			return os.toByteArray();
		}
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

	private void updateProblemConsensusGrade(Connection c, int problemId) throws SQLException {
		String sql = """
				UPDATE problem p
				LEFT JOIN type_grade_system tgs ON p.type_id = tgs.type_id
				LEFT JOIN (
				    SELECT ROUND(AVG(w)) as avg_weight
				    FROM (
				        SELECT gt.weight as w
				        FROM tick t
				        JOIN grade gt ON t.grade_id = gt.id
				        WHERE t.problem_id = ? AND gt.grade != 'n/a'

				        UNION ALL

				        SELECT g.weight as w
				        FROM problem p_inner
				        JOIN grade g ON p_inner.grade_id = g.id
				        WHERE p_inner.id = ? AND g.grade != 'n/a'
				        AND NOT EXISTS (
				            SELECT 1 FROM tick t_check
				            JOIN fa f_check ON t_check.user_id = f_check.user_id
				            WHERE t_check.problem_id = p_inner.id 
				              AND f_check.problem_id = p_inner.id
				              AND t_check.grade_id = p_inner.grade_id
				        )
				    ) votes
				) calc ON 1=1
				LEFT JOIN grade g_final ON g_final.grade_system_id = tgs.grade_system_id 
				                       AND g_final.weight = calc.avg_weight
				SET p.consensus_grade_id = COALESCE(g_final.id, p.grade_id)
				WHERE p.id = ?
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, problemId);
			ps.setInt(2, problemId);
			ps.setInt(3, problemId);
			ps.executeUpdate();
		}
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
		List<Integer> idsToKeep = repeats == null ? List.of() : repeats.stream().filter(x -> x.id() > 0).map(TickRepeat::id).toList();
		if (idsToKeep.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM tick_repeat WHERE tick_id=?")) {
				ps.setInt(1, idTick);
				ps.execute();
			}
		} else {
			String placeholders = String.join(",", Collections.nCopies(idsToKeep.size(), "?"));
			String sqlStr = "DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (" + placeholders + ")";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idTick);
				for (int i = 0; i < idsToKeep.size(); i++) {
					ps.setInt(i + 2, idsToKeep.get(i));
				}
				ps.execute();
			}
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

	private void writeRow(ExcelWorkbook wb, Map<String, ExcelSheet> sheets, ResultSet rst, String sheetName) throws SQLException {
		ExcelSheet sheet = sheets.computeIfAbsent(sheetName, wb::addSheet);
		sheet.incrementRow();
		sheet.writeString("AREA", rst.getString("area_name"));
		sheet.writeString("SECTOR", rst.getString("sector_name"));
		String subType = rst.getString("subtype");
		if (subType != null) {
			sheet.writeString("TYPE", subType);
			int pitches = rst.getInt("num_pitches");
			sheet.writeInt("PITCHES", pitches > 0 ? pitches : 1);
		}
		sheet.writeString("NAME", rst.getString("name"));
		sheet.writeString("FIRST ASCENT", rst.getBoolean("fa") ? "Yes" : "No");
		sheet.writeDate("DATE", rst.getObject("date", LocalDate.class));
		sheet.writeString("GRADE", rst.getString("grade"));
		sheet.writeDouble("STARS", rst.getDouble("stars"));
		sheet.writeString("DESCRIPTION", rst.getString("comment"));
		sheet.writeHyperlink("URL", rst.getString("url"));
	}
}
package com.buldreinfo.dao;

import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.helpers.TimeAgo;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Activity;
import com.buldreinfo.model.MediaIdentity;

@Repository
public class ActivityRepository extends BaseRepository {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String ACTIVITY_TYPE_TICK_REPEAT = "TICK_REPEAT";
	private static final Logger logger = LogManager.getLogger();
	
	public ActivityRepository(ClimbingTransactionManager txManager) {
		super(txManager);
	}
	
	public void fillActivity(int idProblem) throws SQLException {
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("DELETE FROM activity WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}

		LocalDateTime problemActivityTimestamp = null;
		var faUserIds = new ArrayList<Integer>();
		var hasFa = false;
		try (var ps = c.prepareStatement("""
				SELECT p.fa_date, p.last_updated, f.user_id
				FROM problem p
				JOIN grade g ON p.grade_id=g.id
				LEFT JOIN fa f ON p.id=f.problem_id
				WHERE p.id=?
				  AND (g.grade!='n/a' OR f.user_id IS NOT NULL)
				""")) {
			ps.setInt(1, idProblem);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					if (!hasFa) {
						hasFa = true;
						var faDate = rst.getObject("fa_date", LocalDate.class);
						var lastUpdated = rst.getObject("last_updated", LocalDateTime.class);
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
		try (var psAddActivity = c.prepareStatement("INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id, tick_repeat_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
			if (hasFa) {
				psAddActivity.setObject(1, problemActivityTimestamp != null ? problemActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
				psAddActivity.setString(2, ACTIVITY_TYPE_FA);
				psAddActivity.setInt(3, idProblem);
				psAddActivity.setNull(4, Types.INTEGER);
				psAddActivity.setNull(5, Types.INTEGER);
				psAddActivity.setNull(6, Types.INTEGER);
				psAddActivity.setNull(7, Types.INTEGER);
				psAddActivity.addBatch();
			}

			try (var ps = c.prepareStatement("""
					SELECT m.id, m.date_created
					FROM media_problem mp
					JOIN media m ON mp.media_id = m.id
					WHERE mp.problem_id = ? AND m.deleted_timestamp IS NULL
					ORDER BY m.date_created ASC
					""")) {
				ps.setInt(1, idProblem);
				try (var rst = ps.executeQuery()) {
					record MediaItem(int id) {}
					var buffer = new ArrayList<MediaItem>();
					var groupAnchor = problemActivityTimestamp; 
					var latestInGroup = problemActivityTimestamp;
					while (rst.next()) {
						int id = rst.getInt("id");
						var current = rst.getObject("date_created", LocalDateTime.class);
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

			try (var ps = c.prepareStatement("""
					SELECT t.user_id, t.date, t.created,
					       ROW_NUMBER() OVER (PARTITION BY DAY(t.created) ORDER BY t.created) ix_on_created_date
					FROM tick t
					WHERE t.problem_id=?
					ORDER BY t.date, t.created
					""")) {
				ps.setInt(1, idProblem);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						int userId = rst.getInt("user_id");
						LocalDateTime tickActivityTimestamp = null;
						if (faUserIds.contains(userId) && problemActivityTimestamp != null) {
							tickActivityTimestamp = problemActivityTimestamp;
						}
						else {
							var tickDate = rst.getObject("date", LocalDate.class);
							var tickCreated = rst.getObject("created", LocalDateTime.class);
							if (tickDate != null && tickCreated != null) {
								if (tickCreated.toLocalDate().isAfter(tickDate)) {
									int ixOnCreatedDate = rst.getInt("ix_on_created_date");
									tickActivityTimestamp = tickDate.atTime(23, 59, Math.min(ixOnCreatedDate, 59));
								}
								else {
									tickActivityTimestamp = tickDate.atTime(tickCreated.getHour(), tickCreated.getMinute(), tickCreated.getSecond());
								}
							}
							else if (tickDate != null) {
								tickActivityTimestamp = tickDate.atStartOfDay();
							}
						}
						psAddActivity.setObject(1, tickActivityTimestamp != null ? tickActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
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

			try (var ps = c.prepareStatement("""
					SELECT r.id, t.user_id, r.date, r.created
					FROM tick t
					JOIN tick_repeat r ON t.id=r.tick_id
					WHERE t.problem_id=?
					ORDER BY r.tick_id, r.date, r.id
					""")) {
				ps.setInt(1, idProblem);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						int userId = rst.getInt("user_id");
						var tickDate = rst.getObject("date", LocalDate.class);
						var tickCreated = rst.getObject("created", LocalDateTime.class);
						LocalDateTime tickRepeatActivityTimestamp = null;
						if (tickDate != null && tickCreated != null) {
							tickRepeatActivityTimestamp = tickDate.atTime(tickCreated.getHour(), tickCreated.getMinute(), tickCreated.getSecond());
						}
						else if (tickDate != null) {
							tickRepeatActivityTimestamp = tickDate.atStartOfDay();
						}
						psAddActivity.setObject(1, tickRepeatActivityTimestamp != null ? tickRepeatActivityTimestamp : LocalDate.EPOCH.atStartOfDay());
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

			try (var ps = c.prepareStatement("""
					SELECT g.id, g.post_time
					FROM guestbook g
					WHERE g.problem_id=?
					ORDER BY g.post_time
					""")) {
				ps.setInt(1, idProblem);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						var postTime = rst.getObject("post_time", LocalDateTime.class);
						psAddActivity.setObject(1, postTime != null ? postTime : LocalDate.EPOCH.atStartOfDay());
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

			psAddActivity.executeBatch();
		}
	}

	public List<Activity> getActivity(Setup setup, Optional<Integer> authUserId, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media, int offset) throws SQLException {
		var res = new ArrayList<Activity>();
		var faIds = new HashSet<Integer>();
		var tickIds = new HashSet<Integer>();
		var repeatIds = new HashSet<Integer>();
		var mediaIds = new HashSet<Integer>();
		var gbIds = new HashSet<Integer>();
		var showAllTime = offset > 0 || lowerGrade > 0 || !fa || !comments || !ticks || !media || idArea > 0 || idSector > 0;
		var c = txManager.getConnection();
		var sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id, ? show_all_time,
				         ? hide_fa, ? hide_guestbook, ? hide_ticks, ? hide_media, ? min_grade_weight,
				         ? filter_area_id, ? filter_sector_id
				),
				x AS (
				  SELECT a1.id, a1.type, a1.activity_timestamp, a1.problem_id 
				  FROM req
				  CROSS JOIN activity a1
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
				FROM req
				CROSS JOIN x
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY 
				    x.activity_timestamp, a.id, s.id, x.problem_id, p.name, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, x.problem_id DESC
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			var ix = 1;
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
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var ts = rst.getObject("activity_timestamp", LocalDateTime.class);
					var currentActivityIds = new HashSet<Integer>();
					var raw = rst.getString("activities");
					if (raw != null) {
						for (var entry : raw.split(",")) {
							var parts = entry.split("-");
							int id = Integer.parseInt(parts[0]);
							var type = parts[1];
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
		var activityLookup = new HashMap<Integer, Activity>();
		for (var act : res) {
			for (var id : act.getActivityIds()) {
				activityLookup.put(id, act);
			}
		}
		if (!tickIds.isEmpty()) {
			try (var ps = c.prepareStatement("""
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
					ORDER BY u.firstname, u.lastname
					""".formatted(tickIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							var name = rst.getString("name");
							var grade = rst.getString("grade");
							if (grade == null) {
								grade = GradeConverter.NO_PERSONAL_GRADE;
							}
							a.setTick(false, userId, name, rst.getString("description"), rst.getInt("stars"), grade);
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!repeatIds.isEmpty()) {
			try (var ps = c.prepareStatement("""
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
					ORDER BY u.firstname, u.lastname
								""".formatted(repeatIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							var name = rst.getString("name");
							var grade = rst.getString("grade");
							if (grade == null) {
								grade = GradeConverter.NO_PERSONAL_GRADE;
							}
							a.setTick(true, userId, name, rst.getString("description"), rst.getInt("stars"), grade);
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!gbIds.isEmpty()) {
			try (var ps = c.prepareStatement("""
					SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       ma.id avatar_media_id, UNIX_TIMESTAMP(ma.updated_at) avatar_version_stamp, mama.focus_x avatar_focus_x, mama.focus_y avatar_focus_y, mama.primary_color_hex avatar_primary_color_hex,
					       g.message,
					       mg.media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360, m.embed_url
					FROM activity a
					JOIN guestbook g ON a.guestbook_id=g.id
					JOIN user u ON g.user_id=u.id
					LEFT JOIN media ma ON u.media_id=ma.id
					LEFT JOIN media_ml_analysis mama ON ma.id=mama.media_id
					LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id
					LEFT JOIN media m ON mg.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE a.id IN (%s)
					  AND m.deleted_user_id IS NULL
					""".formatted(gbIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							int userId = rst.getInt("user_id");
							var name = rst.getString("name");
							a.setGuestbook(userId, name, rst.getString("message"));
							int mediaId = rst.getInt("media_id");
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.addMedia(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), rst.getBoolean("is_movie"), rst.getBoolean("is_360"), rst.getString("embed_url"));
							}
							int avatarMediaId = rst.getInt("avatar_media_id");
							if (avatarMediaId > 0) {
								long avatarMediaVersionStamp = rst.getLong("avatar_version_stamp");
								int avatarFocusX = rst.getInt("avatar_focus_x");
								int avatarFocusY = rst.getInt("avatar_focus_y");
								var avatarPrimaryColorHex = rst.getString("avatar_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(avatarMediaId, avatarMediaVersionStamp, avatarFocusX, avatarFocusY, avatarPrimaryColorHex), userId, name);
							}
						}
					}
				}
			}
		}
		if (!faIds.isEmpty()) {
			try (var ps = c.prepareStatement("""
					SELECT a.id, p.description, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
					FROM activity a
					JOIN problem p ON a.problem_id=p.id
					LEFT JOIN fa ON p.id=fa.problem_id
					LEFT JOIN user u ON fa.user_id=u.id
					LEFT JOIN media m ON u.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE a.id IN (%s)
					ORDER BY u.firstname, u.lastname
					""".formatted(faIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							var description = rst.getString("description");
							if (description != null) {
								if (a.getDescription() == null) {
									a.setDescription(description);
								}
								else if (!a.getDescription().contains(description)) {
									a.setDescription(a.getDescription() + " (" + description + ")");
								}
							}
							int userId = rst.getInt("user_id");
							var name = rst.getString("name");
							int mediaId = rst.getInt("media_id");
							MediaIdentity mediaIdentity = null;
							if (mediaId > 0) {
								long mediaVersionStamp = rst.getLong("media_version_stamp");
								int mediaFocusX = rst.getInt("media_focus_x");
								int mediaFocusY = rst.getInt("media_focus_y");
								var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
								a.appendActivityThumbnail(new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex), userId, name);
							}
							a.addUser(userId, name, mediaIdentity);
						}
					}
				}
			}
			if (!media) {
				var problemIds = res.stream()
						.filter(a -> a.getActivityIds().stream().anyMatch(faIds::contains))
						.map(Activity::getProblemId)
						.collect(Collectors.toSet());
				if (!problemIds.isEmpty()) {
					try (var ps = c.prepareStatement("""
							SELECT mp.problem_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, 
							       mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360, m.embed_url
							FROM media_problem mp
							JOIN media m ON mp.media_id = m.id
							LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
							WHERE mp.problem_id IN (%s) AND m.deleted_user_id IS NULL
							ORDER BY mp.sorting
							""".formatted(problemIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
						try (var rst = ps.executeQuery()) {
							while (rst.next()) {
								int pId = rst.getInt("problem_id");
								var activities = res.stream()
										.filter(act -> act.getProblemId() == pId && act.getActivityIds().stream().anyMatch(faIds::contains))
										.toList();
								for (var a : activities) {
									var mediaIdentity = new MediaIdentity(rst.getInt("media_id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
									a.addMedia(mediaIdentity, rst.getBoolean("is_movie"), rst.getBoolean("is_360"), rst.getString("embed_url"));
								}
							}
						}
					}
				}
			}
		}
		if (!mediaIds.isEmpty()) {
			try (var ps = c.prepareStatement("""
					SELECT a.id,
					       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
					       m.is_movie, m.is_360, m.embed_url,
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
					""".formatted(mediaIds.stream().map(String::valueOf).collect(Collectors.joining(","))))) {
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var a = activityLookup.get(rst.getInt("id"));
						if (a != null) {
							var mediaIdentity = new MediaIdentity(rst.getInt("media_id"), rst.getLong("version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
							a.addMedia(mediaIdentity, rst.getBoolean("is_movie"), rst.getBoolean("is_360"), rst.getString("embed_url"));
							if (a.getUsers() == null || a.getUsers().isEmpty()) {
								a.appendActivityThumbnail(new MediaIdentity(rst.getInt("photographer_media_id"), rst.getLong("photographer_version_stamp"), 0, 0, null), rst.getInt("user_id"), rst.getString("name"));
							}
						}
					}
				}
			}
		}
		logger.debug("getActivity(offset={}) - res.size()={}", offset, res.size());
		return res;
	}
}
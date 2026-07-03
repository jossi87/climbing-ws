package com.buldreinfo.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.helpers.TimeAgo;
import com.buldreinfo.model.Activity;
import com.buldreinfo.model.MediaIdentity;

@Repository
public class ActivityRepository {
	private record ActivityRecord(LocalDateTime ts, String type, int pid, Integer mid, Integer uid, Integer gid, Integer rid) {}
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String ACTIVITY_TYPE_TICK_REPEAT = "TICK_REPEAT";

	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;

	public ActivityRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void fillActivity(int idProblem) {
		jdbcClient.sql("DELETE FROM activity WHERE problem_id=?")
		.param(1, idProblem)
		.update();

		List<ActivityRecord> batch = new ArrayList<>();
		List<Integer> faUserIds = new ArrayList<>();
		AtomicReference<LocalDateTime> faTsRef = new AtomicReference<>();

		jdbcClient.sql("""
				SELECT p.id, p.fa_date, f.user_id
				FROM problem p
				JOIN grade g ON p.grade_id=g.id
				LEFT JOIN fa f ON p.id=f.problem_id
				WHERE p.id=? AND (g.grade!='n/a' OR f.user_id IS NOT NULL)
				""")
		.param(1, idProblem)
		.query(rs -> {
			int uid = rs.getInt("user_id");
			int pid = rs.getInt("id");
			if (uid > 0) faUserIds.add(uid);
			if (faTsRef.get() == null) {
				LocalDate d = rs.getObject("fa_date", LocalDate.class);
				faTsRef.set(applyIdOffset(d, pid));
			}
		});

		LocalDateTime faTs = faTsRef.get();
		if (faTs != null || !faUserIds.isEmpty()) {
			batch.add(new ActivityRecord(faTs != null ? faTs : applyIdOffset(null, idProblem), ACTIVITY_TYPE_FA, idProblem, null, null, null, null));
		}

		List<Integer> buf = new ArrayList<>();
		var state = new Object() {
			LocalDateTime anchor = faTs;
			LocalDateTime latest = faTs;
		};

		jdbcClient.sql("""
				SELECT m.id, m.date_created
				FROM media_problem mp
				JOIN media m ON mp.media_id = m.id
				WHERE mp.problem_id = ? AND m.deleted_timestamp IS NULL
				ORDER BY m.date_created ASC
				""")
		.param(1, idProblem)
		.query(rs -> {
			int id = rs.getInt("id");
			LocalDateTime cur = rs.getObject("date_created", LocalDateTime.class);

			if (state.anchor == null) {
				state.anchor = (faTs != null) ? faTs : (cur != null ? cur : applyIdOffset(null, id));
			}

			boolean inFA = faTs != null && cur != null && Math.abs(ChronoUnit.DAYS.between(faTs.toLocalDate(), cur.toLocalDate())) <= 7;
			boolean inRolling = state.anchor != null && state.anchor != faTs && cur != null && Math.abs(ChronoUnit.HOURS.between(state.anchor, cur)) <= 24;

			if (!inFA && !inRolling) {
				for (int mid : buf) {
					batch.add(new ActivityRecord(state.latest, ACTIVITY_TYPE_MEDIA, idProblem, mid, null, null, null));
				}
				buf.clear();
				state.anchor = (cur != null) ? cur : (faTs != null ? faTs : state.anchor);
			}

			state.latest = (inFA) ? faTs : (cur != null ? cur : state.anchor);
			buf.add(id);
		});

		for (int mid : buf) {
			batch.add(new ActivityRecord(state.latest, ACTIVITY_TYPE_MEDIA, idProblem, mid, null, null, null));
		}

		jdbcClient.sql("SELECT id, user_id, date FROM tick WHERE problem_id=?")
		.param(1, idProblem)
		.query(rs -> {
			int id = rs.getInt("id");
			int uid = rs.getInt("user_id");
			LocalDate d = rs.getObject("date", LocalDate.class);
			LocalDateTime ts = (faUserIds.contains(uid) && faTs != null) ? faTs : applyIdOffset(d, id);
			batch.add(new ActivityRecord(ts, ACTIVITY_TYPE_TICK, idProblem, null, uid, null, null));
		});

		jdbcClient.sql("SELECT r.id, t.user_id, r.date FROM tick t JOIN tick_repeat r ON t.id=r.tick_id WHERE t.problem_id=?")
		.param(1, idProblem)
		.query(rs -> {
			int id = rs.getInt("id");
			int uid = rs.getInt("user_id");
			LocalDate d = rs.getObject("date", LocalDate.class);
			batch.add(new ActivityRecord(applyIdOffset(d, id), ACTIVITY_TYPE_TICK_REPEAT, idProblem, null, uid, null, id));
		});

		jdbcClient.sql("SELECT id, post_time FROM guestbook WHERE problem_id=?")
		.param(1, idProblem)
		.query(rs -> {
			int id = rs.getInt("id");
			LocalDateTime pt = rs.getObject("post_time", LocalDateTime.class);
			batch.add(new ActivityRecord(pt != null ? pt : applyIdOffset(null, id), ACTIVITY_TYPE_GUESTBOOK, idProblem, null, null, id, null));
		});

		jdbcTemplate.batchUpdate(
				"INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id, tick_repeat_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
				batch,
				100,
				(ps, b) -> {
					ps.setObject(1, b.ts());
					ps.setString(2, b.type());
					ps.setInt(3, b.pid());
					ps.setObject(4, b.mid());
					ps.setObject(5, b.uid());
					ps.setObject(6, b.gid());
					ps.setObject(7, b.rid());
				});
	}

	@Transactional(readOnly = true)
	public List<Activity> getActivity(Setup setup, Optional<Integer> authUserId, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media, int offset) {
		var faIds = new HashSet<Integer>();
		var tickIds = new HashSet<Integer>();
		var repeatIds = new HashSet<Integer>();
		var mediaIds = new HashSet<Integer>();
		var gbIds = new HashSet<Integer>();
		var showAllTime = offset > 0 || lowerGrade > 0 || !fa || !comments || !ticks || !media || idArea > 0 || idSector > 0;

		var sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id, ? show_all_time,
				         ? hide_fa, ? hide_guestbook, ? hide_ticks, ? hide_media, ? min_grade_weight,
				         ? filter_area_id, ? filter_sector_id
				),
				x AS (
				  SELECT a1.id, a1.type, a1.activity_timestamp, a1.problem_id 
				  FROM req CROSS JOIN activity a1
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
				SELECT x.activity_timestamp, a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, 
				       s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, 
				       x.problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, 
				       ty.subtype problem_subtype, g.grade, GROUP_CONCAT(DISTINCT concat(x.id,'-',x.type) SEPARATOR ',') activities 
				FROM req CROSS JOIN x
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY x.activity_timestamp, a.id, s.id, x.problem_id, p.name, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, x.problem_id DESC
				""";

		List<Activity> res = jdbcClient.sql(sqlStr)
				.params(authUserId.orElse(0), setup.idRegion(), showAllTime, !fa, !comments, !ticks, !media, lowerGrade, idArea, idSector, offset)
				.query((rs, _) -> {
					var ts = rs.getObject("activity_timestamp", LocalDateTime.class);
					var currentActivityIds = new HashSet<Integer>();
					var raw = rs.getString("activities");
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
					return new Activity(currentActivityIds, TimeAgo.getTimeAgo(ts.toLocalDate()), 
							rs.getInt("area_id"), rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"),
							rs.getInt("sector_id"), rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"),
							rs.getInt("problem_id"), rs.getBoolean("problem_locked_admin"), rs.getBoolean("problem_locked_superadmin"), 
							rs.getString("problem_name"), rs.getString("problem_subtype"), rs.getString("grade"));
				}).list();

		var activityLookup = new HashMap<Integer, Activity>();
		res.forEach(act -> act.getActivityIds().forEach(id -> activityLookup.put(id, act)));

		if (!tickIds.isEmpty()) {
			String ids = tickIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			jdbcClient.sql("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex, t.comment description, t.stars, g.grade FROM activity a JOIN tick t ON a.problem_id=t.problem_id LEFT JOIN grade g ON t.grade_id=g.id JOIN user u ON t.user_id=u.id AND a.user_id=u.id LEFT JOIN media m ON u.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id WHERE a.id IN (" + ids + ") ORDER BY u.firstname, u.lastname")
			.query(rs -> {
				var a = activityLookup.get(rs.getInt("id"));
				if (a != null) {
					int userId = rs.getInt("user_id");
					var name = rs.getString("name");
					var grade = Optional.ofNullable(rs.getString("grade")).orElse(GradeConverter.NO_PERSONAL_GRADE);
					a.setTick(false, userId, name, rs.getString("description"), rs.getInt("stars"), grade);
					int mediaId = rs.getInt("media_id");
					if (mediaId > 0) a.appendActivityThumbnail(new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")), userId, name);
				}
			});
		}

		if (!repeatIds.isEmpty()) {
			String ids = repeatIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			jdbcClient.sql("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex, r.comment description, t.stars, g.grade FROM activity a JOIN user u ON a.user_id=u.id JOIN tick t ON a.problem_id=t.problem_id AND u.id=t.user_id LEFT JOIN grade g ON t.grade_id=g.id JOIN tick_repeat r ON a.tick_repeat_id=r.id AND t.id=r.tick_id LEFT JOIN media m ON u.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id WHERE a.id IN (" + ids + ") ORDER BY u.firstname, u.lastname")
			.query(rs -> {
				var a = activityLookup.get(rs.getInt("id"));
				if (a != null) {
					int userId = rs.getInt("user_id");
					var name = rs.getString("name");
					var grade = Optional.ofNullable(rs.getString("grade")).orElse(GradeConverter.NO_PERSONAL_GRADE);
					a.setTick(true, userId, name, rs.getString("description"), rs.getInt("stars"), grade);
					int mediaId = rs.getInt("media_id");
					if (mediaId > 0) a.appendActivityThumbnail(new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")), userId, name);
				}
			});
		}

		if (!gbIds.isEmpty()) {
			String ids = gbIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			jdbcClient.sql("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, ma.id avatar_media_id, UNIX_TIMESTAMP(ma.updated_at) avatar_version_stamp, mama.focus_x avatar_focus_x, mama.focus_y avatar_focus_y, mama.primary_color_hex avatar_primary_color_hex, g.message, mg.media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360, m.embed_url FROM activity a JOIN guestbook g ON a.guestbook_id=g.id JOIN user u ON g.user_id=u.id LEFT JOIN media ma ON u.media_id=ma.id LEFT JOIN media_ml_analysis mama ON ma.id=mama.media_id LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id LEFT JOIN media m ON mg.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id WHERE a.id IN (" + ids + ") AND m.deleted_user_id IS NULL")
			.query(rs -> {
				var a = activityLookup.get(rs.getInt("id"));
				if (a != null) {
					int userId = rs.getInt("user_id");
					var name = rs.getString("name");
					a.setGuestbook(userId, name, rs.getString("message"));
					int mediaId = rs.getInt("media_id");
					if (mediaId > 0) a.addMedia(new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")), rs.getBoolean("is_movie"), rs.getBoolean("is_360"), rs.getString("embed_url"));
					int avatarMediaId = rs.getInt("avatar_media_id");
					if (avatarMediaId > 0) a.appendActivityThumbnail(new MediaIdentity(avatarMediaId, rs.getLong("avatar_version_stamp"), rs.getInt("avatar_focus_x"), rs.getInt("avatar_focus_y"), rs.getString("avatar_primary_color_hex")), userId, name);
				}
			});
		}

		if (!faIds.isEmpty()) {
			String ids = faIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			jdbcClient.sql("SELECT a.id, p.description, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex FROM activity a JOIN problem p ON a.problem_id=p.id LEFT JOIN fa ON p.id=fa.problem_id LEFT JOIN user u ON fa.user_id=u.id LEFT JOIN media m ON u.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id WHERE a.id IN (" + ids + ") ORDER BY u.firstname, u.lastname")
			.query(rs -> {
				var a = activityLookup.get(rs.getInt("id"));
				if (a != null) {
					var desc = rs.getString("description");
					if (desc != null) a.setDescription(a.getDescription() == null ? desc : (a.getDescription().contains(desc) ? a.getDescription() : a.getDescription() + " (" + desc + ")"));
					int userId = rs.getInt("user_id");
					var name = rs.getString("name");
					int mediaId = rs.getInt("media_id");
					MediaIdentity mi = (mediaId > 0) ? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) : null;
					if (mi != null) a.appendActivityThumbnail(mi, userId, name);
					a.addUser(userId, name, mi);
				}
			});
			if (!media) {
				var pIds = res.stream().filter(a -> a.getActivityIds().stream().anyMatch(faIds::contains)).map(Activity::getProblemId).collect(Collectors.toSet());
				if (!pIds.isEmpty()) {
					String inClause = String.join(",", Collections.nCopies(pIds.size(), "?"));
					jdbcClient.sql("""
							SELECT mp.problem_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360, m.embed_url 
							FROM media_problem mp 
							JOIN media m ON mp.media_id = m.id 
							LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id 
							WHERE mp.problem_id IN (%s) AND m.deleted_user_id IS NULL 
							ORDER BY mp.sorting
							""".formatted(inClause))
					.params(new ArrayList<>(pIds))
					.query(rs -> {
						int pId = rs.getInt("problem_id");
						var mi = new MediaIdentity(rs.getInt("media_id"), rs.getLong("version_stamp"), rs.getInt("focus_x"), rs.getInt("focus_y"), rs.getString("media_primary_color_hex"));
						var isMovie = rs.getBoolean("is_movie");
						var is360 = rs.getBoolean("is_360");
						var embedUrl = rs.getString("embed_url");
						res.stream().filter(act -> act.getProblemId() == pId && act.getActivityIds().stream().anyMatch(faIds::contains)).forEach(a -> a.addMedia(mi, isMovie, is360, embedUrl));
					});
				}
			}
		}

		if (!mediaIds.isEmpty()) {
			String ids = mediaIds.stream().map(String::valueOf).collect(Collectors.joining(","));
			jdbcClient.sql("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, m.id media_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360, m.embed_url, COALESCE(m_photographer.id, m_creator.id) photographer_media_id, UNIX_TIMESTAMP(COALESCE(m_photographer.updated_at,m_creator.updated_at)) photographer_version_stamp FROM activity a JOIN media m ON a.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id JOIN media_problem mp ON m.id=mp.media_id AND a.problem_id=mp.problem_id LEFT JOIN user u ON m.photographer_user_id=u.id LEFT JOIN media m_photographer ON u.media_id=m_photographer.id LEFT JOIN user u_creator ON m.uploader_user_id=u_creator.id LEFT JOIN media m_creator ON u_creator.media_id=m_creator.id WHERE a.id IN (" + ids + ")")
			.query(rs -> {
				var a = activityLookup.get(rs.getInt("id"));
				if (a != null) {
					a.addMedia(new MediaIdentity(rs.getInt("media_id"), rs.getLong("version_stamp"), rs.getInt("focus_x"), rs.getInt("focus_y"), rs.getString("media_primary_color_hex")), rs.getBoolean("is_movie"), rs.getBoolean("is_360"), rs.getString("embed_url"));
					if (a.getUsers() == null || a.getUsers().isEmpty()) a.appendActivityThumbnail(new MediaIdentity(rs.getInt("photographer_media_id"), rs.getLong("photographer_version_stamp"), 0, 0, null), rs.getInt("user_id"), rs.getString("name"));
				}
			});
		}

		return res;
	}

	private LocalDateTime applyIdOffset(LocalDate date, int id) {
		return (date != null ? date : LocalDate.EPOCH)
				.atStartOfDay()
				.plusSeconds(id);
	}
}
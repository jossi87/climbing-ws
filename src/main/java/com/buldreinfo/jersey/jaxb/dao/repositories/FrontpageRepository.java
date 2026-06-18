package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageFirstAscent;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageLastComment;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageNewestMedia;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRecentAscent;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageStats;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public record FrontpageRepository(Gson gson) {
	private static final Logger logger = LogManager.getLogger();

	public FrontpageRepository() {
		this(new Gson());
	}

	public List<FrontpageFirstAscent> getFrontpageFirstAscents(Optional<Integer> authUserId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<FrontpageFirstAscent>();
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				WITH req AS (
					SELECT ? auth_user_id, ? region_id
				),
				x AS (
					SELECT p1.id AS problem_id, p1.fa_date AS activity_timestamp
					FROM req
					CROSS JOIN problem p1
					JOIN sector s1 ON p1.sector_id = s1.id
					JOIN area ar1 ON s1.area_id = ar1.id
					LEFT JOIN user_region ur1 ON (ar1.region_id = ur1.region_id AND ur1.user_id = req.auth_user_id)
					WHERE p1.fa_date IS NOT NULL AND p1.trash IS NULL
					  AND (ar1.region_id = req.region_id OR ur1.user_id IS NOT NULL)
					  AND ar1.region_id IN (
						SELECT r.id FROM region r 
						JOIN region_type rt ON r.id = rt.region_id 
						WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
					)
					  AND ar1.trash IS NULL AND ((ar1.locked_admin=0 AND ar1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND ar1.locked_superadmin=0))
					  AND s1.trash IS NULL AND ((s1.locked_admin=0 AND s1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND s1.locked_superadmin=0))
					  AND p1.trash IS NULL AND ((p1.locked_admin=0 AND p1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND p1.locked_superadmin=0))
					ORDER BY p1.fa_date DESC, p1.id DESC LIMIT 8
				)
				SELECT 
					x.activity_timestamp, a.id area_id, a.name area_name,
					p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, ty.subtype problem_subtype,
					g.grade grade,
					GROUP_CONCAT(
						DISTINCT concat(u.id,':',u.firstname,' ',COALESCE(u.lastname,''),':',COALESCE(m.id,0),':',COALESCE(UNIX_TIMESTAMP(m.updated_at),0),':',COALESCE(mma.focus_x,0),':',COALESCE(mma.focus_y,0)) 
						ORDER BY u.firstname ASC, COALESCE(u.lastname,'') ASC 
						SEPARATOR '|'
					) user_data
				FROM req
				CROSS JOIN x
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN fa ON p.id = fa.problem_id
				LEFT JOIN user u ON fa.user_id = u.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY x.activity_timestamp, a.id, a.name, p.id, p.name, p.locked_admin, p.locked_superadmin, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, p.id DESC
				         """;
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					var grade = rst.getString("grade");
					var users = new ArrayList<User>();
					var rawUsers = rst.getString("user_data");
					if (rawUsers != null) {
						for (var userRecord : rawUsers.split("\\|")) {
							var p = userRecord.split(":");
							int mediaId = Integer.parseInt(p[2]);
							var mi = mediaId > 0 ? new MediaIdentity(mediaId, Long.parseLong(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]), p.length > 6 ? p[6] : null) : null;
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
		logger.debug("getFrontpageFirstAscents(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public List<FrontpageLastComment> getFrontpageLastComments(Optional<Integer> authUserId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<FrontpageLastComment>();
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				)
				SELECT g.post_time AS activity_timestamp, a.id area_id, a.name area_name,
				       p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       g.message, u.id user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) user_name,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM req
				CROSS JOIN guestbook g
				JOIN user u ON g.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				JOIN problem p ON g.problem_id=p.id
				JOIN sector s ON p.sector_id=s.id 
				JOIN area a ON s.area_id=a.id 
				LEFT JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=req.auth_user_id)
				WHERE a.region_id IN (
				    SELECT r.id FROM region r 
				    JOIN region_type rt ON r.id=rt.region_id 
				    LEFT JOIN user_region ur2 ON (r.id=ur2.region_id AND ur2.user_id=req.auth_user_id)
				    WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				      AND (r.id=req.region_id OR ur2.user_id IS NOT NULL)
				)
				AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY g.id DESC LIMIT 4
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					int mediaId = rst.getInt("media_id");
					var mi = mediaId > 0 ? new MediaIdentity(mediaId, rst.getLong("media_version_stamp"), rst.getInt("media_focus_x"), rst.getInt("media_focus_y"), rst.getString("media_primary_color_hex")) : null;
					var user = new User(rst.getInt("user_id"), rst.getString("user_name"), mi);
					res.add(new FrontpageLastComment(TimeAgo.getTimeAgo(ts.toLocalDate()), rst.getInt("area_id"), rst.getString("area_name"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), user, rst.getString("message")));
				}
			}
		}
		logger.debug("getFrontpageLastComments(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public List<FrontpageRecentAscent> getFrontpageNewestAscents(Optional<Integer> authUserId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<FrontpageRecentAscent>();
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				),
				recent_activity AS (
				    SELECT t.id AS tick_id, t.problem_id, t.user_id, t.grade_id, t.date AS tick_date, tr.date AS repeat_date,
				           GREATEST(COALESCE(t.date, '1000-01-01'), COALESCE(tr.date, '1000-01-01')) AS activity_timestamp,
				           IF(tr.date IS NOT NULL AND (t.date IS NULL OR tr.date > t.date), 1, 0) AS is_repeat
				    FROM req
				    CROSS JOIN tick t
				    LEFT JOIN (
				        SELECT tick_id, MAX(date) AS date
				        FROM tick_repeat
				        GROUP BY tick_id
				    ) tr ON t.id = tr.tick_id
				    JOIN problem p ON t.problem_id = p.id
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    LEFT JOIN user_region ur1 ON (a.region_id = ur1.region_id AND ur1.user_id = req.auth_user_id)
				    WHERE a.region_id IN (
				        SELECT r.id FROM region r 
				        JOIN region_type rt ON r.id = rt.region_id 
				        LEFT JOIN user_region ur2 ON (r.id = ur2.region_id AND ur2.user_id = req.auth_user_id)
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				          AND (r.id = req.region_id OR ur2.user_id IS NOT NULL)
				    )
				      AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND a.locked_superadmin=0))
				      AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND s.locked_superadmin=0))
				      AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND p.locked_superadmin=0))
				    ORDER BY activity_timestamp DESC, t.id DESC
				    LIMIT 8
				)
				SELECT ra.activity_timestamp, ra.is_repeat, ra.tick_id, a.id area_id, a.name area_name,
				       p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, ty.subtype problem_subtype,
				       g.grade tick_grade, u.id user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) user_name,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM req
				CROSS JOIN recent_activity ra
				JOIN problem p ON ra.problem_id = p.id
				JOIN user u ON ra.user_id = u.id
				LEFT JOIN grade g ON ra.grade_id = g.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY ra.activity_timestamp DESC, ra.tick_id DESC
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var ts = rst.getTimestamp("activity_timestamp").toLocalDateTime();
					var tickGrade = rst.getString("tick_grade");
					if (tickGrade == null) {
						tickGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					var repeat = rst.getInt("is_repeat") == 1; 
					int mediaId = rst.getInt("media_id");
					var mi = mediaId > 0 ? new MediaIdentity(mediaId, rst.getLong("media_version_stamp"), rst.getInt("media_focus_x"), rst.getInt("media_focus_y"), rst.getString("media_primary_color_hex")) : null;
					var user = new User(rst.getInt("user_id"), rst.getString("user_name"), mi);
					res.add(new FrontpageRecentAscent(TimeAgo.getTimeAgo(ts.toLocalDate()),  rst.getInt("area_id"), rst.getString("area_name"),
							rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), 
							rst.getString("problem_name"), rst.getString("problem_subtype"), tickGrade, user, repeat));
				}
			}
		}
		logger.debug("getFrontpageNewestAscents(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public List<FrontpageNewestMedia> getFrontpageNewestMedia(Optional<Integer> authUserId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<FrontpageNewestMedia>();
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id
				),
				m_list AS (
				  SELECT m.id AS media_id, mp.problem_id
				  FROM req
				  CROSS JOIN media m
				  JOIN media_problem mp ON m.id = mp.media_id AND mp.trivia = 0
				  JOIN problem p1 ON mp.problem_id = p1.id
				  JOIN sector s1 ON p1.sector_id = s1.id
				  JOIN area ar1 ON s1.area_id = ar1.id
				  WHERE m.deleted_user_id IS NULL
				    AND ar1.region_id IN (
				        SELECT r.id FROM region r 
				        JOIN region_type rt ON r.id = rt.region_id 
				        LEFT JOIN user_region ur2 ON (r.id = ur2.region_id AND ur2.user_id = req.auth_user_id)
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				          AND (r.id = req.region_id OR ur2.user_id IS NOT NULL)
				    )
				  ORDER BY m.id DESC
				  LIMIT 50
				)
				SELECT 
				  ml.media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.is_movie, m.is_360,
				  p.id problem_id, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				  g.grade
				FROM req
				CROSS JOIN m_list ml
				JOIN media m ON ml.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN problem p ON ml.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = req.auth_user_id)
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY ml.media_id DESC
				LIMIT 12
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var mi = new MediaIdentity(rst.getInt("media_id"), rst.getLong("media_version_stamp"), rst.getInt("focus_x"), rst.getInt("focus_y"), rst.getString("media_primary_color_hex"));
					res.add(new FrontpageNewestMedia(mi, rst.getBoolean("is_movie"), rst.getBoolean("is_360"), rst.getInt("problem_id"), rst.getBoolean("problem_locked_admin"), rst.getBoolean("problem_locked_superadmin"), rst.getString("problem_name"), rst.getString("grade")));
				}
			}
		}
		logger.debug("getFrontpageNewestMedia(authUserId={}, setup={}) - res={}, duration={}", authUserId, setup, res, stopwatch);
		return res;
	}

	public List<FrontpageRandomMedia> getFrontpageRandomMedia(Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<FrontpageRandomMedia>();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				WITH req AS (
				    SELECT ? region_id
				),
				random_id AS (
				    SELECT id FROM (
				        SELECT m_sub.id,
				               mma_sub.is_action_shot,
				               ROW_NUMBER() OVER (ORDER BY 
				                   IFNULL(mma_sub.is_action_shot, 0) DESC,
				                   RAND()
				               ) as random_rank
				        FROM req
				        CROSS JOIN media m_sub
				        JOIN media_problem mp_sub ON m_sub.id=mp_sub.media_id
				        JOIN problem p_sub ON mp_sub.problem_id=p_sub.id
				        JOIN sector s_sub ON p_sub.sector_id=s_sub.id
				        JOIN area a_sub ON s_sub.area_id=a_sub.id
				        LEFT JOIN media_ml_analysis mma_sub ON m_sub.id = mma_sub.media_id
				        WHERE a_sub.region_id=req.region_id
				          AND m_sub.deleted_user_id IS NULL
				          AND a_sub.trash IS NULL AND s_sub.trash IS NULL AND p_sub.trash IS NULL
				          AND a_sub.access_closed IS NULL AND s_sub.access_closed IS NULL
				          AND m_sub.is_movie=0 AND m_sub.is_360=0
				          AND mp_sub.trivia=0
				          AND a_sub.locked_admin=0 AND a_sub.locked_superadmin=0
				          AND s_sub.locked_admin=0 AND s_sub.locked_superadmin=0
				          AND p_sub.locked_admin=0 AND p_sub.locked_superadmin=0
				    ) ranked_pool
				    WHERE random_rank<=500
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
				JOIN grade g ON p.consensus_grade_id = g.id
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
			try (var rst = ps.executeQuery()) {
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
					var identity = new MediaIdentity(idMedia, versionStamp, focusX, focusY, mediaPrimaryColorHex);
					var photographer = photographerJson == null ? null : gson.fromJson(photographerJson, User.class);
					List<User> tagged = taggedJson == null ? null : gson.fromJson("[" + taggedJson + "]", new TypeToken<List<User>>(){}.getType());
					res.add(new FrontpageRandomMedia(identity, width, height, idArea, area, idSector, sector, idProblem, problem, grade, photographer, tagged));
				}
			}
		}
		logger.debug("getFrontpageRandomMedia(setup={}) - res.size()={}, duration={}", setup, res.size(), stopwatch);
		return res;
	}

	public FrontpageStats getFrontpageStats(Optional<Integer> authUserId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		FrontpageStats res = null;
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				)
				SELECT COUNT(DISTINCT a.id) areas,
				       COUNT(DISTINCT p.id) problems,
				       COUNT(DISTINCT t.id) ticks
				FROM req
				CROSS JOIN region_type rt
				JOIN region r ON rt.region_id=r.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				LEFT JOIN area a ON r.id=a.region_id AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				LEFT JOIN sector s ON a.id=s.area_id AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				LEFT JOIN problem p ON s.id=p.sector_id AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				LEFT JOIN tick t ON p.id=t.problem_id
				WHERE rt.type_id IN (SELECT x.type_id FROM region_type x WHERE x.region_id=req.region_id)
				  AND (a.region_id=req.region_id OR ur.user_id IS NOT NULL)
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
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
}
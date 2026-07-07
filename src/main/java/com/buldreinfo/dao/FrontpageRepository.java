package com.buldreinfo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.helpers.TimeAgo;
import com.buldreinfo.model.Frontpage.FrontpageFirstAscent;
import com.buldreinfo.model.Frontpage.FrontpageLastComment;
import com.buldreinfo.model.Frontpage.FrontpageNewestMedia;
import com.buldreinfo.model.Frontpage.FrontpageRandomMedia;
import com.buldreinfo.model.Frontpage.FrontpageRecentAscent;
import com.buldreinfo.model.Frontpage.FrontpageStats;
import com.buldreinfo.model.MediaIdentity;
import com.buldreinfo.model.User;
import com.buldreinfo.util.JsonHelper;

@Repository
public class FrontpageRepository {
	private final JdbcClient jdbcClient;
	private final JsonHelper jsonHelper;

	public FrontpageRepository(JdbcClient jdbcClient, JsonHelper jsonHelper) {
		this.jdbcClient = jdbcClient;
		this.jsonHelper = jsonHelper;
	}

	@Transactional(readOnly = true)
	public List<FrontpageFirstAscent> getFrontpageFirstAscents(Optional<Integer> authUserId, Setup setup) {
		return jdbcClient.sql("""
				WITH req AS (
				    SELECT ? AS auth_user_id, ? AS region_id
				),
				x AS (
				    SELECT p1.id AS problem_id, p1.fa_date AS activity_timestamp
				    FROM problem p1
				    JOIN sector s1 ON p1.sector_id = s1.id
				    JOIN area ar1 ON s1.area_id = ar1.id
				    LEFT JOIN user_region ur1 ON (ar1.region_id = ur1.region_id AND ur1.user_id = (SELECT auth_user_id FROM req))
				    WHERE p1.fa_date IS NOT NULL AND p1.trash IS NULL
				      AND (ar1.region_id = (SELECT region_id FROM req) OR ur1.user_id IS NOT NULL)
				      AND ar1.region_id IN (
				          SELECT r.id FROM region r 
				          JOIN region_type rt ON r.id = rt.region_id 
				          WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = (SELECT region_id FROM req))
				      )
				      AND ar1.trash IS NULL AND ((ar1.locked_admin=0 AND ar1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND ar1.locked_superadmin=0))
				      AND s1.trash IS NULL AND ((s1.locked_admin=0 AND s1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND s1.locked_superadmin=0))
				      AND p1.trash IS NULL AND ((p1.locked_admin=0 AND p1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND p1.locked_superadmin=0))
				    ORDER BY p1.fa_date DESC, p1.id DESC LIMIT 8
				)
				SELECT 
				    x.activity_timestamp, a.id AS area_id, a.name AS area_name,
				    p.id AS problem_id, p.name AS problem_name, p.locked_admin AS problem_locked_admin, p.locked_superadmin AS problem_locked_superadmin, ty.subtype AS problem_subtype,
				    g.grade AS grade,
				    IF(COUNT(u.id) > 0,
				        JSON_ARRAYAGG(
				            JSON_OBJECT(
				                'id', u.id,
				                'name', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))),
				                'mediaIdentity', IF(m.id IS NOT NULL, 
				                    JSON_OBJECT(
				                        'id', m.id,
				                        'versionStamp', UNIX_TIMESTAMP(m.updated_at),
				                        'focusX', COALESCE(mma.focus_x, 0),
				                        'focusY', COALESCE(mma.focus_y, 0),
				                        'primaryColorHex', mma.primary_color_hex
				                    ), 
				                NULL)
				            )
				        ),
				    NULL) AS user_data
				FROM x
				JOIN problem p ON x.problem_id = p.id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN fa ON p.id = fa.problem_id
				LEFT JOIN user u ON fa.user_id = u.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = (SELECT auth_user_id FROM req))
				GROUP BY x.activity_timestamp, a.id, a.name, p.id, p.name, p.locked_admin, p.locked_superadmin, ty.subtype, g.grade
				ORDER BY x.activity_timestamp DESC, p.id DESC
				""")
				.params(authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> {
					var ts = rs.getTimestamp("activity_timestamp").toLocalDateTime();
					List<User> users = jsonHelper.parseArray(rs.getString("user_data"), User[].class);
					return new FrontpageFirstAscent(
							TimeAgo.getTimeAgo(ts.toLocalDate()),
							rs.getInt("area_id"),
							rs.getString("area_name"),
							rs.getInt("problem_id"),
							rs.getBoolean("problem_locked_admin"),
							rs.getBoolean("problem_locked_superadmin"),
							rs.getString("problem_name"),
							rs.getString("problem_subtype"),
							rs.getString("grade"),
							users
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public List<FrontpageLastComment> getFrontpageLastComments(Optional<Integer> authUserId, Setup setup) {
		return jdbcClient.sql("""
				WITH req AS (
					SELECT ? AS auth_user_id, ? AS region_id
				)
				SELECT g.post_time AS activity_timestamp, a.id AS area_id, a.name AS area_name,
				       p.id AS problem_id, p.name AS problem_name, p.locked_admin AS problem_locked_admin, p.locked_superadmin AS problem_locked_superadmin,
				       g.message, u.id AS user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) AS user_name,
				       m.id AS media_id, UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp, mma.focus_x AS media_focus_x, mma.focus_y AS media_focus_y, mma.primary_color_hex AS media_primary_color_hex
				FROM guestbook g
				JOIN user u ON g.user_id = u.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN problem p ON g.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = (SELECT auth_user_id FROM req))
				WHERE a.region_id IN (
				    SELECT r.id FROM region r 
				    JOIN region_type rt ON r.id = rt.region_id 
				    LEFT JOIN user_region ur2 ON (r.id = ur2.region_id AND ur2.user_id = (SELECT auth_user_id FROM req))
				    WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = (SELECT region_id FROM req))
				      AND (r.id = (SELECT region_id FROM req) OR ur2.user_id IS NOT NULL)
				)
				AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY g.id DESC LIMIT 4
				""")
				.params(authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> {
					var ts = rs.getTimestamp("activity_timestamp").toLocalDateTime();
					int mediaId = rs.getInt("media_id");
					MediaIdentity mi = (mediaId > 0) 
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) 
									: null;
					User user = new User(rs.getInt("user_id"), rs.getString("user_name"), mi);
					return new FrontpageLastComment(
							TimeAgo.getTimeAgo(ts.toLocalDate()),
							rs.getInt("area_id"),
							rs.getString("area_name"),
							rs.getInt("problem_id"),
							rs.getBoolean("problem_locked_admin"),
							rs.getBoolean("problem_locked_superadmin"),
							rs.getString("problem_name"),
							user,
							rs.getString("message")
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public List<FrontpageRecentAscent> getFrontpageNewestAscents(Optional<Integer> authUserId, Setup setup) {
		return jdbcClient.sql("""
				WITH req AS (
				    SELECT ? AS auth_user_id, ? AS region_id
				),
				recent_activity AS (
				    SELECT t.id AS tick_id, t.problem_id, t.user_id, t.grade_id, t.date AS tick_date, tr.date AS repeat_date,
				           GREATEST(COALESCE(t.date, '1000-01-01'), COALESCE(tr.date, '1000-01-01')) AS activity_timestamp,
				           IF(tr.date IS NOT NULL AND (t.date IS NULL OR tr.date > t.date), 1, 0) AS is_repeat
				    FROM tick t
				    LEFT JOIN (SELECT tick_id, MAX(date) AS date FROM tick_repeat GROUP BY tick_id) tr ON t.id = tr.tick_id
				    JOIN problem p ON t.problem_id = p.id
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    LEFT JOIN user_region ur1 ON (a.region_id = ur1.region_id AND ur1.user_id = (SELECT auth_user_id FROM req))
				    WHERE a.region_id IN (
				        SELECT r.id FROM region r 
				        JOIN region_type rt ON r.id = rt.region_id 
				        LEFT JOIN user_region ur2 ON (r.id = ur2.region_id AND ur2.user_id = (SELECT auth_user_id FROM req))
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = (SELECT region_id FROM req))
				          AND (r.id = (SELECT region_id FROM req) OR ur2.user_id IS NOT NULL)
				    )
				      AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND a.locked_superadmin=0))
				      AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND s.locked_superadmin=0))
				      AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND p.locked_superadmin=0))
				    ORDER BY activity_timestamp DESC, t.id DESC
				    LIMIT 8
				)
				SELECT ra.activity_timestamp, ra.is_repeat, ra.tick_id, a.id AS area_id, a.name AS area_name,
				       p.id AS problem_id, p.name AS problem_name, p.locked_admin AS problem_locked_admin, p.locked_superadmin AS problem_locked_superadmin, ty.subtype AS problem_subtype,
				       g.grade AS tick_grade, u.id AS user_id, TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))) AS user_name,
				       m.id AS media_id, UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp, mma.focus_x AS media_focus_x, mma.focus_y AS media_focus_y, mma.primary_color_hex AS media_primary_color_hex
				FROM recent_activity ra
				JOIN problem p ON ra.problem_id = p.id
				JOIN user u ON ra.user_id = u.id
				LEFT JOIN grade g ON ra.grade_id = g.id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN type ty ON p.type_id = ty.id 
				JOIN sector s ON p.sector_id = s.id 
				JOIN area a ON s.area_id = a.id 
				LEFT JOIN user_region ur ON (a.region_id = ur.region_id AND ur.user_id = (SELECT auth_user_id FROM req))
				WHERE a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY ra.activity_timestamp DESC, ra.tick_id DESC
				""")
				.params(authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> {
					var ts = rs.getTimestamp("activity_timestamp").toLocalDateTime();
					String tickGrade = rs.getString("tick_grade");
					if (tickGrade == null) {
						tickGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					int mediaId = rs.getInt("media_id");
					MediaIdentity mi = (mediaId > 0) 
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) 
									: null;
					User user = new User(rs.getInt("user_id"), rs.getString("user_name"), mi);
					return new FrontpageRecentAscent(
							TimeAgo.getTimeAgo(ts.toLocalDate()),
							rs.getInt("area_id"),
							rs.getString("area_name"),
							rs.getInt("problem_id"),
							rs.getBoolean("problem_locked_admin"),
							rs.getBoolean("problem_locked_superadmin"),
							rs.getString("problem_name"),
							rs.getString("problem_subtype"),
							tickGrade,
							user,
							rs.getInt("is_repeat") == 1
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public List<FrontpageNewestMedia> getFrontpageNewestMedia(Optional<Integer> authUserId, Setup setup) {
		return jdbcClient.sql("""
				WITH req AS (
					SELECT ? AS auth_user_id, ? AS region_id
				),
				m_list AS (
				    SELECT m.id AS media_id, mp.problem_id
				    FROM media m
				    JOIN media_problem mp ON m.id = mp.media_id AND mp.trivia = 0
				    JOIN problem p1 ON mp.problem_id = p1.id
				    JOIN sector s1 ON p1.sector_id = s1.id
				    JOIN area ar1 ON s1.area_id = ar1.id
				    LEFT JOIN user_region ur1 ON (ar1.region_id = ur1.region_id AND ur1.user_id = (SELECT auth_user_id FROM req))
				    WHERE m.deleted_user_id IS NULL
				      AND ar1.region_id IN (
				          SELECT r.id FROM region r 
				          JOIN region_type rt ON r.id = rt.region_id 
				          LEFT JOIN user_region ur2 ON (r.id = ur2.region_id AND ur2.user_id = (SELECT auth_user_id FROM req))
				          WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = (SELECT region_id FROM req))
				            AND (r.id = (SELECT region_id FROM req) OR ur2.user_id IS NOT NULL)
				      )
				      AND ar1.trash IS NULL AND ((ar1.locked_admin=0 AND ar1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND ar1.locked_superadmin=0))
				      AND s1.trash IS NULL AND ((s1.locked_admin=0 AND s1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND s1.locked_superadmin=0))
				      AND p1.trash IS NULL AND ((p1.locked_admin=0 AND p1.locked_superadmin=0) OR (ur1.superadmin_read=1) OR (ur1.admin_read=1 AND p1.locked_superadmin=0))
				    ORDER BY m.id DESC
				    LIMIT 12
				)
				SELECT 
				    ml.media_id, UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex AS media_primary_color_hex, m.is_movie, m.is_360,
				    p.id AS problem_id, p.name AS problem_name, p.locked_admin AS problem_locked_admin, p.locked_superadmin AS problem_locked_superadmin,
				    g.grade
				FROM m_list ml
				JOIN media m ON ml.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN problem p ON ml.problem_id = p.id
				JOIN grade g ON p.consensus_grade_id = g.id
				ORDER BY ml.media_id DESC
				""")
				.params(authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> new FrontpageNewestMedia(
						new MediaIdentity(
								rs.getInt("media_id"),
								rs.getLong("media_version_stamp"),
								rs.getInt("focus_x"),
								rs.getInt("focus_y"),
								rs.getString("media_primary_color_hex")
								),
						rs.getBoolean("is_movie"),
						rs.getBoolean("is_360"),
						rs.getInt("problem_id"),
						rs.getBoolean("problem_locked_admin"),
						rs.getBoolean("problem_locked_superadmin"),
						rs.getString("problem_name"),
						rs.getString("grade")
						))
				.list();
	}

	@Transactional(readOnly = true)
	public List<FrontpageRandomMedia> getFrontpageRandomMedia(Setup setup) {
		return jdbcClient.sql("""
				WITH req AS (
					SELECT ? AS region_id
				),
				random_id AS (
				    SELECT id FROM (
				        SELECT m_sub.id, ROW_NUMBER() OVER (ORDER BY IFNULL(mma_sub.is_action_shot, 0) DESC, RAND()) AS random_rank
				        FROM media m_sub
				        JOIN media_problem mp_sub ON m_sub.id = mp_sub.media_id
				        JOIN problem p_sub ON mp_sub.problem_id = p_sub.id
				        JOIN sector s_sub ON p_sub.sector_id = s_sub.id
				        JOIN area a_sub ON s_sub.area_id = a_sub.id
				        LEFT JOIN media_ml_analysis mma_sub ON m_sub.id = mma_sub.media_id
				        WHERE a_sub.region_id = (SELECT region_id FROM req)
				          AND m_sub.deleted_user_id IS NULL
				          AND a_sub.trash IS NULL AND s_sub.trash IS NULL AND p_sub.trash IS NULL
				          AND a_sub.access_closed IS NULL AND s_sub.access_closed IS NULL
				          AND m_sub.is_movie = 0 AND m_sub.is_360 = 0 AND mp_sub.trivia = 0
				          AND a_sub.locked_admin = 0 AND a_sub.locked_superadmin = 0
				          AND s_sub.locked_admin = 0 AND s_sub.locked_superadmin = 0
				          AND p_sub.locked_admin = 0 AND p_sub.locked_superadmin = 0
				    ) ranked_pool
				    WHERE random_rank <= 500
				    ORDER BY RAND()
				    LIMIT 20
				)
				SELECT m.id AS id_media, UNIX_TIMESTAMP(m.updated_at) AS version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex AS media_primary_color_hex,
				       m.width, m.height, a.id AS id_area, a.name AS area, s.id AS id_sector, s.name AS sector, p.id AS id_problem, p.name AS problem, g.grade AS grade,
				       IF(u.id IS NOT NULL,
				           JSON_OBJECT(
				               'id', u.id,
				               'name', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))),
				               'mediaIdentity', IF(ma.id IS NOT NULL,
				                   JSON_OBJECT(
				                       'id', ma.id,
				                       'versionStamp', UNIX_TIMESTAMP(ma.updated_at),
				                       'focusX', COALESCE(mma_u.focus_x, 0),
				                       'focusY', COALESCE(mma_u.focus_y, 0),
				                       'primaryColorHex', COALESCE(mma_u.primary_color_hex, '')
				                   ),
				               NULL)
				           ),
				       NULL) AS photographer,
				       IF(COUNT(u2.id) > 0,
				           CONCAT('[',
				               GROUP_CONCAT(
				                   DISTINCT IF(u2.id IS NOT NULL,
				                       JSON_OBJECT(
				                           'id', u2.id,
				                           'name', TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname, ''))),
				                           'mediaIdentity', IF(ma2.id IS NOT NULL,
				                               JSON_OBJECT(
				                                   'id', ma2.id,
				                                   'versionStamp', UNIX_TIMESTAMP(ma2.updated_at),
				                                   'focusX', COALESCE(mma_u2.focus_x, 0),
				                                   'focusY', COALESCE(mma_u2.focus_y, 0),
				                                   'primaryColorHex', COALESCE(mma_u2.primary_color_hex, '')
				                               ),
				                           NULL)
				                       ),
				                   NULL)
				                   SEPARATOR ','
				               ),
				           ']'),
				       NULL) AS tagged
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
				GROUP BY m.id, m.updated_at, m.width, m.height, a.id, a.name, s.id, s.name, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname, u.id, ma.id, ma.updated_at, mma.focus_x, mma.focus_y, mma.primary_color_hex, mma_u.focus_x, mma_u.focus_y, mma_u.primary_color_hex, g.grade
				""")
				.params(setup.idRegion())
				.query((rs, _) -> {
					int idMedia = rs.getInt("id_media");
					MediaIdentity identity = new MediaIdentity(
							idMedia,
							rs.getLong("version_stamp"),
							rs.getInt("focus_x"),
							rs.getInt("focus_y"),
							rs.getString("media_primary_color_hex")
							);
					User photographer = jsonHelper.parseObject(rs.getString("photographer"), User.class);
					List<User> tagged = jsonHelper.parseArray(rs.getString("tagged"), User[].class);
					return new FrontpageRandomMedia(
							identity,
							rs.getInt("width"),
							rs.getInt("height"),
							rs.getInt("id_area"),
							rs.getString("area"),
							rs.getInt("id_sector"),
							rs.getString("sector"),
							rs.getInt("id_problem"),
							rs.getString("problem"),
							rs.getString("grade"),
							photographer,
							tagged
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public FrontpageStats getFrontpageStats(Optional<Integer> authUserId, Setup setup) {
		FrontpageStats res = jdbcClient.sql("""
				WITH req AS (
				    SELECT ? AS auth_user_id, ? AS region_id
				),
				valid_areas AS (
				    SELECT a.id AS area_id, ur.superadmin_read, ur.admin_read
				    FROM area a
				    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = (SELECT auth_user_id FROM req)
				    WHERE a.region_id IN (
				        SELECT r.id FROM region r
				        JOIN region_type rt ON r.id = rt.region_id
				        WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = (SELECT region_id FROM req))
				    )
				    AND (a.region_id = (SELECT region_id FROM req) OR ur.user_id IS NOT NULL)
				    AND a.trash IS NULL 
				    AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				),
				valid_problems AS (
				    SELECT p.id AS problem_id
				    FROM valid_areas va
				    JOIN sector s ON va.area_id = s.area_id
				    JOIN problem p ON s.id = p.sector_id
				    WHERE s.trash IS NULL 
				      AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (va.superadmin_read=1) OR (va.admin_read=1 AND s.locked_superadmin=0))
				      AND p.trash IS NULL 
				      AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (va.superadmin_read=1) OR (va.admin_read=1 AND p.locked_superadmin=0))
				)
				SELECT 
				    (SELECT COUNT(*) FROM valid_areas) AS areas,
				    (SELECT COUNT(*) FROM valid_problems) AS problems,
				    (SELECT COUNT(*) FROM tick t WHERE EXISTS (SELECT 1 FROM valid_problems vp WHERE vp.problem_id = t.problem_id)) AS ticks;
				""")
				.params(authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> new FrontpageStats(
						rs.getInt("areas"),
						rs.getInt("problems"),
						rs.getInt("ticks")
						))
				.single();

		return res;
	}
}
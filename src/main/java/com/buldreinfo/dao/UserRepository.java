package com.buldreinfo.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Auth0Profile;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.excel.ExcelSheet;
import com.buldreinfo.excel.ExcelWorkbook;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.helpers.TimeAgo;
import com.buldreinfo.model.Administrator;
import com.buldreinfo.model.AuthenticatedUser;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.MediaIdentity;
import com.buldreinfo.model.PermissionUser;
import com.buldreinfo.model.Profile.ProfileDiscipline;
import com.buldreinfo.model.Profile.ProfileDisciplineGradeDistribution;
import com.buldreinfo.model.Profile.ProfileIdentity;
import com.buldreinfo.model.Profile.ProfileKpis;
import com.buldreinfo.model.ProfileAscent;
import com.buldreinfo.model.ProfileTodo;
import com.buldreinfo.model.ProfileTodo.ProfileTodoArea;
import com.buldreinfo.model.ProfileTodo.ProfileTodoProblem;
import com.buldreinfo.model.ProfileTodo.ProfileTodoSector;
import com.buldreinfo.model.User;
import com.buldreinfo.model.UserRegion;

@Repository
public class UserRepository {
	public static final int USER_ID_UNKNOWN = 1049;
	private static final Logger logger = LogManager.getLogger();
	private final JdbcClient jdbcClient;

	public UserRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public void ensureUserExists(int userId) {
		if (userId <= 0) {
			throw new IllegalArgumentException("Invalid userId=" + userId);
		}

		boolean exists = jdbcClient.sql("SELECT 1 FROM user WHERE id = ?")
				.param(userId)
				.query(Integer.class)
				.optional()
				.isPresent();

		if (!exists) {
			throw new NoSuchElementException("Could not find user with id=" + userId);
		}
	}

	@Transactional(readOnly = true)
	public List<Administrator> getAdministrators(Setup setup) {
		return jdbcClient.sql("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) AS name,
				       (SELECT GROUP_CONCAT(DISTINCT e.email ORDER BY e.email SEPARATOR ';') 
				        FROM user_email e 
				        WHERE e.user_id = u.id AND e.email NOT LIKE '%@missing-email.com'
				        AND u.email_visible_to_all = 1) AS emails,
				       m.id AS media_id, 
				       UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp, 
				       mma.focus_x AS media_focus_x, 
				       mma.focus_y AS media_focus_y, 
				       mma.primary_color_hex AS media_primary_color_hex,
				       l.when AS last_login
				FROM user_region ur
				JOIN user_login l ON l.user_id = ur.user_id AND l.region_id = ur.region_id
				JOIN user u ON u.id = ur.user_id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				WHERE ur.region_id = ?
				  AND (ur.admin_write = 1 OR ur.superadmin_write = 1)
				ORDER BY name
				""")
				.param(setup.idRegion())
				.query((rs, _) -> {
					int mediaId = rs.getInt("media_id");
					MediaIdentity mediaIdentity = (mediaId > 0) 
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex"))
									: null;

					String emailsStr = rs.getString("emails");
					List<String> emails = (emailsStr == null || emailsStr.isBlank()) ? null : List.of(emailsStr.split(";"));

					return new Administrator(
							rs.getInt("id"), 
							rs.getString("name"), 
							emails, 
							mediaIdentity, 
							TimeAgo.getTimeAgo(rs.getObject("last_login", LocalDate.class))
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public Optional<AuthenticatedUser> getAuthenticatedUser(Setup setup, Optional<Integer> authUserId) {
		if (authUserId.isEmpty()) return Optional.empty();

		return jdbcClient.sql("""
				SELECT ur.admin_write, ur.superadmin_write,
				       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) authenticated_name, u.theme_preference,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM user u
				LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?)
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE u.id=?
				""")
				.params(setup.idRegion(), authUserId.get())
				.query((rs, _) -> {
					boolean isSuperAdmin = rs.getBoolean("superadmin_write");
					boolean isAdmin = isSuperAdmin || rs.getBoolean("admin_write");

					int mediaId = rs.getInt("media_id");
					MediaIdentity mediaIdentity = (mediaId > 0) 
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex"))
									: null;

					return new AuthenticatedUser(
							true, 
							isAdmin, 
							isSuperAdmin, 
							rs.getInt("user_id"), 
							rs.getString("authenticated_name"), 
							rs.getString("theme_preference"), 
							mediaIdentity
							);
				})
				.optional();
	}

	@Transactional
	public Optional<Integer> getAuthUserId(Auth0Profile profile) {
		return jdbcClient.sql("SELECT user_id FROM user_email WHERE lower(email)=?")
				.param(1, profile.email().toLowerCase())
				.query(Integer.class)
				.optional()
				.or(() -> Optional.of(addUser(profile.email(), profile.firstname(), profile.lastname())));
	}

	@Transactional(readOnly = true)
	public List<PermissionUser> getPermissions(Setup setup, Optional<Integer> authUserId) {
		return jdbcClient.sql("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) AS name,
				       m.id AS media_id,
				       UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp,
				       mma.focus_x AS media_focus_x,
				       mma.focus_y AS media_focus_y,
				       mma.primary_color_hex AS media_primary_color_hex,
				       l.when AS last_login,
				       COALESCE(ur.admin_read, 0) AS admin_read,
				       COALESCE(ur.admin_write, 0) AS admin_write,
				       COALESCE(ur.superadmin_read, 0) AS superadmin_read,
				       COALESCE(ur.superadmin_write, 0) AS superadmin_write
				FROM user u
				LEFT JOIN user_region ur ON u.id = ur.user_id AND ur.region_id = ?
				LEFT JOIN user_login l ON u.id = l.user_id AND l.region_id = ?
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				WHERE ur.user_id IS NOT NULL OR l.user_id IS NOT NULL
				ORDER BY superadmin_write DESC, superadmin_read DESC, admin_write DESC, admin_read DESC, name
				""")
				.params(setup.idRegion(), setup.idRegion())
				.query((rs, _) -> {
					int mediaId = rs.getInt("media_id");
					MediaIdentity mediaIdentity = (mediaId > 0)
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex"))
									: null;

					return new PermissionUser(
							rs.getInt("id"),
							rs.getString("name"),
							mediaIdentity,
							TimeAgo.getTimeAgo(rs.getObject("last_login", LocalDate.class)),
							rs.getBoolean("admin_read"),
							rs.getBoolean("admin_write"),
							rs.getBoolean("superadmin_read"),
							rs.getBoolean("superadmin_write"),
							authUserId.orElse(0) == rs.getInt("id")
							);
				})
				.list();
	}

	@Transactional(readOnly = true)
	public List<ProfileAscent> getProfileAscents(Optional<Integer> authUserId, Setup setup, int reqId) {
		List<ProfileAscent> res = new ArrayList<>();
		Map<Integer, ProfileAscent> idProblemTickMap = new HashMap<>();

		res.addAll(jdbcClient.sql("""
				SELECT r.name region_name, a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id id_tick, 0 id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches,
				       p.id id_problem, p.nr, p.locked_admin, p.locked_superadmin, p.name,
				       CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment,
				       DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date,
				       DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr,
				       CASE WHEN t.id IS NULL THEN -1 ELSE t.stars END stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa,
				       (CASE WHEN t.id IS NOT NULL AND gt.grade IS NOT NULL THEN gt.weight ELSE g.weight END) grade_weight,
				       (CASE WHEN t.id IS NOT NULL AND gt.grade IS NOT NULL THEN gt.grade ELSE g.grade END) grade,
				       CASE WHEN t.id IS NOT NULL AND gt.id IS NULL THEN 1 ELSE 0 END no_personal_grade
				FROM problem p
				JOIN grade g ON p.grade_id=g.id JOIN type ty ON p.type_id=ty.id JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id JOIN region r ON a.region_id=r.id JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN problem_section ps ON p.id=ps.problem_id
				LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?
				LEFT JOIN grade gt ON t.grade_id=gt.id LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)
				WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL)
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, g.grade, gt.grade
				""")
				.params(authUserId.orElse(0), reqId, reqId, setup.idRegion())
				.query((rs, _) -> {
					var tick = new ProfileAscent(rs.getString("region_name"), rs.getInt("area_id"), rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), rs.getInt("sector_id"), rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"), rs.getInt("id_tick"), rs.getInt("id_tick_repeat"), (rs.getInt("num_pitches") > 1 ? "Multi-pitch " : "") + rs.getString("subtype"), rs.getInt("num_pitches"), rs.getInt("id_problem"), rs.getInt("nr"), rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getString("name"), rs.getString("comment"), rs.getString("date"), rs.getString("date_hr"), rs.getDouble("stars"), rs.getBoolean("fa"), rs.getString("grade"), rs.getInt("grade_weight"), rs.getBoolean("no_personal_grade"));
					idProblemTickMap.put(tick.getIdProblem(), tick);
					return tick;
				}).list());

		res.addAll(jdbcClient.sql("""
				SELECT r.name region_name, a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id id_tick, tr.id id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches,
				       p.id id_problem, p.nr, p.locked_admin, p.locked_superadmin, p.name, tr.comment,
				       DATE_FORMAT(tr.date,'%Y-%m-%d') date, DATE_FORMAT(tr.date,'%d/%m-%y') date_hr, t.stars, 0 fa, g.weight grade_weight, g.grade
				FROM problem p JOIN type ty ON p.type_id=ty.id JOIN sector s ON p.sector_id=s.id JOIN area a ON s.area_id=a.id JOIN region r ON a.region_id=r.id JOIN region_type rt ON r.id=rt.region_id
				JOIN tick t ON p.id=t.problem_id AND t.user_id=? LEFT JOIN grade g ON t.grade_id=g.id JOIN tick_repeat tr ON t.id=tr.tick_id
				LEFT JOIN problem_section ps ON p.id=ps.problem_id LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY s.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, tr.id, ty.subtype, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, tr.comment, tr.date, t.stars, g.weight, g.grade
				""")
				.params(reqId, authUserId.orElse(0), setup.idRegion())
				.query((rs, _) -> new ProfileAscent(rs.getString("region_name"), rs.getInt("area_id"), rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), rs.getInt("sector_id"), rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"), rs.getInt("id_tick"), rs.getInt("id_tick_repeat"), (rs.getInt("num_pitches") > 1 ? "Multi-pitch " : "") + rs.getString("subtype"), rs.getInt("num_pitches"), rs.getInt("id_problem"), rs.getInt("nr"), rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getString("name"), rs.getString("comment"), rs.getString("date"), rs.getString("date_hr"), rs.getDouble("stars"), rs.getBoolean("fa"), rs.getString("grade"), rs.getInt("grade_weight"), rs.getString("grade") == null))
				.list());

		if (!setup.isBouldering()) {
			jdbcClient.sql("""
					SELECT r.name region_name, a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
					       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, COUNT(DISTINCT ps.id) num_pitches,
					       p.id id_problem, p.nr, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr
					FROM problem p JOIN sector s ON p.sector_id=s.id JOIN area a ON s.area_id=a.id JOIN region r ON a.region_id=r.id JOIN region_type rt ON r.id=rt.region_id
					JOIN fa_aid aid ON p.id=aid.problem_id JOIN fa_aid_user aid_u ON p.id=aid_u.problem_id AND aid_u.user_id=?
					LEFT JOIN problem_section ps ON p.id=ps.problem_id LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
					WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date
					""")
			.params(reqId, authUserId.orElse(0), setup.idRegion())
			.query(rs -> {
				int pid = rs.getInt("id_problem");
				var existing = idProblemTickMap.get(pid);
				if (existing != null) {
					existing.setFa(true);
					if (existing.getDate() == null) existing.setDate(rs.getString("date"));
					if (existing.getDateHr() == null) existing.setDateHr(rs.getString("date_hr"));
				} else {
					var tick = new ProfileAscent(rs.getString("region_name"), rs.getInt("area_id"), rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), rs.getInt("sector_id"), rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"), 0, 0, "Aid", rs.getInt("num_pitches"), pid, rs.getInt("nr"), rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getString("name"), (rs.getString("description") != null && !rs.getString("description").isBlank() ? "First ascent (AID): " + rs.getString("description") : "First ascent (AID)"), rs.getString("date"), rs.getString("date_hr"), 0, true, "n/a", 0, false);
					idProblemTickMap.put(pid, tick);
					res.add(tick);
				}
			});
		}

		if (!idProblemTickMap.isEmpty()) {
			var coords = getProblemCoordinates(new ArrayList<>(idProblemTickMap.keySet()));
			idProblemTickMap.forEach((id, tick) -> tick.setCoordinates(coords.get(id)));
		}

		res.sort((t1, t2) -> {
			int cmp = (t2.getDate() == null ? "" : t2.getDate()).compareTo(t1.getDate() == null ? "" : t1.getDate());
			if (cmp == 0) cmp = Integer.compare(t2.getId(), t1.getId());
			return (cmp == 0) ? Integer.compare(t2.getIdProblem(), t1.getIdProblem()) : cmp;
		});

		for (int i = 0; i < res.size(); i++) res.get(i).setNum(i);
		return res;
	}

	@Transactional(readOnly = true)
	public List<ProfileDiscipline> getProfileDisciplines(Setup setup, int userId) {
		var start = System.nanoTime();

		var res = jdbcClient.sql("""
				WITH req AS (
				    SELECT ? AS user_id, ? AS req_is_bouldering, ? AS req_is_climbing, ? AS req_is_ice
				),
				top_urls AS (
				    SELECT discipline, url
				    FROM (
				        SELECT 
				            ty.group AS discipline,
				            r.url,
				            ROW_NUMBER() OVER (PARTITION BY ty.group ORDER BY COUNT(*) DESC) as rn
				        FROM (
				            SELECT problem_id FROM fa WHERE user_id = (SELECT user_id FROM req)
				            UNION ALL
				            SELECT problem_id FROM tick WHERE user_id = (SELECT user_id FROM req)
				        ) active_problems
				        JOIN problem p ON active_problems.problem_id = p.id
				        JOIN type ty ON p.type_id = ty.id
				        JOIN sector s ON p.sector_id = s.id
				        JOIN area a ON s.area_id = a.id
				        JOIN region r ON a.region_id = r.id
				        GROUP BY ty.group, r.url
				    ) ranked_regions
				    WHERE rn = 1
				),
				pitch_counts AS (
				    SELECT problem_id, COUNT(*) AS total_pitches
				    FROM problem_section
				    GROUP BY problem_id
				),
				raw_activity AS (
				    SELECT 
				        ty.group AS discipline, g.grade, clr.hex_code color, g.weight, 1 is_fa, 0 is_tick,
				        CASE WHEN ty.id=2 THEN 1 ELSE 0 END is_bolted,
				        COALESCE(pc.total_pitches, 0) AS pitches
				    FROM req
				    JOIN fa f ON f.user_id = req.user_id
				    JOIN problem p ON f.problem_id = p.id
				    JOIN type ty ON p.type_id = ty.id
				    JOIN grade g ON p.grade_id = g.id
				    JOIN grade_color clr ON g.grade_color_id = clr.id
				    LEFT JOIN pitch_counts pc ON p.id = pc.problem_id
				    UNION ALL
				    SELECT 
				        ty.group AS discipline, COALESCE(g.grade,'No personal grade') grade, 
				        COALESCE(clr.hex_code,'#CCCCCC') color, g.weight, 0 is_fa, 1 is_tick,
				        CASE WHEN ty.id=2 THEN 1 ELSE 0 END is_bolted,
				        COALESCE(pc.total_pitches, 0) AS pitches
				    FROM req
				    JOIN tick t ON t.user_id = req.user_id
				    JOIN problem p ON t.problem_id = p.id
				    JOIN type ty ON p.type_id = ty.id
				    LEFT JOIN grade g ON t.grade_id = g.id
				    LEFT JOIN grade_color clr ON g.grade_color_id = clr.id
				    LEFT JOIN pitch_counts pc ON p.id = pc.problem_id
				    WHERE NOT EXISTS (SELECT 1 FROM fa f2 WHERE f2.user_id = req.user_id AND f2.problem_id = t.problem_id)
				),
				categorized_activity AS (
				    SELECT 
				        discipline, grade, color, weight, is_fa, is_tick,
				        CASE 
				            WHEN discipline = 'Bouldering' THEN 'Boulder problems'
				            WHEN discipline = 'Ice' THEN 'Climbing routes (ice)'
				            WHEN pitches > 0 THEN 'Climbing routes (multi-pitch)'
				            WHEN is_bolted = 1 THEN 'Climbing routes (single-pitch bolted)'
				            ELSE 'Climbing routes (single-pitch traditional)'
				        END AS type,
				        CASE 
				            WHEN discipline = 'Bouldering' THEN 'Boulder'
				            WHEN discipline = 'Ice' THEN 'Ice'
				            WHEN pitches > 0 THEN 'Multi'
				            WHEN is_bolted = 1 THEN 'Single bolted'
				            ELSE 'Single traditional'
				        END AS internal_subtype
				    FROM raw_activity
				)
				SELECT 
				    v.type,
				    CONCAT(MAX(u.url), '/user/', req.user_id) url,
				    v.grade, v.color, SUM(v.is_fa) num_fa, SUM(v.is_tick) num_tick
				FROM categorized_activity v
				JOIN req ON true
				LEFT JOIN top_urls u ON u.discipline = (CASE WHEN v.discipline IN ('Bouldering', 'Ice') THEN v.discipline ELSE 'Climbing' END)
				GROUP BY req.user_id, req.req_is_bouldering, req.req_is_climbing, req.req_is_ice, v.type, v.grade, v.color, v.weight, v.internal_subtype
				ORDER BY 
				    CASE 
				        WHEN req.req_is_bouldering = 1 THEN (CASE WHEN v.internal_subtype = 'Boulder' THEN 1 WHEN v.internal_subtype = 'Single bolted' THEN 2 WHEN v.internal_subtype = 'Single traditional' THEN 3 WHEN v.internal_subtype = 'Multi' THEN 4 ELSE 5 END)
				        WHEN req.req_is_climbing = 1 THEN (CASE WHEN v.internal_subtype = 'Single bolted' THEN 1 WHEN v.internal_subtype = 'Single traditional' THEN 2 WHEN v.internal_subtype = 'Multi' THEN 3 WHEN v.internal_subtype = 'Ice' THEN 4 ELSE 5 END)
				        WHEN req.req_is_ice = 1 THEN (CASE WHEN v.internal_subtype = 'Ice' THEN 1 WHEN v.internal_subtype = 'Single bolted' THEN 2 WHEN v.internal_subtype = 'Single traditional' THEN 3 WHEN v.internal_subtype = 'Multi' THEN 4 ELSE 5 END)
				        ELSE (CASE WHEN v.internal_subtype = 'Boulder' THEN 1 WHEN v.internal_subtype = 'Single bolted' THEN 2 WHEN v.internal_subtype = 'Single traditional' THEN 3 WHEN v.internal_subtype = 'Multi' THEN 4 ELSE 5 END)
				    END,
				    v.weight DESC
				""")
				.params(userId, setup.isBouldering(), setup.isClimbing(), setup.isIce())
				.query((rs, _) -> {
					String type = rs.getString("type");
					String url = rs.getString("url");
					String grade = rs.getString("grade");
					String color = rs.getString("color");
					int fa = rs.getInt("num_fa");
					int tick = rs.getInt("num_tick");
					return new Object[] { type, url, new ProfileDisciplineGradeDistribution(grade, color, fa, tick) };
				})
				.list();

		List<ProfileDiscipline> result = new ArrayList<>();
		String currentType = null;
		String currentUrl = null;
		List<ProfileDisciplineGradeDistribution> distributions = null;

		for (Object[] row : res) {
			String type = (String) row[0];
			String url = (String) row[1];
			ProfileDisciplineGradeDistribution dist = (ProfileDisciplineGradeDistribution) row[2];

			if (!type.equals(currentType)) {
				if (currentType != null) {
					result.add(new ProfileDiscipline(currentType, currentUrl, distributions));
				}
				currentType = type;
				currentUrl = url;
				distributions = new ArrayList<>();
			}
			distributions.add(dist);
		}
		if (currentType != null) {
			result.add(new ProfileDiscipline(currentType, currentUrl, distributions));
		}

		logger.debug("getProfileDisciplines(userId={}) - res.size()={}, duration={}", userId, result.size(), Duration.ofNanos(System.nanoTime() - start));
		return result;
	}

	@Transactional(readOnly = true)
	public ProfileIdentity getProfileIdentity(Setup setup, int userId) {
		var start = System.nanoTime();

		var res = jdbcClient.sql("""
				SELECT u.firstname, u.lastname, u.email_visible_to_all, u.theme_preference,
				       m.id AS media_id, UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp,
				       mma.focus_x AS media_focus_x, mma.focus_y AS media_focus_y, mma.primary_color_hex AS media_primary_color_hex,
				       e.emails, l.last_login
				FROM user u
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN (SELECT user_id, GROUP_CONCAT(DISTINCT email ORDER BY email SEPARATOR ';') AS emails
				           FROM user_email WHERE email NOT LIKE '%@missing-email.com' GROUP BY user_id) e 
				           ON e.user_id = u.id AND u.email_visible_to_all = 1
				LEFT JOIN (SELECT user_id, MAX(`when`) AS last_login FROM user_login GROUP BY user_id) l ON l.user_id = u.id
				WHERE u.id = ?
				""")
				.param(1, userId)
				.query((rs, _) -> {
					int mediaId = rs.getInt("media_id");
					MediaIdentity mediaIdentity = (mediaId > 0)
							? new MediaIdentity(mediaId, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex"))
									: null;

					String emailsStr = rs.getString("emails");
					List<String> emails = (emailsStr == null || emailsStr.isBlank()) ? null : List.of(emailsStr.split(";"));

					var lastLogin = rs.getObject("last_login", LocalDateTime.class);
					return new ProfileIdentity(
							userId,
							rs.getString("firstname"),
							rs.getString("lastname"),
							rs.getBoolean("email_visible_to_all"),
							rs.getString("theme_preference"),
							mediaIdentity,
							emails,
							getUserRegion(userId, setup),
							(lastLogin == null) ? null : TimeAgo.getTimeAgo(lastLogin.toLocalDate())
							);
				})
				.optional()
				.orElseThrow(() -> new NoSuchElementException("Could not find user with id=" + userId));

		logger.debug("getProfileIdentity(authUserId={}) - res={}, duration={}", userId, res, Duration.ofNanos(System.nanoTime() - start));
		return res;
	}

	@Transactional(readOnly = true)
	public ProfileKpis getProfileKpis(int userId) {
		var start = System.nanoTime();

		ProfileKpis res = jdbcClient.sql("""
				WITH req AS (SELECT ? user_id),
				valid_media AS (
				    SELECT m.id, m.is_movie, m.photographer_user_id
				    FROM req
				    CROSS JOIN media m
				    LEFT JOIN media_problem mp ON m.id = mp.media_id
				    LEFT JOIN media_sector ms ON m.id = ms.media_id
				    LEFT JOIN media_area ma ON m.id = ma.media_id
				    LEFT JOIN media_trail mt ON m.id = mt.media_id
				    WHERE m.deleted_user_id IS NULL
				      AND (m.photographer_user_id = req.user_id OR EXISTS (SELECT 1 FROM media_user mu WHERE mu.media_id = m.id AND mu.user_id = req.user_id))
				      AND (mp.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR ma.media_id IS NOT NULL OR mt.media_id IS NOT NULL OR m.embed_url IS NOT NULL)
				)
				SELECT 
				    COUNT(DISTINCT CASE WHEN vm.photographer_user_id = req.user_id AND vm.is_movie = 0 THEN vm.id END) as created_img,
				    COUNT(DISTINCT CASE WHEN vm.photographer_user_id = req.user_id AND vm.is_movie = 1 THEN vm.id END) as created_vid,
				    COUNT(DISTINCT CASE WHEN mu.user_id = req.user_id AND vm.is_movie = 0 THEN vm.id END) as tagged_img,
				    COUNT(DISTINCT CASE WHEN mu.user_id = req.user_id AND vm.is_movie = 1 THEN vm.id END) as tagged_vid
				FROM req
				CROSS JOIN valid_media vm
				LEFT JOIN media_user mu ON vm.id = mu.media_id AND mu.user_id = req.user_id
				""")
				.param(1, userId)
				.query((rs, _) -> new ProfileKpis(
						rs.getInt("created_img"),
						rs.getInt("created_vid"),
						rs.getInt("tagged_img"),
						rs.getInt("tagged_vid")
						))
				.single();

		logger.debug("getProfileKpis(userId={}) - res={}, duration={}", userId, res, Duration.ofNanos(System.nanoTime() - start));
		return res;
	}

	@Transactional(readOnly = true)
	public ProfileTodo getProfileTodo(Optional<Integer> authUserId, Setup setup, int userId) {
		var start = System.nanoTime();
		ProfileTodo res = new ProfileTodo(new ArrayList<>());
		Map<Integer, ProfileTodoArea> areaLookup = new HashMap<>();
		Map<Integer, ProfileTodoSector> sectorLookup = new HashMap<>();

		RowMapper<ProfileTodoProblem> mapper = (rs, _) -> {
			int areaId = rs.getInt("area_id");
			ProfileTodoArea a = areaLookup.computeIfAbsent(areaId, id -> {
				try {
					var newArea = new ProfileTodoArea(id, rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), new ArrayList<>());
					res.areas().add(newArea);
					return newArea;
				} catch (SQLException e) { throw new RuntimeException(e); }
			});

			int sectorId = rs.getInt("sector_id");
			ProfileTodoSector s = sectorLookup.computeIfAbsent(sectorId, id -> {
				try {
					var newSector = new ProfileTodoSector(id, rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"), new ArrayList<>());
					a.sectors().add(newSector);
					return newSector;
				} catch (SQLException e) { throw new RuntimeException(e); }
			});

			int coordId = rs.getInt("coord_id");
			Coordinates coords = (!rs.wasNull() && coordId > 0) 
					? new Coordinates(coordId, rs.getDouble("lat"), rs.getDouble("lon"), rs.getDouble("ele"), rs.getString("ele_src"))
							: null;

			List<User> partners = new ArrayList<>();
			String raw = rs.getString("partners_raw");
			if (raw != null && !raw.isEmpty()) {
				for (String part : raw.split("\\|")) {
					String[] bits = part.split(":", 2);
					if (bits.length == 2) partners.add(User.from(Integer.parseInt(bits[0]), bits[1]));
				}
			}

			ProfileTodoProblem p = new ProfileTodoProblem(rs.getInt("todo_id"), rs.getInt("problem_id"), rs.getBoolean("problem_locked_admin"), rs.getBoolean("problem_locked_superadmin"), rs.getInt("problem_nr"), rs.getString("problem_name"), rs.getString("problem_grade"), coords, partners);
			s.problems().add(p);
			return p;
		};

		jdbcClient.sql("""
				WITH req AS (SELECT ? user_id, ? auth_user_id, ? region_id)
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
				        FROM todo t_other JOIN user u_other ON t_other.user_id = u_other.id CROSS JOIN req
				        WHERE t_other.problem_id = p.id AND t_other.user_id != req.user_id) AS partners_raw
				FROM req JOIN todo t ON t.user_id = req.user_id JOIN problem p ON t.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id JOIN area a ON s.area_id = a.id
				JOIN region_type rt ON a.region_id = rt.region_id JOIN grade g ON p.grade_id = g.id
				LEFT JOIN coordinates ac ON a.coordinates_id = ac.id LEFT JOIN coordinates sc ON s.parking_coordinates_id = sc.id
				LEFT JOIN coordinates pc ON p.coordinates_id = pc.id LEFT JOIN sector_outline so ON s.id = so.sector_id AND so.sorting = 1
				LEFT JOIN coordinates oc ON so.coordinates_id = oc.id LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				  AND (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY p.id, t.id
				ORDER BY a.name, s.name, p.nr
				""")
		.params(userId, authUserId.orElse(0), setup.idRegion())
		.query(mapper)
		.list();

		res.areas().sort(Comparator.comparing(ProfileTodoArea::name));
		logger.debug("getProfileTodo(id={}) - duration={}", userId, Duration.ofNanos(System.nanoTime() - start));
		return res;
	}

	@Transactional(readOnly = true)
	public List<User> getUserSearch(Optional<Integer> authUserId, String value) {
		if (authUserId.isEmpty()) {
			throw new UnauthorizedException("User not logged in...");
		}

		if (value == null || value.isBlank()) {
			return Collections.emptyList();
		}

		var searchRegexPattern = "(^|\\W)" + Pattern.quote(value);

		List<User> res = jdbcClient.sql("""
				SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name
				FROM user u
				WHERE regexp_like(TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))),?,'i')
				ORDER BY u.firstname, u.lastname
				""")
				.param(1, searchRegexPattern)
				.query((rs, _) -> User.from(rs.getInt("id"), rs.getString("name")))
				.list();

		var grouped = res.stream().collect(Collectors.groupingBy(User::name));

		for (int i = 0; i < res.size(); i++) {
			User u = res.get(i);
			if (grouped.get(u.name()).size() > 1) {
				res.set(i, u.withIdAsNameSuffix());
			}
		}

		return res;
	}

	@Transactional(readOnly = true)
	public byte[] getUserTicks(Optional<Integer> authUserId) {
		int userId = authUserId.orElseThrow();

		try (var workbook = new ExcelWorkbook(); var os = new ByteArrayOutputStream()) {
			Map<String, ExcelSheet> sheets = new HashMap<>();

			RowCallbackHandler handler = rs -> {
				try {
					String sheetName = rs.getString("type");
					var sheet = sheets.computeIfAbsent(sheetName, workbook::addSheet);
					sheet.incrementRow();
					sheet.writeString("AREA", rs.getString("area_name"));
					sheet.writeString("SECTOR", rs.getString("sector_name"));
					var subType = rs.getString("subtype");
					if (subType != null) {
						sheet.writeString("TYPE", subType);
						int pitches = rs.getInt("num_pitches");
						sheet.writeInt("PITCHES", pitches > 0 ? pitches : 1);
					}
					sheet.writeString("NAME", rs.getString("name"));
					sheet.writeString("FIRST ASCENT", rs.getBoolean("fa") ? "Yes" : "No");
					sheet.writeDate("DATE", rs.getObject("date", LocalDate.class));
					sheet.writeString("GRADE", rs.getString("grade"));
					sheet.writeDouble("STARS", rs.getDouble("stars"));
					sheet.writeString("DESCRIPTION", rs.getString("comment"));
					sheet.writeHyperlink("URL", rs.getString("url"));
				} catch (Exception e) {
					logger.error("Error processing row for sheet processing", e);
					throw new RuntimeException(e);
				}
			};

			jdbcClient.sql("""
					SELECT 
					    ty.type, pt.subtype, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    IF(t.id IS NOT NULL, t.comment, p.description) AS comment,
					    DATE_FORMAT(IF(t.date IS NULL AND f.user_id IS NOT NULL, p.fa_date, t.date), '%Y-%m-%d') AS date,
					    t.stars, IF(f.user_id IS NOT NULL, 1, 0) AS fa, g.grade,
					    COUNT(DISTINCT ps.id) AS num_pitches
					FROM problem p
					JOIN type pt ON p.type_id = pt.id JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id JOIN region r ON a.region_id = r.id
					JOIN region_type rt ON r.id = rt.region_id AND rt.type_id = p.type_id
					JOIN type ty ON rt.type_id = ty.id JOIN type_grade_system tgs ON p.type_id = tgs.type_id
					LEFT JOIN problem_section ps ON p.id = ps.problem_id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					LEFT JOIN tick t ON p.id = t.problem_id AND t.user_id = ?
					LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = ?
					LEFT JOIN grade g ON g.grade_system_id = tgs.grade_system_id 
					    AND g.id = IF(t.id IS NOT NULL, t.grade_id, p.grade_id)
					WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL)
					  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					GROUP BY p.id, t.id, ty.type, pt.subtype, r.url, a.name, s.name, p.name, t.comment, p.description, t.date, p.fa_date, t.stars, f.user_id, g.grade
					ORDER BY ty.type, a.name, s.name, p.name
					""")
			.params(userId, userId, userId)
			.query(handler);

			jdbcClient.sql("""
					SELECT 
					    CONCAT(ty.type, ' (repeats)') AS type, pt.subtype, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    tr.comment, DATE_FORMAT(tr.date, '%Y-%m-%d') AS date, t.stars, 0 AS fa, g.grade,
					    COUNT(DISTINCT ps.id) AS num_pitches
					FROM problem p
					JOIN type pt ON p.type_id = pt.id JOIN sector s ON p.sector_id = s.id
					JOIN area a ON s.area_id = a.id JOIN region r ON a.region_id = r.id
					JOIN region_type rt ON r.id = rt.region_id AND rt.type_id = p.type_id
					JOIN type ty ON rt.type_id = ty.id JOIN type_grade_system tgs ON p.type_id = tgs.type_id
					JOIN tick t ON p.id = t.problem_id AND t.user_id = ?
					JOIN tick_repeat tr ON t.id = tr.tick_id
					LEFT JOIN problem_section ps ON p.id = ps.problem_id
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					LEFT JOIN grade g ON g.grade_system_id = tgs.grade_system_id AND g.id = t.grade_id
					WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					GROUP BY tr.id, p.id, ty.type, pt.subtype, r.url, a.name, s.name, p.name, tr.comment, tr.date, t.stars, g.grade
					ORDER BY ty.type, a.name, s.name, p.name, tr.date
					""")
			.params(userId, userId)
			.query(handler);

			jdbcClient.sql("""
					SELECT 
					    p.id AS problem_id, r.url, a.name AS area_name, s.name AS sector_name, p.name, 
					    aid.aid_description AS comment, DATE_FORMAT(aid.aid_date, '%Y-%m-%d') AS date
					FROM problem p
					JOIN sector s ON p.sector_id = s.id JOIN area a ON s.area_id = a.id JOIN region r ON a.region_id = r.id
					JOIN fa_aid aid ON p.id = aid.problem_id
					JOIN fa_aid_user aid_u ON p.id = aid_u.problem_id AND aid_u.user_id = ?
					LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = ?
					WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					GROUP BY p.id, r.url, a.name, s.name, p.name, aid.aid_description, aid.aid_date
					ORDER BY a.name, s.name, p.name
					""")
			.params(userId, userId)
			.query(rs -> {
				ExcelSheet aidSheet = sheets.computeIfAbsent("First_AID_Ascent", _ -> workbook.addSheet("First_AID_Ascent"));
				aidSheet.incrementRow();
				aidSheet.writeString("AREA", rs.getString("area_name"));
				aidSheet.writeString("SECTOR", rs.getString("sector_name"));
				aidSheet.writeString("NAME", rs.getString("name"));
				aidSheet.writeDate("DATE", rs.getObject("date", LocalDate.class));
				aidSheet.writeString("DESCRIPTION", rs.getString("comment"));
				aidSheet.writeHyperlink("URL", rs.getString("url") + "/problem/" + rs.getInt("problem_id"));
			});

			if (sheets.isEmpty()) {
				logger.warn("Excel export for user {} generated an empty workbook (0 rows found).", userId);
			}
			workbook.write(os);
			return os.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Transactional(readOnly = true)
	public boolean hasAvatar(int userId) {
		return jdbcClient.sql("SELECT m.id FROM user u JOIN media m ON u.media_id = m.id WHERE u.id = ?")
				.param(userId)
				.query(Integer.class)
				.optional()
				.isPresent();
	}

	@Transactional
	public void setProfile(Optional<Integer> authUserId, ProfileIdentity profile) {
		if (authUserId.orElse(0) != profile.id()) {
			throw new ForbiddenException("Wrong input");
		}
		if (profile.firstname() == null || profile.firstname().isBlank()) {
			throw new IllegalArgumentException("Firstname cannot be null");
		}
		if (profile.lastname() == null || profile.lastname().isBlank()) {
			throw new IllegalArgumentException("Lastname cannot be null");
		}

		String theme = (profile.themePreference() != null && ("light".equals(profile.themePreference()) || "dark".equals(profile.themePreference()))) 
				? profile.themePreference() 
						: null;

		jdbcClient.sql("""
				UPDATE user 
				SET firstname=?, lastname=?, email_visible_to_all=?, 
				    theme_preference=COALESCE(theme_preference, ?) 
				WHERE id=?
				""")
		.params(
				profile.firstname(), 
				profile.lastname(), 
				profile.emailVisibleToAll(), 
				theme, 
				authUserId.orElseThrow()
				)
		.update();
	}

	@Transactional
	public void setThemePreference(Optional<Integer> authUserId, String themePreference) {
		if (themePreference == null || (!"light".equals(themePreference) && !"dark".equals(themePreference))) {
			throw new IllegalArgumentException("themePreference must be 'light' or 'dark'");
		}

		jdbcClient.sql("UPDATE user SET theme_preference=? WHERE id=?")
		.params(themePreference, authUserId.orElseThrow())
		.update();
	}

	@Transactional
	public void setUserRegion(Optional<Integer> authUserId, int regionId, boolean delete) {
		int userId = authUserId.orElseThrow();

		if (delete) {
			jdbcClient.sql("DELETE FROM user_region WHERE user_id=? AND region_id=?")
			.params(userId, regionId)
			.update();
		} else {
			jdbcClient.sql("INSERT INTO user_region (user_id, region_id, region_visible) VALUES (?, ?, 1)")
			.params(userId, regionId)
			.update();
		}
	}

	@Transactional
	public void upsertPermissionUser(Setup setup, PermissionUser u) {
		jdbcClient.sql("""
				INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) 
				VALUES (?, ?, ?, ?, ?, ?) 
				ON DUPLICATE KEY UPDATE 
				admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?
				""")
		.params(
				u.userId(), 
				setup.idRegion(), 
				u.adminRead(), 
				u.adminWrite(), 
				u.superadminRead(), 
				u.superadminWrite(),
				u.adminRead(), 
				u.adminWrite(), 
				u.superadminRead(), 
				u.superadminWrite()
				)
		.update();

		jdbcClient.sql("""
				DELETE FROM user_region 
				WHERE admin_read=0 AND admin_write=0 AND superadmin_read=0 AND superadmin_write=0 AND region_visible=0
				""")
		.update();
	}

	@Transactional
	public void upsertUserLogin(Setup setup, int userId, String headers) {
		jdbcClient.sql("""
				INSERT INTO user_login (user_id, region_id, headers) 
				VALUES (?, ?, ?) AS new_rows
				ON DUPLICATE KEY UPDATE `when` = CURRENT_TIMESTAMP, headers = new_rows.headers
				""")
		.params(userId, setup.idRegion(), headers)
		.update();
	}

	private Map<Integer, Coordinates> getProblemCoordinates(List<Integer> idProblems) {
		if (idProblems.isEmpty()) {
			throw new IllegalArgumentException("idProblems is empty");
		}

		String in = Collections.nCopies(idProblems.size(), "?").stream().collect(Collectors.joining(","));
		String sql = """
				SELECT p.id id_problem, COALESCE(pc.id,c.id,sc.id,ac.id) coordinates_id, 
				       COALESCE(pc.latitude,c.latitude,sc.latitude,ac.latitude) latitude, 
				       COALESCE(pc.longitude,c.longitude,sc.longitude,ac.longitude) longitude, 
				       COALESCE(pc.elevation,c.elevation,sc.elevation,ac.elevation) elevation, 
				       COALESCE(pc.elevation_source,c.elevation_source,sc.elevation_source,ac.elevation_source) elevation_source 
				FROM problem p 
				INNER JOIN sector s ON p.sector_id=s.id 
				INNER JOIN area a ON s.area_id=a.id 
				LEFT JOIN coordinates ac ON a.coordinates_id=ac.id 
				LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id 
				LEFT JOIN coordinates pc ON p.coordinates_id=pc.id 
				LEFT JOIN sector_outline so ON s.id=so.sector_id AND so.sorting=1 
				LEFT JOIN coordinates c ON so.coordinates_id=c.id 
				WHERE p.id IN (%s)
				""".formatted(in);

		Map<Integer, Coordinates> res = jdbcClient.sql(sql)
				.params(idProblems)
				.query((rs, _) -> {
					int idCoordinates = rs.getInt("coordinates_id");
					return (idCoordinates > 0) 
							? Map.entry(rs.getInt("id_problem"), new Coordinates(
									idCoordinates, 
									rs.getDouble("latitude"), 
									rs.getDouble("longitude"), 
									rs.getDouble("elevation"), 
									rs.getString("elevation_source")
									)) 
									: null;
				})
				.list()
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		logger.debug("getProblemCoordinates(idProblems.size()={}) - res.size()={}", idProblems.size(), res.size());
		return res;
	}

	private List<UserRegion> getUserRegion(int userId, Setup setup) {
		var start = System.nanoTime();

		List<UserRegion> res = jdbcClient.sql("""
				WITH req AS (SELECT ? region_id, ? user_id),
				target_types AS (SELECT rt.type_id FROM region_type rt JOIN req ON rt.region_id = req.region_id),
				user_activity AS (
				    SELECT DISTINCT region_id FROM (
				        SELECT a.region_id FROM fa f JOIN problem p ON f.problem_id = p.id JOIN sector s ON p.sector_id = s.id JOIN area a ON s.area_id = a.id JOIN req ON f.user_id = req.user_id
				        UNION ALL
				        SELECT a.region_id FROM tick t JOIN problem p ON t.problem_id = p.id JOIN sector s ON p.sector_id = s.id JOIN area a ON s.area_id = a.id JOIN req ON t.user_id = req.user_id
				    ) acts
				)
				SELECT r.id, r.name,
				       MAX(CASE WHEN r.id = req.region_id OR ur.admin_read = 1 OR ur.admin_write = 1 OR ur.superadmin_read = 1 OR ur.superadmin_write = 1 THEN 1 ELSE 0 END) AS read_only,
				       MAX(ur.region_visible) AS region_visible,
				       MAX(CASE WHEN ur.superadmin_write = 1 THEN 'Superadmin' WHEN ur.superadmin_read = 1 THEN 'Superadmin (read)' WHEN ur.admin_read = 1 THEN 'Admin (read)' WHEN ur.admin_write = 1 THEN 'Admin' END) AS role,
				       MAX(CASE WHEN ua.region_id IS NOT NULL THEN 1 ELSE 0 END) AS activity
				FROM req
				JOIN region r ON 1=1
				JOIN region_type rt ON r.id = rt.region_id
				LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.user_id
				LEFT JOIN user_activity ua ON r.id = ua.region_id
				WHERE rt.type_id IN (SELECT type_id FROM target_types)
				GROUP BY r.id, r.name 
				ORDER BY r.name
				""")
				.params(setup.idRegion(), userId)
				.query((rs, _) -> new UserRegion(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getString("role"),
						rs.getBoolean("read_only") || rs.getBoolean("region_visible"),
						rs.getBoolean("read_only"),
						rs.getBoolean("activity")
						))
				.list();

		logger.debug("getUserRegion() - res.size()={}, duration={}", res.size(), Duration.ofNanos(System.nanoTime() - start));
		return res;
	}

	protected int addUser(String email, String firstname, String lastname) {
		var keyHolder = new GeneratedKeyHolder();

		jdbcClient.sql("INSERT INTO user (firstname, lastname) VALUES (?, ?)")
		.params(firstname, lastname)
		.update(keyHolder);

		int id = keyHolder.getKey().intValue();

		if (id <= 0) {
			throw new IllegalStateException("Failed to generate ID for firstname=" + firstname + ", lastname=" + lastname);
		}

		if (email != null && !email.isBlank()) {
			jdbcClient.sql("""
					INSERT INTO user_email (user_id, email) 
					VALUES (?, ?) 
					ON DUPLICATE KEY UPDATE user_id=user_id
					""")
			.params(id, email.toLowerCase())
			.update();
		}

		logger.debug("addUser(email={}, firstname={}, lastname={}) - id={}", email, firstname, lastname, id);
		return id;
	}

	protected int getExistingOrInsertUser(String name) {
		if (name == null || name.isBlank()) {
			return USER_ID_UNKNOWN;
		}

		Integer existingId = jdbcClient.sql("SELECT id FROM user WHERE CONCAT(firstname, ' ', COALESCE(lastname,''))=?")
				.param(1, name)
				.query(Integer.class)
				.optional()
				.orElse(null);

		if (existingId != null) {
			return existingId;
		}

		int usId = addUser(null, name, null);
		if (usId <= 0) {
			throw new IllegalStateException("Failed to create user: " + name);
		}
		return usId;
	}
}
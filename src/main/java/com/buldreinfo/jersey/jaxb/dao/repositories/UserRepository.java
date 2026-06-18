package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Auth0Profile;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.excel.ExcelSheet;
import com.buldreinfo.jersey.jaxb.excel.ExcelWorkbook;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.AuthenticatedUser;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileDiscipline;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileDisciplineGradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileIdentity;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileKpis;
import com.buldreinfo.jersey.jaxb.model.ProfileAscent;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoArea;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoProblem;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo.ProfileTodoSector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserRegion;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

public record UserRepository(Dao dao) {
	private static final Logger logger = LogManager.getLogger();
	private static final int USER_ID_UNKNOWN = 1049;
	
	public void ensureUserExists(int userId) throws SQLException {
		Preconditions.checkArgument(userId > 0, "Invalid userId=%s", userId);
		var c = DatabaseContext.getConnection();
		var exists = false;
		try (var ps = c.prepareStatement("SELECT id FROM user WHERE id=?")) {
			ps.setInt(1, userId);
			try (var rst = ps.executeQuery()) {
				exists = rst.next();
			}
		}
		if (!exists) {
			throw new NoSuchElementException("Could not find user with id=" + userId);
		}
	}
	
	public List<Administrator> getAdministrators(Setup setup) throws SQLException {
		var res = new ArrayList<Administrator>();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) AS name,
				       CASE WHEN u.email_visible_to_all = 1 THEN 
				            (SELECT GROUP_CONCAT(DISTINCT e.email ORDER BY e.email SEPARATOR ';') 
				             FROM user_email e 
				             WHERE e.user_id = u.id AND e.email NOT LIKE '%@missing-email.com') 
				       END AS emails,
				       m.id AS media_id, 
				       UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp, 
				       mma.focus_x AS media_focus_x, 
				       mma.focus_y AS media_focus_y, 
				       mma.primary_color_hex AS media_primary_color_hex,
				       DATE_FORMAT(l.when, '%Y.%m.%d') AS last_login
				FROM user_region ur
				JOIN user_login l ON l.user_id = ur.user_id AND l.region_id = ur.region_id
				JOIN user u ON u.id = ur.user_id
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				WHERE ur.region_id = ?
				  AND (ur.admin_write = 1 OR ur.superadmin_write = 1)
				ORDER BY name;
				""")) {
			ps.setInt(1, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				final var formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					var name = rst.getString("name");
					var emailsStr = rst.getString("emails");
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
						var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, versionStamp, focusX, focusY, mediaPrimaryColorHex);
					}
					var lastLogin = rst.getString("last_login");
					var timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new Administrator(userId, name, emails, mediaIdentity, timeAgo));
				}
			}
		}
		return res;
	}

	public Optional<AuthenticatedUser> getAuthenticatedUser(Setup setup, Optional<Integer> authUserId) throws SQLException {
		if (authUserId.isEmpty()) {
			return Optional.empty();
		}
		var c = DatabaseContext.getConnection();
		var sql = """
				SELECT ur.admin_write, ur.superadmin_write,
				       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) authenticated_name, u.theme_preference,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM user u
				LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?)
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE u.id=?
				""";
		try (var ps = c.prepareStatement(sql)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.get());
			try (var rst = ps.executeQuery()) {
				if (rst.next()) {
					boolean isSuperAdmin = rst.getBoolean("superadmin_write");
					boolean isAdmin = isSuperAdmin || rst.getBoolean("admin_write");
					int userId = rst.getInt("user_id");
					var authenticatedName = rst.getString("authenticated_name");
					var themePreference = rst.getString("theme_preference");
					MediaIdentity mediaIdentity = null;
					int mediaId = rst.getInt("media_id");
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					return Optional.of(new AuthenticatedUser(true, isAdmin, isSuperAdmin, userId, authenticatedName, themePreference, mediaIdentity));
				}
			}
		}
		return Optional.empty();
	}
	
	public Optional<Integer> getAuthUserId(Auth0Profile profile) throws SQLException {
		Optional<Integer> authUserId = Optional.empty();
		var hasAvatar = false;
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT e.user_id, CASE WHEN m.id IS NOT NULL THEN 1 ELSE 0 END has_avatar
				FROM user_email e
				JOIN user u ON e.user_id=u.id
				LEFT JOIN media m ON u.media_id=m.id
				WHERE lower(e.email)=?
				""")) {
			ps.setString(1, profile.email().toLowerCase());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					authUserId = Optional.of(rst.getInt("user_id"));
					hasAvatar = rst.getBoolean("has_avatar");
				}
			}
		}
		if (authUserId.isEmpty()) {
			authUserId = Optional.of(addUser(profile.email(), profile.firstname(), profile.lastname()));
			try (var ps = c.prepareStatement("""
					SELECT CASE WHEN m.id IS NOT NULL THEN 1 ELSE 0 END has_avatar
					FROM user_email e
					JOIN user u ON e.user_id=u.id
					LEFT JOIN media m ON u.media_id=m.id
					WHERE lower(e.email)=?
					""")) {
				ps.setString(1, profile.email().toLowerCase());
				try (var rst = ps.executeQuery()) {
					if (rst.next()) {
						hasAvatar = rst.getBoolean("has_avatar");
					}
				}
			}
		}
		final var finalUserId = authUserId;
		if (!hasAvatar && profile.picture() != null) {
			try {
				byte[] avatarBytes;
				try (var remoteStream = URI.create(profile.picture()).toURL().openStream()) {
					avatarBytes = StorageManager.getInstance().readBoundedStream(remoteStream);
				}
				var photographer = User.from(USER_ID_UNKNOWN, null);
				var m = new Media(null, false, 0, 0, false, false, null, null, photographer, null, null, null, 0, null, null, 0, false, null, null, null, null, 0, finalUserId.get().intValue());
				dao.getMediaRepo().addMediaImage(finalUserId, m, StorageType.JPG, () -> new ByteArrayInputStream(avatarBytes));
			} catch (Exception e) {
				logger.error("Failed to cleanly download and apply login avatar profile image", e);
			}
		}
		logger.debug("getAuthUserId(profile={}) - authUserId={}", profile, authUserId);
		return authUserId;
	}
	
	public void upsertUserLogin(Setup setup, int userId, String headers) throws Exception {
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				INSERT INTO user_login (user_id, region_id, headers) 
				VALUES (?, ?, ?) AS new_rows
				ON DUPLICATE KEY UPDATE `when` = CURRENT_TIMESTAMP, headers = new_rows.headers
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, userId);
			ps.setInt(2, setup.idRegion());
			ps.setString(3, headers);
			ps.execute();
		}
	}

	public List<PermissionUser> getPermissions(Setup setup, Optional<Integer> authUserId) throws SQLException {
		dao.getRegionRepo().ensureAdminWriteRegion(setup, authUserId);
		var res = new ArrayList<PermissionUser>();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT u.id,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) AS name,
				       m.id AS media_id,
				       UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp,
				       mma.focus_x AS media_focus_x,
				       mma.focus_y AS media_focus_y,
				       mma.primary_color_hex AS media_primary_color_hex,
				       DATE_FORMAT(l.when, '%Y.%m.%d') AS last_login,
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
				ORDER BY COALESCE(ur.superadmin_write, 0) DESC,
				         COALESCE(ur.superadmin_read, 0) DESC,
				         COALESCE(ur.admin_write, 0) DESC,
				         COALESCE(ur.admin_read, 0) DESC,
				         name
				         """)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				final var formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					var name = rst.getString("name");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					var lastLogin = rst.getString("last_login");
					boolean adminRead = rst.getBoolean("admin_read");
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superadminRead = rst.getBoolean("superadmin_read");
					boolean superadminWrite = rst.getBoolean("superadmin_write");
					var timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new PermissionUser(userId, name, mediaIdentity, timeAgo, adminRead, adminWrite, superadminRead, superadminWrite, authUserId.orElse(0) == userId));
				}
			}
		}
		return res;
	}
	
	public List<ProfileAscent> getProfileAscents(Optional<Integer> authUserId, Setup setup, int reqId) throws SQLException {
		var res = new ArrayList<ProfileAscent>();
		var idProblemTickMap = new HashMap<Integer, ProfileAscent>();
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				SELECT r.name region_name,
				       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
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
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, g.grade, gt.grade
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var regionName = rst.getString("region_name");
					int areaId = rst.getInt("area_id");
					var areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
					var sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					var subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					int nr = rst.getInt("nr");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					var name = rst.getString("name");
					var comment = rst.getString("comment");
					var date = rst.getString("date");
					var dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int gradeWeight = rst.getInt("grade_weight");
					var grade = rst.getString("grade");
					boolean noPersonalGrade = rst.getBoolean("no_personal_grade");
					var tick = new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, nr, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, grade, gradeWeight, noPersonalGrade);
					res.add(tick);
					idProblemTickMap.put(idProblem, tick);
				}
			}
		}
		sqlStr = """
				SELECT r.name region_name,
				       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       t.id id_tick, tr.id id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches,
				       p.id id_problem, p.nr, p.locked_admin, p.locked_superadmin, p.name, tr.comment,
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
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY s.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.name, s.locked_admin, s.locked_superadmin, t.id, tr.id, ty.subtype, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, tr.comment, tr.date, t.stars, g.weight, g.grade
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var regionName = rst.getString("region_name");
					int areaId = rst.getInt("area_id");
					var areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
					var sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					var subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					int nr = rst.getInt("nr");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					var name = rst.getString("name");
					var comment = rst.getString("comment");
					var date = rst.getString("date");
					var dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int gradeWeight = rst.getInt("grade_weight");
					var grade = rst.getString("grade");
					boolean noPersonalGrade = grade == null;
					res.add(new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, nr, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, grade, gradeWeight, noPersonalGrade));
				}
			}
		}
		if (!setup.isBouldering()) {
			try (var ps = c.prepareStatement("""
					SELECT r.name region_name,
					       a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
					       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, COUNT(DISTINCT ps.id) num_pitches,
					       p.id id_problem, p.nr, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description,
					       DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr
					FROM problem p
					JOIN sector s ON p.sector_id=s.id
					JOIN area a ON s.area_id=a.id
					JOIN region r ON a.region_id=r.id
					JOIN region_type rt ON r.id=rt.region_id
					JOIN fa_aid aid ON p.id=aid.problem_id
					JOIN fa_aid_user aid_u ON p.id=aid_u.problem_id AND aid_u.user_id=?
					LEFT JOIN problem_section ps ON p.id=ps.problem_id
					LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
					WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.nr, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date
					""")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId.orElse(0));
				ps.setInt(3, setup.idRegion());
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						var regionName = rst.getString("region_name");
						int areaId = rst.getInt("area_id");
						var areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int sectorId = rst.getInt("sector_id");
						var sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int numPitches = rst.getInt("num_pitches");
						int idProblem = rst.getInt("id_problem");
						int nr = rst.getInt("nr");
						boolean lockedAdmin = rst.getBoolean("locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
						var name = rst.getString("name");
						var comment = rst.getString("description");
						if (!Strings.isNullOrEmpty(comment)) {
							comment = "First ascent (AID): " + comment;
						}
						else {
							comment = "First ascent (AID)";
						}
						var date = rst.getString("date");
						var dateHr = rst.getString("date_hr");
						var grade = "n/a";
						int gradeWeight = 0;
						boolean noPersonalGrade = false;
						var optTick = res.stream()
								.filter(x -> x.getIdProblem() == idProblem)
								.findAny();
						if (optTick.isPresent()) {
							var tick = optTick.get();
							tick.setFa(true);
							if (tick.getDate() == null && date != null) {
								tick.setDate(date);
							}
							if (tick.getDateHr() == null && dateHr != null) {
								tick.setDateHr(dateHr);
							}
						}
						else {
							var tick = new ProfileAscent(regionName, areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, 0, 0, "Aid", numPitches, idProblem, nr, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, 0, true, grade, gradeWeight, noPersonalGrade);
							idProblemTickMap.put(idProblem, tick);
						}
					}
				}
			}
		}
		if (!idProblemTickMap.isEmpty()) {
			var idProblemCoordinates = getProblemCoordinates(idProblemTickMap.keySet());
			for (int idProblem : idProblemCoordinates.keySet()) {
				var coordinates = idProblemCoordinates.get(idProblem);
				idProblemTickMap.get(idProblem).setCoordinates(coordinates);
			}
		}
		res.sort((t1, t2) -> -ComparisonChain
				.start()
				.compare(Strings.nullToEmpty(t1.getDate()), Strings.nullToEmpty(t2.getDate()))
				.compare(t1.getId(), t2.getId())
				.compare(t1.getIdProblem(), t2.getIdProblem())
				.result());
		for (var i = 0; i < res.size(); i++) {
			res.get(i).setNum(i);
		}
		return res;
	}
	
	public List<ProfileDiscipline> getProfileDisciplines(Setup setup, int userId) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<ProfileDiscipline>();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
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
						ty.group AS discipline,
						g.grade, 
						clr.hex_code color, 
						g.weight, 
						1 is_fa, 
						0 is_tick,
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
						ty.group AS discipline,
						COALESCE(g.grade,'No personal grade') grade, 
						COALESCE(clr.hex_code,'#CCCCCC') color, 
						g.weight, 
						0 is_fa, 
						1 is_tick,
						COALESCE(pc.total_pitches, 0) AS pitches
					FROM req
					JOIN tick t ON t.user_id = req.user_id
					JOIN problem p ON t.problem_id = p.id
					JOIN type ty ON p.type_id = ty.id
					LEFT JOIN grade g ON t.grade_id = g.id
					LEFT JOIN grade_color clr ON g.grade_color_id = clr.id
					LEFT JOIN pitch_counts pc ON p.id = pc.problem_id
					WHERE NOT EXISTS (
						SELECT 1 
						FROM fa f2 
						WHERE f2.user_id = req.user_id 
						  AND f2.problem_id = t.problem_id
					)
				),
				categorized_activity AS (
					SELECT 
						discipline,
						grade,
						color,
						weight,
						is_fa,
						is_tick,
						CASE 
							WHEN discipline = 'Bouldering' THEN 'Boulder problems'
							WHEN discipline = 'Ice' THEN 'Climbing routes (ice)'
							WHEN pitches > 0 THEN 'Climbing routes (multi-pitch)'
							ELSE 'Climbing routes (single-pitch)'
						END AS type,
						CASE 
							WHEN discipline = 'Bouldering' THEN 'Boulder'
							WHEN discipline = 'Ice' THEN 'Ice'
							WHEN pitches > 0 THEN 'Multi'
							ELSE 'Single'
						END AS internal_subtype
					FROM raw_activity
				)
				SELECT 
					v.type,
					CONCAT(MAX(u.url), '/user/', req.user_id) url,
					v.grade, 
					v.color, 
					SUM(v.is_fa) num_fa, 
					SUM(v.is_tick) num_tick
				FROM categorized_activity v
				JOIN req ON true
				LEFT JOIN top_urls u ON u.discipline = (
					CASE 
						WHEN v.discipline IN ('Bouldering', 'Ice') THEN v.discipline
						ELSE 'Climbing'
					END
				)
				GROUP BY 
					req.user_id,
					req.req_is_bouldering,
					req.req_is_climbing,
					req.req_is_ice,
					v.type, 
					v.grade, 
					v.color, 
					v.weight,
					v.internal_subtype
				ORDER BY 
					CASE 
						WHEN req.req_is_bouldering = 1 THEN 
							CASE 
								WHEN v.internal_subtype = 'Boulder' THEN 1
								WHEN v.internal_subtype = 'Single' THEN 2
								WHEN v.internal_subtype = 'Multi' THEN 3
								ELSE 4
							END
						WHEN req.req_is_climbing = 1 THEN 
							CASE 
								WHEN v.internal_subtype = 'Single' THEN 1
								WHEN v.internal_subtype = 'Multi' THEN 2
								WHEN v.internal_subtype = 'Ice' THEN 3
								ELSE 4
							END
						WHEN req.req_is_ice = 1 THEN 
							CASE 
								WHEN v.internal_subtype = 'Ice' THEN 1
								WHEN v.internal_subtype = 'Single' THEN 2
								WHEN v.internal_subtype = 'Multi' THEN 3
								ELSE 4
							END
						ELSE 
							CASE 
								WHEN v.internal_subtype = 'Boulder' THEN 1
								WHEN v.internal_subtype = 'Single' THEN 2
								WHEN v.internal_subtype = 'Multi' THEN 3
								ELSE 4
							END
					END,
					v.weight DESC
				""")) {
			ps.setInt(1, userId);
			ps.setBoolean(2, setup.isBouldering());
			ps.setBoolean(3, setup.isClimbing());
			ps.setBoolean(4, setup.isIce());
			try (var rst = ps.executeQuery()) {
				String currentType = null;
				String currentUrl = null;
				List<ProfileDisciplineGradeDistribution> distributions = null;
				while (rst.next()) {
					var type = rst.getString("type");
					var url = rst.getString("url");
					var grade = rst.getString("grade");
					var color = rst.getString("color");
					int fa = rst.getInt("num_fa");
					int tick = rst.getInt("num_tick");
					if (!type.equals(currentType)) {
						if (currentType != null) {
							res.add(new ProfileDiscipline(currentType, currentUrl, distributions));
						}
						currentType = type;
						currentUrl = url;
						distributions = new ArrayList<>();
					}
					distributions.add(new ProfileDisciplineGradeDistribution(grade, color, fa, tick));
				}

				if (currentType != null) {
					res.add(new ProfileDiscipline(currentType, currentUrl, distributions));
				}
			}
		}
		logger.debug("getProfileDisciplines(userId={}) - res.size()={}, duration={}", userId, res.size(), stopwatch);
		return res;
	}
	
	public ProfileIdentity getProfileIdentity(Setup setup, int userId) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT u.firstname, 
				       u.lastname, 
				       u.email_visible_to_all, 
				       u.theme_preference,
				       m.id AS media_id,  
				       UNIX_TIMESTAMP(m.updated_at) AS media_version_stamp,  
				       mma.focus_x AS media_focus_x, 
				       mma.focus_y AS media_focus_y, 
				       mma.primary_color_hex AS media_primary_color_hex,
				       e.emails, 
				       l.last_login
				FROM user u
				LEFT JOIN media m ON u.media_id = m.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN (
				    SELECT user_id, GROUP_CONCAT(DISTINCT email ORDER BY email SEPARATOR ';') AS emails
				    FROM user_email
				    WHERE email NOT LIKE '%@missing-email.com'
				    GROUP BY user_id
				) e ON e.user_id = u.id AND u.email_visible_to_all = 1
				LEFT JOIN (
				    SELECT user_id, MAX(`when`) AS last_login
				    FROM user_login
				    GROUP BY user_id
				) l ON l.user_id = u.id
				WHERE u.id = ?
				""")) {
			ps.setInt(1, userId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var firstname = rst.getString("firstname");
					var lastname = rst.getString("lastname");
					boolean emailVisibleToAll = rst.getBoolean("email_visible_to_all");
					var themePreference = rst.getString("theme_preference");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					var emailsStr = rst.getString("emails");
					List<String> emails = Strings.isNullOrEmpty(emailsStr) ? null : Splitter.on(';')
							.trimResults()
							.omitEmptyStrings()
							.splitToList(emailsStr);
					var userRegions = getUserRegion(userId, setup);
					var lastLogin = rst.getObject("last_login", LocalDateTime.class);
					var lastActivity = lastLogin == null ? null : TimeAgo.getTimeAgo(lastLogin.toLocalDate());
					var res = new ProfileIdentity(userId, firstname, lastname, emailVisibleToAll, themePreference, mediaIdentity, emails, userRegions, lastActivity);
					logger.debug("getProfileIdentity(authUserId={}) - res={}, duration={}", userId, res, stopwatch);
					return res;
				}
			}
		}
		throw new NoSuchElementException("Could not find user with id=" + userId);
	}
	
	public ProfileKpis getProfileKpis(int userId) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		ProfileKpis res = null;
		var c = DatabaseContext.getConnection();
		var sqlStr = """
				WITH req AS (
				    SELECT ? user_id
				),
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
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, userId);
			try (var rst = ps.executeQuery()) {
				if (rst.next()) {
					res = new ProfileKpis(rst.getInt("created_img"), rst.getInt("created_vid"), rst.getInt("tagged_img"), rst.getInt("tagged_vid"));
				}
			}
		}
		logger.debug("getProfileKpis(userId={}) - res={}, duration={}", userId, res, stopwatch);
		return res;
	}
	
	public ProfileTodo getProfileTodo(Optional<Integer> authUserId, Setup setup, int userId) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ProfileTodo(new ArrayList<>());
		var areaLookup = new HashMap<Integer, ProfileTodoArea>();
		var sectorLookup = new HashMap<Integer, ProfileTodoSector>();
		var c = DatabaseContext.getConnection();
		var sql = """
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
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY p.id, t.id
				ORDER BY a.name, s.name, p.nr
				""";
		try (var ps = c.prepareStatement(sql)) {
			ps.setInt(1, userId);
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					var a = areaLookup.computeIfAbsent(areaId, id -> {
						try {
							var newArea = new ProfileTodoArea(id, rst.getString("area_name"), 
									rst.getBoolean("area_locked_admin"), rst.getBoolean("area_locked_superadmin"), new ArrayList<>());
							res.areas().add(newArea);
							return newArea;
						} catch (SQLException e) {
							throw new RuntimeException(e);
						}
					});
					int sectorId = rst.getInt("sector_id");
					var s = sectorLookup.computeIfAbsent(sectorId, id -> {
						try {
							var newSector = new ProfileTodoSector(id, rst.getString("sector_name"), 
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
					var partners = new ArrayList<User>();
					var rawPartners = rst.getString("partners_raw");
					if (rawPartners != null && !rawPartners.isEmpty()) {
						for (var part : rawPartners.split("\\|")) {
							var bits = part.split(":", 2);
							if (bits.length == 2) {
								partners.add(User.from(Integer.parseInt(bits[0]), bits[1]));
							}
						}
					}
					var p = new ProfileTodoProblem(
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
	
	public List<User> getUserSearch(Optional<Integer> authUserId, String value) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in...");
		var res = new ArrayList<User>();
		var c = DatabaseContext.getConnection();
		if (!Strings.isNullOrEmpty(value)) {
			var searchRegexPattern = "(^|\\W)" + Pattern.quote(value);
			try (var ps = c.prepareStatement("""
					SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name
					FROM user u
					WHERE regexp_like(TRIM(CONCAT(u.firstname,' ',COALESCE(u.lastname,''))),?,'i')
					ORDER BY u.firstname, u.lastname
					""")) {
				ps.setString(1, searchRegexPattern);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						var name = rst.getString("name");
						res.add(User.from(id, name));
					}
				}
			}
			for (var u : res.stream()
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
	public byte[] getUserTicks(Optional<Integer> authUserId) throws SQLException, IOException {
		int userId = authUserId.orElseThrow();
		var c = DatabaseContext.getConnection();
		try (var workbook = new ExcelWorkbook(); var os = new ByteArrayOutputStream()) {
			Map<String, ExcelSheet> sheets = new HashMap<>();

			var sqlTicks = """
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
					  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
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

			var sqlRepeats = """
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
					WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
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

			var sqlAid = """
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
					WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
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
	
	public void setProfile(Optional<Integer> authUserId, ProfileIdentity profile) throws SQLException {
		Preconditions.checkArgument(authUserId.orElse(0) == profile.id(), "Wrong input");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.firstname()), "Firstname cannot be null");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(profile.lastname()), "Lastname cannot be null");
		var c = DatabaseContext.getConnection();
		var theme = (profile.themePreference() != null && (profile.themePreference().equals("light") || profile.themePreference().equals("dark"))) ? profile.themePreference() : null;
		try (var ps = c.prepareStatement("UPDATE user SET firstname=?, lastname=?, email_visible_to_all=?, theme_preference=COALESCE(theme_preference, ?) WHERE id=?")) {
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
	}

	public void setThemePreference(Optional<Integer> authUserId, String themePreference) throws SQLException {
		Preconditions.checkArgument(themePreference != null && (themePreference.equals("light") || themePreference.equals("dark")), "themePreference must be 'light' or 'dark'");
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("UPDATE user SET theme_preference=? WHERE id=?")) {
			ps.setString(1, themePreference);
			ps.setInt(2, authUserId.orElseThrow());
			ps.execute();
		}
	}

	public void setUserRegion(Optional<Integer> authUserId, int regionId, boolean delete) throws SQLException {
		var c = DatabaseContext.getConnection();
		if (delete) {
			try (var ps = c.prepareStatement("DELETE FROM user_region WHERE user_id=? AND region_id=?")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
		else {
			try (var ps = c.prepareStatement("INSERT INTO user_region (user_id, region_id, region_visible) VALUES (?, ?, 1)")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
	}

	public void upsertPermissionUser(Setup setup, Optional<Integer> authUserId, PermissionUser u) throws SQLException {
		dao.getRegionRepo().ensureAdminWriteRegion(setup, authUserId);
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?")) {
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
		try (var ps = c.prepareStatement("DELETE FROM user_region WHERE admin_read=0 AND admin_write=0 AND superadmin_read=0 AND superadmin_write=0 AND region_visible=0")) {
			ps.execute();
		}
	}

	private Map<Integer, Coordinates> getProblemCoordinates(Collection<Integer> idProblems) throws SQLException {
		Preconditions.checkArgument(!idProblems.isEmpty(), "idProblems is empty");
		var res = new HashMap<Integer, Coordinates>();
		var c = DatabaseContext.getConnection();
		var in = ",?".repeat(idProblems.size()).substring(1);
		var sqlStr = "SELECT p.id id_problem, COALESCE(pc.id,c.id,sc.id,ac.id) coordinates_id, COALESCE(pc.latitude,c.latitude,sc.latitude,ac.latitude) latitude, COALESCE(pc.longitude,c.longitude,sc.longitude,ac.longitude) longitude, COALESCE(pc.elevation,c.elevation,sc.elevation,ac.elevation) elevation, COALESCE(pc.elevation_source,c.elevation_source,sc.elevation_source,ac.elevation_source) elevation_source FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) LEFT JOIN coordinates ac ON a.coordinates_id=ac.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates pc ON p.coordinates_id=pc.id) LEFT JOIN sector_outline so ON s.id=so.sector_id AND so.sorting=1) LEFT JOIN coordinates c ON so.coordinates_id=c.id) WHERE p.id IN (" + in + ")";
		try (var ps = c.prepareStatement(sqlStr)) {
			var parameterIndex = 1;
			for (int idSector : idProblems) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (var rst = ps.executeQuery()) {
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

	private List<UserRegion> getUserRegion(int userId, Setup setup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<UserRegion>();
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("""
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
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					var name = rst.getString("name");
					var role = rst.getString("role");
					boolean readOnly = rst.getBoolean("read_only");
					boolean enabled = readOnly || rst.getBoolean("region_visible");
					boolean activity = rst.getBoolean("activity");
					res.add(new UserRegion(id, name, role, enabled, readOnly, activity));
				}
			}
		}
		logger.debug("getUserRegion() - res.size()={}, duration={}", res.size(), stopwatch);
		return res;
	}
	
	private void writeRow(ExcelWorkbook wb, Map<String, ExcelSheet> sheets, ResultSet rst, String sheetName) throws SQLException {
		var sheet = sheets.computeIfAbsent(sheetName, wb::addSheet);
		sheet.incrementRow();
		sheet.writeString("AREA", rst.getString("area_name"));
		sheet.writeString("SECTOR", rst.getString("sector_name"));
		var subType = rst.getString("subtype");
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
	
	protected int addUser(String email, String firstname, String lastname) throws SQLException {
		int id = -1;
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("INSERT INTO user (firstname, lastname) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, firstname);
			ps.setString(2, lastname);
			ps.executeUpdate();
			try (var rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					id = rst.getInt(1);
					logger.debug("addUser(email={}, firstname={}, lastname={}) - getInt(1)={}", email, firstname, lastname, id);
				}
			}
		}
		Preconditions.checkArgument(id > 0, "id=" + id + ", firstname=" + firstname + ", lastname=" + lastname);
		if (!Strings.isNullOrEmpty(email)) {
			try (var ps = c.prepareStatement("""
					INSERT INTO user_email (user_id, email) 
					VALUES (?, ?) 
					ON DUPLICATE KEY UPDATE user_id=LAST_INSERT_ID(user_id)
					""", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, id);
				ps.setString(2, email.toLowerCase());
				ps.executeUpdate();
				try (var rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						id = rst.getInt(1);
					}
				}
			}
		}
		return id;
	}
	
	protected int getExistingOrInsertUser(String name) throws SQLException {
		if (Strings.isNullOrEmpty(name)) {
			return USER_ID_UNKNOWN;
		}
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("SELECT id FROM user WHERE CONCAT(firstname, ' ', COALESCE(lastname,''))=?")) {
			ps.setString(1, name);
			try (var rst = ps.executeQuery()) {
				if (rst.next()) {
					return rst.getInt("id");
				}
			}
		}
		int usId = addUser(null, name, null);
		Preconditions.checkArgument(usId > 0);
		return usId;
	}
}
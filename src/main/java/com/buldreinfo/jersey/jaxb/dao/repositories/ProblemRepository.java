package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.dao.JdbcUtils;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.HitsFormatter;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.FaAid;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Problem.Neighbour;
import com.buldreinfo.jersey.jaxb.model.ProblemComment;
import com.buldreinfo.jersey.jaxb.model.ProblemSearchResult;
import com.buldreinfo.jersey.jaxb.model.ProblemSection;
import com.buldreinfo.jersey.jaxb.model.ProblemTick;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Trail;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public record ProblemRepository(Dao dao, Gson gson) {
	private static Logger logger = LogManager.getLogger();

	public ProblemRepository(Dao dao) {
        this(dao, new Gson());
    }
	
	public Problem getProblem(Connection c, Optional<Integer> authUserId, Setup s, int reqId, boolean showHiddenMedia, boolean shouldUpdateHits) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (shouldUpdateHits) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		boolean isTodo = false;
		if (authUserId.isPresent()) {
			try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM todo WHERE user_id=? AND problem_id=?")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, reqId);
				try (ResultSet rs = ps.executeQuery()) {
					isTodo = rs.next();
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
				       p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m/%y') fa_date_hr,
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
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
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
					List<Coordinates> sectorOutline = dao.getSectorRepo().getSectorOutline(c, sectorId);
					CompassDirection sectorWallDirectionCalculated = dao.getGeoRepo().getCompassDirection(s, rst.getInt("sector_compass_direction_id_calculated"));
					CompassDirection sectorWallDirectionManual = dao.getGeoRepo().getCompassDirection(s, rst.getInt("sector_compass_direction_id_manual"));
					Collection<Trail> trails = dao.getSectorRepo().getSectorTrails(c, authUserId, Collections.singleton(sectorId)).get(sectorId);
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
					List<Media> allMedia = dao.getMediaRepo().getMediaProblem(c, s, authUserId, areaId, sectorId, id, showHiddenMedia);
					Map<Boolean, List<Media>> partitioned = Optional.ofNullable(allMedia)
							.orElse(List.of())
							.stream()
							.collect(Collectors.partitioningBy(
									x -> x.problems().stream().anyMatch(mp -> mp.trivia() && mp.problemId() == reqId)
									));

					List<Media> triviaMedia = partitioned.get(true);
					List<Media> media = partitioned.get(false);
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					String trivia = rst.getString("trivia");
					String startingAltitude = rst.getString("starting_altitude");
					String aspect = rst.getString("aspect");
					String descent = rst.getString("descent");
					var neighbours = getProblemNeighbours(c, authUserId, sectorId, reqId, rock);
					var externalLinks = dao.getExternalLinksRepo().getExternalLinks(c, 0, 0, reqId);
					p = new Problem(null, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour,
							sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed,
							sectorSunFromHour, sectorSunToHour,
							sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, trails,
							neighbours,
							id, broken, false, lockedAdmin, lockedSuperadmin, nr, name, rock, comment,
							grade, originalGrade, faDate, faDateHr, fa, lengthMeter, coordinates,
							media, numTicks, stars, ticked, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), t, new ArrayList<>(), isTodo, externalLinks, pageViews,
							null, trivia, triviaMedia, startingAltitude, aspect, descent);
				}
			}
		}
		if (p == null) {
			try {
				Redirect res = dao.getHierarchyRepo().getCanonicalUrl(c, s, 0, 0, reqId);
				if (!Strings.isNullOrEmpty(res.redirectUrl())) {
					return new Problem(res.redirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, 0, 0, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, 0, null, null, 0, 0.0, false, null, null, null, null, null, false, null, null, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
			}
		}

		if (p == null) {
			throw new NoSuchElementException("Could not find problem with id=" + reqId);
		}
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
			ps.setInt(1, p.id());
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
			ps.setInt(1, p.id());
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
			ps.setInt(1, p.id());
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
			ps.setInt(1, p.id());
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
					List<Media> media = dao.getMediaRepo().getMediaGuestbook(c, authUserId, id);
					p.addComment(id, date, idUser, mediaIdentity, name, message, danger, resolved, media);
				}
				if (p.comments() != null && !p.comments().isEmpty()) {
					Optional<ProblemComment> lastComment = p.comments().stream().max(Comparator.comparing(ProblemComment::getId));
					if (lastComment.isPresent() && lastComment.get().getIdUser() == authUserId.orElse(0)) {
						lastComment.get().setEditable(true);
					}
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT ps.id, ps.nr, ps.description, g.grade
				FROM problem_section ps
				JOIN grade g ON ps.grade_id=g.id
				WHERE ps.problem_id=?
				ORDER BY ps.nr
				""")) {
			ps.setInt(1, p.id());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					String grade = rst.getString("grade");
					List<Media> sectionMedia = null;
					if (p.media() != null) {
						sectionMedia = p.media()
								.stream()
								.filter(x -> x.problems().stream().filter(y -> y.problemId() == reqId && y.problemPitch() == nr).findAny().isPresent())
								.toList();
						p.media().removeAll(sectionMedia);
					}
					p.addSection(id, nr, description, grade, sectionMedia);
				}
			}
		}
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
				ps.setInt(1, p.id());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String aidDate = rst.getString("aid_date");
						String aidDateHr = rst.getString("aid_date_hr");
						String aidDescription = rst.getString("aid_description");
						FaAid faAid = p.faAid();
						if (faAid == null) {
							faAid = new FaAid(p.id(), aidDate, aidDateHr, aidDescription, new ArrayList<>());
							p = p.withFaAid(faAid);
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
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}) - duration={} - p.id()={}", authUserId, s.idRegion(), reqId, stopwatch, p.id());
		return p;
	}
	
	public List<ProblemSearchResult> getProblemsSearch(Connection c, Optional<Integer> authUserId, Setup setup, String search) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in...");
		if (search == null || search.strip().isEmpty()) {
			return List.of();
		}
		String quotedSearch = Pattern.quote(search); // Quote the literal search string to escape special characters like '('
		String searchRegexPattern = "(^|\\W)" + quotedSearch;
		String sql = """
				WITH req AS (
				    SELECT ? AS auth_user_id, ? AS region_id, ? AS search_regex
				)
				SELECT p.id, a.name AS area_name, s.name AS sector_name, p.name AS problem_name, g.grade, COUNT(DISTINCT ps.id) AS num_pitches
				FROM req
				CROSS JOIN area a
				INNER JOIN region r ON a.region_id = r.id
				JOIN region_type rt ON r.id = rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				JOIN sector s ON a.id = s.area_id
				JOIN problem p ON (s.id = p.sector_id AND rt.type_id = p.type_id)
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				   LEFT JOIN problem_section ps ON p.id=ps.problem_id
				WHERE (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				  AND REGEXP_LIKE(p.name, req.search_regex, 'i')
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY p.id, a.name, s.name, p.name, g.grade
				ORDER BY p.name, s.name, a.name
				LIMIT 50
				""";
		List<ProblemSearchResult> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, setup.idRegion());
			ps.setString(3, searchRegexPattern);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int numPitches = rst.getInt("num_pitches");
					if (numPitches == 0) {
						numPitches = 1;
					}
					res.add(new ProblemSearchResult(rst.getInt("id"), rst.getString("area_name"), rst.getString("sector_name"), rst.getString("problem_name"), rst.getString("grade"), numPitches));
				}
			}
		}
		return res;
	}
	
	public Redirect setProblem(Connection c, Optional<Integer> authUserId, Setup s, Problem p) throws SQLException, InterruptedException {
		final boolean orderByGrade = s.isBouldering();
		final LocalDate dt = Strings.isNullOrEmpty(p.faDate())? null : LocalDate.parse(p.faDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		int idProblem = -1;
		final boolean isLockedAdmin = p.lockedSuperadmin()? false : p.lockedAdmin();
		if (p.coordinates() != null) {
			if (p.coordinates().getLatitude() == 0 || p.coordinates().getLongitude() == 0) {
				p = p.withCoordinates(null);
			}
			else {
				dao.getGeoRepo().ensureCoordinatesInDbWithElevationAndId(c, Lists.newArrayList(p.coordinates()));
			}
		}
		dao.getSectorRepo().tryFixSectorOrdering(c, p.sectorId(), p.id(), p.nr());
		int gradeId = s.gradeConverter().getIdGradeFromGrade(p.originalGrade());
		if (p.id() > 0) {
			try (PreparedStatement ps = c.prepareStatement("""
					UPDATE problem p
					JOIN sector s ON p.sector_id=s.id
					JOIN area a ON s.area_id=a.id
					JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)
					SET p.name=?, p.rock=?, p.description=?, p.grade_id=?, p.fa_date=?, p.coordinates_id=?, p.broken=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, length_meter=?, descent=?, p.trash=CASE WHEN ? THEN NOW() ELSE NULL END, p.trash_by=?, p.last_updated=now()
					WHERE p.id=?
					""")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setString(2, GlobalFunctions.stripString(p.name()));
				ps.setString(3, GlobalFunctions.stripString(p.rock()));
				ps.setString(4, GlobalFunctions.stripString(p.comment()));
				ps.setInt(5, gradeId);
				ps.setObject(6, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 7, p.coordinates() == null? 0 : p.coordinates().getId());
				ps.setString(8, GlobalFunctions.stripString(p.broken()));
				ps.setBoolean(9, isLockedAdmin);
				ps.setBoolean(10, p.lockedSuperadmin());
				ps.setInt(11, p.nr());
				ps.setInt(12, p.t().id());
				ps.setString(13, GlobalFunctions.stripString(p.trivia()));
				ps.setString(14, GlobalFunctions.stripString(p.startingAltitude()));
				ps.setString(15, GlobalFunctions.stripString(p.aspect()));
				JdbcUtils.setNullablePositiveInteger(ps, 16, p.lengthMeter());
				ps.setString(17, GlobalFunctions.stripString(p.descent()));
				ps.setBoolean(18, p.trash());
				ps.setInt(19, p.trash()? authUserId.orElseThrow() : 0);
				ps.setInt(20, p.id());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idProblem = p.id();
			updateProblemConsensusGrade(c, idProblem);
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem (sector_id, name, rock, description, grade_id, consensus_grade_id, fa_date, coordinates_id, broken, locked_admin, locked_superadmin, nr, type_id, trivia, starting_altitude, aspect, length_meter, descent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, p.sectorId());
				ps.setString(2, GlobalFunctions.stripString(p.name()));
				ps.setString(3, GlobalFunctions.stripString(p.rock()));
				ps.setString(4, GlobalFunctions.stripString(p.comment()));
				ps.setInt(5, gradeId);
				ps.setInt(6, gradeId);
				ps.setObject(7, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 8, p.coordinates() == null? 0 : p.coordinates().getId());
				ps.setString(9, GlobalFunctions.stripString(p.broken()));
				ps.setBoolean(10, isLockedAdmin);
				ps.setBoolean(11, p.lockedSuperadmin());
				ps.setInt(12, p.nr() == 0 ? dao.getSectorRepo().getSector(c, authUserId, orderByGrade, s, p.sectorId(), false).problems().stream().map(x -> x.nr()).mapToInt(Integer::intValue).max().orElse(0) + 1 : p.nr());
				ps.setInt(13, p.t().id());
				ps.setString(14, GlobalFunctions.stripString(p.trivia()));
				ps.setString(15, GlobalFunctions.stripString(p.startingAltitude()));
				ps.setString(16, GlobalFunctions.stripString(p.aspect()));
				JdbcUtils.setNullablePositiveInteger(ps, 17, p.lengthMeter());
				ps.setString(18, GlobalFunctions.stripString(p.descent()));
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
		String sqlStr = "UPDATE problem p, sector s, area a SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, idProblem);
			int res = ps.executeUpdate();
			if (res == 0) {
				throw new SQLException("Insufficient credentials");
			}
		}
		if (p.fa() != null) {
			Set<Integer> fas = new HashSet<>();
			try (PreparedStatement ps = c.prepareStatement("SELECT user_id FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						fas.add(rst.getInt("user_id"));
					}
				}
			}
			for (User x : p.fa()) {
				Preconditions.checkArgument(x.id() != 0);
				if (x.id() > 0) {
					boolean exists = fas.remove(x.id());
					if (!exists) {
						try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
							ps2.setInt(1, idProblem);
							ps2.setInt(2, x.id());
							ps2.execute();
						}
					}
				} else {
					int idUser = dao.getUserRepo().addUser(c, null, x.name(), null);
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
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM problem_section WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}
		if (p.sections() != null && p.sections().size() > 1) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade_id) VALUES (?, ?, ?, ?)")) {
				for (ProblemSection section : p.sections()) {
					ps.setInt(1, idProblem);
					ps.setInt(2, section.nr());
					ps.setString(3, GlobalFunctions.stripString(section.description()));
					JdbcUtils.setNullablePositiveInteger(ps, 4, s.gradeConverter().getIdGradeFromGrade(section.grade()));
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa_aid WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM fa_aid_user WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			if (p.faAid() != null) {
				FaAid faAid = p.faAid();
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
								idUser = dao.getUserRepo().addUser(c, null, u.name(), null);
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
		dao.getExternalLinksRepo().upsertExternalLinks(c, p.externalLinks(), 0, 0, idProblem);
		dao.getActivityRepo().fillActivity(c, idProblem);
		if (p.trash()) {
			return Redirect.fromIdSector(p.sectorId());
		}
		return Redirect.fromIdProblem(idProblem);
	}
	
	public int upsertComment(Connection c, Optional<Integer> authUserId, Setup s, Comment co) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		int idGuestbook = co.id();
		if (idGuestbook > 0) {
			List<ProblemComment> comments = getProblem(c, authUserId, s, co.idProblem(), false, false).comments();
			Preconditions.checkArgument(!comments.isEmpty(), "No comment on problem " + co.idProblem());
			ProblemComment comment = comments.stream().filter(x -> x.getId() == co.id()).findAny().orElseThrow();
			if (comment.isEditable()) {
				if (co.delete()) {
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM guestbook WHERE id=?")) {
						ps.setInt(1, co.id());
						ps.execute();
						idGuestbook = 0;
					}
				}
				else {
					try (PreparedStatement ps = c.prepareStatement("UPDATE guestbook SET message=?, danger=?, resolved=? WHERE id=?")) {
						ps.setString(1, GlobalFunctions.stripString(co.comment()));
						ps.setBoolean(2, co.danger());
						ps.setBoolean(3, co.resolved());
						ps.setInt(4, co.id());
						ps.execute();
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
		}
		else {
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
				JdbcUtils.setNullablePositiveInteger(ps, 4, parentId);
				ps.setBoolean(5, co.danger());
				ps.setBoolean(6, co.resolved());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idGuestbook = rst.getInt(1);
					}
				}
			}
		}
		dao.getActivityRepo().fillActivity(c, co.idProblem());
		return idGuestbook;
	}
	
	private List<Neighbour> getProblemNeighbours(Connection c, Optional<Integer> authUserId, int sectorId, int problemId, String rock) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Neighbour> res = new ArrayList<>();
		if (rock == null) {
			String sql = """
					WITH req AS (
					    SELECT ? user_id, ? sector_id, ? problem_id
					)
					SELECT n_id, n_nr, n_name, n_grade, n_tick, n_todo         
					FROM (
					    SELECT 
					        p.id,
					        LAG(p.id) OVER (ORDER BY p.nr) AS prev_id,
					        LAG(p.nr) OVER (ORDER BY p.nr) AS prev_nr,
					        LAG(p.name) OVER (ORDER BY p.nr) AS prev_name,
					        LAG(g.grade) OVER (ORDER BY p.nr) AS prev_grade,
					        LAG(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS prev_tick,
					        LAG(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS prev_todo,
					        LEAD(p.id) OVER (ORDER BY p.nr) AS next_id,
					        LEAD(p.nr) OVER (ORDER BY p.nr) AS next_nr,
					        LEAD(p.name) OVER (ORDER BY p.nr) AS next_name,
					        LEAD(g.grade) OVER (ORDER BY p.nr) AS next_grade,
					        LEAD(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS next_tick,
					        LEAD(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS next_todo,
					        FIRST_VALUE(p.id) OVER (ORDER BY p.nr) AS first_id,
					        FIRST_VALUE(p.nr) OVER (ORDER BY p.nr) AS first_nr,
					        FIRST_VALUE(p.name) OVER (ORDER BY p.nr) AS first_name,
					        FIRST_VALUE(g.grade) OVER (ORDER BY p.nr) AS first_grade,
					        FIRST_VALUE(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS first_tick,
					        FIRST_VALUE(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS first_todo,
					        LAST_VALUE(p.id) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_id,
					        LAST_VALUE(p.nr) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_nr,
					        LAST_VALUE(p.name) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_name,
					        LAST_VALUE(g.grade) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_grade,
					        LAST_VALUE(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_tick,
					        LAST_VALUE(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_todo
					    FROM req
					    JOIN sector s ON req.sector_id = s.id
					    JOIN problem p ON s.id = p.sector_id
					    JOIN area a ON s.area_id = a.id
					    JOIN grade g ON p.consensus_grade_id = g.id
					    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.user_id
					    LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = req.user_id
					    LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = req.user_id
					    LEFT JOIN tick ON p.id = tick.problem_id AND tick.user_id = req.user_id
					    WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					) sub         
					JOIN req ON 1=1
					CROSS JOIN LATERAL (
					    SELECT COALESCE(prev_id, last_id) AS n_id, COALESCE(prev_nr, last_nr) AS n_nr, COALESCE(prev_name, last_name) AS n_name, COALESCE(prev_grade, last_grade) AS n_grade, COALESCE(prev_tick, last_tick) AS n_tick, COALESCE(prev_todo, last_todo) AS n_todo
					    UNION ALL
					    SELECT COALESCE(next_id, first_id), COALESCE(next_nr, first_nr), COALESCE(next_name, first_name), COALESCE(next_grade, first_grade), COALESCE(next_tick, first_tick), COALESCE(next_todo, first_todo)
					) AS n         
					WHERE sub.id = req.problem_id AND n_id != req.problem_id
					""";

			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, authUserId.orElse(0));
				ps.setInt(2, sectorId);
				ps.setInt(3, problemId);

				try (ResultSet rst = ps.executeQuery()) {
					Set<Integer> seenIds = new HashSet<>();
					while (rst.next()) {
						int neighborId = rst.getInt("n_id");
						if (neighborId > 0 && seenIds.add(neighborId)) {
							res.add(new Neighbour(neighborId, rst.getInt("n_nr"), rst.getString("n_name"), rst.getString("n_grade"), rst.getBoolean("n_tick"), rst.getBoolean("n_todo")
									));
						}
					}
				}
			}
		}
		else {
			try (PreparedStatement ps = c.prepareStatement("""
					WITH req AS (
						SELECT ? user_id, ? sector_id, ? problem_id, ? rock
					)
					SELECT p.id, p.name, p.nr, g.grade, CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END tick, CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END todo
					FROM req
					JOIN sector s ON req.sector_id = s.id
					JOIN problem p ON s.id = p.sector_id
					JOIN area a ON s.area_id = a.id
					JOIN grade g ON p.consensus_grade_id = g.id
					LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.user_id
					LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = req.user_id
					LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = req.user_id
					LEFT JOIN tick ON p.id = tick.problem_id AND tick.user_id = req.user_id
					WHERE p.rock = req.rock AND p.id != req.problem_id
					  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					ORDER BY p.nr
								""")) {
				ps.setInt(1, authUserId.orElse(0));
				ps.setInt(2, sectorId);
				ps.setInt(3, problemId);
				ps.setString(4, rock);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						res.add(new Neighbour(rst.getInt("id"), rst.getInt("nr"), rst.getString("name"), rst.getString("grade"), rst.getBoolean("tick"), rst.getBoolean("todo")));
					}
				}
			}
		}
		logger.debug("getProblemNeighbours(sectorId={}, problemId={}) - res.size={}, duration={}", sectorId, problemId, res.size(), stopwatch);
		return res;
	}

	protected void ensureAdminWriteProblem(Connection c, Optional<Integer> authUserId, int problemId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT ur.admin_write, ur.superadmin_write 
				FROM problem p
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN user_region ur ON a.region_id=ur.region_id
				WHERE p.id=?
				  AND ur.user_id=?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0)) 
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0)) 
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				""")) {
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
	
	protected void updateProblemConsensusGrade(Connection c, int problemId) throws SQLException {
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
}
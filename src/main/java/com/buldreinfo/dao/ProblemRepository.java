package com.buldreinfo.dao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.helpers.HitsFormatter;
import com.buldreinfo.model.Comment;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.FaAid;
import com.buldreinfo.model.MediaIdentity;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.Problem.Neighbour;
import com.buldreinfo.model.Problem.ProblemTodo;
import com.buldreinfo.model.ProblemComment;
import com.buldreinfo.model.ProblemSearchResult;
import com.buldreinfo.model.ProblemSection;
import com.buldreinfo.model.ProblemTick;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector.SectorProblem;
import com.buldreinfo.model.Type;
import com.buldreinfo.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ProblemRepository {
	private final ActivityRepository activityRepo;
	private final ExternalLinksRepository externalLinksRepo;
	private final GeoRepository geoRepo;
	private final ObjectProvider<HierarchyRepository> hierarchyRepo;
	private final JdbcClient jdbcClient;
	private final ObjectProvider<MediaRepository> mediaRepo;
	private final ObjectMapper objectMapper;
	private final ObjectProvider<SectorRepository> sectorRepo;
	private final UserRepository userRepo;

	public ProblemRepository(JdbcClient jdbcClient,
			ObjectMapper objectMapper,
			ActivityRepository activityRepo,
			ExternalLinksRepository externalLinksRepo,
			GeoRepository geoRepo,
			ObjectProvider<HierarchyRepository> hierarchyRepo,
			ObjectProvider<MediaRepository> mediaRepo,
			ObjectProvider<SectorRepository> sectorRepo,
			UserRepository userRepo) {
		this.jdbcClient = jdbcClient;
		this.objectMapper = objectMapper;
		this.activityRepo = activityRepo;
		this.externalLinksRepo = externalLinksRepo;
		this.geoRepo = geoRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.sectorRepo = sectorRepo;
		this.userRepo = userRepo;
	}

	@Transactional
	public Problem getProblem(Optional<Integer> authUserId, Setup s, int reqId, boolean showHiddenMedia, boolean shouldUpdateHits) {
		if (shouldUpdateHits) {
			jdbcClient.sql("UPDATE problem SET hits = hits + 1 WHERE id = ?").param(reqId).update();
		}

		boolean isTodo = authUserId.isPresent() && jdbcClient.sql("SELECT 1 FROM todo WHERE user_id = ? AND problem_id = ?")
				.params(authUserId.get(), reqId)
				.query((_, _) -> true)
				.optional()
				.orElse(false);

		var linksFuture = CompletableFuture.supplyAsync(() -> externalLinksRepo.getExternalLinks(0, 0, reqId));

		Problem p = jdbcClient.sql("""
				WITH req AS (SELECT ? auth_user_id, ? region_id, ? problem_id),
				stars_count AS (
				    SELECT p_sub.id AS pid, COUNT(DISTINCT t_sub.id) AS num_ticks,
				           ROUND(ROUND(AVG(NULLIF(t_sub.stars, -1)) * 2) / 2, 1) AS stars
				    FROM req JOIN problem p_sub ON p_sub.id = req.problem_id
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
				       p.trivia, p.starting_altitude, p.aspect, p.descent,
				       (
				           SELECT GROUP_CONCAT(
				               CONCAT('{"id":', ps_sub.id, ',"nr":', ps_sub.nr, ',"description":"', COALESCE(REPLACE(ps_sub.description, '"', '\\\\u0022'), ''), '","grade":"', g_sub.grade, '"}')
				               ORDER BY ps_sub.nr SEPARATOR ','
				           )
				           FROM problem_section ps_sub
				           JOIN grade g_sub ON ps_sub.grade_id = g_sub.id
				           WHERE ps_sub.problem_id = p.id
				       ) compiled_sections
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
				""")
				.params(authUserId.orElse(0), s.idRegion(), reqId)
				.query((rs, _) -> {
					try {
						int sectorId = rs.getInt("sector_id");
						String rock = rs.getString("rock");
						var outline = sectorRepo.getObject().getSectorOutline(sectorId);
						var trails = sectorRepo.getObject().getSectorTrails(authUserId, Collections.singleton(sectorId)).get(sectorId);
						var neighbours = getProblemNeighbours(authUserId, sectorId, reqId, rock);
						int areaId = rs.getInt("area_id");
						int parkingId = rs.getInt("sector_parking_coordinates_id");
						var parking = parkingId == 0 ? null : new Coordinates(parkingId, rs.getDouble("sector_parking_latitude"), rs.getDouble("sector_parking_longitude"), rs.getDouble("sector_parking_elevation"), rs.getString("sector_parking_elevation_source"));
						var wallDirCalc = geoRepo.getCompassDirection(s, rs.getInt("sector_compass_direction_id_calculated"));
						var wallDirMan = geoRepo.getCompassDirection(s, rs.getInt("sector_compass_direction_id_manual"));
						int id = rs.getInt("id");
						var faStr = rs.getString("fa");
						List<User> fa = (faStr == null || faStr.isEmpty()) ? null : objectMapper.readValue("[" + faStr + "]", new TypeReference<List<User>>() {});
						int coordId = rs.getInt("coordinates_id");
						var coords = coordId == 0 ? null : new Coordinates(coordId, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"));
						var allMedia = mediaRepo.getObject().getMediaProblem(s, authUserId, areaId, sectorId, id, showHiddenMedia);
						var partitioned = Optional.ofNullable(allMedia).orElse(List.of()).stream().collect(Collectors.partitioningBy(x -> x.problems().stream().anyMatch(mp -> mp.trivia() && mp.problemId() == reqId)));
						var triviaMedia = partitioned.get(true);
						var media = partitioned.get(false);
						var sectionsStr = rs.getString("compiled_sections");
						List<ProblemSection> sections = (sectionsStr == null || sectionsStr.isEmpty()) ? new ArrayList<>() : new ArrayList<>(objectMapper.readValue("[" + sectionsStr + "]", new TypeReference<List<ProblemSection>>() {}));
						if (media != null && !sections.isEmpty()) {
							for (var section : sections) {
								var sectionMedia = media.stream().filter(x -> x.problems().stream().anyMatch(y -> y.problemId() == reqId && y.problemPitch() == section.nr())).toList();
								media.removeAll(sectionMedia);
								sections.set(sections.indexOf(section), section.withMedia(sectionMedia));
							}
						}
						return new Problem(null, areaId, rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), rs.getString("area_name"), rs.getString("area_access_info"), rs.getString("area_access_closed"), rs.getBoolean("area_no_dogs_allowed"), rs.getInt("area_sun_from_hour"), rs.getInt("area_sun_to_hour"), sectorId, rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"), rs.getString("sector_name"), rs.getString("sector_access_info"), rs.getString("sector_access_closed"), rs.getInt("sector_sun_from_hour"), rs.getInt("sector_sun_to_hour"), parking, outline, wallDirCalc, wallDirMan, trails, neighbours, id, rs.getString("broken"), false, rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getInt("nr"), rs.getString("name"), rock, rs.getString("description"), rs.getString("grade"), rs.getString("original_grade"), rs.getString("fa_date"), rs.getString("fa_date_hr"), fa, rs.getInt("length_meter"), coords, media, rs.getInt("num_ticks"), rs.getDouble("stars"), rs.getBoolean("ticked"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Type(rs.getInt("type_id"), rs.getString("type"), rs.getString("subtype")), sections, isTodo, linksFuture.join(), HitsFormatter.formatHits(rs.getLong("hits")), null, rs.getString("trivia"), triviaMedia, rs.getString("starting_altitude"), rs.getString("aspect"), rs.getString("descent"));
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				})
				.optional()
				.orElse(null);

		if (p == null) {
			try {
				var res = hierarchyRepo.getObject().getCanonicalUrl(s, 0, 0, reqId);
				if (res.redirectUrl() != null && !res.redirectUrl().isEmpty()) {
					return new Problem(res.redirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, 0, 0, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, 0, null, null, 0, 0.0, false, null, null, null, null, null, false, null, null, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
			}
			throw new NoSuchElementException("Could not find problem with id=" + reqId);
		}
		return p.withTicks(fetchTicks(authUserId, p.id()))
				.withTodos(fetchTodos(p.id()))
				.withComments(fetchComments(authUserId, p.id()))
				.withFaAid(s.isBouldering() ? null : fetchFaAid(p.id()));
	}

	@Transactional(readOnly = true)
	public List<ProblemSearchResult> getProblemsSearch(Optional<Integer> authUserId, Setup setup, String search) {
		if (authUserId.isEmpty()) {
			throw new IllegalArgumentException("User not logged in...");
		}
		if (search == null || search.strip().isEmpty()) {
			return List.of();
		}

		var searchRegexPattern = "(^|\\W)" + Pattern.quote(search);

		return jdbcClient.sql("""
				WITH req AS (SELECT ? AS auth_user_id, ? AS region_id, ? AS search_regex)
				SELECT p.id, a.name AS area_name, s.name AS sector_name, p.name AS problem_name, g.grade, COUNT(DISTINCT ps.id) AS num_pitches
				FROM req
				CROSS JOIN area a
				INNER JOIN region r ON a.region_id = r.id
				JOIN region_type rt ON r.id = rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				JOIN sector s ON a.id = s.area_id
				JOIN problem p ON (s.id = p.sector_id AND rt.type_id = p.type_id)
				JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				LEFT JOIN problem_section ps ON p.id = ps.problem_id
				WHERE (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				  AND REGEXP_LIKE(p.name, req.search_regex, 'i')
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY p.id, a.name, s.name, p.name, g.grade
				ORDER BY p.name, s.name, a.name
				LIMIT 50
				""")
				.params(authUserId.get(), setup.idRegion(), searchRegexPattern)
				.query((rs, _) -> new ProblemSearchResult(
						rs.getInt("id"),
						rs.getString("area_name"),
						rs.getString("sector_name"),
						rs.getString("problem_name"),
						rs.getString("grade"),
						Math.max(1, rs.getInt("num_pitches"))
						))
				.list();
	}

	@Transactional
	public Redirect setProblem(Optional<Integer> authUserId, Setup s, Problem p) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("User not logged in");

		var dt = (p.faDate() == null || p.faDate().isEmpty()) ? null : LocalDate.parse(p.faDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		var isLockedAdmin = p.lockedSuperadmin() ? false : p.lockedAdmin();

		if (p.coordinates() != null) {
			if (p.coordinates().getLatitude() == 0 || p.coordinates().getLongitude() == 0) {
				p = p.withCoordinates(null);
			} else {
				geoRepo.ensureCoordinatesInDbWithElevationAndId(List.of(p.coordinates()));
			}
		}

		sectorRepo.getObject().tryFixSectorOrdering(p.sectorId(), p.id(), p.nr());
		var gradeId = s.gradeConverter().getIdGradeFromGrade(p.originalGrade());
		int idProblem = p.id();

		if (idProblem > 0) {
			ensureAdminWriteProblem(authUserId, idProblem);
			jdbcClient.sql("""
					UPDATE problem p
					JOIN sector s ON p.sector_id=s.id
					JOIN area a ON s.area_id=a.id
					JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)
					SET p.name=?, p.rock=?, p.description=?, p.grade_id=?, p.fa_date=?, p.coordinates_id=?, p.broken=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, length_meter=?, descent=?, p.trash=CASE WHEN ? THEN NOW() ELSE NULL END, p.trash_by=?, p.last_updated=now()
					WHERE p.id=?
					""")
			.params(authUserId.get(), GlobalFunctions.stripString(p.name()), GlobalFunctions.stripString(p.rock()), GlobalFunctions.stripString(p.comment()), gradeId, dt, p.coordinates() == null ? 0 : p.coordinates().getId(), GlobalFunctions.stripString(p.broken()), isLockedAdmin, p.lockedSuperadmin(), p.nr(), p.t().id(), GlobalFunctions.stripString(p.trivia()), GlobalFunctions.stripString(p.startingAltitude()), GlobalFunctions.stripString(p.aspect()), p.lengthMeter() == 0 ? null : p.lengthMeter(), GlobalFunctions.stripString(p.descent()), p.trash(), p.trash() ? authUserId.get() : 0, idProblem)
			.update();
			updateProblemConsensusGrade(idProblem);
		} else {
			sectorRepo.getObject().ensureAdminWriteSector(authUserId, p.sectorId());
			int nr = p.nr() == 0 ? sectorRepo.getObject().getSector(authUserId, s.isBouldering(), s, p.sectorId(), false)
					.problems().stream().mapToInt(SectorProblem::nr).max().orElse(0) + 1 : p.nr();
			var keyHolder = new GeneratedKeyHolder();
			jdbcClient.sql("""
					INSERT INTO problem (sector_id, name, rock, description, grade_id, consensus_grade_id, fa_date, 
					                     coordinates_id, broken, locked_admin, locked_superadmin, nr, type_id, 
					                     trivia, starting_altitude, aspect, length_meter, descent) 
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""")
			.params(p.sectorId(), GlobalFunctions.stripString(p.name()), GlobalFunctions.stripString(p.rock()), 
					GlobalFunctions.stripString(p.comment()), gradeId, gradeId, dt, 
					p.coordinates() == null ? 0 : p.coordinates().getId(), 
							GlobalFunctions.stripString(p.broken()), isLockedAdmin, p.lockedSuperadmin(), 
							nr, p.t().id(), GlobalFunctions.stripString(p.trivia()), 
							GlobalFunctions.stripString(p.startingAltitude()), GlobalFunctions.stripString(p.aspect()), 
							p.lengthMeter() == 0 ? null : p.lengthMeter(), 
									GlobalFunctions.stripString(p.descent()))
			.update(keyHolder);

			idProblem = keyHolder.getKeyAs(Integer.class);
		}

		jdbcClient.sql("UPDATE problem p, sector s, area a SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id").param(idProblem).update();

		if (p.fa() != null) {
			var existingFa = jdbcClient.sql("SELECT user_id FROM fa WHERE problem_id=?").param(idProblem).query((rs, _) -> rs.getInt("user_id")).list();
			var toDelete = new HashSet<>(existingFa);
			for (var x : p.fa()) {
				if (x.id() == 0) throw new IllegalArgumentException("FA user id must not be 0");
				int userId = x.id() > 0 ? x.id() : userRepo.addUser(null, x.name(), null);
				if (userId <= 0) throw new IllegalArgumentException("Failed to create user");
				if (!toDelete.remove(userId)) jdbcClient.sql("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)").params(idProblem, userId).update();
			}
			for (var userId : toDelete) jdbcClient.sql("DELETE FROM fa WHERE problem_id=? AND user_id=?").params(idProblem, userId).update();
		} else {
			jdbcClient.sql("DELETE FROM fa WHERE problem_id=?").param(idProblem).update();
		}

		jdbcClient.sql("DELETE FROM problem_section WHERE problem_id=?").param(idProblem).update();
		if (p.sections() != null && p.sections().size() > 1) {
			for (var section : p.sections()) {
				jdbcClient.sql("INSERT INTO problem_section (problem_id, nr, description, grade_id) VALUES (?, ?, ?, ?)").params(idProblem, section.nr(), GlobalFunctions.stripString(section.description()), s.gradeConverter().getIdGradeFromGrade(section.grade())).update();
			}
		}

		if (!s.isBouldering()) {
			jdbcClient.sql("DELETE FROM fa_aid WHERE problem_id=?").param(idProblem).update();
			jdbcClient.sql("DELETE FROM fa_aid_user WHERE problem_id=?").param(idProblem).update();
			if (p.faAid() != null) {
				var aidDt = (p.faAid().date() == null || p.faAid().date().isEmpty()) ? null : LocalDate.parse(p.faAid().date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				jdbcClient.sql("INSERT INTO fa_aid (problem_id, aid_date, aid_description) VALUES (?, ?, ?)").params(idProblem, aidDt, GlobalFunctions.stripString(p.faAid().description())).update();
				for (var u : p.faAid().users()) {
					int userId = u.id() > 0 ? u.id() : userRepo.addUser(null, u.name(), null);
					if (userId <= 0) throw new IllegalArgumentException("Failed to create user for faAid");
					jdbcClient.sql("INSERT INTO fa_aid_user (problem_id, user_id) VALUES (?, ?)").params(idProblem, userId).update();
				}
			}
		}

		externalLinksRepo.upsertExternalLinks(p.externalLinks(), 0, 0, idProblem);
		activityRepo.fillActivity(idProblem);
		return p.trash() ? Redirect.fromIdSector(p.sectorId()) : Redirect.fromIdProblem(idProblem);
	}

	@Transactional
	public int upsertComment(Optional<Integer> authUserId, Setup s, Comment co) {
		int userId = authUserId.orElseThrow(() -> new IllegalArgumentException("Not logged in"));
		int idGuestbook = co.id();

		if (idGuestbook > 0) {
			Problem p = getProblem(authUserId, s, co.idProblem(), false, false);
			ProblemComment comment = p.comments().stream()
					.filter(x -> x.id() == co.id())
					.findAny()
					.orElseThrow(() -> new IllegalArgumentException("No comment on problem " + co.idProblem()));

			if (comment.editable()) {
				if (co.delete()) {
					jdbcClient.sql("DELETE FROM guestbook WHERE id = ?").param(co.id()).update();
					idGuestbook = 0;
				} else {
					jdbcClient.sql("UPDATE guestbook SET message = ?, danger = ?, resolved = ? WHERE id = ?")
					.params(GlobalFunctions.stripString(co.comment()), co.danger(), co.resolved(), co.id())
					.update();
				}
			} else if (!comment.danger() && !comment.resolved() && co.danger()) {
				jdbcClient.sql("UPDATE guestbook SET danger = ? WHERE id = ?")
				.params(co.danger(), co.id())
				.update();
			} else {
				throw new IllegalArgumentException("Comment not editable by " + userId + ". Other users can only mark as dangerous");
			}
		} else {
			Objects.requireNonNull(GlobalFunctions.stripString(co.comment()));

			Integer parentId = jdbcClient.sql("SELECT MIN(id) FROM guestbook WHERE problem_id = ?")
					.param(co.idProblem())
					.query((rs, _) -> rs.getInt(1))
					.optional()
					.orElse(0);

			var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
			jdbcClient.sql("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id, danger, resolved) VALUES (now(), ?, ?, ?, ?, ?, ?)")
			.params(GlobalFunctions.stripString(co.comment()), co.idProblem(), userId, parentId == 0 ? null : parentId, co.danger(), co.resolved())
			.update(keyHolder);

			idGuestbook = keyHolder.getKeyAs(Integer.class);
		}

		activityRepo.fillActivity(co.idProblem());
		return idGuestbook;
	}

	private List<ProblemComment> fetchComments(Optional<Integer> authUserId, int problemId) {
		List<ProblemComment> comments = new ArrayList<>();
		jdbcClient.sql("""
				SELECT g.id, CAST(g.post_time AS char) date, u.id user_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, 
				       mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, g.message, g.danger, g.resolved
				FROM guestbook g 
				JOIN user u ON g.user_id=u.id 
				LEFT JOIN media m ON u.media_id=m.id 
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE g.problem_id=? ORDER BY g.post_time DESC
				""")
		.param(1, problemId)
		.query(rs -> {
			MediaIdentity mId = rs.getInt("media_id") > 0 
					? new MediaIdentity(rs.getInt("media_id"), rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) 
							: null;
			comments.add(new ProblemComment(
					rs.getInt("id"), 
					rs.getString("date"), 
					rs.getInt("user_id"), 
					mId, 
					rs.getString("name"), 
					rs.getString("message"), 
					rs.getBoolean("danger"), 
					rs.getBoolean("resolved"), 
					mediaRepo.getObject().getMediaGuestbook(authUserId, rs.getInt("id")), 
					false
					));
		});
		int currentUserId = authUserId.orElse(0);
		int maxId = comments.stream().mapToInt(ProblemComment::id).max().orElse(-1);
		return comments.stream()
				.map(c -> (c.id() == maxId && c.idUser() == currentUserId) 
						? c.withEditable(true) : c)
				.toList();
	}

	private FaAid fetchFaAid(int problemId) {
		AtomicReference<FaAid> faAidRef = new AtomicReference<>(null);
		jdbcClient.sql("""
				SELECT DATE_FORMAT(a.aid_date,'%Y-%m-%d') aid_date, DATE_FORMAT(a.aid_date,'%d/%m-%y') aid_date_hr, a.aid_description, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name,
				       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex
				FROM fa_aid a 
				LEFT JOIN fa_aid_user au ON a.problem_id=au.problem_id 
				LEFT JOIN user u ON au.user_id=u.id 
				LEFT JOIN media m ON u.media_id=m.id 
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE a.problem_id=?
				""")
		.param(1, problemId)
		.query(rs -> {
			if (faAidRef.get() == null) {
				faAidRef.set(new FaAid(problemId, rs.getString("aid_date"), rs.getString("aid_date_hr"), rs.getString("aid_description"), new ArrayList<>()));
			}

			if (rs.getInt("id") != 0) {
				MediaIdentity mId = rs.getInt("media_id") > 0 
						? new MediaIdentity(rs.getInt("media_id"), rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) 
								: null;
				faAidRef.get().users().add(User.from(rs.getInt("id"), rs.getString("name"), mId));
			}
		});
		return faAidRef.get();
	}

	private List<ProblemTick> fetchTicks(Optional<Integer> authUserId, int problemId) {
		Map<Integer, ProblemTick> tickLookup = new LinkedHashMap<>();
		jdbcClient.sql("""
				SELECT t.id id_tick, u.id id_user, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, 
				       mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				       CAST(t.date AS char) date, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, 
				       t.comment, t.stars, g.grade
				FROM tick t 
				LEFT JOIN grade g ON t.grade_id=g.id 
				JOIN user u ON t.user_id=u.id 
				LEFT JOIN media m ON u.media_id=m.id 
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE t.problem_id=?
				ORDER BY t.date DESC, t.id DESC
				""")
		.param(1, problemId)
		.query(rs -> {
			int id = rs.getInt("id_tick");
			MediaIdentity mId = rs.getInt("media_id") > 0 
					? new MediaIdentity(rs.getInt("media_id"), rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) 
							: null;

			tickLookup.put(id, new ProblemTick(
					id, 
					rs.getInt("id_user"), 
					mId, 
					rs.getString("date"), 
					rs.getString("name"), 
					rs.getString("grade"), 
					rs.getString("grade") == null, 
					rs.getString("comment"), 
					rs.getDouble("stars"), 
					rs.getInt("id_user") == authUserId.orElse(0)
					));
		});
		jdbcClient.sql("""
				SELECT r.id, r.tick_id, r.date, r.comment 
				FROM tick t, tick_repeat r 
				WHERE t.problem_id=? AND t.id=r.tick_id 
				ORDER BY r.tick_id, r.date, r.id
				""")
		.param(1, problemId)
		.query(rs -> {
			ProblemTick tick = tickLookup.get(rs.getInt("tick_id"));
			if (tick != null) {
				tick.addRepeat(rs.getInt("id"), rs.getInt("tick_id"), rs.getString("date"), rs.getString("comment"));
			}
		});
		return List.copyOf(tickLookup.values());
	}

	private List<ProblemTodo> fetchTodos(int problemId) {
		return jdbcClient.sql("""
				SELECT u.id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name
				FROM todo t JOIN user u ON t.user_id=u.id LEFT JOIN media m ON u.media_id=m.id LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				WHERE t.problem_id=?
				ORDER BY u.firstname, u.lastname
				""")
				.param(problemId)
				.query((rs, _) -> new Problem.ProblemTodo(rs.getInt("id"), rs.getInt("media_id") > 0 ? new MediaIdentity(rs.getInt("media_id"), rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex")) : null, rs.getString("name")))
				.list();
	}

	private List<Neighbour> getProblemNeighbours(Optional<Integer> authUserId, int sectorId, int problemId, String rock) {
		int userId = authUserId.orElse(0);
		if (rock == null) {
			return jdbcClient.sql("""
					WITH req AS (SELECT ? AS user_id, ? AS sector_id, ? AS problem_id)
					SELECT n_id, n_nr, n_name, n_grade, n_tick, n_todo         
					FROM (
					    SELECT p.id,
					        LAG(p.id) OVER (ORDER BY p.nr) AS prev_id, LAG(p.nr) OVER (ORDER BY p.nr) AS prev_nr,
					        LAG(p.name) OVER (ORDER BY p.nr) AS prev_name, LAG(g.grade) OVER (ORDER BY p.nr) AS prev_grade,
					        LAG(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS prev_tick,
					        LAG(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS prev_todo,
					        LEAD(p.id) OVER (ORDER BY p.nr) AS next_id, LEAD(p.nr) OVER (ORDER BY p.nr) AS next_nr,
					        LEAD(p.name) OVER (ORDER BY p.nr) AS next_name, LEAD(g.grade) OVER (ORDER BY p.nr) AS next_grade,
					        LEAD(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS next_tick,
					        LEAD(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS next_todo,
					        FIRST_VALUE(p.id) OVER (ORDER BY p.nr) AS first_id, FIRST_VALUE(p.nr) OVER (ORDER BY p.nr) AS first_nr,
					        FIRST_VALUE(p.name) OVER (ORDER BY p.nr) AS first_name, FIRST_VALUE(g.grade) OVER (ORDER BY p.nr) AS first_grade,
					        FIRST_VALUE(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS first_tick,
					        FIRST_VALUE(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr) AS first_todo,
					        LAST_VALUE(p.id) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_id,
					        LAST_VALUE(p.nr) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_nr,
					        LAST_VALUE(p.name) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_name,
					        LAST_VALUE(g.grade) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_grade,
					        LAST_VALUE(CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_tick,
					        LAST_VALUE(CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END) OVER (ORDER BY p.nr ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_todo
					    FROM req JOIN sector s ON req.sector_id = s.id JOIN problem p ON s.id = p.sector_id JOIN area a ON s.area_id = a.id
					    JOIN grade g ON p.consensus_grade_id = g.id LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.user_id
					    LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = req.user_id
					    LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = req.user_id
					    LEFT JOIN tick ON p.id = tick.problem_id AND tick.user_id = req.user_id
					    WHERE p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
					) sub JOIN req ON 1=1
					CROSS JOIN LATERAL (
					    SELECT COALESCE(prev_id, last_id) AS n_id, COALESCE(prev_nr, last_nr) AS n_nr, COALESCE(prev_name, last_name) AS n_name, COALESCE(prev_grade, last_grade) AS n_grade, COALESCE(prev_tick, last_tick) AS n_tick, COALESCE(prev_todo, last_todo) AS n_todo
					    UNION ALL
					    SELECT COALESCE(next_id, first_id), COALESCE(next_nr, first_nr), COALESCE(next_name, first_name), COALESCE(next_grade, first_grade), COALESCE(next_tick, first_tick), COALESCE(next_todo, first_todo)
					) AS n         
					WHERE sub.id = req.problem_id AND n_id != req.problem_id
					""")
					.params(userId, sectorId, problemId)
					.query((rs, _) -> new Neighbour(rs.getInt("n_id"), rs.getInt("n_nr"), rs.getString("n_name"), rs.getString("n_grade"), rs.getBoolean("n_tick"), rs.getBoolean("n_todo")))
					.list()
					.stream()
					.distinct()
					.toList();
		}
		return jdbcClient.sql("""
				WITH req AS (SELECT ? AS user_id, ? AS sector_id, ? AS problem_id, ? AS rock)
				SELECT p.id, p.name, p.nr, g.grade, 
				       CASE WHEN f.user_id IS NOT NULL OR tick.id IS NOT NULL THEN 1 ELSE 0 END tick, 
				       CASE WHEN todo.user_id IS NOT NULL THEN 1 ELSE 0 END todo
				FROM req JOIN sector s ON req.sector_id = s.id JOIN problem p ON s.id = p.sector_id
				JOIN area a ON s.area_id = a.id JOIN grade g ON p.consensus_grade_id = g.id
				LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.user_id
				LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = req.user_id
				LEFT JOIN fa f ON p.id = f.problem_id AND f.user_id = req.user_id
				LEFT JOIN tick ON p.id = tick.problem_id AND tick.user_id = req.user_id
				WHERE p.rock = req.rock AND p.id != req.problem_id
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY p.nr
				""")
				.params(userId, sectorId, problemId, rock)
				.query((rs, _) -> new Neighbour(rs.getInt("id"), rs.getInt("nr"), rs.getString("name"), rs.getString("grade"), rs.getBoolean("tick"), rs.getBoolean("todo")))
				.list();
	}

	@Transactional(readOnly = true)
	protected void ensureAdminWriteProblem(Optional<Integer> authUserId, int problemId) {
		int userId = authUserId.orElseThrow(() -> new IllegalArgumentException("User not authenticated"));

		boolean ok = jdbcClient.sql("""
				SELECT ur.admin_write, ur.superadmin_write 
				FROM problem p
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				JOIN user_region ur ON a.region_id = ur.region_id
				WHERE p.id = ? AND ur.user_id = ?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0)) 
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0)) 
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				""")
				.params(problemId, userId)
				.query((rs, _) -> rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write"))
				.optional()
				.orElse(false);

		if (!ok) {
			throw new IllegalArgumentException("Insufficient permissions");
		}
	}

	@Transactional
	protected void updateProblemConsensusGrade(int problemId) {
		jdbcClient.sql("""
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
				""")
		.params(problemId, problemId, problemId)
		.update();
	}
}
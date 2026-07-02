package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.HitsFormatter;
import com.buldreinfo.helpers.SectorSort;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.ExternalLink;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Media.MediaSector;
import com.buldreinfo.model.Sector;
import com.buldreinfo.model.Sector.SectorProblem;
import com.buldreinfo.model.Sector.SectorProblemOrder;
import com.buldreinfo.model.Trail;
import com.buldreinfo.model.Trail.TrailBuilder;
import com.buldreinfo.model.Type;
import com.buldreinfo.util.StringUtils;

@Repository
public class SectorRepository {
	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;

	public SectorRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public void ensureAdminWriteSector(Optional<Integer> authUserId, int sectorId) {
		boolean ok = jdbcClient.sql("""
				SELECT ur.admin_write, ur.superadmin_write
				FROM user_region ur
				JOIN area a ON ur.region_id=a.region_id
				JOIN sector s ON a.id=s.area_id
				WHERE s.id=?
				  AND ur.user_id=?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				""")
				.params(sectorId, authUserId.orElseThrow())
				.query((rs, _) -> rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write"))
				.optional()
				.orElse(false);

		if (!ok) {
			throw new IllegalArgumentException("Insufficient permissions");
		}
	}
	
	@Transactional(readOnly = true)
	public Coordinates getFirstParkingCoordinateForSectors(List<Integer> sectorIds) {
		if (sectorIds == null || sectorIds.isEmpty()) return null;
		var inClause = Collections.nCopies(sectorIds.size(), "?").stream().collect(Collectors.joining(","));
		return jdbcClient.sql("SELECT c.latitude, c.longitude FROM sector s JOIN coordinates c ON s.parking_coordinates_id = c.id WHERE s.id IN (" + inClause + ") LIMIT 1")
				.params(sectorIds)
				.query((rs, _) -> new Coordinates(0, rs.getDouble("latitude"), rs.getDouble("longitude"), 0.0, null))
				.optional()
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public int getNextProblemNr(int sectorId) {
	    return jdbcClient.sql("SELECT COALESCE(MAX(nr), 0) + 1 FROM problem WHERE sector_id = ?")
	            .param(sectorId)
	            .query((rs, _) -> rs.getInt(1))
	            .single();
	}

	@Transactional(readOnly = true)
	public Sector getSectorBase(Setup setup, Optional<Integer> authUserId, int reqId, boolean orderByGrade,
			Supplier<List<Coordinates>> outlineSupplier,
			Supplier<Map<Integer, List<Trail>>> trailsSupplier,
			Supplier<List<Media>> mediaSupplier,
			Supplier<List<ExternalLink>> linksSupplier,
			Supplier<Map<Integer, List<SectorProblem>>> problemsSupplier) {

		Sector s = jdbcClient.sql("""
				WITH req AS (SELECT ? region_id, ? auth_user_id, ? sector_id)
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
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				GROUP BY a.id, a.locked_admin, a.locked_superadmin, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits
				""")
				.params(setup.idRegion(), authUserId.orElse(0), reqId)
				.query((rs, _) -> {
					int cid = rs.getInt("coordinates_id");
					var parking = cid == 0 ? null : new Coordinates(cid, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"));
					var mediaRes = mediaSupplier.get();
					var partitioned = Optional.ofNullable(mediaRes).orElse(List.of()).stream().collect(Collectors.partitioningBy(x -> x.sectors().stream().anyMatch(MediaSector::trivia)));
					return new Sector(null, orderByGrade, rs.getInt("area_id"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"), rs.getString("area_access_info"), rs.getString("area_access_closed"), rs.getBoolean("area_no_dogs_allowed"), rs.getInt("area_sun_from_hour"), rs.getInt("area_sun_to_hour"), rs.getString("area_name"), reqId, false, rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getString("name"), rs.getString("description"), rs.getString("access_info"), rs.getString("access_closed"), rs.getInt("sun_from_hour"), rs.getInt("sun_to_hour"), parking, outlineSupplier.get(), setup.getCompassDirection(rs.getInt("compass_direction_id_calculated")), setup.getCompassDirection(rs.getInt("compass_direction_id_manual")), trailsSupplier.get().get(reqId), partitioned.get(false), partitioned.get(true), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), linksSupplier.get(), HitsFormatter.formatHits(rs.getLong("hits")));
				}).optional().orElse(null);

		if (s == null) {
			return null;
		}

		s.sectors().addAll(
				jdbcClient.sql("SELECT s.id, s.locked_admin, s.locked_superadmin, s.name, s.sorting FROM ((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) WHERE a.id=? AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0)) GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.sorting ORDER BY s.sorting, s.name")
				.params(authUserId.orElse(0), s.areaId())
				.query((rs, _) -> new Sector.SectorJump(
						rs.getInt("id"), 
						rs.getBoolean("locked_admin"), 
						rs.getBoolean("locked_superadmin"), 
						rs.getString("name"), 
						rs.getInt("sorting")
						))
				.list()
				);

		s.sectors().sort((o1, o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()));
		Optional.ofNullable(problemsSupplier.get().get(reqId)).ifPresent(spList -> {
			for (SectorProblem sp : spList) {
				s.problems().add(sp);
				s.problemOrder().add(new Sector.SectorProblemOrder(sp.id(), sp.name(), sp.nr()));
			}
		});

		if (orderByGrade) s.problems().sort(Comparator.comparing(SectorProblem::gradeWeight).reversed());
		return s;
	}

	@Transactional(readOnly = true)
	public List<Coordinates> getSectorOutline(int idSector) {
		Map<Integer, List<Coordinates>> idSectorOutline = getSectorOutlines(Collections.singleton(idSector));
		if (idSectorOutline == null || idSectorOutline.isEmpty()) { 
			return null;
		}
		return new ArrayList<>(idSectorOutline.getOrDefault(idSector, List.of()));
	}

	@Transactional(readOnly = true)
	public Map<Integer, List<Coordinates>> getSectorOutlines(Collection<Integer> idSectors) {
		if (idSectors.isEmpty()) throw new IllegalArgumentException("idSectors is empty");
		var res = new HashMap<Integer, List<Coordinates>>();
		jdbcClient.sql("SELECT so.sector_id, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source " +
				"FROM sector_outline so " +
				"JOIN coordinates c ON so.coordinates_id = c.id " +
				"WHERE so.sector_id IN (:ids) " +
				"ORDER BY so.sorting")
		.param("ids", idSectors)
		.query(rs -> {
			res.computeIfAbsent(rs.getInt("sector_id"), _ -> new ArrayList<>())
			.add(new Coordinates(
					rs.getInt("id"),
					rs.getDouble("latitude"),
					rs.getDouble("longitude"),
					rs.getDouble("elevation"),
					rs.getString("elevation_source")
					));
		});
		return res;
	}

	@Transactional(readOnly = true)
	public Map<Integer, List<SectorProblem>> getSectorProblems(Setup setup, Optional<Integer> authUserId, int optAreaId, int optSectorId) {
		if (!((optAreaId == 0 && optSectorId > 0) || (optAreaId > 0 && optSectorId == 0))) {
			throw new IllegalArgumentException("Invalid area/sector id combination");
		}

		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? area_id, ? sector_id, ? include_fa_aid
				),
				filtered_problems AS (
				    SELECT p.*
				    FROM problem p
				    JOIN sector s ON s.id = p.sector_id
				    JOIN area a ON s.area_id = a.id
				    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = (SELECT auth_user_id FROM req)
				    WHERE (( (SELECT area_id FROM req) > 0 AND a.id = (SELECT area_id FROM req)) 
				        OR ((SELECT sector_id FROM req) > 0 AND p.sector_id = (SELECT sector_id FROM req)))
				      AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				),
				fa_agg AS (
				    SELECT f.problem_id, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_names,
				           MAX(CASE WHEN u.id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_is_fa
				    FROM fa f JOIN user u ON f.user_id = u.id WHERE f.problem_id IN (SELECT id FROM filtered_problems) GROUP BY f.problem_id
				),
				fa_aid_agg AS (
				    SELECT a.problem_id, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_aid_names, YEAR(a.aid_date) fa_aid_date
				    FROM fa_aid a JOIN fa_aid_user au ON a.problem_id=au.problem_id JOIN user u ON au.user_id = u.id WHERE a.problem_id IN (SELECT id FROM filtered_problems) GROUP BY a.problem_id, a.aid_date
				),
				tick_agg AS (
				    SELECT t.problem_id, COUNT(t.id) AS total_ticks, ROUND(AVG(NULLIF(t.stars, -1)), 1) AS avg_stars, MAX(CASE WHEN t.user_id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_ticked
				    FROM tick t WHERE t.problem_id IN (SELECT id FROM filtered_problems) GROUP BY t.problem_id
				),
				media_agg AS (
				    SELECT mp.problem_id, COUNT(DISTINCT CASE WHEN m.is_movie = 0 THEN m.id END) AS num_images, COUNT(DISTINCT CASE WHEN m.is_movie = 1 THEN m.id END) AS num_movies
				    FROM media_problem mp JOIN media m ON mp.media_id = m.id WHERE mp.trivia = 0 AND m.deleted_user_id IS NULL AND mp.problem_id IN (SELECT id FROM filtered_problems) GROUP BY mp.problem_id
				)
				SELECT p.sector_id, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, fa_aid.fa_aid_names, fa_aid.fa_aid_date, fa.fa_names, YEAR(p.fa_date) AS ffa_year, ty.id AS type_id, ty.type, ty.subtype, g.weight, g.grade, COALESCE(t.total_ticks, 0) AS total_ticks, COALESCE(t.avg_stars, 0) AS stars, GREATEST(COALESCE(t.user_ticked, 0), COALESCE(fa.user_is_fa, 0)) AS ticked, CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END AS todo, gb.danger, p.length_meter, co.id AS coordinates_id, co.latitude, co.longitude, co.elevation, co.elevation_source, (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p.id) AS num_pitches, COALESCE(m.num_images, 0) AS num_images, COALESCE(m.num_movies, 0) AS num_movies, CASE WHEN EXISTS (SELECT 1 FROM svg WHERE svg.problem_id = p.id) THEN 1 ELSE 0 END AS has_topo
				FROM filtered_problems p
				JOIN grade g ON p.consensus_grade_id = g.id
				JOIN type ty ON p.type_id = ty.id
				LEFT JOIN coordinates co ON p.coordinates_id = co.id
				LEFT JOIN fa_agg fa ON p.id = fa.problem_id
				LEFT JOIN fa_aid_agg fa_aid ON p.id = fa_aid.problem_id
				LEFT JOIN tick_agg t ON p.id = t.problem_id
				LEFT JOIN media_agg m ON p.id = m.problem_id
				LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = (SELECT auth_user_id FROM req)
				LEFT JOIN LATERAL (SELECT danger FROM guestbook WHERE problem_id = p.id AND (danger = 1 OR resolved = 1) ORDER BY id DESC LIMIT 1) gb ON TRUE
				ORDER BY p.nr
				""";

		Map<Integer, List<SectorProblem>> res = new LinkedHashMap<>();
		jdbcClient.sql(sql)
		.params(authUserId.orElse(0), optAreaId, optSectorId, setup.isBouldering() ? 0 : 1)
		.query(rs -> {
			int sid = rs.getInt("sector_id");
			int cid = rs.getInt("coordinates_id");
			var coords = cid == 0 ? null : new Coordinates(cid, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"));
			var p = new SectorProblem(
					rs.getInt("id"), rs.getString("broken"), rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"),
					rs.getInt("nr"), rs.getString("name"), rs.getString("rock"), rs.getString("description"),
					rs.getInt("weight"), rs.getString("grade"), rs.getString("fa_aid_names"), rs.getInt("fa_aid_date"),
					rs.getString("fa_names"), rs.getInt("ffa_year"), rs.getInt("length_meter"), rs.getInt("num_pitches"),
					rs.getInt("num_images") > 0, rs.getInt("num_movies") > 0, rs.getBoolean("has_topo"), coords,
					rs.getInt("total_ticks"), rs.getDouble("stars"), rs.getBoolean("ticked"), rs.getBoolean("todo"),
					new Type(rs.getInt("type_id"), rs.getString("type"), rs.getString("subtype")), rs.getBoolean("danger")
					);
			res.computeIfAbsent(sid, _ -> new ArrayList<>()).add(p);
		});
		return res;
	}

	@Transactional(readOnly = true)
	public List<Integer> getSectorsForTrails(List<Integer> existingTrailIds) {
		var inClause = Collections.nCopies(existingTrailIds.size(), "?").stream().collect(Collectors.joining(","));
		return jdbcClient.sql("SELECT sector_id FROM sector_trail WHERE trail_id IN (" + inClause + ")")
				.params(existingTrailIds)
				.query((rs, _) -> rs.getInt("sector_id"))
				.list();
	}

	@Transactional(readOnly = true)
	public Map<Integer, List<Trail>> getSectorTrails(Collection<Integer> sectorIds, Function<List<Integer>, Map<Integer, List<Media>>> mediaTrailsResolver) {
		if (sectorIds.isEmpty()) throw new IllegalArgumentException("sectorIds is empty");

		var trailBuilders = new LinkedHashMap<Integer, TrailBuilder>();
		var sectorToTrailIds = new HashMap<Integer, List<Integer>>();

		var inClause = Collections.nCopies(sectorIds.size(), "?").stream().collect(Collectors.joining(","));
		jdbcClient.sql("SELECT st.sector_id, t.id, t.is_descent, t.title, t.description FROM sector_trail st JOIN trail t ON st.trail_id = t.id WHERE st.sector_id IN (" + inClause + ") AND t.trash IS NULL ORDER BY t.is_descent, t.title")
		.params(new ArrayList<>(sectorIds))
		.query(rs -> {
			int sid = rs.getInt("sector_id");
			int tid = rs.getInt("id");
			var isDescent = rs.getBoolean("is_descent");
			var title = rs.getString("title");
			var description = rs.getString("description");
			sectorToTrailIds.computeIfAbsent(sid, _ -> new ArrayList<>()).add(tid);
			trailBuilders.computeIfAbsent(tid, id -> new TrailBuilder(id, isDescent, title, description));
		});

		if (trailBuilders.isEmpty()) return new HashMap<>();

		var trailIdsList = new ArrayList<>(trailBuilders.keySet());
		var pathInClause = Collections.nCopies(trailBuilders.size(), "?").stream().collect(Collectors.joining(","));

		jdbcClient.sql("SELECT tc.trail_id, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM trail_coordinate tc JOIN coordinates c ON tc.coordinates_id = c.id WHERE tc.trail_id IN (" + pathInClause + ") ORDER BY tc.trail_id, tc.sorting")
		.params(trailIdsList)
		.query(rs -> {
			trailBuilders.get(rs.getInt("trail_id")).path.add(new Coordinates(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source")));
		});

		jdbcClient.sql("SELECT tm.trail_id, tm.label, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM trail_marker tm JOIN coordinates c ON tm.coordinates_id = c.id WHERE tm.trail_id IN (" + pathInClause + ")")
		.params(trailIdsList)
		.query(rs -> {
			trailBuilders.get(rs.getInt("trail_id")).markers.add(new Trail.TrailMarker(new Coordinates(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source")), rs.getString("label")));
		});

		var mediaMap = mediaTrailsResolver.apply(trailIdsList);
		var finalTrailsMap = new HashMap<Integer, Trail>();
		trailBuilders.values().forEach(b -> 
		finalTrailsMap.put(b.id, Trail.withCalculatedStats(b.id, b.isDescent, false, b.title, b.description, b.path, b.markers, mediaMap.get(b.id), null)));

		Map<Integer, List<Trail>> res = new HashMap<>();
		sectorIds.forEach(sid -> {
			var tIds = sectorToTrailIds.get(sid);
			if (tIds != null) {
				var trails = tIds.stream().map(finalTrailsMap::get).filter(Objects::nonNull).toList();
				if (!trails.isEmpty()) res.put(sid, trails);
			}
		});

		return res;
	}

	@Transactional
	public int setSectorDb(Optional<Integer> authUserId, Sector s, boolean isLockedAdmin, Integer parkingId, Integer calcCompass, Integer manualCompass, boolean setPermissionRecursive) {
		Integer trashBy = s.trash() ? authUserId.get() : null;
		int idSector;
		
		if (s.id() > 0) {
			jdbcClient.sql("""
					UPDATE sector s, area a, user_region ur 
					SET s.name=?, s.description=?, s.access_info=?, s.access_closed=?, s.sun_from_hour=?, s.sun_to_hour=?, 
					    s.parking_coordinates_id=?, s.locked_admin=?, s.locked_superadmin=?, s.compass_direction_id_calculated=?, 
					    s.compass_direction_id_manual=?, s.trash=CASE WHEN ? THEN NOW() ELSE NULL END, s.trash_by=? 
					WHERE s.id=? AND s.area_id=a.id AND a.region_id=ur.region_id AND ur.user_id=? 
					  AND (ur.admin_write=1 OR ur.superadmin_write=1)
					""")
			.params(StringUtils.stripToNull(s.name()), StringUtils.stripToNull(s.comment()), 
					StringUtils.stripToNull(s.accessInfo()), StringUtils.stripToNull(s.accessClosed()), 
					s.sunFromHour(), s.sunToHour(), parkingId, 
					isLockedAdmin, s.lockedSuperadmin(), calcCompass, manualCompass, 
					s.trash(), trashBy, s.id(), authUserId.get())
			.update();

			idSector = s.id();

			if (setPermissionRecursive) {
				jdbcClient.sql("""
						UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id 
						SET a.last_updated=now(), s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, 
						    p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE s.id=?
						""")
				.params(isLockedAdmin, s.lockedSuperadmin(), isLockedAdmin, s.lockedSuperadmin(), idSector)
				.update();
			} else {
				jdbcClient.sql("""
						UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id 
						SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?
						""")
				.param(1, idSector)
				.update();
			}
		} else {
			var keyHolder = new GeneratedKeyHolder();
			jdbcClient.sql("""
					INSERT INTO sector (area_id, name, description, access_info, access_closed, parking_coordinates_id, 
					locked_admin, locked_superadmin, compass_direction_id_calculated, compass_direction_id_manual, last_updated) 
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
					""")
			.params(s.areaId(), s.name(), StringUtils.stripToNull(s.comment()), StringUtils.stripToNull(s.accessInfo()), 
					StringUtils.stripToNull(s.accessClosed()), parkingId, 
					isLockedAdmin, s.lockedSuperadmin(), calcCompass, manualCompass)
			.update(keyHolder, "id");
			idSector = keyHolder.getKey().intValue();
		}

		jdbcClient.sql("DELETE FROM sector_outline WHERE sector_id=?").param(1, idSector).update();
		if (s.outline() != null && !s.outline().isEmpty()) {
			int[] sorting = {0};
			for (Coordinates coord : s.outline()) {
				jdbcClient.sql("INSERT INTO sector_outline (sector_id, coordinates_id, sorting) VALUES (?, ?, ?)")
				.params(idSector, coord.getId(), ++sorting[0])
				.update();
			}
		}
		
		return idSector;
	}

	@Transactional
	public void setSectorProblemOrder(List<SectorProblemOrder> lst) {
		if (lst == null || lst.isEmpty()) return;
		jdbcTemplate.batchUpdate(
				"UPDATE problem SET nr=? WHERE id=?",
				lst,
				100,
				(ps, item) -> {
					ps.setInt(1, item.nr());
					ps.setInt(2, item.id());
				}
				);
	}

	@Transactional
	public void tryFixSectorOrdering(int sectorId, int problemId, int problemNewNr) {
		List<SectorProblemOrder> lst = new ArrayList<>();
		var counter = new Object() { int nr = 0; };
		String sql = (problemId > 0) ? """
				WITH x AS (
				  SELECT p.sector_id, COUNT(p.id) num_problems, MAX(p.nr) max_num
				  FROM problem p
				  WHERE p.sector_id=?
				  GROUP BY p.sector_id
				)
				SELECT p.id
				FROM problem p_input, x, problem p
				WHERE p_input.id=? AND p_input.nr!=?
				  AND p_input.sector_id=x.sector_id AND x.num_problems=x.max_num
				  AND p_input.sector_id=p.sector_id
				  AND p.id!=p_input.id
				ORDER BY p.nr, p.id
				"""
				:
						"""
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
						ORDER BY p.nr, p.id
						""";
		var spec = jdbcClient.sql(sql);
		if (problemId > 0) {
			spec.params(sectorId, problemId, problemNewNr);
		}
		else if (problemNewNr != 0) {
			spec.param(1, sectorId);
		}
		else {
			return;
		}
		spec.query(rs -> {
			counter.nr++;
			if (counter.nr == problemNewNr) {
				counter.nr++;
			}
			lst.add(new SectorProblemOrder(rs.getInt("id"), null, counter.nr));
		});

		setSectorProblemOrder(lst);
	}

	@Transactional
	public void upsertTrailsDb(Optional<Integer> authUserId, List<Trail> trails) {
		for (Trail t : trails.stream().sorted(Comparator.comparingInt(Trail::id)).toList()) {
			int trailId = t.id();
			if (t.delete()) {
				jdbcClient.sql("UPDATE trail SET trash = NOW(), trash_by = ? WHERE id = ?").params(authUserId.orElseThrow(), trailId).update();
				continue;
			}

			if (trailId > 0) {
				jdbcClient.sql("UPDATE trail SET is_descent = ?, title = ?, description = ?, trash = NULL, trash_by = 0 WHERE id = ?")
				.params(t.isDescent(), t.title(), t.description(), trailId).update();
			} else {
				var keyHolder = new GeneratedKeyHolder();
				jdbcClient.sql("INSERT INTO trail (is_descent, title, description) VALUES (?, ?, ?)")
				.params(t.isDescent(), t.title(), t.description())
				.update(keyHolder, "id");
				trailId = keyHolder.getKey().intValue();
			}

			jdbcClient.sql("DELETE FROM trail_coordinate WHERE trail_id = ?").param(1, trailId).update();
			if (t.path() != null) {
				int[] sort = {0};
				for (Coordinates coord : t.path()) 
					jdbcClient.sql("INSERT INTO trail_coordinate (trail_id, coordinates_id, sorting) VALUES (?, ?, ?)").params(trailId, coord.getId(), sort[0]++).update();
			}

			jdbcClient.sql("DELETE FROM trail_marker WHERE trail_id = ?").param(1, trailId).update();
			if (t.markers() != null) {
				for (Trail.TrailMarker m : t.markers()) 
					if (m.coordinates() != null) jdbcClient.sql("INSERT INTO trail_marker (trail_id, coordinates_id, label) VALUES (?, ?, ?)").params(trailId, m.coordinates().getId(), m.label()).update();
			}

			jdbcClient.sql("DELETE FROM sector_trail WHERE trail_id = ?").param(1, trailId).update();
			if (t.sectors() != null) {
				for (Trail.TrailSector s : t.sectors()) 
					jdbcClient.sql("INSERT INTO sector_trail (sector_id, trail_id) VALUES (?, ?)").params(s.sectorId(), trailId).update();
			}
		}
	}
}
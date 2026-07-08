package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.helpers.HitsFormatter;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Area.AreaSector;
import com.buldreinfo.model.Area.GradeCount;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.MediaIdentity;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.util.StringUtils;

@Repository
public class AreaRepository {
	private final ExternalLinksRepository externalLinksRepo;
	private final JdbcClient jdbcClient;

	public AreaRepository(JdbcClient jdbcClient,
			ExternalLinksRepository externalLinksRepo) {
		this.jdbcClient = jdbcClient;
		this.externalLinksRepo = externalLinksRepo;
	}

	@Transactional(readOnly = true)
	public void ensureAdminWriteArea(Optional<Integer> authUserId, int areaId) {
		boolean ok = jdbcClient.sql("""
				SELECT ur.admin_write, ur.superadmin_write
				FROM area a
				JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE a.id=?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				""")
				.params(authUserId.orElseThrow(), areaId)
				.query((rs, _) -> rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write"))
				.optional()
				.orElse(false);

		if (!ok) {
			throw new ForbiddenException("Insufficient permissions");
		}
	}

	@Transactional(readOnly = true)
	public Area getAreaBase(Setup setup, Optional<Integer> authUserId, int reqId, List<Media> partitionedFalse, List<Media> partitionedTrue) {
		return jdbcClient.sql("""
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
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY r.name, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.name, a.sun_from_hour, a.sun_to_hour, a.description,
				         c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				""")
				.params(setup.idRegion(), authUserId.orElse(0), reqId)
				.query((rs, _) -> {
					int cid = rs.getInt("coordinates_id");
					var coords = cid == 0 ? null : new Coordinates(cid, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0);
					return new Area(null, rs.getString("region_name"), reqId, false, rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getBoolean("for_developers"), rs.getString("access_info"), rs.getString("access_closed"), rs.getBoolean("no_dogs_allowed"), rs.getInt("sun_from_hour"), rs.getInt("sun_to_hour"), rs.getString("name"), rs.getString("description"), coords, -1, -1, new ArrayList<>(), new ArrayList<>(), partitionedFalse, partitionedTrue, externalLinksRepo.getExternalLinks(reqId, 0, 0), HitsFormatter.formatHits(rs.getLong("hits")));
				}).optional()
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public Collection<Area> getAreaList(Optional<Integer> authUserId, int reqIdRegion) {
		var sqlStr = """
				SELECT r.name region_name, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, 
				       a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, 
				       a.name, a.description, c.id coordinates_id, c.latitude, c.longitude, c.elevation, 
				       c.elevation_source, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits
				FROM area a
				JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN coordinates c ON a.coordinates_id=c.id
				LEFT JOIN sector s ON a.id=s.area_id
				LEFT JOIN problem p ON s.id=p.sector_id
				LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY r.name, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				ORDER BY r.name, a.name
				""";

		return jdbcClient.sql(sqlStr)
				.params(authUserId.orElse(0), reqIdRegion, reqIdRegion)
				.query((rs, _) -> {
					String comment = rs.getString("description");
					if (comment != null) {
						int ix = comment.indexOf("<strong>Forhold:</strong>");
						if (ix != -1) {
							comment = comment.substring(ix + 25);
							int endIx = comment.indexOf("<strong>");
							comment = (endIx != -1) ? comment.substring(0, endIx) : comment;
						}
					}

					int cid = rs.getInt("coordinates_id");
					var coords = cid == 0 ? null : new Coordinates(cid, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0);

					return new Area(null, rs.getString("region_name"), rs.getInt("id"), false, 
							rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"), rs.getBoolean("for_developers"), 
							rs.getString("access_info"), rs.getString("access_closed"), rs.getBoolean("no_dogs_allowed"), 
							rs.getInt("sun_from_hour"), rs.getInt("sun_to_hour"), rs.getString("name"), comment, coords, 
							rs.getInt("num_sectors"), rs.getInt("num_problems"), null, null, null, null, null, 
							HitsFormatter.formatHits(rs.getLong("hits")));
				}).list();
	}

	public Map<Integer, AreaSector> getAreaSectors(Setup setup, Optional<Integer> authUserId, int areaId, String areaName) {
		var sqlStr = """
				WITH ranked_media AS (
				  SELECT s.id sector_id,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         ROW_NUMBER() OVER (PARTITION BY p.sector_id ORDER BY m.is_360, m.is_movie, m.id DESC) rn
				  FROM area a
				  JOIN sector s ON a.id=s.area_id
				  JOIN problem p ON s.id=p.sector_id
				  JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
				  JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=:authUserId
				  WHERE a.id = :areaId
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				),
				ranked_sector_media AS (
				  SELECT ms.sector_id,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         ROW_NUMBER() OVER (PARTITION BY ms.sector_id ORDER BY m.is_360, m.is_movie, m.id DESC) rn
				  FROM sector s
				  JOIN media_sector ms ON s.id=ms.sector_id
				  JOIN media m ON ms.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  WHERE s.area_id = :areaId
				)
				SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual,
				       COALESCE(rm.media_id, rsm.media_id) media_id,
				       COALESCE(rm.media_version_stamp, rsm.media_version_stamp) media_version_stamp,
				       COALESCE(rm.media_focus_x, rsm.media_focus_x) media_focus_x,
				       COALESCE(rm.media_focus_y, rsm.media_focus_y) media_focus_y,
				       COALESCE(rm.media_primary_color_hex, rsm.media_primary_color_hex) media_primary_color_hex
				FROM area a
				JOIN sector s ON a.id=s.area_id
				LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=:authUserId
				LEFT JOIN ranked_media rm ON s.id=rm.sector_id AND rm.rn=1
				LEFT JOIN ranked_sector_media rsm ON s.id=rsm.sector_id AND rsm.rn=1
				WHERE a.id = :areaId
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				ORDER BY s.sorting, s.name
				""";

		Map<Integer, AreaSector> sectorLookup = new LinkedHashMap<>();
		jdbcClient.sql(sqlStr)
		.param("authUserId", authUserId.orElse(0))
		.param("areaId", areaId)
		.query(rs -> {
			int id = rs.getInt("id");
			int coordId = rs.getInt("coordinates_id");
			var parking = coordId == 0 ? null : new Coordinates(coordId, rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0);

			MediaIdentity mediaIdentity = null;
			int mid = rs.getInt("media_id");
			if (mid > 0) {
				mediaIdentity = new MediaIdentity(mid, rs.getLong("media_version_stamp"), rs.getInt("media_focus_x"), rs.getInt("media_focus_y"), rs.getString("media_primary_color_hex"));
			}

			sectorLookup.put(id, new AreaSector(
					areaName, id, rs.getInt("sorting"), rs.getBoolean("locked_admin"), rs.getBoolean("locked_superadmin"),
					rs.getString("name"), rs.getString("description"), rs.getString("access_info"), rs.getString("access_closed"),
					rs.getInt("sun_from_hour"), rs.getInt("sun_to_hour"), parking, new ArrayList<>(),
					setup.getCompassDirection(rs.getInt("compass_direction_id_calculated")),
					setup.getCompassDirection(rs.getInt("compass_direction_id_manual")),
					null, mediaIdentity, new ArrayList<>(), 0, new ArrayList<>()
					));
		});
		return sectorLookup;
	}

	public void loadSimplifiedGradeCounts(int areaId, Map<Integer, AreaSector> sectorLookup) {
		var sqlStr = """
				WITH target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM area a
				  JOIN region_type rt ON a.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				  WHERE a.id = :areaId
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
				FROM sector s
				CROSS JOIN all_labels al
				LEFT JOIN problem p ON s.id = p.sector_id 
				    AND EXISTS (
				        SELECT 1 FROM grade g_p 
				        WHERE p.consensus_grade_id = g_p.id 
				        AND g_p.label_compact = al.label_compact 
				        AND g_p.grade_system_id = al.grade_system_id
				    )
				    AND p.trash IS NULL AND p.locked_admin = 0 AND p.locked_superadmin = 0
				WHERE s.area_id = :areaId
				GROUP BY s.id, al.label_compact, al.hex_code, al.sort_weight
				ORDER BY s.id, al.sort_weight
				""";

		jdbcClient.sql(sqlStr)
		.param("areaId", areaId)
		.query(rs -> {
			AreaSector sector = sectorLookup.get(rs.getInt("sector_id"));
			if (sector != null) {
				sector.gradeCounts().add(new GradeCount(
						rs.getString("label_compact"), 
						rs.getString("color"), 
						rs.getInt("num")
						));
			}
		});
	}

	@Transactional
	public Redirect setArea(Setup s, Optional<Integer> authUserId, Area a) {
		if (authUserId.isEmpty()) throw new UnauthorizedException("Not logged in");
		if (s.idRegion() <= 0) throw new ForbiddenException("Insufficient credentials");

		final boolean isLockedAdmin = !a.lockedSuperadmin() && a.lockedAdmin();
		boolean setPermissionRecursive = false;

		int idArea;
		if (a.id() > 0) {
			ensureAdminWriteArea(authUserId, a.id());
			var currArea = getAreaBase(s, authUserId, a.id(), List.of(), List.of());
			setPermissionRecursive = currArea.lockedAdmin() != isLockedAdmin || currArea.lockedSuperadmin() != a.lockedSuperadmin();

			jdbcClient.sql("""
					UPDATE area SET name=?, description=?, coordinates_id=?, locked_admin=?, locked_superadmin=?, 
					for_developers=?, access_info=?, access_closed=?, no_dogs_allowed=?, sun_from_hour=?, sun_to_hour=?, 
					trash=CASE WHEN ? THEN NOW() ELSE NULL END, trash_by=? WHERE id=?
					""")
			.params(StringUtils.stripToNull(a.name()), StringUtils.stripToNull(a.comment()), 
					a.coordinates() == null ? 0 : a.coordinates().id(), isLockedAdmin, a.lockedSuperadmin(), 
							a.forDevelopers(), StringUtils.stripToNull(a.accessInfo()), 
							StringUtils.stripToNull(a.accessClosed()), a.noDogsAllowed(), a.sunFromHour(), 
							a.sunToHour(), a.trash(), a.trash() ? authUserId.get() : 0, a.id())
			.update();

			idArea = a.id();

			if (a.sectorOrder() != null) {
				for (var x : a.sectorOrder()) {
					jdbcClient.sql("UPDATE sector SET sorting=? WHERE id=?").params(x.sorting(), x.id()).update();
				}
			}

			if (setPermissionRecursive) {
				jdbcClient.sql("""
						UPDATE area a LEFT JOIN sector s ON a.id=s.area_id LEFT JOIN problem p ON s.id=p.sector_id 
						SET a.last_updated=now(), a.locked_admin=?, a.locked_superadmin=?, 
						s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, 
						p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE a.id=?
						""")
				.params(isLockedAdmin, a.lockedSuperadmin(), isLockedAdmin, a.lockedSuperadmin(), 
						isLockedAdmin, a.lockedSuperadmin(), idArea)
				.update();
			} else {
				jdbcClient.sql("""
						UPDATE area a LEFT JOIN sector s ON a.id=s.area_id LEFT JOIN problem p ON s.id=p.sector_id 
						SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?
						""")
				.param(idArea)
				.update();
			}
		} else {
			var keyHolder = new GeneratedKeyHolder();
			jdbcClient.sql("""
					INSERT INTO area (region_id, name, description, coordinates_id, locked_admin, locked_superadmin, 
					for_developers, access_info, access_closed, no_dogs_allowed, last_updated) 
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
					""")
			.params(s.idRegion(), StringUtils.stripToNull(a.name()), StringUtils.stripToNull(a.comment()), 
					a.coordinates() == null ? 0 : a.coordinates().id(), isLockedAdmin, a.lockedSuperadmin(), 
							a.forDevelopers(), StringUtils.stripToNull(a.accessInfo()), 
							StringUtils.stripToNull(a.accessClosed()), a.noDogsAllowed())
			.update(keyHolder, "id");
			idArea = keyHolder.getKey().intValue();
		}

		externalLinksRepo.upsertExternalLinks(a.externalLinks(), idArea, 0, 0);
		return a.trash() ? Redirect.fromRoot() : Redirect.fromIdArea(idArea);
	}
}
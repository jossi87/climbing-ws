package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.helpers.HitsFormatter;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.DangerousArea.DangerousProblem;
import com.buldreinfo.jersey.jaxb.model.DangerousArea.DangerousSector;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion.RestrictionsArea;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion.RestrictionsSector;
import com.buldreinfo.jersey.jaxb.model.Toc.TocArea;
import com.buldreinfo.jersey.jaxb.model.Toc.TocPitch;
import com.buldreinfo.jersey.jaxb.model.Toc.TocProblem;
import com.buldreinfo.jersey.jaxb.model.Toc.TocRegion;
import com.buldreinfo.jersey.jaxb.model.Toc.TocSector;
import com.buldreinfo.jersey.jaxb.model.Top.TopRank;
import com.buldreinfo.jersey.jaxb.model.Top.TopUser;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public record HierarchyRepository(Dao dao) {
	private static Logger logger = LogManager.getLogger();
	
	public Redirect getCanonicalUrl(Connection c, Setup setup, int idArea, int idSector, int idProblem) throws SQLException {
		String sqlStr = null;
		int id = 0;
		if (idArea > 0) {
			sqlStr = """
					SELECT CONCAT(r.url,'/area/',a.id) url
					FROM region r
					JOIN area a ON r.id=a.region_id
					WHERE r.id!=? AND a.id=?
					""";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = """
					SELECT CONCAT(r.url,'/sector/',s.id) url
					FROM region r
					JOIN area a ON r.id=a.region_id
					JOIN sector s ON a.id=s.area_id
					WHERE r.id!=? AND s.id=?
					""";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = """
					SELECT CONCAT(r.url,'/problem/',p.id) url
					FROM region r
					JOIN area a ON r.id=a.region_id
					JOIN sector s ON a.id=s.area_id
					JOIN problem p ON s.id=p.sector_id
					WHERE r.id!=? AND p.id=?
					""";
			id = idProblem;
		}
		Preconditions.checkArgument(id > 0 && sqlStr != null, "Invalid parameters: idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		Redirect res = null;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = Redirect.fromRedirectUrl(rst.getString("url"));
				}
			}
		}
		if (res == null) {
			throw new NoSuchElementException("Could not find canonical url for idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		}
		return res;
	}

	public Collection<GradeDistribution> getContentGraph(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN region_type rt ON req.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				),
				x AS (
				  SELECT g.label_major g_base, r.id region_id, r.name region, COALESCE(ty.subtype,'Boulder') t, COUNT(DISTINCT p.id) num
				  FROM req
				  JOIN region r ON 1=1
				  JOIN region_type rt ON r.id = rt.region_id
				  JOIN area a ON r.id = a.region_id
				  JOIN sector s ON a.id = s.area_id
				  JOIN problem p ON s.id = p.sector_id
				  JOIN type ty ON p.type_id = ty.id
				  JOIN grade g ON p.consensus_grade_id = g.id
				  JOIN target_systems ts ON g.grade_system_id = ts.grade_system_id
				  LEFT JOIN user_region ur ON r.id = ur.region_id AND ur.user_id = req.auth_user_id
				  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				    AND (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				  GROUP BY r.id, r.name, g.label_major, ty.subtype
				)
				SELECT g.label_major grade, clr.hex_code color, x.region_id, x.region, x.t, COALESCE(x.num, 0) num
				FROM req
				JOIN target_systems ts ON 1=1
				JOIN (
				  SELECT label_major, grade_system_id, grade_color_id, MIN(weight) sort 
				  FROM grade GROUP BY label_major, grade_system_id, grade_color_id
				) g ON g.grade_system_id = ts.grade_system_id
				JOIN grade_color clr ON g.grade_color_id = clr.id
				LEFT JOIN x ON g.label_major = x.g_base
				ORDER BY g.sort, x.region, x.t
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String label = rst.getString("grade");
					String color = rst.getString("color");
					GradeDistribution dist = res.computeIfAbsent(label, k -> new GradeDistribution(k, color));
					int regionId = rst.getInt("region_id");
					if (!rst.wasNull()) {
						dist.addSector(regionId, rst.getString("region"), rst.getString("t"), rst.getInt("num"));
					}
				}
			}
		}
		return res.values();
	}

	public Collection<DangerousArea> getDangerous(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, DangerousArea> areasLookup = new LinkedHashMap<>();
		Map<Integer, DangerousSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour,
				       s.id sector_id, s.name sector_name, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.sun_from_hour sector_sun_from_hour, s.sun_to_hour sector_sun_to_hour,
				       p.id problem_id, p.broken problem_broken, p.nr problem_nr, gr.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, DATE_FORMAT(g.post_time,'%Y.%m.%d') post_time, g.message
				FROM area a
				JOIN region_type rt ON a.region_id=rt.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN grade gr ON p.grade_id=gr.id
				JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)
				JOIN user u ON g.user_id=u.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				  AND (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour,
				         s.id, s.name, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, s.sun_from_hour, s.sun_to_hour,
				         p.id, p.broken, p.nr, gr.grade, p.name, p.locked_admin, p.locked_superadmin,
				         u.firstname, u.lastname, g.post_time, g.message
				ORDER BY a.name, s.name, p.nr
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					DangerousArea a = areasLookup.get(areaId);
					if (a == null) {
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new DangerousArea(areaId, areaName, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour, new ArrayList<>());
						areasLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					DangerousSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorName = rst.getString("sector_name");
						CompassDirection sectorWallDirectionCalculated = dao.getGeoRepo().getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = dao.getGeoRepo().getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
						int sectorSunToHour = rst.getInt("sector_sun_to_hour");
						s = new DangerousSector(sectorId, sectorName, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin, sectorSunFromHour, sectorSunToHour, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("problem_id");
					String broken = rst.getString("problem_broken");
					int nr = rst.getInt("problem_nr");
					String grade = rst.getString("problem_grade");
					boolean lockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String name = rst.getString("problem_name");
					String postBy = rst.getString("name");
					String postWhen = rst.getString("post_time");
					String postTxt = rst.getString("message");
					s.problems().add(new DangerousProblem(id, broken, lockedAdmin, lockedSuperadmin, nr, name, grade, postBy, postWhen, postTxt));
				}
			}
		}
		logger.debug("getDangerous() - areasLookup.size()={}, duration={}", areasLookup.size(), stopwatch);
		return areasLookup.values();
	}
	
	public Collection<GradeDistribution> getGradeDistribution(Connection c, Optional<Integer> authUserId, int optionalAreaId, int optionalSectorId) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? sector_id, ? area_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN area a ON a.id = COALESCE(NULLIF(req.area_id, 0), (SELECT area_id FROM sector WHERE id = req.sector_id))
				  JOIN region_type rt ON a.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
				),
				x AS (
				  SELECT g.label_major g_base, s.sorting, s.id sector_id, s.name sector, COALESCE(ty.subtype,'Boulder') t, COUNT(p.id) num
				  FROM req
				  JOIN sector s ON (CASE WHEN req.sector_id != 0 THEN s.id = req.sector_id ELSE s.area_id = req.area_id END)
				  JOIN area a ON s.area_id = a.id
				  JOIN problem p ON s.id = p.sector_id 
				  JOIN type ty ON p.type_id = ty.id 
				  JOIN grade g ON p.consensus_grade_id = g.id
				  JOIN target_systems ts ON g.grade_system_id = ts.grade_system_id
				  LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id 
				  WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				  GROUP BY s.sorting, s.id, s.name, g.label_major, ty.subtype
				)
				SELECT g.label_major grade, clr.hex_code color, x.sector_id, x.sector, x.t, COALESCE(x.num, 0) num
				FROM req
				JOIN target_systems ts ON 1=1
				JOIN (
				  SELECT label_major, grade_system_id, grade_color_id, MIN(weight) sort 
				  FROM grade GROUP BY label_major, grade_system_id, grade_color_id
				) g ON g.grade_system_id = ts.grade_system_id
				JOIN grade_color clr ON g.grade_color_id = clr.id
				LEFT JOIN x ON g.label_major = x.g_base
				ORDER BY g.sort, x.sorting, x.sector, x.t
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, optionalSectorId);
			ps.setInt(3, optionalAreaId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String label = rst.getString("grade");
					String color = rst.getString("color");
					GradeDistribution dist = res.computeIfAbsent(label, k -> new GradeDistribution(k, color));
					int sectorId = rst.getInt("sector_id");
					if (!rst.wasNull()) {
						dist.addSector(sectorId, rst.getString("sector"), rst.getString("t"), rst.getInt("num"));
					}
				}
			}
		}
		return res.values();
	}
	
	public Collection<RestrictionsRegion> getRestrictions(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, RestrictionsRegion> regionsLookup = new LinkedHashMap<>();
		Map<Integer, RestrictionsArea> areasLookup = new LinkedHashMap<>();
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				  SELECT ? user_id, ? region_id
				)
				SELECT r.id region_id, r.name region_name,
				       a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, a.access_closed area_access_closed, a.access_info area_access_info,
				       s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, s.access_closed sector_access_closed, s.access_info sector_access_info
				FROM req
				JOIN region r ON true
				JOIN area a ON r.id=a.region_id
				JOIN region_type rt ON r.id=rt.region_id
				LEFT JOIN sector s ON a.id=s.area_id AND (s.access_info IS NOT NULL OR s.access_closed IS NOT NULL)
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (a.region_id=req.region_id OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				  AND (s.id IS NULL OR is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1)
				  AND (a.access_info IS NOT NULL OR a.access_closed IS NOT NULL OR s.access_info IS NOT NULL OR s.access_closed IS NOT NULL)
				GROUP BY r.id, r.name, a.id, a.locked_admin, a.locked_superadmin, a.name, a.access_closed, a.access_info, a.last_updated, s.id, s.locked_admin, s.locked_superadmin, s.name, s.access_closed, s.access_info, s.last_updated
				ORDER BY r.name, a.name, s.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Region
					int regionId = rst.getInt("region_id");
					RestrictionsRegion r = regionsLookup.get(regionId);
					if (r == null) {
						String name = rst.getString("region_name");
						r = new RestrictionsRegion(regionId, name, new ArrayList<>());
						regionsLookup.put(regionId, r);
					}
					// Area
					int areaId = rst.getInt("area_id");
					RestrictionsArea a = areasLookup.get(areaId);
					if (a == null) {
						String name = rst.getString("area_name");
						boolean lockedAdmin = rst.getBoolean("area_locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						String accessClosed = rst.getString("area_access_closed");
						String accessInfo = rst.getString("area_access_info");
						a = new RestrictionsArea(areaId, lockedAdmin, lockedSuperadmin, name, accessClosed, accessInfo, new ArrayList<>());
						r.areas().add(a);
						areasLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					String name = rst.getString("sector_name");
					boolean lockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					String accessClosed = rst.getString("sector_access_closed");
					String accessInfo = rst.getString("sector_access_info");
					var s = new RestrictionsSector(sectorId, lockedAdmin, lockedSuperadmin, name, accessClosed, accessInfo);
					a.sectors().add(s);
				}
			}
		}
		logger.debug("getRestrictions() - regionsLookup.size()={}, duration={}", regionsLookup.size(), stopwatch);
		return regionsLookup.values();
	}
	
	public List<Search> getSearch(Connection c, Optional<Integer> authUserId, Setup setup, String search) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		
		String cleanSearch = search.replaceAll("[^\\p{L}0-9]", "");
		String wildCardSearch = "%" + cleanSearch + "%";
		
		List<Search> areas = new ArrayList<>();
		List<Search> externalAreas = new ArrayList<>();
		List<Search> sectors = new ArrayList<>();
		List<Search> problems = new ArrayList<>();
		List<Search> users = new ArrayList<>();
		Set<Integer> areaIdsVisible = new HashSet<>();
		String sqlStr = """
				WITH req AS (
				  SELECT ? auth_user_id, ? region_id, ? search_term
				),
				ranked_area_media AS (
				   SELECT ma.area_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY ma.area_id ORDER BY m.is_movie, ma.sorting, m.id DESC) rn
				   FROM media_area ma
				   JOIN media m ON ma.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				),
				ranked_sector_media AS (
				   SELECT ms.sector_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY ms.sector_id ORDER BY m.is_movie, ms.sorting, m.id DESC) rn
				   FROM media_sector ms
				   JOIN media m ON ms.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				),
				ranked_problem_media AS (
				   SELECT mp.problem_id, m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				          ROW_NUMBER() OVER (PARTITION BY mp.problem_id ORDER BY m.is_movie, mp.sorting, m.id DESC) rn
				   FROM media_problem mp
				   JOIN media m ON mp.media_id=m.id AND m.deleted_user_id IS NULL
				   LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				   WHERE mp.trivia=0
				)
				(SELECT 'AREA' result_type, a.id, a.name title, NULL sub_title, r.name breadcrumb, 
				        ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex,
				        a.hits, NULL external_url,
				        a.locked_admin, a.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE REPLACE(REPLACE(REPLACE(a.name, '’', ''), '\\'', ''), ' ', '') LIKE req.search_term
				   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				 GROUP BY a.id, r.name, ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex, a.hits, a.locked_admin, a.locked_superadmin
				 ORDER BY a.hits DESC, a.name LIMIT 8)

				UNION ALL

				(SELECT 'EXTERNAL' result_type, a_ext.id, a_ext.name title, NULL sub_title, r_ext.name breadcrumb, 
				        ma_ext.media_id, ma_ext.media_version_stamp, ma_ext.media_focus_x, ma_ext.media_focus_y, ma_ext.media_primary_color_hex,
				        a_ext.hits, CONCAT(r_ext.url, '/area/', a_ext.id) external_url,
				        a_ext.locked_admin, a_ext.locked_superadmin
				 FROM req
				 JOIN region_type rt ON rt.region_id=req.region_id
				 JOIN region_type rt_ext ON rt_ext.type_id=rt_ext.type_id
				 JOIN region r_ext ON rt_ext.region_id=r_ext.id AND r_ext.id != req.region_id
				 JOIN area a_ext ON r_ext.id=a_ext.region_id AND a_ext.locked_admin=0 AND a_ext.locked_superadmin=0
				             LEFT JOIN ranked_area_media ma_ext ON a_ext.id=ma_ext.area_id AND ma_ext.rn=1
				 LEFT JOIN user_region ur_check ON r_ext.id=ur_check.region_id AND ur_check.user_id=req.auth_user_id
				 WHERE ur_check.region_id IS NULL
				   AND REPLACE(REPLACE(REPLACE(a_ext.name, '’', ''), '\\'', ''), ' ', '') LIKE req.search_term
				 GROUP BY a_ext.id, r_ext.name, r_ext.url, ma_ext.media_id, ma_ext.media_version_stamp, ma_ext.media_focus_x, ma_ext.media_focus_y, ma_ext.media_primary_color_hex, a_ext.hits, a_ext.locked_admin, a_ext.locked_superadmin
				 ORDER BY a_ext.hits DESC, a_ext.name LIMIT 3)

				UNION ALL

				(SELECT 'SECTOR' result_type, s.id, s.name title, NULL sub_title, a.name breadcrumb,
				        COALESCE(ms.media_id,ma.media_id) media_id, COALESCE(ms.media_version_stamp,ma.media_version_stamp) media_version_stamp, COALESCE(ms.media_focus_x,ma.media_focus_x) media_focus_x, COALESCE(ms.media_focus_y,ma.media_focus_y) media_focus_y, COALESCE(ms.media_primary_color_hex,ma.media_primary_color_hex) media_primary_color_hex,
				        s.hits, NULL external_url,
				        s.locked_admin, s.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 JOIN sector s ON a.id=s.area_id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_sector_media ms ON s.id=ms.sector_id AND ms.rn=1
				             LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE REPLACE(REPLACE(REPLACE(s.name, '’', ''), '\\'', ''), ' ', '') LIKE req.search_term
				   AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				 GROUP BY s.id, a.name, s.hits, s.locked_admin, s.locked_superadmin,
				                      ms.media_id, ms.media_version_stamp, ms.media_focus_x, ms.media_focus_y, ms.media_primary_color_hex,
				                      ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex
				 ORDER BY s.hits DESC, a.name, s.name LIMIT 8)

				UNION ALL

				(SELECT 'PROBLEM' result_type, p.id, p.name title, g.grade sub_title, CONCAT(a.name, ' / ', s.name, CASE WHEN p.rock IS NOT NULL THEN CONCAT(' (', p.rock,')') ELSE '' END) breadcrumb,
				        COALESCE(mp.media_id,ms.media_id,ma.media_id) media_id, COALESCE(mp.media_version_stamp,ms.media_version_stamp,ma.media_version_stamp) media_version_stamp, COALESCE(mp.media_focus_x,ms.media_focus_x,ma.media_focus_x) media_focus_x, COALESCE(mp.media_focus_y,ms.media_focus_y,ma.media_focus_y) media_focus_y, COALESCE(mp.media_primary_color_hex,ms.media_primary_color_hex,ma.media_primary_color_hex) media_primary_color_hex,
				        p.hits, NULL external_url,
				        p.locked_admin, p.locked_superadmin
				 FROM req
				 JOIN region r ON r.id = req.region_id OR r.id IN (SELECT rt.region_id FROM region_type rt WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id))
				 JOIN area a ON r.id=a.region_id
				 JOIN sector s ON a.id=s.area_id
				 JOIN problem p ON s.id=p.sector_id
				 JOIN grade g ON p.consensus_grade_id = g.id
				 LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				 LEFT JOIN ranked_problem_media mp ON p.id=mp.problem_id AND mp.rn=1
				             LEFT JOIN ranked_sector_media ms ON s.id=ms.sector_id AND ms.rn=1
				             LEFT JOIN ranked_area_media ma ON a.id=ma.area_id AND ma.rn=1
				 WHERE (REPLACE(REPLACE(REPLACE(p.name, '’', ''), '\\'', ''), ' ', '') LIKE req.search_term OR REPLACE(REPLACE(REPLACE(p.rock, '’', ''), '\\'', ''), ' ', '') LIKE req.search_term)
				   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				 GROUP BY p.id, a.name, s.name, g.grade, p.hits, p.locked_admin, p.locked_superadmin,
				                      mp.media_id, mp.media_version_stamp, mp.media_focus_x, mp.media_focus_y, mp.media_primary_color_hex,
				                      ms.media_id, ms.media_version_stamp, ms.media_focus_x, ms.media_focus_y, ms.media_primary_color_hex,
				                      ma.media_id, ma.media_version_stamp, ma.media_focus_x, ma.media_focus_y, ma.media_primary_color_hex
				 ORDER BY p.hits DESC, p.name LIMIT 8)

				UNION ALL

				(SELECT 'USER' result_type, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) title, NULL sub_title, NULL breadcrumb,
				        m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_y, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				        0 hits, NULL external_url,
				        0 locked_admin, 0 locked_superadmin
				 FROM req
				 JOIN user u ON REPLACE(REPLACE(REPLACE(CONCAT(u.firstname, COALESCE(u.lastname,'')), '’', ''), '\\'', ''), ' ', '') LIKE req.search_term
				 LEFT JOIN media m ON u.media_id=m.id
				 LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				 ORDER BY TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) LIMIT 8)
				 		""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			ps.setString(3, wildCardSearch);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String type = rst.getString("result_type");
					int id = rst.getInt("id");
					String title = rst.getString("title");
					String subTitle = rst.getString("sub_title");
					String breadcrumb = rst.getString("breadcrumb");
					long hits = rst.getLong("hits");
					String pageViews = HitsFormatter.formatHits(hits);
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					switch (type) {
					case "AREA" -> {
						areaIdsVisible.add(id);
						areas.add(new Search(title, subTitle, breadcrumb, "/area/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "EXTERNAL" -> {
						externalAreas.add(new Search(title, subTitle, breadcrumb, null, rst.getString("external_url"), null, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "SECTOR" -> {
						sectors.add(new Search(title, subTitle, breadcrumb, "/sector/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "PROBLEM" -> {
						problems.add(new Search(title, subTitle, breadcrumb, "/problem/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					case "USER" -> {
						users.add(new Search(title, null, null, "/user/" + id, null, mediaIdentity, hits, pageViews, lockedAdmin, lockedSuperadmin));
					}
					default -> throw new IllegalArgumentException("Invalid type: " + type);
					}
				}
			}
		}
		while (areas.size() + sectors.size() + problems.size() + users.size() > 10) {
			if (problems.size() > 5) {
				problems.removeLast();
			}
			else if (areas.size() > 2) {
				areas.removeLast();
			}
			else if (sectors.size() > 2) {
				sectors.removeLast();
			}
			else if (users.size() > 1) {
				users.removeLast();
			}
		}
		List<Search> filteredExternal = externalAreas.stream()
				.filter(ea -> {
					try {
						String url = ea.externalUrl();
						int extId = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1));
						return !areaIdsVisible.contains(extId);
					} catch (Exception _) {
						return true;
					}
				}).toList();
		List<Search> res = new ArrayList<>();
		res.addAll(areas);
		res.addAll(sectors);
		res.addAll(problems);
		res.sort((r1, r2) -> Long.compare(r2.hits(), r1.hits()));
		res.addAll(users);
		res.addAll(filteredExternal);
		logger.debug("getSearch(search={}) - res.size()={}, duration={}", search, res.size(), stopwatch);
		return res;
	}

	public String getSitemapTxt(Connection c, Setup setup) throws SQLException {
		List<String> urls = new ArrayList<>();
		// Fixed urls
		urls.add(setup.url());
		urls.add(setup.url() + "/about");
		urls.add(setup.url() + "/activity");
		urls.add(setup.url() + "/areas");
		urls.add(setup.url() + "/donate");
		urls.add(setup.url() + "/dangerous");
		urls.add(setup.url() + "/gpl-3.0.txt");
		urls.add(setup.url() + "/graph");
		urls.add(setup.url() + "/privacy-policy");
		urls.add(setup.url() + "/problems");
		urls.add(setup.url() + "/restrictions");
		urls.add(setup.url() + "/regions/bouldering");
		urls.add(setup.url() + "/regions/climbing");
		urls.add(setup.url() + "/regions/ice");
		urls.add(setup.url() + "/webcams");
		// Users
		try (PreparedStatement ps = c.prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=t.problem_id GROUP BY t.user_id")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					urls.add(setup.url() + "/user/" + userId);
				}
			}
		}
		// Areas, sectors, problems
		try (PreparedStatement ps = c.prepareStatement("SELECT CONCAT('/area/', a.id) suffix FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					urls.add(setup.url() + rst.getString("suffix"));
				}
			}
		}
		return Joiner.on("\r\n").join(urls);
	}

	public Toc getToc(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, TocRegion> regionLookup = new LinkedHashMap<>();
		Map<Integer, TocArea> areaLookup = new HashMap<>();
		Map<Integer, TocSector> sectorLookup = new HashMap<>();
		int numProblems = 0;
		String sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				),
				filtered_problems AS (
				    SELECT p.id AS pid, p.sector_id, p.consensus_grade_id, p.type_id, p.coordinates_id,
				           p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, 
				           p.length_meter, p.starting_altitude, p.fa_date,
				           r.id AS region_id, r.name AS region_name, r.url AS region_url,
				           a.id AS area_id, a.name AS area_name, a.coordinates_id AS area_coordinates_id,
				           a.locked_admin AS area_locked_admin, a.locked_superadmin AS area_locked_superadmin,
				           a.sun_from_hour AS area_sun_from_hour, a.sun_to_hour AS area_sun_to_hour,
				           s.id AS sector_id_val, s.name AS sector_name, s.sorting AS sector_sorting,
				           s.sun_from_hour AS sector_sun_from_hour, s.sun_to_hour AS sector_sun_to_hour,
				           s.parking_coordinates_id AS sector_parking_coordinates_id,
				           s.compass_direction_id_calculated AS sector_compass_direction_id_calculated,
				           s.compass_direction_id_manual AS sector_compass_direction_id_manual,
				           s.locked_admin AS sector_locked_admin, s.locked_superadmin AS sector_locked_superadmin
				    FROM req
				    JOIN area a ON 1=1
				    JOIN region r ON a.region_id = r.id
				    JOIN region_type rt ON r.id = rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = req.region_id)
				    JOIN sector s ON a.id = s.area_id
				    JOIN problem p ON s.id = p.sector_id AND rt.type_id = p.type_id
				    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				    WHERE (a.region_id = req.region_id OR ur.user_id IS NOT NULL)
				      AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash) = 1
				      AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash) = 1
				      AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash) = 1
				),
				tick_agg AS (
				    SELECT t.problem_id,
				           COUNT(t.id) AS num_ticks,
				           ROUND(ROUND(AVG(NULLIF(t.stars, -1)) * 2) / 2, 1) AS stars,
				           MAX(CASE WHEN t.user_id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_ticked
				    FROM tick t
				    WHERE t.problem_id IN (SELECT pid FROM filtered_problems)
				    GROUP BY t.problem_id
				),
				fa_agg AS (
				    SELECT f.problem_id,
				           GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS ffa_names,
				           MAX(CASE WHEN u.id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_is_fa
				    FROM fa f
				    JOIN user u ON f.user_id = u.id
				    WHERE f.problem_id IN (SELECT pid FROM filtered_problems)
				    GROUP BY f.problem_id
				),
				fa_aid_agg AS (
				    SELECT a.problem_id,
				           GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_aid_names,
				           YEAR(a.aid_date) AS fa_aid_date
				    FROM fa_aid a
				    JOIN fa_aid_user au ON a.problem_id = au.problem_id
				    JOIN user u ON au.user_id = u.id
				    WHERE a.problem_id IN (SELECT pid FROM filtered_problems)
				    GROUP BY a.problem_id, a.aid_date
				)
				SELECT 
				    p.region_id, p.region_name, p.area_id, CONCAT(p.region_url, '/area/', p.area_id) AS area_url, p.area_name, 
				    ac.id AS area_coordinates_id, ac.latitude AS area_latitude, ac.longitude AS area_longitude, ac.elevation AS area_elevation, ac.elevation_source AS area_elevation_source, 
				    p.area_locked_admin, p.area_locked_superadmin, p.area_sun_from_hour, p.area_sun_to_hour,
				    p.sector_id_val AS sector_id, CONCAT(p.region_url, '/sector/', p.sector_id_val) AS sector_url, p.sector_name, p.sector_sorting, p.sector_sun_from_hour, p.sector_sun_to_hour, 
				    sc.id AS sector_parking_coordinates_id, sc.latitude AS sector_parking_latitude, sc.longitude AS sector_parking_longitude, sc.elevation AS sector_parking_elevation, sc.elevation_source AS sector_parking_elevation_source, 
				    p.sector_compass_direction_id_calculated, p.sector_compass_direction_id_manual, 
				    p.sector_locked_admin, p.sector_locked_superadmin,
				    p.pid AS id, CONCAT(p.region_url, '/problem/', p.pid) AS url, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, p.length_meter, REGEXP_SUBSTR(p.starting_altitude, '[0-9]+') AS starting_altitude,
				    c.id AS coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				    gf.grade,
				    fa_aid.fa_aid_names AS fa_user,
				    COALESCE(fa_aid.fa_aid_date, 0) AS fa_year,
				    fa.ffa_names AS ffa_user,
				    COALESCE(YEAR(p.fa_date), 0) AS ffa_year,
				    COALESCE(t.num_ticks, 0) AS num_ticks, 
				    COALESCE(t.stars, 0) AS stars, 
				    GREATEST(COALESCE(t.user_ticked, 0), COALESCE(fa.user_is_fa, 0)) AS ticked,
				    CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END AS todo,
				    ty.id AS type_id, ty.type, ty.subtype, 
				    (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p.pid) AS num_pitches 
				FROM filtered_problems p
				JOIN type ty ON p.type_id = ty.id
				LEFT JOIN grade gf ON p.consensus_grade_id = gf.id
				LEFT JOIN coordinates ac ON p.area_coordinates_id = ac.id
				LEFT JOIN coordinates sc ON p.sector_parking_coordinates_id = sc.id
				LEFT JOIN coordinates c ON p.coordinates_id = c.id
				LEFT JOIN tick_agg t ON p.pid = t.problem_id
				LEFT JOIN fa_agg fa ON p.pid = fa.problem_id
				LEFT JOIN fa_aid_agg fa_aid ON p.pid = fa_aid.problem_id
				LEFT JOIN todo ON p.pid = todo.problem_id AND todo.user_id = (SELECT auth_user_id FROM req)
				ORDER BY p.region_name, p.area_name, p.sector_sorting, p.sector_name, p.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Region
					int regionId = rst.getInt("region_id");
					TocRegion r = regionLookup.get(regionId);
					if (r == null) {
						String regionName = rst.getString("region_name");
						r = new TocRegion(regionId, regionName, new ArrayList<>());
						regionLookup.put(regionId, r);
					}
					// Area
					int areaId = rst.getInt("area_id");
					TocArea a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						int areaidCoordinates = rst.getInt("area_coordinates_id");
						Coordinates areaCoordinates = areaidCoordinates == 0? null : new Coordinates(areaidCoordinates, rst.getDouble("area_latitude"), rst.getDouble("area_longitude"), rst.getDouble("area_elevation"), rst.getString("area_elevation_source"));
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new TocArea(areaId, areaUrl, areaName, areaCoordinates, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour, new ArrayList<>());
						r.areas().add(a);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					TocSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						int sectorSorting = rst.getInt("sector_sorting");
						int sectorSunFromHour = rst.getInt("sector_sun_from_hour");
						int sectorSunToHour = rst.getInt("sector_sun_to_hour");
						int sectorParkingidCoordinates = rst.getInt("sector_parking_coordinates_id");
						Coordinates sectorParking = sectorParkingidCoordinates == 0? null : new Coordinates(sectorParkingidCoordinates, rst.getDouble("sector_parking_latitude"), rst.getDouble("sector_parking_longitude"), rst.getDouble("sector_parking_elevation"), rst.getString("sector_parking_elevation_source"));
						CompassDirection sectorWallDirectionCalculated = dao.getGeoRepo().getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = dao.getGeoRepo().getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new TocSector(sectorId, sectorUrl, sectorName, sectorSorting, sectorParking, new ArrayList<>(), sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin, sectorSunFromHour, sectorSunToHour, new ArrayList<>());
						a.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					String name = rst.getString("name");
					String description = rst.getString("description");
					int lengthMeter = rst.getInt("length_meter");
					int startingAltitude = rst.getInt("starting_altitude");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					String grade = rst.getString("grade");
					String faUser = rst.getString("fa_user");
					int faYear = rst.getInt("fa_year");
					String ffaUser = rst.getString("ffa_user");
					int ffaYear = rst.getInt("ffa_year");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean todo = rst.getBoolean("todo");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int numPitches = rst.getInt("num_pitches");
					TocProblem p = new TocProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, description, lengthMeter, startingAltitude, coordinates, grade, faUser, faYear, ffaUser, ffaYear, numTicks, stars, ticked, todo, t, numPitches);
					s.problems().add(p);
					numProblems++;
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = dao.getSectorRepo().getSectorOutlines(c, sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				sectorLookup.get(idSector).outline().addAll(idSectorOutline.get(idSector));
			}
		}
		Toc res = new Toc(regionLookup.size(), areaLookup.size(), sectorLookup.size(), numProblems, Lists.newArrayList(regionLookup.values()));
		res.regions().forEach(r -> {
			r.areas().sort(Comparator.comparing(TocArea::name)); // Sorting (ae, oe, aa is sorted wrong by MySQL)
			r.areas().forEach(a -> a.orderSectors());
		});
		logger.debug("getToc(authUserId={}, setup={}) - duration={}", authUserId, setup, stopwatch);
		return res;
	}

	public List<TocPitch> getTocPitches(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		List<TocPitch> res = new ArrayList<>();
		String sqlStr = """
				SELECT r.name region_name, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name problem_name, ps.nr pitch, g.grade, ps.description
				FROM area a INNER JOIN region r ON a.region_id=r.id
				JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)
				JOIN problem_section ps ON p.id=ps.problem_id
				JOIN grade g ON ps.grade_id=g.id
				JOIN type ty ON p.type_id=ty.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE (a.region_id=? OR ur.user_id IS NOT NULL)
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY r.name, r.url, p.id, a.name, s.name, p.name, ps.nr, g.grade, ps.description
				ORDER BY r.name, a.name, s.sorting, s.name, p.nr, ps.nr
				""";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String regionName = rst.getString("region_name");
					String url = rst.getString("url");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					String problemName = rst.getString("problem_name");
					int pitch = rst.getInt("pitch");
					String grade = rst.getString("grade");
					String description = rst.getString("description");
					res.add(new TocPitch(regionName, url, areaName, sectorName, problemName, pitch, grade, description));
				}
			}
		}
		return res;
	}

	public Top getTop(Connection c, Optional<Integer> authUserId, int areaId, int sectorId) throws SQLException {
		String columnCondition = (sectorId > 0) ? "s.id" : "a.id";
		int filterId = (sectorId > 0) ? sectorId : areaId;
		String sqlStr = """
				WITH total_problems AS (
				    SELECT COUNT(DISTINCT p.id) AS sum
				    FROM area a
				    JOIN sector s ON a.id = s.area_id
				    JOIN problem p ON s.id = p.sector_id AND p.broken IS NULL
				    JOIN grade g ON p.grade_id = g.id
				    LEFT JOIN fa f ON p.id = f.problem_id
				    LEFT JOIN tick t ON p.id = t.problem_id
				    LEFT JOIN fa_aid_user aid ON p.id = aid.problem_id
				    WHERE %1$s = ? 
				      AND (g.grade != 'n/a' OR aid.user_id IS NOT NULL OR f.user_id IS NOT NULL OR t.id IS NOT NULL)
				),
				user_completions AS (
				    SELECT f.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN fa f ON p.id = f.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND f.user_id IS NOT NULL

				    UNION

				    SELECT t.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN tick t ON p.id = t.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND t.user_id IS NOT NULL

				    UNION

				    SELECT aid.user_id, p.id AS problem_id
				    FROM problem p
				    JOIN sector s ON p.sector_id = s.id
				    JOIN area a ON s.area_id = a.id
				    JOIN fa_aid_user aid ON p.id = aid.problem_id
				    WHERE %1$s = ? AND p.broken IS NULL AND aid.user_id IS NOT NULL
				)
				SELECT 
				    u.id AS user_id,
				    TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) AS name,
				    m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				    ROUND((COUNT(DISTINCT uc.problem_id) / NULLIF(tp.sum, 0)) * 100, 2) AS percentage
				FROM user_completions uc
				JOIN user u ON uc.user_id = u.id
				LEFT JOIN media m ON u.media_id=m.id
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				CROSS JOIN total_problems tp
				GROUP BY u.id, u.firstname, u.lastname, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, tp.sum
				ORDER BY percentage DESC, name ASC
				""".formatted(columnCondition);
		Map<Double, TopRank> topByPercentage = new LinkedHashMap<>();
		Set<Integer> uniqueUserIds = new HashSet<>();
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, filterId);
			ps.setInt(2, filterId);
			ps.setInt(3, filterId);
			ps.setInt(4, filterId);
			try (ResultSet rst = ps.executeQuery()) {
				double prevPercentage = -1.0;
				int rank = 0;
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					uniqueUserIds.add(userId);
					String name = rst.getString("name");
					int mediaId = rst.getInt("media_id");
					MediaIdentity mediaIdentity = null;
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						String mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					double percentage = rst.getDouble("percentage");
					if (prevPercentage != percentage) {
						rank++;
					}
					prevPercentage = percentage;
					boolean mine = authUserId.orElse(0) == userId;
					var top = topByPercentage.get(percentage);
					if (top == null) {
						top = new TopRank(rank, percentage, new ArrayList<>());
						topByPercentage.put(percentage, top);
					}
					top.users().add(new TopUser(userId, name, mediaIdentity, mine));
				}
			}
		}
		var rows = topByPercentage.values();
		return new Top(rows, uniqueUserIds.size());
	}
}
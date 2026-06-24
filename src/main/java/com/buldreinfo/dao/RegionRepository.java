package com.buldreinfo.dao;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.Grade;
import com.buldreinfo.model.LatLng;
import com.buldreinfo.model.Region;
import com.buldreinfo.model.Type;

@Repository
public class RegionRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();

	public RegionRepository(ClimbingTransactionManager txManager) {
		super(txManager);
	}

	public void ensureAdminWriteRegion(Setup setup, Optional<Integer> authUserId) throws SQLException {
		if (authUserId.isEmpty()) {
			throw new IllegalArgumentException("Not logged in");
		}
		var c = txManager.getConnection();
		var ok = false;
		try (var ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElseThrow());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		if (!ok) {
			throw new IllegalArgumentException("Insufficient permissions");
		}
	}

	public void ensureSuperadminWriteRegion(Setup setup, Optional<Integer> authUserId) throws SQLException {
		if (authUserId.isEmpty()) {
			throw new IllegalArgumentException("Not logged in");
		}
		var c = txManager.getConnection();
		var ok = false;
		try (var ps = c.prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElseThrow());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("superadmin_write");
				}
			}
		}
		if (!ok) {
			throw new IllegalArgumentException("Insufficient permissions");
		}
	}

	public List<Integer> getFaYears(int regionId) throws SQLException {
		var res = new ArrayList<Integer>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT year(p.fa_date) fa_year
				FROM area a, sector s, problem p
				WHERE a.region_id=? AND a.id=s.area_id AND s.id=p.sector_id
				  AND p.fa_date IS NOT NULL GROUP BY year(p.fa_date) ORDER BY year(p.fa_date) DESC
				""")) {
			ps.setInt(1, regionId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int faYear = rst.getInt("fa_year");
					res.add(faYear);
				}
			}
		}
		return res;
	}

	public List<Region> getRegions(int currIdRegion) throws SQLException {
		Map<Integer, Region> regionLookup = new LinkedHashMap<>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("SELECT r.id region_id, t.group, r.name, r.url FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name")) {
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					var group = rst.getString("group");
					var name = rst.getString("name");
					var url = rst.getString("url");
					boolean active = idRegion == currIdRegion;
					regionLookup.put(idRegion, new Region(group, name, url, active, new ArrayList<>()));
				}
			}
		}
		if (!regionLookup.isEmpty()) {
			var idRegionOutline = getRegionOutlines(regionLookup.keySet());
			for (int idRegion : idRegionOutline.keySet()) {
				List<Coordinates> outline = new ArrayList<>(idRegionOutline.get(idRegion));
				regionLookup.get(idRegion).outline().addAll(outline);
			}
		}
		return new ArrayList<>(regionLookup.values());
	}

	@Cacheable(value = CacheConstants.REGION_CACHE_NAME, key = "'setups'")
	public List<Setup> getSetups() throws SQLException {
		var start = System.nanoTime();
		var res = new ArrayList<Setup>();
		var c = txManager.getConnection();
		var compassDirections = getCompassDirections();
		var converterCache = new HashMap<Integer, GradeConverter>();
		String sql = """
				SELECT DISTINCT r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				FROM region r
				JOIN region_type rt ON r.id = rt.region_id
				JOIN type t ON rt.type_id = t.id
				JOIN type_grade_system tgs ON t.id = tgs.type_id
				ORDER BY r.id
				""";
		try (var ps = c.prepareStatement(sql);
				var rst = ps.executeQuery()) {
			while (rst.next()) {
				int gradeSystemId = rst.getInt("grade_system_id");
				var gradeConverter = converterCache.computeIfAbsent(gradeSystemId, id -> {
					try {
						return new GradeConverter(getGrades(id));
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				});
				res.add(Setup.newBuilder(
						rst.getString("url").replace("https://", "").replace("http://", ""),
						rst.getString("group"))
						.withIdRegion(rst.getInt("id"))
						.withTitle(rst.getString("title"))
						.withDescription(rst.getString("description"))
						.withDefaultCenter(new LatLng(rst.getDouble("latitude"), rst.getDouble("longitude")))
						.withDefaultZoom(rst.getInt("default_zoom"))
						.withCompassDirections(compassDirections)
						.withGradeConverter(gradeConverter)
						.build());
			}
		}
		logger.debug("getSetups() - res.size()={}, duration={}", res.size(), Duration.ofNanos(System.nanoTime() - start));
		return res;
	}

	public List<Type> getTypes(int regionId) throws SQLException {
		var res = new ArrayList<Type>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")) {
			ps.setInt(1, regionId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					var type = rst.getString("type");
					var subtype = rst.getString("subtype");
					res.add(new Type(id, type, subtype));
				}
			}
		}
		return res;
	}

	private List<CompassDirection> getCompassDirections() throws SQLException {
		var c = txManager.getConnection();
		var res = new ArrayList<CompassDirection>();
		try (var ps = c.prepareStatement("SELECT id, direction FROM compass_direction ORDER BY id")) {
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					var direction = rst.getString("direction");
					res.add(new CompassDirection(id, direction));
				}
			}
		}
		return res;
	}

	private List<Grade> getGrades(int gradeSystemId) throws SQLException {
		var res = new ArrayList<Grade>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT g.id, g.grade, g.label_compact, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				WHERE g.grade_system_id=?
				ORDER BY g.weight
				""")) {
			ps.setInt(1, gradeSystemId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					var grade = rst.getString("grade");
					var labelCompact = rst.getString("label_compact");
					var color = rst.getString("color");
					res.add(new Grade(id, grade, labelCompact, color));
				}
			}
		}
		return res;
	}

	private Map<Integer, List<Coordinates>> getRegionOutlines(Collection<Integer> idRegions) throws SQLException {
		if (idRegions.isEmpty()) {
			throw new IllegalArgumentException("idProblems is empty");
		}
		var start = System.nanoTime();
		var c = txManager.getConnection();
		Map<Integer, List<Coordinates>> res = new HashMap<>();
		var in = ",?".repeat(idRegions.size()).substring(1);
		var sqlStr = "SELECT ro.region_id id_region, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM region_outline ro, coordinates c WHERE ro.region_id IN (" + in + ") AND ro.coordinates_id=c.id ORDER BY ro.sorting";
		try (var ps = c.prepareStatement(sqlStr)) {
			var parameterIndex = 1;
			for (int idSector : idRegions) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					var elevationSource = rst.getString("elevation_source");
					res.computeIfAbsent(idRegion, _ -> new ArrayList<>()).add(new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getRegionOutlines(idRegions.size()={}) - res.size()={}, duration={}", idRegions.size(), res.size(), Duration.ofNanos(System.nanoTime() - start));
		return res;
	}
}
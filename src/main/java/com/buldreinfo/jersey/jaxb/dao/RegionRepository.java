package com.buldreinfo.jersey.jaxb.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.Region;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import jakarta.inject.Inject;

public class RegionRepository extends BaseRepository {
	private record CacheEntry(List<Setup> data, long expiry) {}
	private static final Logger logger = LogManager.getLogger();
	private final AtomicReference<CacheEntry> cache = new AtomicReference<>();
	private final GeoRepository geoRepo;

	@Inject
	public RegionRepository(TransactionManager txManager, GeoRepository geoRepo) {
		super(txManager);
		this.geoRepo = geoRepo;
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
				List<Coordinates> outline = Lists.newArrayList(idRegionOutline.get(idRegion));
				regionLookup.get(idRegion).outline().addAll(outline);
			}
		}
		return Lists.newArrayList(regionLookup.values());
	}

	public List<Setup> getSetups() throws Exception {
		CacheEntry current = cache.get();
		if (current == null || System.currentTimeMillis() > current.expiry()) {
			synchronized (this) {
				current = cache.get();
				if (current == null || System.currentTimeMillis() > current.expiry()) {
					List<Setup> freshData = fetchSetupsFromDb();
					current = new CacheEntry(freshData, System.currentTimeMillis() + 60_000);
					cache.set(current);
				}
			}
		}
		return current.data();
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

	private List<Setup> fetchSetupsFromDb() throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<Setup>();
		var c = txManager.getConnection();
		var compassDirections = geoRepo.getCompassDirections();
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
		logger.debug("getSetups() - res.size()={}, duration={}", res.size(), stopwatch);
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

	private Multimap<Integer, Coordinates> getRegionOutlines(Collection<Integer> idRegions) throws SQLException {
		Preconditions.checkArgument(!idRegions.isEmpty(), "idProblems is empty");
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
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
					res.put(idRegion, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getRegionOutlines(idRegions.size()={}) - res.size()={}, duration={}", idRegions.size(), res.size(), stopwatch);
		return res;
	}

	protected void ensureAdminWriteRegion(Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
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
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	protected void ensureSuperadminWriteRegion(Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
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
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}
}
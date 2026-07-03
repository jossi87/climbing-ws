package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.Grade;
import com.buldreinfo.model.LatLng;
import com.buldreinfo.model.Region;
import com.buldreinfo.model.Type;

@Repository
public class RegionRepository {
	private final JdbcClient jdbcClient;

	public RegionRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public void ensureAdminWriteRegion(Setup setup, Optional<Integer> authUserId) {
		if (authUserId.isEmpty()) throw new UnauthorizedException("Not logged in");

		var authorized = jdbcClient.sql("SELECT admin_write, superadmin_write FROM user_region WHERE region_id=? AND user_id=?")
				.params(setup.idRegion(), authUserId.get())
				.query((rs, _) -> rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write"))
				.optional()
				.orElse(false);

		if (!authorized) throw new ForbiddenException("Insufficient permissions");
	}

	@Transactional(readOnly = true)
	public void ensureSuperadminWriteRegion(Setup setup, Optional<Integer> authUserId) {
		if (authUserId.isEmpty()) throw new UnauthorizedException("Not logged in");

		var authorized = jdbcClient.sql("SELECT superadmin_write FROM user_region WHERE region_id=? AND user_id=?")
				.params(setup.idRegion(), authUserId.get())
				.query((rs, _) -> rs.getBoolean("superadmin_write"))
				.optional()
				.orElse(false);

		if (!authorized) throw new ForbiddenException("Insufficient permissions");
	}

	@Transactional(readOnly = true)
	public List<Integer> getFaYears(int regionId) {
		return jdbcClient.sql("""
				SELECT year(p.fa_date) fa_year
				FROM area a
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				WHERE a.region_id=? AND p.fa_date IS NOT NULL
				GROUP BY year(p.fa_date) ORDER BY year(p.fa_date) DESC
				""")
				.param(regionId)
				.query(Integer.class)
				.list();
	}

	@Transactional(readOnly = true)
	public List<Region> getRegions(int currIdRegion) {
		List<Region> regions = jdbcClient.sql("""
				SELECT r.id region_id, t.group, r.name, r.url 
				FROM region r, region_type rt, type t 
				WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) 
				GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name
				""")
				.query((rs, _) -> new Region(
						rs.getInt("region_id"),
						rs.getString("group"), 
						rs.getString("name"), 
						rs.getString("url"), 
						rs.getInt("region_id") == currIdRegion, 
						new ArrayList<>()
						))
				.list();

		if (!regions.isEmpty()) {
			Map<Integer, List<Coordinates>> outlines = getRegionOutlines(regions.stream().map(Region::id).toList());
			regions.forEach(r -> {
				if (outlines.containsKey(r.id())) {
					r.outline().addAll(outlines.get(r.id()));
				}
			});
		}
		return regions;
	}

	@Cacheable(value = CacheConstants.REGION_CACHE_NAME, key = "'setups'")
	@Transactional(readOnly = true)
	public List<Setup> getSetups() {
		var compassDirections = getCompassDirections();
		var converterCache = new HashMap<Integer, GradeConverter>();

		var res = jdbcClient.sql("""
				SELECT DISTINCT r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				FROM region r
				JOIN region_type rt ON r.id = rt.region_id
				JOIN type t ON rt.type_id = t.id
				JOIN type_grade_system tgs ON t.id = tgs.type_id
				ORDER BY r.id
				""")
				.query((rs, _) -> {
					int gradeSystemId = rs.getInt("grade_system_id");
					var converter = converterCache.computeIfAbsent(gradeSystemId, this::getGradesConverter);

					return Setup.newBuilder(rs.getString("url").replace("https://", "").replace("http://", ""), rs.getString("group"))
							.withIdRegion(rs.getInt("id"))
							.withTitle(rs.getString("title"))
							.withDescription(rs.getString("description"))
							.withDefaultCenter(new LatLng(rs.getDouble("latitude"), rs.getDouble("longitude")))
							.withDefaultZoom(rs.getInt("default_zoom"))
							.withCompassDirections(compassDirections)
							.withGradeConverter(converter)
							.build();
				}).list();

		return res;
	}

	@Transactional(readOnly = true)
	public List<Type> getTypes(int regionId) {
		return jdbcClient.sql("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")
				.param(regionId)
				.query((rs, _) -> new Type(rs.getInt("id"), rs.getString("type"), rs.getString("subtype")))
				.list();
	}

	private List<CompassDirection> getCompassDirections() {
		return jdbcClient.sql("SELECT id, direction FROM compass_direction ORDER BY id")
				.query((rs, _) -> new CompassDirection(rs.getInt("id"), rs.getString("direction")))
				.list();
	}

	private List<Grade> getGrades(int gradeSystemId) {
		return jdbcClient.sql("""
				SELECT g.id, g.grade, g.label_compact, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				WHERE g.grade_system_id=? ORDER BY g.weight
				""")
				.param(gradeSystemId)
				.query((rs, _) -> new Grade(rs.getInt("id"), rs.getString("grade"), rs.getString("label_compact"), rs.getString("color")))
				.list();
	}

	private GradeConverter getGradesConverter(int gradeSystemId) {
		return new GradeConverter(getGrades(gradeSystemId));
	}

	private Map<Integer, List<Coordinates>> getRegionOutlines(Collection<Integer> idRegions) {
		Map<Integer, List<Coordinates>> res = new HashMap<>();
		jdbcClient.sql("SELECT ro.region_id, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM region_outline ro JOIN coordinates c ON ro.coordinates_id=c.id WHERE ro.region_id IN (:ids) ORDER BY ro.sorting")
		.param("ids", idRegions)
		.query(rs -> {
			int idRegion = rs.getInt("region_id");
			res.computeIfAbsent(idRegion, _ -> new ArrayList<>())
			.add(new Coordinates(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0));
		});
		return res;
	}
}
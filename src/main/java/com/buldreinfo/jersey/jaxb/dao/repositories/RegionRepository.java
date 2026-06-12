package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
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

public record RegionRepository(Dao dao) {
	private static Logger logger = LogManager.getLogger();
	
	public List<Integer> getFaYears(Connection c, int regionId) throws SQLException {
		List<Integer> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT year(p.fa_date) fa_year
				FROM area a, sector s, problem p
				WHERE a.region_id=? AND a.id=s.area_id AND s.id=p.sector_id
				  AND p.fa_date IS NOT NULL GROUP BY year(p.fa_date) ORDER BY year(p.fa_date) DESC
				""")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int faYear = rst.getInt("fa_year");
					res.add(faYear);
				}
			}
		}
		return res;
	}
	
	public List<Region> getRegions(Connection c, int currIdRegion) throws SQLException {
		Map<Integer, Region> regionLookup = new LinkedHashMap<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT r.id region_id, t.group, r.name, r.url FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String group = rst.getString("group");
					String name = rst.getString("name");
					String url = rst.getString("url");
					boolean active = idRegion == currIdRegion;
					regionLookup.put(idRegion, new Region(group, name, url, active, new ArrayList<>()));
				}
			}
		}
		if (!regionLookup.isEmpty()) {
			// Fill region outlines
			Multimap<Integer, Coordinates> idRegionOutline = getRegionOutlines(c, regionLookup.keySet());
			for (int idRegion : idRegionOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idRegionOutline.get(idRegion));
				regionLookup.get(idRegion).outline().addAll(outline);
			}
		}
		return Lists.newArrayList(regionLookup.values());
	}
	
	public List<Setup> getSetups(Connection c) throws SQLException {
		List<Setup> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT r.id id_region, r.title, r.description, REPLACE(REPLACE(r.url,'https://',''),'http://','') domain, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				FROM region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN type t ON rt.type_id=t.id
				JOIN type_grade_system tgs ON t.id=tgs.type_id
				GROUP BY r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group, tgs.grade_system_id
				ORDER BY r.id
				""")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					String title = rst.getString("title");
					String description = rst.getString("description");
					String domain = rst.getString("domain");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					int defaultZoom = rst.getInt("default_zoom");
					String group = rst.getString("group");
					int gradeSystemId = rst.getInt("grade_system_id");
					List<CompassDirection> compassDirections = dao.getGeoRepo().getCompassDirections(c);
					GradeConverter gradeConverter = new GradeConverter(getGrades(c, gradeSystemId));
					res.add(Setup.newBuilder(domain, group)
							.withIdRegion(idRegion)
							.withTitle(title)
							.withDescription(description)
							.withDefaultCenter(new LatLng(latitude, longitude))
							.withDefaultZoom(defaultZoom)
							.withCompassDirections(compassDirections)
							.withGradeConverter(gradeConverter)
							.build());
				}
			}
		}
		logger.debug("getSetups() - res.size()={}", res.size());
		return res;
	}
	
	public List<Type> getTypes(Connection c, int regionId) throws SQLException {
		List<Type> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String type = rst.getString("type");
					String subtype = rst.getString("subtype");
					res.add(new Type(id, type, subtype));
				}
			}
		}
		return res;
	}
	
	private List<Grade> getGrades(Connection c, int gradeSystemId) throws SQLException {
		List<Grade> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("""
				SELECT g.id, g.grade, g.label_compact, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				WHERE g.grade_system_id=?
				ORDER BY g.weight
				""")) {
			ps.setInt(1, gradeSystemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String grade = rst.getString("grade");
					String labelCompact = rst.getString("label_compact");
					String color = rst.getString("color");
					res.add(new Grade(id, grade, labelCompact, color));
				}
			}
		}
		return res;
	}
	
	private Multimap<Integer, Coordinates> getRegionOutlines(Connection c, Collection<Integer> idRegions) throws SQLException {
		Preconditions.checkArgument(!idRegions.isEmpty(), "idProblems is empty");
		Stopwatch stopwatch = Stopwatch.createStarted();
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idRegions.size()).substring(1);
		String sqlStr = "SELECT ro.region_id id_region, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM region_outline ro, coordinates c WHERE ro.region_id IN (" + in + ") AND ro.coordinates_id=c.id ORDER BY ro.sorting";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idRegions) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					res.put(idRegion, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getRegionOutlines(idRegions.size()={}) - res.size()={}, duration={}", idRegions.size(), res.size(), stopwatch);
		return res;
	}
	
	protected void ensureAdminWriteRegion(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}
	
	protected void ensureSuperadminWriteRegion(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		boolean ok = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}
}
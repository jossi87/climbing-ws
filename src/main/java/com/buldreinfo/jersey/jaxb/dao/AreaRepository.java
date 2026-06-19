package com.buldreinfo.jersey.jaxb.dao;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.HitsFormatter;
import com.buldreinfo.jersey.jaxb.helpers.JdbcUtils;
import com.buldreinfo.jersey.jaxb.helpers.SectorSort;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Area.AreaSector;
import com.buldreinfo.jersey.jaxb.model.Area.AreaSectorOrder;
import com.buldreinfo.jersey.jaxb.model.Area.GradeCount;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.Media.MediaArea;
import com.buldreinfo.jersey.jaxb.model.MediaIdentity;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Sector.SectorProblem;
import com.buldreinfo.jersey.jaxb.model.Trail;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class AreaRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();
	private final ExternalLinksRepository externalLinksRepo;
	private final GeoRepository geoRepo;
	private final Provider<HierarchyRepository> hierarchyRepo;
	private final Provider<MediaRepository> mediaRepo;
	private final Provider<RegionRepository> regionRepo;
	private final Provider<SectorRepository> sectorRepo;

	@Inject
	public AreaRepository(TransactionManager txManager,
			ExternalLinksRepository externalLinksRepo,
			GeoRepository geoRepo,
			Provider<HierarchyRepository> hierarchyRepo,
			Provider<MediaRepository> mediaRepo,
			Provider<RegionRepository> regionRepo,
			Provider<SectorRepository> sectorRepo) {
		super(txManager);
		this.externalLinksRepo = externalLinksRepo;
		this.geoRepo = geoRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
	}

	public Area getArea(Setup setup, Optional<Integer> authUserId, int reqId, boolean shouldUpdateHits) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		if (shouldUpdateHits) {
			try (var ps = c.prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Area a = null;
		try (var ps = c.prepareStatement("""
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
				""")) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, reqId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var regionName = rst.getString("region_name");
					var lockedAdmin = rst.getBoolean("locked_admin");
					var lockedSuperadmin = rst.getBoolean("locked_superadmin");
					var forDevelopers = rst.getBoolean("for_developers");
					var accessInfo = rst.getString("access_info");
					var accessClosed = rst.getString("access_closed");
					var noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					var name = rst.getString("name");
					var comment = rst.getString("description");
					int idCoordinates = rst.getInt("coordinates_id");
					var coordinates = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					var pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					var allMedia = mediaRepo.get().getMediaArea(authUserId, reqId, false);
					var partitioned = Optional.ofNullable(allMedia)
							.orElse(List.of())
							.stream()
							.collect(Collectors.partitioningBy(
									x -> x.areas().stream().anyMatch(MediaArea::trivia)
									));
					var triviaMedia = partitioned.get(true);
					var media = partitioned.get(false);
					var externalLinks = externalLinksRepo.getExternalLinks(reqId, 0, 0);

					a = new Area(null, regionName, reqId, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, -1, -1, new ArrayList<>(), new ArrayList<>(), media, triviaMedia, externalLinks, pageViews);
				}
			}
		}
		if (a == null) {
			try {
				var res = hierarchyRepo.get().getCanonicalUrl(setup, reqId, 0, 0);
				if (!Strings.isNullOrEmpty(res.redirectUrl())) {
					return new Area(res.redirectUrl(), null, -1, false, false, false, false, null, null, false, 0, 0, null, null, null, 0, 0, null, null, null, null, null, null);
				}
			} catch (NoSuchElementException _) {
			}
		}
		if (a == null) {
			throw new NoSuchElementException("Could not find area with id=" + reqId);
		}
		var sectorLookup = getAreaSectors(setup, authUserId, a.id(), a.name());
		if (!sectorLookup.isEmpty()) {
			try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
				var problemsFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.get().getSectorProblems(setup, authUserId, reqId, 0)), executor);
				var outlinesFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.get().getSectorOutlines(sectorLookup.keySet())), executor);
				var trailsFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.get().getSectorTrails(authUserId, sectorLookup.keySet())), executor);

				var sectorProblems = problemsFuture.join();
				for (var sectorId : sectorProblems.keySet()) {
					var sector = sectorLookup.get(sectorId);
					if (sector != null) {
						sector.problems().addAll(sectorProblems.get(sectorId));
					}
				}
				if (authUserId.isPresent()) {
					for (var entry : sectorLookup.entrySet()) {
						var sector = entry.getValue();
						long totalProblems = sector.problems().stream()
								.filter(p -> p.broken() == null)
								.filter(p -> !"n/a".equalsIgnoreCase(p.grade()) || p.faUser() != null || p.ffaUser() != null)
								.count();
						if (totalProblems != 0) {
							long completedProblems = sector.problems().stream()
									.filter(p -> p.broken() == null)
									.filter(p -> !"n/a".equalsIgnoreCase(p.grade()) || p.faUser() != null || p.ffaUser() != null)
									.filter(SectorProblem::ticked)
									.count();
							int percentage = (int) Math.round((double) completedProblems / totalProblems * 100);
							sectorLookup.put(entry.getKey(), sector.withProgress(percentage));
						}
					}
				}
				var idSectorOutline = outlinesFuture.join();
				for (var idSector : idSectorOutline.keySet()) {
					var sector = sectorLookup.get(idSector);
					if (sector != null) {
						sector.outline().addAll(idSectorOutline.get(idSector));
					}
				}
				var sectorTrailsMultimap = trailsFuture.join();
				for (var idSector : sectorTrailsMultimap.keySet()) {
					var sector = sectorLookup.get(idSector);
					if (sector != null) {
						Collection<Trail> trails = sectorTrailsMultimap.get(idSector);
						sectorLookup.put(idSector, sector.withTrails(trails));
					}
				}

				loadSimplifiedGradeCounts(reqId, sectorLookup);
				for (var sector : sectorLookup.values().stream()
						.sorted((o1, o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()))
						.toList()) {
					a.sectors().add(sector);
					a.sectorOrder().add(new AreaSectorOrder(sector.id(), sector.name(), sector.sorting()));
				}
			}
		}
		logger.debug("getArea(authUserId={}, reqId={}, shouldUpdateHits={}) - duration={}", authUserId, reqId, shouldUpdateHits, stopwatch);
		return a;
	}

	public Collection<Area> getAreaList(Optional<Integer> authUserId, int reqIdRegion) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var res = new ArrayList<Area>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("""
				SELECT r.name region_name,
				       a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,
				       COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits
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
				GROUP BY r.name, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description,
				         c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits
				ORDER BY r.name, a.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var regionName = rst.getString("region_name");
					int id = rst.getInt("id");
					var lockedAdmin = rst.getBoolean("locked_admin");
					var lockedSuperadmin = rst.getBoolean("locked_superadmin");
					var forDevelopers = rst.getBoolean("for_developers");
					var accessInfo = rst.getString("access_info");
					var accessClosed = rst.getString("access_closed");
					var noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					var name = rst.getString("name");
					var comment = rst.getString("description");
					if (comment != null) {
						int ix = comment.indexOf("<strong>Forhold:</strong>");
						if (ix != -1) {
							comment = comment.substring(ix + 25);
							ix = comment.indexOf("<strong>");
							comment = comment.substring(0, ix);
						}
					}
					int idCoordinates = rst.getInt("coordinates_id");
					var coordinates = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int numSectors = rst.getInt("num_sectors");
					int numProblems = rst.getInt("num_problems");
					var pageViews = HitsFormatter.formatHits(rst.getLong("hits"));
					res.add(new Area(null, regionName, id, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, numSectors, numProblems, null, null, null, null, null, pageViews));
				}
			}
		}
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public Redirect setArea(Setup s, Optional<Integer> authUserId, Area a) throws SQLException, InterruptedException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkArgument(s.idRegion() > 0, "Insufficient credentials");
		var c = txManager.getConnection();
		regionRepo.get().ensureAdminWriteRegion(s, authUserId);
		int idArea = -1;
		final var isLockedAdmin = a.lockedSuperadmin() ? false : a.lockedAdmin();
		var setPermissionRecursive = false;
		if (a.coordinates() != null) {
			if (a.coordinates().getLatitude() == 0 || a.coordinates().getLongitude() == 0) {
				a = a.withCoordinates(null);
			}
			else {
				geoRepo.ensureCoordinatesInDbWithElevationAndId(Lists.newArrayList(a.coordinates()));
			}
		}
		if (a.id() > 0) {
			ensureAdminWriteArea(authUserId, a.id());
			var currArea = getArea(s, authUserId, a.id(), false);
			setPermissionRecursive = currArea.lockedAdmin() != isLockedAdmin || currArea.lockedSuperadmin() != a.lockedSuperadmin();
			try (var ps = c.prepareStatement("UPDATE area SET name=?, description=?, coordinates_id=?, locked_admin=?, locked_superadmin=?, for_developers=?, access_info=?, access_closed=?, no_dogs_allowed=?, sun_from_hour=?, sun_to_hour=?, trash=CASE WHEN ? THEN NOW() ELSE NULL END, trash_by=? WHERE id=?")) {
				ps.setString(1, GlobalFunctions.stripString(a.name()));
				ps.setString(2, GlobalFunctions.stripString(a.comment()));
				JdbcUtils.setNullablePositiveInteger(ps, 3, a.coordinates() == null ? 0 : a.coordinates().getId());
				ps.setBoolean(4, isLockedAdmin);
				ps.setBoolean(5, a.lockedSuperadmin());
				ps.setBoolean(6, a.forDevelopers());
				ps.setString(7, GlobalFunctions.stripString(a.accessInfo()));
				ps.setString(8, GlobalFunctions.stripString(a.accessClosed()));
				ps.setBoolean(9, a.noDogsAllowed());
				JdbcUtils.setNullablePositiveDouble(ps, 10, a.sunFromHour());
				JdbcUtils.setNullablePositiveDouble(ps, 11, a.sunToHour());
				ps.setBoolean(12, a.trash());
				ps.setInt(13, a.trash() ? authUserId.orElseThrow() : 0);
				ps.setInt(14, a.id());
				ps.execute();
			}
			idArea = a.id();

			if (a.sectorOrder() != null) {
				for (var x : a.sectorOrder()) {
					try (var ps = c.prepareStatement("UPDATE sector SET sorting=? WHERE id=?")) {
						ps.setInt(1, x.sorting());
						ps.setInt(2, x.id());
						ps.execute();
					}
				}
			}

			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.locked_admin=?, a.locked_superadmin=?, s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE a.id=?";
				try (var ps = c.prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, a.lockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, a.lockedSuperadmin());
					ps.setBoolean(5, isLockedAdmin);
					ps.setBoolean(6, a.lockedSuperadmin());
					ps.setInt(7, a.id());
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?";
				try (var ps = c.prepareStatement(sqlStr)) {
					ps.setInt(1, idArea);
					ps.execute();
				}
			}
		} else {
			try (var ps = c.prepareStatement("INSERT INTO area (region_id, name, description, coordinates_id, locked_admin, locked_superadmin, for_developers, access_info, access_closed, no_dogs_allowed, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, s.idRegion());
				ps.setString(2, GlobalFunctions.stripString(a.name()));
				ps.setString(3, GlobalFunctions.stripString(a.comment()));
				JdbcUtils.setNullablePositiveInteger(ps, 4, a.coordinates() == null ? 0 : a.coordinates().getId());
				ps.setBoolean(5, isLockedAdmin);
				ps.setBoolean(6, a.lockedSuperadmin());
				ps.setBoolean(7, a.forDevelopers());
				ps.setString(8, GlobalFunctions.stripString(a.accessInfo()));
				ps.setString(9, GlobalFunctions.stripString(a.accessClosed()));
				ps.setBoolean(10, a.noDogsAllowed());
				ps.executeUpdate();
				try (var rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idArea = rst.getInt(1);
					}
				}
			}
		}
		if (idArea == -1) {
			throw new SQLException("idArea == -1");
		}
		externalLinksRepo.upsertExternalLinks(a.externalLinks(), idArea, 0, 0);
		if (a.trash()) {
			return Redirect.fromRoot();
		}
		return Redirect.fromIdArea(idArea);
	}

	private Map<Integer, AreaSector> getAreaSectors(Setup setup, Optional<Integer> authUserId, int areaId, String areaName) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var sectorLookup = new HashMap<Integer, AreaSector>();
		var c = txManager.getConnection();
		try (var ps = c.prepareStatement("""
				WITH req AS (
				  SELECT ? auth_user_id, ? area_id
				),
				ranked_media AS (
				  SELECT s.id sector_id,
				         m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y, mma.primary_color_hex media_primary_color_hex,
				         ROW_NUMBER() OVER (PARTITION BY p.sector_id ORDER BY m.is_360, m.is_movie, m.id DESC) rn
				  FROM req
				  JOIN area a ON req.area_id=a.id
				  JOIN sector s ON a.id=s.area_id
				  JOIN problem p ON s.id=p.sector_id
				  JOIN media_problem mp ON p.id=mp.problem_id AND mp.trivia=0
				  JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL
				  LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				  LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				)
				SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, s.sun_from_hour, s.sun_to_hour,
				       c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual,
				       rm.media_id, rm.media_version_stamp, rm.media_focus_x, rm.media_focus_y, rm.media_primary_color_hex
				FROM req
				JOIN area a ON a.id=req.area_id
				JOIN sector s ON a.id=s.area_id
				LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id
				LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				LEFT JOIN ranked_media rm ON s.id=rm.sector_id AND rm.rn=1
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				ORDER BY s.sorting, s.name
				""")) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, areaId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int sorting = rst.getInt("sorting");
					var lockedAdmin = rst.getBoolean("locked_admin");
					var lockedSuperadmin = rst.getBoolean("locked_superadmin");
					var name = rst.getString("name");
					var comment = rst.getString("description");
					var accessInfo = rst.getString("access_info");
					var accessClosed = rst.getString("access_closed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					int idCoordinates = rst.getInt("coordinates_id");
					var parking = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					var wallDirectionCalculated = geoRepo.getCompassDirection(setup, rst.getInt("compass_direction_id_calculated"));
					var wallDirectionManual = geoRepo.getCompassDirection(setup, rst.getInt("compass_direction_id_manual"));
					MediaIdentity mediaIdentity = null;
					int mediaId = rst.getInt("media_id");
					if (mediaId > 0) {
						long mediaVersionStamp = rst.getLong("media_version_stamp");
						int mediaFocusX = rst.getInt("media_focus_x");
						int mediaFocusY = rst.getInt("media_focus_y");
						var mediaPrimaryColorHex = rst.getString("media_primary_color_hex");
						mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY, mediaPrimaryColorHex);
					}
					else {
						var inherited = false;
						var showHiddenMedia = true;
						var x = mediaRepo.get().getMediaSector(setup, authUserId, id, 0, inherited, 0, 0, 0, showHiddenMedia);
						if (!x.isEmpty()) {
							mediaIdentity = x.getFirst().identity();
						}
					}
					var as = new AreaSector(areaName, id, sorting, lockedAdmin, lockedSuperadmin, name, comment,
							accessInfo, accessClosed, sunFromHour, sunToHour,
							parking, new ArrayList<>(), wallDirectionCalculated, wallDirectionManual,
							null, mediaIdentity, new ArrayList<>(), 0, new ArrayList<>());
					sectorLookup.put(id, as);
				}
			}
		}
		logger.debug("getAreaSectors(areaId={}, areaName={}) - sectorLookup.size()={}, duration={}", areaId, areaName, sectorLookup.size(), stopwatch);
		return sectorLookup;
	}

	private void loadSimplifiedGradeCounts(int areaId, Map<Integer, AreaSector> sectorLookup) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		var sqlStr = """
				WITH req AS (
				  SELECT ? area_id
				),
				target_systems AS (
				  SELECT DISTINCT tgs.grade_system_id 
				  FROM req 
				  JOIN area a ON a.id = req.area_id
				  JOIN region_type rt ON a.region_id = rt.region_id 
				  JOIN type_grade_system tgs ON rt.type_id = tgs.type_id
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
				FROM req
				JOIN sector s ON s.area_id = req.area_id
				CROSS JOIN all_labels al
				LEFT JOIN problem p ON s.id = p.sector_id 
				    AND EXISTS (
				        SELECT 1 FROM grade g_p 
				        WHERE p.consensus_grade_id = g_p.id 
				        AND g_p.label_compact = al.label_compact 
				        AND g_p.grade_system_id = al.grade_system_id
				    )
				    AND p.trash IS NULL AND p.locked_admin = 0 AND p.locked_superadmin = 0
				GROUP BY s.id, al.label_compact, al.hex_code, al.sort_weight
				ORDER BY s.id, al.sort_weight
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, areaId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					var sector = sectorLookup.get(rst.getInt("sector_id"));
					if (sector != null) {
						sector.gradeCounts().add(new GradeCount(rst.getString("label_compact"), rst.getString("color"), rst.getInt("num")));
					}
				}
			}
		}
		logger.debug("loadSimplifiedGradeCounts(areaId={}, sectorLookup.size()={}) - duration={}", areaId, sectorLookup.size(), stopwatch);
	}

	protected void ensureAdminWriteArea(Optional<Integer> authUserId, int areaId) throws SQLException {
		var c = txManager.getConnection();
		var ok = false;
		try (var ps = c.prepareStatement("""
				SELECT ur.admin_write, ur.superadmin_write
				FROM area a
				JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?
				WHERE a.id=?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				""")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, areaId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}
}
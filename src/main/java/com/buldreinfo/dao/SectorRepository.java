package com.buldreinfo.dao;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.helpers.HitsFormatter;
import com.buldreinfo.helpers.JdbcUtils;
import com.buldreinfo.helpers.SectorSort;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Media.MediaSector;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.model.Sector.SectorProblem;
import com.buldreinfo.model.Sector.SectorProblemOrder;
import com.buldreinfo.model.Trail;
import com.buldreinfo.model.Trail.TrailBuilder;
import com.buldreinfo.model.Type;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Repository
public class SectorRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();
	private final ObjectProvider<AreaRepository> areaRepo;
	private final ExternalLinksRepository externalLinksRepo;
	private final GeoRepository geoRepo;
	private final ObjectProvider<HierarchyRepository> hierarchyRepo;
	private final ObjectProvider<MediaRepository> mediaRepo;
	private final ObjectProvider<SectorRepository> sectorRepo;

	public SectorRepository(ClimbingTransactionManager txManager,
			ObjectProvider<AreaRepository> areaRepo,
			ExternalLinksRepository externalLinksRepo,
			GeoRepository geoRepo,
			ObjectProvider<HierarchyRepository> hierarchyRepo,
			ObjectProvider<MediaRepository> mediaRepo,
			ObjectProvider<SectorRepository> sectorRepo) {
		super(txManager);
		this.areaRepo = areaRepo;
		this.externalLinksRepo = externalLinksRepo;
		this.geoRepo = geoRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.sectorRepo = sectorRepo;
	}

	public Sector getSector(Optional<Integer> authUserId, boolean orderByGrade, Setup setup, int reqId, boolean shouldUpdateHits) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		if (shouldUpdateHits) {
			try (var ps = c.prepareStatement("UPDATE sector SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}

		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var outlineFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.getObject().getSectorOutline(reqId)), executor);
			var trailsFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.getObject().getSectorTrails(authUserId, Collections.singleton(reqId))), executor);
			var mediaFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> mediaRepo.getObject().getMediaSector(setup, authUserId, reqId, 0, false, 0, 0, 0, false)), executor);
			var linksFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> externalLinksRepo.getExternalLinks(0, reqId, 0)), executor);
			var problemsFuture = CompletableFuture.supplyAsync(() -> executeConcurrentTask(() -> sectorRepo.getObject().getSectorProblems(setup, authUserId, 0, reqId)), executor);

			Sector s = null;
			try (var ps = c.prepareStatement("""
					WITH req AS (
						SELECT ? region_id, ? auth_user_id, ? sector_id
					)
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
					""")) {
				ps.setInt(1, setup.idRegion());
				ps.setInt(2, authUserId.orElse(0));
				ps.setInt(3, reqId);
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						int areaId = rst.getInt("area_id");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						String areaAccessInfo = rst.getString("area_access_info");
						String areaAccessClosed = rst.getString("area_access_closed");
						boolean areaNoDogsAllowed = rst.getBoolean("area_no_dogs_allowed");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						String areaName = rst.getString("area_name");
						boolean lockedAdmin = rst.getBoolean("locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						String accessInfo = rst.getString("access_info");
						String accessClosed = rst.getString("access_closed");
						int sunFromHour = rst.getInt("sun_from_hour");
						int sunToHour = rst.getInt("sun_to_hour");
						int idCoordinates = rst.getInt("coordinates_id");
						Coordinates parking = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
						CompassDirection wallDirectionCalculated = geoRepo.getCompassDirection(setup, rst.getInt("compass_direction_id_calculated"));
						CompassDirection wallDirectionManual = geoRepo.getCompassDirection(setup, rst.getInt("compass_direction_id_manual"));
						String pageViews = HitsFormatter.formatHits(rst.getLong("hits"));

						var allMedia = mediaFuture.join();
						var partitioned = Optional.ofNullable(allMedia)
								.orElse(List.of())
								.stream()
								.collect(Collectors.partitioningBy(
										x -> x.sectors().stream().anyMatch(MediaSector::trivia)
										));
						List<Media> triviaMedia = partitioned.get(true);
						List<Media> media = partitioned.get(false);

						s = new Sector(null, orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, areaName, reqId, false, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, sunFromHour, sunToHour, parking, outlineFuture.join(), wallDirectionCalculated, wallDirectionManual, trailsFuture.join().get(reqId), media, triviaMedia, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), linksFuture.join(), pageViews);
					}
				}
			}
			if (s == null) {
				try {
					Redirect res = hierarchyRepo.getObject().getCanonicalUrl(setup, 0, reqId, 0);
					if (!Strings.isNullOrEmpty(res.redirectUrl())) {
						return new Sector(res.redirectUrl(), false, 0, false, false, null, null, false, 0, 0, null, 0, false, false, false, null, null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null);
					}
				} catch (NoSuchElementException _) {
				}
			}
			if (s == null) {
				throw new NoSuchElementException("Could not find sector with id=" + reqId);
			}
			try (var ps = c.prepareStatement("SELECT s.id, s.locked_admin, s.locked_superadmin, s.name, s.sorting FROM ((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) WHERE a.id=? AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0)) GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.sorting ORDER BY s.sorting, s.name")) {
				ps.setInt(1, authUserId.orElse(0));
				ps.setInt(2, s.areaId());
				try (var rst = ps.executeQuery()) {
					while (rst.next()) {
						s.sectors().add(new Sector.SectorJump(rst.getInt("id"), rst.getBoolean("locked_admin"), rst.getBoolean("locked_superadmin"), rst.getString("name"), rst.getInt("sorting")));
					}
				}
			}
			if (s.sectors() != null) {
				s.sectors().sort((o1, o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()));
			}
			var problemsMap = problemsFuture.join();
			var sectorProblems = problemsMap.get(reqId);
			if (sectorProblems != null) {
				for (SectorProblem sp : sectorProblems) {
					s.problems().add(sp);
					s.problemOrder().add(new Sector.SectorProblemOrder(sp.id(), sp.name(), sp.nr()));
				}
			}
			if (!s.problems().isEmpty() && orderByGrade) {
				Collections.sort(s.problems(), Comparator.comparing(SectorProblem::gradeWeight).reversed());
			}
			logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}, shouldUpdateHits={}) - duration={}", authUserId, orderByGrade, reqId, shouldUpdateHits, stopwatch);
			return s;
		}
	}

	public Multimap<Integer, Trail> getSectorTrails(Optional<Integer> authUserId, Collection<Integer> sectorIds) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		Preconditions.checkArgument(!sectorIds.isEmpty(), "sectorIds is empty");
		Multimap<Integer, Trail> res = ArrayListMultimap.create();
		String inClause = ",?".repeat(sectorIds.size()).substring(1);
		Map<Integer, TrailBuilder> trailBuilders = new LinkedHashMap<>();
		Multimap<Integer, Integer> sectorToTrailIds = ArrayListMultimap.create();

		String trailSql = String.format("""
				SELECT st.sector_id, t.id, t.is_descent, t.title, t.description 
				FROM sector_trail st
				JOIN trail t ON st.trail_id = t.id
				WHERE st.sector_id IN (%s) AND t.trash IS NULL
				ORDER BY t.is_descent, t.title
				""", inClause);

		try (var ps = c.prepareStatement(trailSql)) {
			int paramIdx = 1;
			for (int idSector : sectorIds) ps.setInt(paramIdx++, idSector);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					int trailId = rst.getInt("id");
					sectorToTrailIds.put(sectorId, trailId);
					if (!trailBuilders.containsKey(trailId)) {
						trailBuilders.put(trailId, new TrailBuilder(trailId, rst.getBoolean("is_descent"), rst.getString("title"), rst.getString("description")));
					}
				}
			}
		}
		if (trailBuilders.isEmpty()) return res;
		String pathInClause = ",?".repeat(trailBuilders.size()).substring(1);
		String pathSql = String.format("""
				SELECT tc.trail_id, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source 
				FROM trail_coordinate tc
				JOIN coordinates c ON tc.coordinates_id = c.id
				WHERE tc.trail_id IN (%s) 
				ORDER BY tc.trail_id, tc.sorting
				""", pathInClause);

		try (var ps = c.prepareStatement(pathSql)) {
			int paramIdx = 1;
			for (int trailId : trailBuilders.keySet()) ps.setInt(paramIdx++, trailId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int trailId = rst.getInt("trail_id");
					trailBuilders.get(trailId).path.add(new Coordinates(
							rst.getInt("id"), rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source")
							));
				}
			}
		}
		String markerSql = String.format("""
				SELECT tm.trail_id, tm.label, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source 
				FROM trail_marker tm
				JOIN coordinates c ON tm.coordinates_id = c.id
				WHERE tm.trail_id IN (%s)
				""", pathInClause);

		try (var ps = c.prepareStatement(markerSql)) {
			int paramIdx = 1;
			for (int trailId : trailBuilders.keySet()) ps.setInt(paramIdx++, trailId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int trailId = rst.getInt("trail_id");
					Coordinates markerCoords = new Coordinates(
							rst.getInt("id"), rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source")
							);
					trailBuilders.get(trailId).markers.add(new Trail.TrailMarker(markerCoords, rst.getString("label")));
				}
			}
		}

		Multimap<Integer, Media> trailsMediaMap = mediaRepo.getObject().getMediaTrails(authUserId, trailBuilders.keySet());
		Map<Integer, Trail> finalTrailsMap = new HashMap<>();
		for (TrailBuilder b : trailBuilders.values()) {
			List<Media> trailMediaList = (List<Media>) trailsMediaMap.get(b.id);
			Trail compiledTrail = Trail.withCalculatedStats(
					b.id, b.isDescent, false, b.title, b.description, b.path, b.markers, trailMediaList, null
					);
			finalTrailsMap.put(b.id, compiledTrail);
		}
		for (int sectorId : sectorIds) {
			for (int trailId : sectorToTrailIds.get(sectorId)) res.put(sectorId, finalTrailsMap.get(trailId));
		}
		logger.debug("getSectorTrails(sectorIds.size()={}) - res.size()={}, duration={}", sectorIds.size(), res.size(), stopwatch);
		return res;
	}

	public Redirect setSector(Optional<Integer> authUserId, Setup setup, Sector s) throws SQLException, InterruptedException {
		var c = txManager.getConnection();
		int idSector = -1;
		final boolean isLockedAdmin = s.lockedSuperadmin() ? false : s.lockedAdmin();
		boolean setPermissionRecursive = false;
		List<Coordinates> allCoordinates = new ArrayList<>();
		if (s.outline() != null && !s.outline().isEmpty()) {
			allCoordinates.addAll(s.outline());
		}
		if (s.parking() != null) {
			if (s.parking().getLatitude() == 0 || s.parking().getLongitude() == 0) {
				s = s.withParking(null);
			}
			else {
				allCoordinates.add(s.parking());
			}
		}
		geoRepo.ensureCoordinatesInDbWithElevationAndId(allCoordinates);

		if (s.id() > 0) {
			ensureAdminWriteSector(authUserId, s.id());
			Sector currSector = getSector(authUserId, false, setup, s.id(), false);
			setPermissionRecursive = currSector.lockedAdmin() != isLockedAdmin || currSector.lockedSuperadmin() != s.lockedSuperadmin();
			try (var ps = c.prepareStatement("UPDATE sector s, area a, user_region ur SET s.name=?, s.description=?, s.access_info=?, s.access_closed=?, s.sun_from_hour=?, s.sun_to_hour=?, s.parking_coordinates_id=?, s.locked_admin=?, s.locked_superadmin=?, s.compass_direction_id_calculated=?, s.compass_direction_id_manual=?, s.trash=CASE WHEN ? THEN NOW() ELSE NULL END, s.trash_by=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)")) {
				ps.setString(1, GlobalFunctions.stripString(s.name()));
				ps.setString(2, GlobalFunctions.stripString(s.comment()));
				ps.setString(3, GlobalFunctions.stripString(s.accessInfo()));
				ps.setString(4, GlobalFunctions.stripString(s.accessClosed()));
				JdbcUtils.setNullablePositiveDouble(ps, 5, s.sunFromHour());
				JdbcUtils.setNullablePositiveDouble(ps, 6, s.sunToHour());
				JdbcUtils.setNullablePositiveInteger(ps, 7, s.parking() == null ? 0 : s.parking().getId());
				ps.setBoolean(8, isLockedAdmin);
				ps.setBoolean(9, s.lockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.outline());
				JdbcUtils.setNullablePositiveInteger(ps, 10, calculatedWallDirection != null ? calculatedWallDirection.id() : 0);
				JdbcUtils.setNullablePositiveInteger(ps, 11, s.wallDirectionManual() != null ? s.wallDirectionManual().id() : 0);
				ps.setBoolean(12, s.trash());
				ps.setInt(13, s.trash() ? authUserId.orElseThrow() : 0);
				ps.setInt(14, s.id());
				ps.setInt(15, authUserId.orElseThrow());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idSector = s.id();

			if (s.problemOrder() != null) {
				setSectorProblemOrder(s.problemOrder());
			}

			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE s.id=?";
				try (var ps = c.prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, s.lockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, s.lockedSuperadmin());
					ps.setInt(5, idSector);
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?";
				try (var ps = c.prepareStatement(sqlStr)) {
					ps.setInt(1, idSector);
					ps.execute();
				}
			}
		} else {
			areaRepo.getObject().ensureAdminWriteArea(authUserId, s.areaId());
			try (var ps = c.prepareStatement("INSERT INTO sector (area_id, name, description, access_info, access_closed, parking_coordinates_id, locked_admin, locked_superadmin, compass_direction_id_calculated, compass_direction_id_manual, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, s.areaId());
				ps.setString(2, s.name());
				ps.setString(3, GlobalFunctions.stripString(s.comment()));
				ps.setString(4, GlobalFunctions.stripString(s.accessInfo()));
				ps.setString(5, GlobalFunctions.stripString(s.accessClosed()));
				JdbcUtils.setNullablePositiveInteger(ps, 6, s.parking() == null ? 0 : s.parking().getId());
				ps.setBoolean(7, isLockedAdmin);
				ps.setBoolean(8, s.lockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.outline());
				JdbcUtils.setNullablePositiveInteger(ps, 9, calculatedWallDirection != null ? calculatedWallDirection.id() : 0);
				JdbcUtils.setNullablePositiveInteger(ps, 10, s.wallDirectionManual() != null ? s.wallDirectionManual().id() : 0);
				ps.executeUpdate();
				try (var rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idSector = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idSector > 0, "idSector=" + idSector);

		try (var ps = c.prepareStatement("DELETE FROM sector_outline WHERE sector_id=?")) {
			ps.setInt(1, idSector);
			ps.execute();
		}
		if (s.outline() != null && !s.outline().isEmpty()) {
			try (var ps = c.prepareStatement("INSERT INTO sector_outline (sector_id, coordinates_id, sorting) VALUES (?, ?, ?)")) {
				int sorting = 0;
				for (Coordinates coord : s.outline()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}

		externalLinksRepo.upsertExternalLinks(s.externalLinks(), 0, idSector, 0);
		Redirect res = null;
		if (s.trash()) {
			res = Redirect.fromIdArea(s.areaId());
		}
		else {
			res = Redirect.fromIdSector(idSector);
		}
		logger.debug("setSector() - res={}", res);
		return res;
	}

	public void upsertTrails(Optional<Integer> authUserId, List<Trail> trails) throws SQLException, InterruptedException {
		var c = txManager.getConnection();
		if (trails == null || trails.isEmpty()) return;
		Set<Integer> allSectorsToLock = new TreeSet<>();
		List<Integer> existingTrailIds = new ArrayList<>();
		for (Trail t : trails) {
			Preconditions.checkArgument(t.sectors() != null && !t.sectors().isEmpty(), "sectors cannot be empty or null");
			for (var sector : t.sectors()) allSectorsToLock.add(sector.sectorId());
			if (t.id() > 0) existingTrailIds.add(t.id());
		}
		if (!existingTrailIds.isEmpty()) {
			existingTrailIds.sort(Comparator.naturalOrder());
			String sql = "SELECT sector_id FROM sector_trail WHERE trail_id = ?";
			try (var ps = c.prepareStatement(sql)) {
				for (int trailId : existingTrailIds) {
					ps.setInt(1, trailId);
					try (var rst = ps.executeQuery()) {
						while (rst.next()) allSectorsToLock.add(rst.getInt("sector_id"));
					}
				}
			}
		}
		for (int sectorId : allSectorsToLock) ensureAdminWriteSector(authUserId, sectorId);
		for (Trail t : trails) {
			if (t.path() != null && t.path().size() >= 2 && t.sectors() != null && !t.sectors().isEmpty()) {
				String parkingSql = """
							SELECT c.latitude, c.longitude 
							FROM sector s
							JOIN coordinates c ON s.parking_coordinates_id = c.id
							WHERE s.id IN (%s)
							LIMIT 1
						""".formatted(",?".repeat(t.sectors().size()).substring(1));
				try (var ps = c.prepareStatement(parkingSql)) {
					int pIdx = 1;
					for (Trail.TrailSector sector : t.sectors()) ps.setInt(pIdx++, sector.sectorId());
					try (var rst = ps.executeQuery()) {
						if (rst.next()) {
							double parkingLat = rst.getDouble("latitude");
							double parkingLng = rst.getDouble("longitude");
							Coordinates firstCoord = t.path().getFirst();
							Coordinates lastCoord = t.path().getLast();
							double distToStart = GeoHelper.getHaversineDistanceInMeters(parkingLat, parkingLng, firstCoord.getLatitude(), firstCoord.getLongitude());
							double distToEnd = GeoHelper.getHaversineDistanceInMeters(parkingLat, parkingLng, lastCoord.getLatitude(), lastCoord.getLongitude());
							boolean shouldReverse = false;
							if (t.isDescent()) {
								if (distToStart < distToEnd) shouldReverse = true;
							} else {
								if (distToEnd < distToStart) shouldReverse = true;
							}
							if (shouldReverse) {
								Collections.reverse(t.path());
								if (t.markers() != null && !t.markers().isEmpty()) Collections.reverse(t.markers());
							}
						}
					}
				}
			}
		}
		List<Coordinates> allCoordinatesGlobal = new ArrayList<>();
		for (Trail t : trails) {
			if (t.path() != null) allCoordinatesGlobal.addAll(t.path());
			if (t.markers() != null) {
				for (Trail.TrailMarker marker : t.markers()) if (marker.coordinates() != null) allCoordinatesGlobal.add(marker.coordinates());
			}
		}
		if (!allCoordinatesGlobal.isEmpty()) {
			allCoordinatesGlobal.sort((c1, c2) -> {
				if (c1.getId() != c2.getId()) return Integer.compare(c1.getId(), c2.getId());
				int latCompare = Double.compare(c1.getLatitude(), c2.getLatitude());
				if (latCompare != 0) return latCompare;
				return Double.compare(c1.getLongitude(), c2.getLongitude());
			});
			geoRepo.ensureCoordinatesInDbWithElevationAndId(allCoordinatesGlobal);
		}
		List<Trail> sortedTrails = new ArrayList<>(trails);
		sortedTrails.sort(Comparator.comparingInt(Trail::id));
		for (Trail t : sortedTrails) {
			if (t.delete()) {
				String sql = "UPDATE trail SET trash = NOW(), trash_by = ? WHERE id = ?";
				try (var ps = c.prepareStatement(sql)) {
					ps.setInt(1, authUserId.orElseThrow());
					ps.setInt(2, t.id());
					ps.executeUpdate();
				}
				continue;
			}
			int trailId = t.id();
			if (trailId > 0) {
				String sql = "UPDATE trail SET is_descent = ?, title = ?, description = ?, trash = NULL, trash_by = 0 WHERE id = ?";
				try (var ps = c.prepareStatement(sql)) {
					ps.setBoolean(1, t.isDescent());
					ps.setString(2, t.title());
					ps.setString(3, t.description());
					ps.setInt(4, trailId);
					ps.executeUpdate();
				}
			} else {
				String sql = "INSERT INTO trail (is_descent, title, description) VALUES (?, ?, ?)";
				try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					ps.setBoolean(1, t.isDescent());
					ps.setString(2, t.title());
					ps.setString(3, t.description());
					ps.executeUpdate();
					try (var rs = ps.getGeneratedKeys()) {
						if (rs.next()) trailId = rs.getInt(1);
					}
				}
			}
			try (var ps = c.prepareStatement("DELETE FROM trail_coordinate WHERE trail_id = ?")) {
				ps.setInt(1, trailId);
				ps.executeUpdate();
			}
			if (t.path() != null && !t.path().isEmpty()) {
				String sql = "INSERT INTO trail_coordinate (trail_id, coordinates_id, sorting) VALUES (?, ?, ?)";
				try (var ps = c.prepareStatement(sql)) {
					int sorting = 0;
					for (Coordinates coord : t.path()) {
						ps.setInt(1, trailId);
						ps.setInt(2, coord.getId());
						ps.setInt(3, sorting++);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			try (var ps = c.prepareStatement("DELETE FROM trail_marker WHERE trail_id = ?")) {
				ps.setInt(1, trailId);
				ps.executeUpdate();
			}
			if (t.markers() != null && !t.markers().isEmpty()) {
				String sql = "INSERT INTO trail_marker (trail_id, coordinates_id, label) VALUES (?, ?, ?)";
				try (var ps = c.prepareStatement(sql)) {
					for (Trail.TrailMarker marker : t.markers()) {
						if (marker.coordinates() != null) {
							ps.setInt(1, trailId);
							ps.setInt(2, marker.coordinates().getId());
							ps.setString(3, marker.label());
							ps.addBatch();
						}
					}
					ps.executeBatch();
				}
			}
			try (var ps = c.prepareStatement("DELETE FROM sector_trail WHERE trail_id = ?")) {
				ps.setInt(1, trailId);
				ps.executeUpdate();
			}
			if (t.sectors() != null && !t.sectors().isEmpty()) {
				String sql = "INSERT INTO sector_trail (sector_id, trail_id) VALUES (?, ?)";
				try (var ps = c.prepareStatement(sql)) {
					for (Trail.TrailSector sector : t.sectors()) {
						ps.setInt(1, sector.sectorId());
						ps.setInt(2, trailId);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
		}
	}

	protected void ensureAdminWriteSector(Optional<Integer> authUserId, int sectorId) throws SQLException {
		var c = txManager.getConnection();
		boolean ok = false;
		try (var ps = c.prepareStatement("""
				SELECT ur.admin_write, ur.superadmin_write
				FROM user_region ur
				JOIN area a ON ur.region_id=a.region_id
				JOIN sector s ON a.id=s.area_id
				WHERE s.id=?
				  AND ur.user_id=?
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				""")) {
			ps.setInt(1, sectorId);
			ps.setInt(2, authUserId.orElseThrow());
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	protected List<Coordinates> getSectorOutline(int idSector) throws SQLException {
		Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(Collections.singleton(idSector));
		if (idSectorOutline == null || idSectorOutline.isEmpty()) return null;
		return Lists.newArrayList(idSectorOutline.get(idSector));
	}

	protected Multimap<Integer, Coordinates> getSectorOutlines(Collection<Integer> idSectors) throws SQLException {
		var c = txManager.getConnection();
		var stopwatch = Stopwatch.createStarted();
		Preconditions.checkArgument(!idSectors.isEmpty(), "idSectors is empty");
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idSectors.size()).substring(1);
		String sqlStr = "SELECT so.sector_id id_sector, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM sector_outline so, coordinates c WHERE so.sector_id IN (" + in + ") AND so.coordinates_id=c.id ORDER BY so.sorting";
		try (var ps = c.prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idSectors) ps.setInt(parameterIndex++, idSector);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int idSector = rst.getInt("id_sector");
					res.put(idSector, new Coordinates(rst.getInt("id"), rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source")));
				}
			}
		}
		logger.debug("getSectorOutlines(idSectors.size()={}) - res.size()={}, duration={}", idSectors.size(), res.size(), stopwatch);
		return res;
	}

	protected Multimap<Integer, SectorProblem> getSectorProblems(Setup setup, Optional<Integer> authUserId, int optAreaId, int optSectorId) throws SQLException {
		var c = txManager.getConnection();
		Preconditions.checkArgument((optAreaId == 0 && optSectorId > 0) || optAreaId > 0 && optSectorId == 0);
		var stopwatch = Stopwatch.createStarted();
		Multimap<Integer, SectorProblem> res = LinkedListMultimap.create();
		String sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? area_id, ? sector_id, ? include_fa_aid
				),
				filtered_problems AS (
				    SELECT p.*, ur.admin_read, ur.superadmin_read
				    FROM problem p
				    CROSS JOIN req
				    JOIN sector s ON s.id = p.sector_id
				    JOIN area a ON s.area_id = a.id
				    LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				    WHERE ((req.area_id > 0 AND a.id = req.area_id) OR (req.sector_id > 0 AND p.sector_id = req.sector_id))
				      AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				),
				fa_agg AS (
				    SELECT f.problem_id,
				           GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname, ''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_names,
				           MAX(CASE WHEN u.id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_is_fa
				    FROM fa f
				    JOIN user u ON f.user_id = u.id
				    WHERE f.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY f.problem_id
				),
				fa_aid_agg AS (
				    SELECT a.problem_id,
				           GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') AS fa_aid_names,
				           YEAR(a.aid_date) fa_aid_date
				    FROM fa_aid a
				    JOIN fa_aid_user au ON a.problem_id=au.problem_id
				    JOIN user u ON au.user_id = u.id
				    WHERE a.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY a.problem_id, a.aid_date
				),
				tick_agg AS (
				    SELECT t.problem_id,
				           COUNT(t.id) AS total_ticks,
				           ROUND(AVG(NULLIF(t.stars, -1)), 1) AS avg_stars,
				           MAX(CASE WHEN t.user_id = (SELECT auth_user_id FROM req) THEN 1 ELSE 0 END) AS user_ticked
				    FROM tick t
				    WHERE t.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY t.problem_id
				),
				media_agg AS (
				    SELECT mp.problem_id,
				           COUNT(DISTINCT CASE WHEN m.is_movie = 0 THEN m.id END) AS num_images,
				           COUNT(DISTINCT CASE WHEN m.is_movie = 1 THEN m.id END) AS num_movies
				    FROM media_problem mp
				    JOIN media m ON mp.media_id = m.id
				    WHERE mp.trivia = 0 AND m.deleted_user_id IS NULL
				    AND mp.problem_id IN (SELECT id FROM filtered_problems)
				    GROUP BY mp.problem_id
				)
				SELECT p.sector_id, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description,
				       fa_aid.fa_aid_names fa_user, fa_aid.fa_aid_date fa_year,
				       fa.fa_names ffa_user, YEAR(p.fa_date) ffa_year,
				       ty.id AS type_id, ty.type, ty.subtype,
				       g.weight AS problem_grade_weight, g.grade AS problem_grade,
				       COALESCE(t.total_ticks, 0) AS total_ticks,
				       COALESCE(t.avg_stars, 0) AS stars,
				       GREATEST(COALESCE(t.user_ticked, 0), COALESCE(fa.user_is_fa, 0)) AS ticked,
				       CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END AS todo,
				       gb.danger,
				       p.length_meter,
				       co.id AS coordinates_id, co.latitude, co.longitude, co.elevation, co.elevation_source,
				       (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p.id) AS num_pitches,
				       COALESCE(m.num_images, 0) AS num_images,
				       COALESCE(m.num_movies, 0) AS num_movies,
				       CASE WHEN EXISTS (SELECT 1 FROM svg WHERE svg.problem_id = p.id) THEN 1 ELSE 0 END AS has_topo
				FROM filtered_problems p
				JOIN grade g ON p.consensus_grade_id = g.id
				JOIN type ty ON p.type_id = ty.id
				LEFT JOIN coordinates co ON p.coordinates_id = co.id
				LEFT JOIN fa_agg fa ON p.id = fa.problem_id
				LEFT JOIN fa_aid_agg fa_aid ON p.id = fa_aid.problem_id
				LEFT JOIN tick_agg t ON p.id = t.problem_id
				LEFT JOIN media_agg m ON p.id = m.problem_id
				LEFT JOIN todo ON p.id = todo.problem_id AND todo.user_id = (SELECT auth_user_id FROM req)
				LEFT JOIN LATERAL (
				    SELECT danger 
				    FROM guestbook 
				    WHERE problem_id = p.id 
				    AND (danger = 1 OR resolved = 1)
				    ORDER BY id DESC
				    LIMIT 1
				) gb ON TRUE
				ORDER BY p.nr
				""";
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, optAreaId);
			ps.setInt(3, optSectorId);
			ps.setInt(4, setup.isBouldering() ? 0 : 1);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0 ? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					var p = new SectorProblem(rst.getInt("id"), rst.getString("broken"), rst.getBoolean("locked_admin"), rst.getBoolean("locked_superadmin"),
							rst.getInt("nr"), rst.getString("name"), rst.getString("rock"), rst.getString("description"),
							rst.getInt("problem_grade_weight"), rst.getString("problem_grade"), 
							rst.getString("fa_user"), rst.getInt("fa_year"), rst.getString("ffa_user"), rst.getInt("ffa_year"), 
							rst.getInt("length_meter"), rst.getInt("num_pitches"), rst.getInt("num_images") > 0, rst.getInt("num_movies") > 0, rst.getBoolean("has_topo"),
							coordinates, rst.getInt("total_ticks"), rst.getDouble("stars"), rst.getBoolean("ticked"), rst.getBoolean("todo"),
							new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype")), rst.getBoolean("danger")
							);
					res.put(sectorId, p);
				}
			}
		}
		logger.debug("getSectorProblems(optAreaId={}, optSectorId={}) - res.size()={}, duration={}", optAreaId, optSectorId, res.size(), stopwatch);
		return res;
	}

	protected void setSectorProblemOrder(List<SectorProblemOrder> lst) throws SQLException {
		var c = txManager.getConnection();
		if (!lst.isEmpty()) {
			try (var ps = c.prepareStatement("UPDATE problem SET nr=? WHERE id=?")) {
				for (SectorProblemOrder x : lst) {
					ps.setInt(1, x.nr());
					ps.setInt(2, x.id());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
	}

	protected void tryFixSectorOrdering(int sectorId, int problemId, int problemNewNr) throws SQLException {
		var c = txManager.getConnection();
		List<SectorProblemOrder> lst = new ArrayList<>();
		if (problemId > 0) {
			String sqlStr = """
					WITH x AS (
					  SELECT p.sector_id, COUNT(p.id) num_problems, MAX(p.nr) max_num
					  FROM problem p
					  WHERE p.sector_id=?
					  GROUP BY p.sector_id
					)
					SELECT p.id
					FROM problem p_input, x,
					     problem p
					WHERE p_input.id=? AND p_input.nr!=?
					  AND p_input.sector_id=x.sector_id AND x.num_problems=x.max_num
					  AND p_input.sector_id=p.sector_id
					  AND p.id!=p_input.id
					ORDER BY p.nr
					""";
			try (var ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, sectorId);
				ps.setInt(2, problemId);
				ps.setInt(3, problemNewNr);
				try (var rst = ps.executeQuery()) {
					int nr = 0;
					while (rst.next()) {
						if (++nr == problemNewNr) ++nr;
						int id = rst.getInt("id");
						lst.add(new SectorProblemOrder(id, null, nr));
					}
				}
			}
		}
		else if (problemNewNr != 0) {
			String sqlStr = """
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
					ORDER BY p.nr
					""";
			try (var ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, sectorId);
				try (var rst = ps.executeQuery()) {
					int nr = 0;
					while (rst.next()) {
						if (++nr == problemNewNr) ++nr;
						int id = rst.getInt("id");
						lst.add(new SectorProblemOrder(id, null, nr));
					}
				}
			}
		}
		setSectorProblemOrder(lst);
	}
}
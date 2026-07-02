package com.buldreinfo.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.ExternalLinksRepository;
import com.buldreinfo.dao.GeoRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.model.Trail;
import com.buldreinfo.util.GeoUtils;

@Service
public class SectorService {
	private static final Logger logger = LogManager.getLogger();
	private final AreaRepository areaRepo;
	private final ExternalLinksRepository externalLinksRepo;
	private final GeoRepository geoRepo;
	private final HierarchyRepository hierarchyRepo;
	private final MediaRepository mediaRepo;
	private final SectorRepository sectorRepo;

	public SectorService(
			AreaRepository areaRepo,
			ExternalLinksRepository externalLinksRepo,
			GeoRepository geoRepo,
			HierarchyRepository hierarchyRepo,
			MediaRepository mediaRepo,
			SectorRepository sectorRepo) {
		this.areaRepo = areaRepo;
		this.externalLinksRepo = externalLinksRepo;
		this.geoRepo = geoRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.sectorRepo = sectorRepo;
	}

	@Transactional(readOnly = true)
	public Sector getSector(Optional<Integer> authUserId, boolean orderByGrade, Setup setup, int reqId) {
		var startNanos = System.nanoTime();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var outlineFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorOutline(reqId), executor);
			var trailsFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorTrails(Collections.singleton(reqId), trailIds -> mediaRepo.getMediaTrails(authUserId, trailIds)), executor);
			var mediaFuture = CompletableFuture.supplyAsync(() -> mediaRepo.getMediaSector(setup, authUserId, reqId, 0, false, false), executor);
			var linksFuture = CompletableFuture.supplyAsync(() -> externalLinksRepo.getExternalLinks(0, reqId, 0), executor);
			var problemsFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorProblems(setup, authUserId, 0, reqId), executor);

			Sector s = sectorRepo.getSectorBase(setup, authUserId, reqId, orderByGrade,
					() -> outlineFuture.join(),
					() -> trailsFuture.join(),
					() -> mediaFuture.join(),
					() -> linksFuture.join(),
					() -> problemsFuture.join()
					);

			if (s == null) {
				try {
					var res = hierarchyRepo.getCanonicalUrl(setup, 0, reqId, 0);
					if (res.redirectUrl() != null && !res.redirectUrl().isBlank()) {
						return new Sector(res.redirectUrl(), false, 0, false, false, null, null, false, 0, 0, null, 0, false, false, false, null, null, null, null, 0, 0, null, null, null, null, null, null, null, null, null, null, null, null);
					}
				} catch (Exception _) {}
				throw new NoSuchElementException("Could not find sector with id=" + reqId);
			}

			logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, Duration.ofNanos(System.nanoTime() - startNanos));
			return s;
		}
	}

	@Transactional
	public Redirect setSector(Optional<Integer> authUserId, Setup setup, Sector s) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("Not logged in");

		final boolean isLockedAdmin = !s.lockedSuperadmin() && s.lockedAdmin();
		boolean setPermissionRecursive = false;
		List<Coordinates> allCoords = new ArrayList<>();

		if (s.outline() != null && !s.outline().isEmpty()) allCoords.addAll(s.outline());
		if (s.parking() != null) {
			if (s.parking().getLatitude() == 0 || s.parking().getLongitude() == 0) s = s.withParking(null);
			else allCoords.add(s.parking());
		}
		geoRepo.ensureCoordinatesInDbWithElevationAndId(allCoords);

		Integer parkingId = (s.parking() != null && s.parking().getId() > 0) ? s.parking().getId() : null;
		Integer calcCompass = Optional.ofNullable(GeoHelper.calculateCompassDirection(setup, s.outline()))
				.map(CompassDirection::id)
				.filter(id -> id > 0)
				.orElse(null);
		Integer manualCompass = (s.wallDirectionManual() != null && s.wallDirectionManual().id() > 0) 
				? s.wallDirectionManual().id() 
						: null;

		if (s.id() > 0) {
			sectorRepo.ensureAdminWriteSector(authUserId, s.id());
			var curr = getSector(authUserId, false, setup, s.id());
			setPermissionRecursive = curr.lockedAdmin() != isLockedAdmin || curr.lockedSuperadmin() != s.lockedSuperadmin();
		} else {
			areaRepo.ensureAdminWriteArea(authUserId, s.areaId());
		}

		int idSector = sectorRepo.setSectorDb(authUserId, s, isLockedAdmin, parkingId, calcCompass, manualCompass, setPermissionRecursive);

		if (s.problemOrder() != null) sectorRepo.setSectorProblemOrder(s.problemOrder());

		externalLinksRepo.upsertExternalLinks(s.externalLinks(), 0, idSector, 0);
		return s.trash() ? Redirect.fromIdArea(s.areaId()) : Redirect.fromIdSector(idSector);
	}

	@Transactional
	public void upsertTrails(Optional<Integer> authUserId, List<Trail> trails) {
		if (trails == null || trails.isEmpty()) return;

		Set<Integer> allSectorsToLock = new TreeSet<>();
		List<Integer> existingTrailIds = new ArrayList<>();
		for (Trail t : trails) {
			if (t.sectors() == null || t.sectors().isEmpty()) throw new IllegalArgumentException("sectors cannot be empty or null");
			t.sectors().forEach(sec -> allSectorsToLock.add(sec.sectorId()));
			if (t.id() > 0) existingTrailIds.add(t.id());
		}

		if (!existingTrailIds.isEmpty()) {
			existingTrailIds.sort(Comparator.naturalOrder());
			allSectorsToLock.addAll(sectorRepo.getSectorsForTrails(existingTrailIds));
		}

		allSectorsToLock.forEach(id -> sectorRepo.ensureAdminWriteSector(authUserId, id));

		for (Trail t : trails) {
			if (t.path() != null && t.path().size() >= 2 && t.sectors() != null && !t.sectors().isEmpty()) {
				Coordinates parkingCoord = sectorRepo.getFirstParkingCoordinateForSectors(t.sectors().stream().map(Trail.TrailSector::sectorId).toList());
				if (parkingCoord != null) {
					double distToStart = GeoUtils.getHaversineDistanceInMeters(parkingCoord.getLatitude(), parkingCoord.getLongitude(), t.path().getFirst().getLatitude(), t.path().getFirst().getLongitude());
					double distToEnd = GeoUtils.getHaversineDistanceInMeters(parkingCoord.getLatitude(), parkingCoord.getLongitude(), t.path().getLast().getLatitude(), t.path().getLast().getLongitude());
					boolean shouldReverse = t.isDescent() ? (distToStart < distToEnd) : (distToEnd < distToStart);
					if (shouldReverse) {
						Collections.reverse(t.path());
						if (t.markers() != null) Collections.reverse(t.markers());
					}
				}
			}
		}

		List<Coordinates> allCoords = new ArrayList<>();
		trails.forEach(t -> {
			if (t.path() != null) allCoords.addAll(t.path());
			if (t.markers() != null) t.markers().forEach(m -> { if (m.coordinates() != null) allCoords.add(m.coordinates()); });
		});
		if (!allCoords.isEmpty()) geoRepo.ensureCoordinatesInDbWithElevationAndId(allCoords);

		sectorRepo.upsertTrailsDb(authUserId, trails);
	}
}
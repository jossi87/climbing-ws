package com.buldreinfo.service;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.helpers.SectorSort;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Area.AreaSectorOrder;
import com.buldreinfo.model.Media.MediaArea;
import com.buldreinfo.model.Redirect;

@Service
public class AreaService {
	private static final Logger logger = LogManager.getLogger();
	private final AreaRepository areaRepo;
	private final GeoService geoService;
	private final HierarchyRepository hierarchyRepo;
	private final MediaRepository mediaRepo;
	private final SectorRepository sectorRepo;

	public AreaService(AreaRepository areaRepo,
			GeoService geoService,
			HierarchyRepository hierarchyRepo,
			MediaRepository mediaRepo,
			SectorRepository sectorRepo) {
		this.areaRepo = areaRepo;
		this.geoService = geoService;
		this.hierarchyRepo = hierarchyRepo;
		this.mediaRepo = mediaRepo;
		this.sectorRepo = sectorRepo;
	}

	@Transactional(readOnly = true)
	public Area getArea(Setup setup, Optional<Integer> authUserId, int reqId) {
		var start = System.nanoTime();

		var allMedia = mediaRepo.getMediaArea(authUserId, reqId, false);
		var partitioned = Optional.ofNullable(allMedia).orElse(List.of()).stream().collect(Collectors.partitioningBy(x -> x.areas().stream().anyMatch(MediaArea::trivia)));

		Area a = areaRepo.getAreaBase(setup, authUserId, reqId, partitioned.get(false), partitioned.get(true));

		if (a == null) {
			try {
				var res = hierarchyRepo.getCanonicalUrl(setup, reqId, 0, 0);
				if (res.redirectUrl() != null && !res.redirectUrl().isEmpty()) {
					return new Area(res.redirectUrl(), null, -1, false, false, false, false, null, null, false, 0, 0, null, null, null, 0, 0, null, null, null, null, null, null);
				}
			} catch (Exception _) {}
			throw new NoSuchElementException("Could not find area with id=" + reqId);
		}

		var sectorLookup = areaRepo.getAreaSectors(setup, authUserId, a.id(), a.name(), sectorId -> {
			var x = mediaRepo.getMediaSector(setup, authUserId, sectorId, 0, false, true);
			return x.isEmpty() ? null : x.get(0).identity();
		});

		if (!sectorLookup.isEmpty()) {
			try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
				var problemsFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorProblems(setup, authUserId, reqId, 0), executor);
				var outlinesFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorOutlines(sectorLookup.keySet()), executor);
				var trailsFuture = CompletableFuture.supplyAsync(() -> sectorRepo.getSectorTrails(sectorLookup.keySet(), trailIds -> mediaRepo.getMediaTrails(authUserId, trailIds)), executor);

				var sectorProblems = problemsFuture.join();
				sectorProblems.forEach((sid, problems) -> Optional.ofNullable(sectorLookup.get(sid)).ifPresent(s -> s.problems().addAll(problems)));

				if (authUserId.isPresent()) {
					sectorLookup.forEach((sid, s) -> {
						long total = s.problems().stream().filter(p -> p.broken() == null && (!"n/a".equalsIgnoreCase(p.grade()) || p.faUser() != null || p.ffaUser() != null)).count();
						if (total != 0) {
							long completed = s.problems().stream().filter(p -> p.broken() == null && (!"n/a".equalsIgnoreCase(p.grade()) || p.faUser() != null || p.ffaUser() != null) && p.ticked()).count();
							sectorLookup.put(sid, s.withProgress((int) Math.round((double) completed / total * 100)));
						}
					});
				}

				outlinesFuture.join().forEach((sid, outline) -> Optional.ofNullable(sectorLookup.get(sid)).ifPresent(s -> s.outline().addAll(outline)));
				var trails = trailsFuture.join();
				trails.forEach((sid, trailList) -> 
				Optional.ofNullable(sectorLookup.get(sid))
				.ifPresent(s -> sectorLookup.put(sid, s.withTrails(trailList)))
						);

				areaRepo.loadSimplifiedGradeCounts(reqId, sectorLookup);
				sectorLookup.values().stream().sorted((o1, o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()))
				.forEach(s -> { a.sectors().add(s); a.sectorOrder().add(new AreaSectorOrder(s.id(), s.name(), s.sorting())); });
			}
		}
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}ms", authUserId, reqId, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		return a;
	}

	@Transactional(readOnly = true)
	public Collection<Area> getAreaList(Optional<Integer> authUserId, int reqIdRegion) {
		return areaRepo.getAreaList(authUserId, reqIdRegion);
	}

	@Transactional
	public Redirect setArea(Setup s, Optional<Integer> authUserId, Area a) {
		if (a.coordinates() != null) {
			if (a.coordinates().latitude() == 0 || a.coordinates().longitude() == 0) {
				a = a.withCoordinates(null);
			} else {
				var coordMap = geoService.ensureConsistency(List.of(a.coordinates()));
				var key = a.coordinates().latitude() + "," + a.coordinates().longitude();
				var dbCoord = coordMap.get(key);
				if (dbCoord != null) a = a.withCoordinates(dbCoord);
			}
		}
		return areaRepo.setArea(s, authUserId, a);
	}
}

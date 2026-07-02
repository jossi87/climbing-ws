package com.buldreinfo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.dao.GeoRepository;
import com.buldreinfo.model.Coordinates;

@Service
public class GeoService {
	private final GeoRepository geoRepo;
	private final ElevationService elevationService;

	public GeoService(GeoRepository geoRepo, ElevationService elevationService) {
		this.geoRepo = geoRepo;
		this.elevationService = elevationService;
	}

	public int getElevationAt(double lat, double lon) {
		var coord = new Coordinates(0, lat, lon, 0, null, 0).roundTo10Digits();
		var result = resolveCoordinates(List.of(coord));
		return result.isEmpty() ? 0 : (int) Math.round(result.getFirst().elevation());
	}

	@Transactional
	public List<Coordinates> resolveCoordinates(List<Coordinates> coords) {
		var result = geoRepo.resolveCoordinates(coords);
		var missing = geoRepo.getCoordinatesMissingElevation();
		if (!missing.isEmpty()) {
			elevationService.fillElevations(missing);
			geoRepo.updateCoordinatesBatch(missing);
		}
		return result;
	}
}

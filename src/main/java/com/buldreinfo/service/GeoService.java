package com.buldreinfo.service;

import java.util.ArrayList;
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
        List<Coordinates> list = new ArrayList<>(List.of(coord));
        elevationService.fillElevations(list);
        return (int) Math.round(list.get(0).elevation());
    }

	@Transactional
	public void ensureConsistency(List<Coordinates> coords) {
		geoRepo.ensureCoordinatesInDbWithElevationAndId(coords);
		var missing = geoRepo.getCoordinatesMissingElevation();
		if (!missing.isEmpty()) {
			elevationService.fillElevations(missing);
			geoRepo.updateCoordinatesBatch(missing);
		}
	}
}
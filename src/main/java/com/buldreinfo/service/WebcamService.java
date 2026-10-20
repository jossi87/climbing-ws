package com.buldreinfo.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.model.Webcam;

/**
 * Chooses the webcam source that covers the region a request belongs to.
 * <p>
 * Vegvesen publishes Norwegian road cameras only, so regions with {@code country_code=NO} read Vegvesen. Every
 * other country reads Windy, filtered to the region's country and labelled in the region's default language.
 * The country comes from the {@code region} table, so adding a non-Norwegian region is a data change: set
 * {@code region.country_code} to its ISO 3166-1 alpha-2 code (e.g. {@code CZ}).
 */
@Service
public class WebcamService {
	private static final String VEGVESEN_COUNTRY_CODE = "NO";

	private final VegvesenService vegvesenService;
	private final WindyService windyService;

	public WebcamService(VegvesenService vegvesenService, WindyService windyService) {
		this.vegvesenService = vegvesenService;
		this.windyService = windyService;
	}

	public List<Webcam> getCameras(Setup setup) {
		String countryCode = setup.countryCode() == null ? "" : setup.countryCode().strip();
		if (countryCode.isEmpty() || VEGVESEN_COUNTRY_CODE.equalsIgnoreCase(countryCode)) {
			return vegvesenService.getCameras();
		}
		return windyService.getCameras(countryCode.toUpperCase(Locale.ROOT), setup.defaultLanguage());
	}
}

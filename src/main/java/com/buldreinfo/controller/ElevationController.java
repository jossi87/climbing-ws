package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.service.ElevationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Elevation")
@RestController
@RequestMapping("/elevation")
public class ElevationController {
	private final RequestContext requestContext;
	private final ElevationService elevationService;

	public ElevationController(RequestContext requestContext, ElevationService elevationService) {
		this.requestContext = requestContext;
		this.elevationService = elevationService;
	}

	@Operation(summary = "Get elevation by latitude and longitude")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<Integer> getElevation(
			@RequestParam(name = "latitude") double latitude,
			@RequestParam(name = "longitude") double longitude) {

		if (latitude < -90 || latitude > 90) {
			throw new ValidationFailedException("Invalid latitude: must be between -90 and 90");
		}
		if (longitude < -180 || longitude > 180) {
			throw new ValidationFailedException("Invalid longitude: must be between -180 and 180");
		}
		if (latitude == 0.0 && longitude == 0.0) {
			throw new ValidationFailedException("Invalid coordinates (0,0)");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		if (authUserId.isEmpty()) {
			throw new UnauthorizedException("Authentication required");
		}
		int elevation = GeoHelper.getElevation(elevationService, latitude, longitude);
		return ResponseEntity.ok(elevation);
	}
}

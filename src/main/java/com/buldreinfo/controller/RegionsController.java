package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.RegionOutline;
import com.buldreinfo.service.GeoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Regions")
@RestController
@RequestMapping("/regions")
public class RegionsController {
	private final RequestContext requestContext;
	private final RegionRepository regionRepo;
	private final GeoService geoService;

	public RegionsController(RequestContext requestContext, RegionRepository regionRepo, GeoService geoService) {
		this.requestContext = requestContext;
		this.regionRepo = regionRepo;
		this.geoService = geoService;
	}

	@Operation(summary = "Update the outline (polygon) of the active region (superadmin only)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> postRegionsOutline(HttpServletRequest request, @RequestBody RegionOutline body) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		var outline = body == null ? null : body.outline();
		if (outline != null && !outline.isEmpty()) {
			outline = geoService.resolveCoordinates(outline);
		}
		regionRepo.setRegionOutline(setup.idRegion(), outline);
		return ResponseEntity.ok().build();
	}
}

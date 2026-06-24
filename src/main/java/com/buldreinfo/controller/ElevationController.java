package com.buldreinfo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.service.ElevationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Elevation")
@RestController
@RequestMapping("/elevation")
public class ElevationController extends BaseController {
	private final ElevationService elevationService;

	public ElevationController(ClimbingTransactionManager txManager, RegionRepository regionRepo, ElevationService elevationService) {
		super(txManager, regionRepo);
		this.elevationService = elevationService;
	}

	@Operation(summary = "Get elevation by latitude and longitude", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = Integer.class))}),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = "Forbidden: Authentication required"),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getElevation(HttpServletRequest request,
			@Parameter(description = "Latitude (-90 to 90)", required = true) @RequestParam(name = "latitude") double latitude,
			@Parameter(description = "Longitude (-180 to 180)", required = true) @RequestParam(name = "longitude") double longitude) throws Exception {

		if (latitude < -90 || latitude > 90) {
			throw new ValidationFailedException("Invalid latitude: must be between -90 and 90");
		}
		if (longitude < -180 || longitude > 180) {
			throw new ValidationFailedException("Invalid longitude: must be between -180 and 180");
		}
		if (latitude == 0.0 && longitude == 0.0) {
			throw new ValidationFailedException("Invalid coordinates (0,0)");
		}

		return executeContextualTask(request, ctx -> {
			if (ctx.authUserId().isEmpty()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}

			int elevation = GeoHelper.getElevation(elevationService, latitude, longitude);
			return ResponseEntity.ok(String.valueOf(elevation));
		});
	}
}
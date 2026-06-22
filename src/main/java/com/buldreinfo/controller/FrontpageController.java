package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.FrontpageRepository;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Frontpage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/frontpage")
public class FrontpageController extends BaseController {
	private final FrontpageRepository frontpageRepo;

	public FrontpageController(StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, FrontpageRepository frontpageRepo, 
			RegionRepository regionRepo, UserRepository userRepo) {
		super(storage, txManager, mediaRepo, regionRepo, userRepo);
		this.frontpageRepo = frontpageRepo;
	}

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, 
					content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Frontpage.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFrontpage(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			var stats = supplyAsync(() -> frontpageRepo.getFrontpageStats(authUserId, setup));
			var randomMedia = supplyAsync(() -> frontpageRepo.getFrontpageRandomMedia(setup));
			var firstAscents = supplyAsync(() -> frontpageRepo.getFrontpageFirstAscents(authUserId, setup));
			var newestComments = supplyAsync(() -> frontpageRepo.getFrontpageNewestAscents(authUserId, setup));
			var newestMedia = supplyAsync(() -> frontpageRepo.getFrontpageNewestMedia(authUserId, setup));
			var lastComments = supplyAsync(() -> frontpageRepo.getFrontpageLastComments(authUserId, setup));

			return new Frontpage(
					stats.join(),
					randomMedia.join(),
					firstAscents.join(),
					newestComments.join(),
					newestMedia.join(),
					lastComments.join()
					);
		}));
	}
}
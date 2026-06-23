package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.FrontpageRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
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

	public FrontpageController(ClimbingTransactionManager txManager, FrontpageRepository frontpageRepo, RegionRepository regionRepo) {
		super(txManager, regionRepo);
		this.frontpageRepo = frontpageRepo;
	}

	@Operation(summary = "Get frontpage", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, 
					content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Frontpage.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Frontpage> getFrontpage(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeContextualTask(request, ctx -> {
			var stats = supplyAsync(() -> frontpageRepo.getFrontpageStats(ctx.authUserId(), ctx.setup()));
			var randomMedia = supplyAsync(() -> frontpageRepo.getFrontpageRandomMedia(ctx.setup()));
			var firstAscents = supplyAsync(() -> frontpageRepo.getFrontpageFirstAscents(ctx.authUserId(), ctx.setup()));
			var newestComments = supplyAsync(() -> frontpageRepo.getFrontpageNewestAscents(ctx.authUserId(), ctx.setup()));
			var newestMedia = supplyAsync(() -> frontpageRepo.getFrontpageNewestMedia(ctx.authUserId(), ctx.setup()));
			var lastComments = supplyAsync(() -> frontpageRepo.getFrontpageLastComments(ctx.authUserId(), ctx.setup()));

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
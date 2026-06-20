package com.buldreinfo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Activity")
@RestController
@RequestMapping("/activity")
public class ActivityController extends BaseController {
	private final ActivityRepository activityRepo;

	@Autowired
	public ActivityController(ClimbingTransactionManager txManager, ActivityRepository activityRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.activityRepo = activityRepo;
	}

	@Operation(summary = "Get activity feed", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Activity.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getActivity(HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @RequestParam(name = "idSector") int idSector,
			@Parameter(description = "Filter on lower grade") @RequestParam(name = "lowerGrade", defaultValue = "0") int lowerGrade,
			@Parameter(description = "Include first ascents") @RequestParam(name = "fa", defaultValue = "false") boolean fa,
			@Parameter(description = "Include comments") @RequestParam(name = "comments", defaultValue = "false") boolean comments,
			@Parameter(description = "Include ticks (public ascents)") @RequestParam(name = "ticks", defaultValue = "false") boolean ticks,
			@Parameter(description = "Include new media") @RequestParam(name = "media", defaultValue = "false") boolean media,
			@Parameter(description = "Offset (see more)") @RequestParam(name = "offset", defaultValue = "0") int offset) throws Exception {

		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		if (lowerGrade < 0) {
			return createBadRequestResponse("lowerGrade cannot be negative");
		}
		if (offset < 0) {
			return createBadRequestResponse("offset cannot be negative");
		}

		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			var activity = activityRepo.getActivity(setup, authUserId, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset);
			return ResponseEntity.ok(activity);
		});
	}
}
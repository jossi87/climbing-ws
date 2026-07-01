package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Activity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Activity")
@RestController
@RequestMapping("/activity")
public class ActivityController {
	private final RequestContext requestContext;
	private final ActivityRepository activityRepo;

	public ActivityController(RequestContext requestContext, ActivityRepository activityRepo) {
		this.requestContext = requestContext;
		this.activityRepo = activityRepo;
	}

	@Operation(summary = "Get activity feed")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Activity>> getActivity(HttpServletRequest request,
			@RequestParam(name = "idArea") int idArea,
			@RequestParam(name = "idSector") int idSector,
			@RequestParam(name = "lowerGrade", defaultValue = "0") int lowerGrade,
			@RequestParam(name = "fa", defaultValue = "false") boolean fa,
			@RequestParam(name = "comments", defaultValue = "false") boolean comments,
			@RequestParam(name = "ticks", defaultValue = "false") boolean ticks,
			@RequestParam(name = "media", defaultValue = "false") boolean media,
			@RequestParam(name = "offset", defaultValue = "0") int offset) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		if (lowerGrade < 0) throw new ValidationFailedException("lowerGrade cannot be negative");
		if (offset < 0) throw new ValidationFailedException("offset cannot be negative");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(activityRepo.getActivity(setup, authUserId, idArea, idSector, lowerGrade, fa, comments, ticks, media, offset));
	}
}

package com.buldreinfo.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			/**
			 * Lowest grade per grade system, formatted as `gradeSystemId:weight` (comma-separated or repeated).
			 * A threshold only applies to problems graded in its own system, so several systems can be filtered
			 * independently (e.g. a Saxon threshold does not hide Uiaa-graded problems).
			 */
			@RequestParam(name = "gradeFilters", required = false) List<String> gradeFilters,
			@RequestParam(name = "fa", defaultValue = "false") boolean fa,
			@RequestParam(name = "comments", defaultValue = "false") boolean comments,
			@RequestParam(name = "ticks", defaultValue = "false") boolean ticks,
			@RequestParam(name = "media", defaultValue = "false") boolean media,
			@RequestParam(name = "offset", defaultValue = "0") int offset) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		if (offset < 0) throw new ValidationFailedException("offset cannot be negative");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(activityRepo.getActivity(setup, authUserId, idArea, idSector, parseGradeFilters(gradeFilters), fa, comments, ticks, media, offset));
	}

	/**
	 * Parse `gradeSystemId:weight` pairs into a map keyed by grade system. Later pairs for the same system win.
	 * Every threshold must be strictly positive; `0` means “no filter” and is represented by omitting the pair.
	 * A single comma-separated value is accepted in addition to repeated parameters, so the contract does not depend
	 * on how a client (or an intermediate proxy) chooses to serialise the list.
	 */
	private static Map<Integer, Integer> parseGradeFilters(List<String> gradeFilters) {
		Map<Integer, Integer> res = new LinkedHashMap<>();
		if (gradeFilters == null) {
			return res;
		}
		for (var gradeFilter : gradeFilters) {
			if (gradeFilter == null) {
				continue;
			}
			for (var rawPair : gradeFilter.split(",")) {
				var pair = rawPair.trim();
				if (pair.isEmpty()) {
					continue;
				}
				var parts = pair.split(":", -1);
				if (parts.length != 2) {
					throw new ValidationFailedException("gradeFilters must be formatted as gradeSystemId:weight");
				}
				int gradeSystemId;
				int weight;
				try {
					gradeSystemId = Integer.parseInt(parts[0].trim());
					weight = Integer.parseInt(parts[1].trim());
				} catch (NumberFormatException _) {
					throw new ValidationFailedException("gradeFilters must be formatted as gradeSystemId:weight");
				}
				if (gradeSystemId <= 0) {
					throw new ValidationFailedException("gradeFilters gradeSystemId must be positive");
				}
				if (weight <= 0) {
					throw new ValidationFailedException("gradeFilters weight must be positive");
				}
				res.put(gradeSystemId, weight);
			}
		}
		return res;
	}
}

package com.buldreinfo.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.FrontpageRepository;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Frontpage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/frontpage")
public class FrontpageController {
	private final FrontpageRepository frontpageRepo;
	private final RequestContext requestContext;

	public FrontpageController(RequestContext requestContext, FrontpageRepository frontpageRepo) {
		this.requestContext = requestContext;
		this.frontpageRepo = frontpageRepo;
	}

	@Operation(summary = "Get frontpage")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Frontpage> getFrontpage(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var userId = requestContext.getAuthenticatedUserId();
		var statsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageStats(userId, setup));
		var randomMediaFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageRandomMedia(setup));
		var firstAscentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageFirstAscents(userId, setup));
		var newestCommentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageNewestAscents(userId, setup));
		var newestMediaFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageNewestMedia(userId, setup));
		var lastCommentsFuture = CompletableFuture.supplyAsync(() -> frontpageRepo.getFrontpageLastComments(userId, setup));
		CompletableFuture.allOf(statsFuture, randomMediaFuture, firstAscentsFuture, newestCommentsFuture, newestMediaFuture, lastCommentsFuture).join();
		return ResponseEntity.ok(new Frontpage(
				statsFuture.join(),
				randomMediaFuture.join(),
				firstAscentsFuture.join(),
				newestCommentsFuture.join(),
				newestMediaFuture.join(),
				lastCommentsFuture.join()
				));
	}
}

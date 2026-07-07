package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Frontpage;
import com.buldreinfo.service.FrontpageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/frontpage")
public class FrontpageController {
	private final FrontpageService frontpageService;
	private final RequestContext requestContext;

	public FrontpageController(RequestContext requestContext, FrontpageService frontpageService) {
		this.requestContext = requestContext;
		this.frontpageService = frontpageService;
	}

	@Operation(summary = "Get frontpage")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Frontpage> getFrontpage(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var userId = requestContext.getAuthenticatedUserId();
		if (userId.isEmpty()) {
			return ResponseEntity.ok(frontpageService.getAnonymousFrontpage(setup));
		}

		return ResponseEntity.ok(frontpageService.getAuthenticatedFrontpage(userId.get(), setup));
	}
}

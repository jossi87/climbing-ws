package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.User;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
public class UsersController {
	private final RequestContext requestContext;
	private final UserRepository userRepo;

	public UsersController(RequestContext requestContext, UserRepository userRepo) {
		this.requestContext = requestContext;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Search for user")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<User>> getUsersSearch(@RequestParam(name = "value") String value) {
		if (value == null || value.isBlank()) {
			throw new ValidationFailedException("Search keyword is required");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getUserSearch(authUserId, value));
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/ticks", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> getUsersTicks() {
		var authUserId = requestContext.getAuthenticatedUserId();
		byte[] bytes = userRepo.getUserTicks(authUserId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("Buldreinfo_BratteLinjer", StorageType.XLSX)))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(bytes);
	}

	@Operation(summary = "Update visible regions")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/regions")
	public ResponseEntity<Void> postUserRegions(@RequestParam(name = "regionId") int regionId,
			@RequestParam(name = "delete") boolean delete) {
		if (regionId <= 0) throw new ValidationFailedException("Invalid regionId=" + regionId);
		var authUserId = requestContext.getAuthenticatedUserId();
		userRepo.setUserRegion(authUserId, regionId, delete);
		return ResponseEntity.ok().build();
	}
}

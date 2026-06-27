package com.buldreinfo.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Profile;
import com.buldreinfo.model.Profile.ProfileIdentity;
import com.buldreinfo.model.ProfileAscent;
import com.buldreinfo.model.ProfileTodo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Profiles")
@RestController
@RequestMapping("/profiles")
public class ProfilesController {
	private final RequestContext requestContext;
	private final MediaRepository mediaRepo;
	private final UserRepository userRepo;

	public ProfilesController(RequestContext requestContext, MediaRepository mediaRepo, UserRepository userRepo) {
		this.requestContext = requestContext;
		this.mediaRepo = mediaRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get profile by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Profile.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Profile> getProfiles(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int reqUserId) {
		if (reqUserId <= 0) throw new ValidationFailedException("Invalid user id=" + reqUserId);
		userRepo.ensureUserExists(reqUserId);
		var setup = requestContext.getSetup(request);
		var identityFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileIdentity(setup, reqUserId));
		var kpisFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileKpis(reqUserId));
		var disciplinesFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileDisciplines(setup, reqUserId));
		return ResponseEntity.ok(new Profile(identityFuture.join(), kpisFuture.join(), disciplinesFuture.join()));
	}

	@Operation(summary = "Get profile ascents", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ProfileAscent.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/ascents", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ProfileAscent>> getProfilesAscents(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getProfileAscents(authUserId, setup, id));
	}

	@Operation(summary = "Get profile media by user id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Media.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/media", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Media>> getProfilesMedia(@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media") @RequestParam(name = "captured", defaultValue = "false") boolean captured) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(mediaRepo.getProfileMedia(authUserId, id, captured));
	}

	@Operation(summary = "Get profile todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProfileTodo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProfileTodo> getProfilesTodo(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getProfileTodo(authUserId, setup, id));
	}

	@Operation(summary = "Update profile identity", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(value = "/identity")
	public ResponseEntity<Void> postProfilesIdentity(@RequestBody ProfileIdentity profile) {
		if (profile == null) throw new ValidationFailedException("Profile identity payload is missing");
		var authUserId = requestContext.getAuthenticatedUserId();
			userRepo.setProfile(authUserId, profile);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update theme preference (light/dark)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(value = "/theme")
	public ResponseEntity<Void> postProfilesTheme(@Parameter(description = "Theme preference", required = true) @RequestParam(name = "themePreference") String themePreference) {
		if (themePreference == null || (!themePreference.equals("light") && !themePreference.equals("dark"))) {
			throw new ValidationFailedException("Invalid theme preference. Must be 'light' or 'dark'.");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
			userRepo.setThemePreference(authUserId, themePreference);
		return ResponseEntity.ok().build();
	}
}
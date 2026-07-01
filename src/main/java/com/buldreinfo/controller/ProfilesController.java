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

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Profile;
import com.buldreinfo.model.Profile.ProfileIdentity;
import com.buldreinfo.model.ProfileAscent;
import com.buldreinfo.model.ProfileTodo;

import io.swagger.v3.oas.annotations.Operation;
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

	@Operation(summary = "Get profile by id")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Profile> getProfiles(HttpServletRequest request, 
			@RequestParam(name = "id") int reqUserId) {
		if (reqUserId <= 0) throw new ValidationFailedException("Invalid user id=" + reqUserId);
		userRepo.ensureUserExists(reqUserId);
		var setup = requestContext.getSetup(request);
		var identityFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileIdentity(setup, reqUserId));
		var kpisFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileKpis(reqUserId));
		var disciplinesFuture = CompletableFuture.supplyAsync(() -> userRepo.getProfileDisciplines(setup, reqUserId));
		return ResponseEntity.ok(new Profile(identityFuture.join(), kpisFuture.join(), disciplinesFuture.join()));
	}

	@Operation(summary = "Get profile ascents")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/ascents", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ProfileAscent>> getProfilesAscents(HttpServletRequest request, 
			@RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getProfileAscents(authUserId, setup, id));
	}

	@Operation(summary = "Get profile media by user id")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/media", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Media>> getProfilesMedia(@RequestParam(name = "id") int id,
			@RequestParam(name = "captured", defaultValue = "false") boolean captured) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(mediaRepo.getProfileMedia(authUserId, id, captured));
	}

	@Operation(summary = "Get profile todo")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProfileTodo> getProfilesTodo(HttpServletRequest request, 
			@RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid user id=" + id);
		userRepo.ensureUserExists(id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getProfileTodo(authUserId, setup, id));
	}

	@Operation(summary = "Update profile identity")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/identity")
	public ResponseEntity<Void> postProfilesIdentity(@RequestBody ProfileIdentity profile) {
		if (profile == null) throw new ValidationFailedException("Profile identity payload is missing");
		var authUserId = requestContext.getAuthenticatedUserId();
		userRepo.setProfile(authUserId, profile);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update theme preference (light/dark)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/theme")
	public ResponseEntity<Void> postProfilesTheme(@RequestParam(name = "themePreference") String themePreference) {
		if (themePreference == null || (!themePreference.equals("light") && !themePreference.equals("dark"))) {
			throw new ValidationFailedException("Invalid theme preference. Must be 'light' or 'dark'.");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		userRepo.setThemePreference(authUserId, themePreference);
		return ResponseEntity.ok().build();
	}
}

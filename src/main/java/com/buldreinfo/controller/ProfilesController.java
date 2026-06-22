package com.buldreinfo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
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
public class ProfilesController extends BaseController {
	private final MediaRepository mediaRepo;
	private final UserRepository userRepo;

	public ProfilesController(ClimbingTransactionManager txManager, MediaRepository mediaRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, mediaRepo, regionRepo, userRepo);
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
	public ResponseEntity<?> getProfiles(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int reqUserId) throws Exception {
		if (reqUserId <= 0) return createBadRequestResponse("Invalid user id=" + reqUserId);

		return ResponseEntity.ok(executeSetupTask(request, setup -> {
			userRepo.ensureUserExists(reqUserId);
			var identity = supplyAsync(() -> userRepo.getProfileIdentity(setup, reqUserId));
			var kpis = supplyAsync(() -> userRepo.getProfileKpis(reqUserId));
			var disciplines = supplyAsync(() -> userRepo.getProfileDisciplines(setup, reqUserId));
			return new Profile(identity.join(), kpis.join(), disciplines.join());
		}));
	}

	@Operation(summary = "Get profile ascents", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ProfileAscent.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/ascents", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getProfilesAscents(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid user id=" + id);

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			userRepo.ensureUserExists(id);
			return userRepo.getProfileAscents(authUserId, setup, id);
		}));
	}

	@Operation(summary = "Get profile media by user id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Media.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/media", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getProfilesMedia(HttpServletRequest request,
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media") @RequestParam(name = "captured", defaultValue = "false") boolean captured) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid user id=" + id);

		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.ensureUserExists(id);
			return mediaRepo.getProfileMedia(authUserId, id, captured);
		}));
	}

	@Operation(summary = "Get profile todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProfileTodo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getProfilesTodo(HttpServletRequest request, 
			@Parameter(description = "User id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid user id=" + id);

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			userRepo.ensureUserExists(id);
			return userRepo.getProfileTodo(authUserId, setup, id);
		}));
	}

	@Operation(summary = "Update profile identity", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = "/identity")
	public ResponseEntity<?> postProfilesIdentity(HttpServletRequest request, @RequestBody ProfileIdentity profile) throws Exception {
		if (profile == null) return createBadRequestResponse("Profile identity payload is missing");

		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.setProfile(authUserId, profile);
			return null;
		}));
	}

	@Operation(summary = "Update theme preference (light/dark)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = "/theme")
	public ResponseEntity<?> postProfilesTheme(HttpServletRequest request, 
			@Parameter(description = "Theme preference", required = true) @RequestParam(name = "themePreference") String themePreference) throws Exception {
		if (themePreference == null || (!themePreference.equals("light") && !themePreference.equals("dark")))
			return createBadRequestResponse("Invalid theme preference. Must be 'light' or 'dark'.");

		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.setThemePreference(authUserId, themePreference);
			return null;
		}));
	}
}
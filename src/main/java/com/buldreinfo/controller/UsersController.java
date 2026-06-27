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
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.model.User;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

	@Operation(summary = "Search for user", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = User.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<User>> getUsersSearch(@Parameter(description = "Search keyword", required = true) @RequestParam(name = "value") String value) {
		if (value == null || value.isBlank()) {
			throw new ValidationFailedException("Search keyword is required");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getUserSearch(authUserId, value));
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/ticks", produces = OpenApiConstants.APPLICATION_XLSX)
	public ResponseEntity<byte[]> getUsersTicks() {
		var authUserId = requestContext.getAuthenticatedUserId();
		byte[] bytes = userRepo.getUserTicks(authUserId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("Buldreinfo_BratteLinjer", StorageType.XLSX)))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf(OpenApiConstants.APPLICATION_XLSX))
				.body(bytes);
	}

	@Operation(summary = "Update visible regions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping("/regions")
	public ResponseEntity<Void> postUserRegions(@Parameter(description = "Region id", required = true) @RequestParam(name = "regionId") int regionId,
			@Parameter(description = "Delete (TRUE=hide, FALSE=show)", required = true) @RequestParam(name = "delete") boolean delete) {
		if (regionId <= 0) throw new ValidationFailedException("Invalid regionId=" + regionId);
		var authUserId = requestContext.getAuthenticatedUserId();
		userRepo.setUserRegion(authUserId, regionId, delete);
		return ResponseEntity.ok().build();
	}
}
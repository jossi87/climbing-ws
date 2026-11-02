package com.buldreinfo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.excel.TickTemplate;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.AdminUser;
import com.buldreinfo.model.MergeDismissal;
import com.buldreinfo.model.TickImportResult;
import com.buldreinfo.model.UserSearchResult;
import com.buldreinfo.service.TickImportService;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
public class UsersController {
	private final RequestContext requestContext;
	private final RegionRepository regionRepo;
	private final UserRepository userRepo;
	private final TickImportService tickImportService;

	public UsersController(RequestContext requestContext, RegionRepository regionRepo, UserRepository userRepo, TickImportService tickImportService) {
		this.requestContext = requestContext;
		this.regionRepo = regionRepo;
		this.userRepo = userRepo;
		this.tickImportService = tickImportService;
	}

	@Operation(summary = "Search for user")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<UserSearchResult>> getUsersSearch(HttpServletRequest request, @RequestParam(name = "value") String value) {
		if (value == null || value.isBlank()) {
			throw new ValidationFailedException("Search keyword is required");
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(userRepo.getUserSearch(setup, authUserId, value));
	}

	@Operation(summary = "Get all users (newest first) so a superadmin can manage accounts")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<AdminUser>> getUsers(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(userRepo.getUsers(authUserId.orElseThrow()));
	}

	@Operation(summary = "Get user pairs marked as \"not merge candidates\" (superadmin)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/merge-dismissals", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<MergeDismissal>> getMergeDismissals(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(userRepo.getMergeDismissals());
	}

	@Operation(summary = "Mark two users as \"not merge candidates\" so they are no longer suggested for merging (superadmin)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/merge-dismissals")
	public ResponseEntity<Void> postMergeDismissal(HttpServletRequest request,
			@RequestParam(name = "userId1") int userId1,
			@RequestParam(name = "userId2") int userId2) {
		if (userId1 <= 0 || userId2 <= 0 || userId1 == userId2) {
			throw new ValidationFailedException("Invalid userId1=" + userId1 + "/userId2=" + userId2);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		userRepo.addMergeDismissal(userId1, userId2);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Undo marking two users as \"not merge candidates\" (superadmin)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@DeleteMapping("/merge-dismissals")
	public ResponseEntity<Void> deleteMergeDismissal(HttpServletRequest request,
			@RequestParam(name = "userId1") int userId1,
			@RequestParam(name = "userId2") int userId2) {
		if (userId1 <= 0 || userId2 <= 0 || userId1 == userId2) {
			throw new ValidationFailedException("Invalid userId1=" + userId1 + "/userId2=" + userId2);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		userRepo.removeMergeDismissal(userId1, userId2);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Merge two users (superadmin)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/merge")
	public ResponseEntity<Void> postMergeUsers(HttpServletRequest request,
			@RequestParam(name = "keepUserId") int keepUserId,
			@RequestParam(name = "deleteUserId") int deleteUserId) {
		if (keepUserId <= 0 || deleteUserId <= 0 || keepUserId == deleteUserId) {
			throw new ValidationFailedException("Invalid keepUserId=" + keepUserId + "/deleteUserId=" + deleteUserId);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		if (authUserId.isPresent() && authUserId.get() == deleteUserId) {
			throw new ValidationFailedException("You cannot merge away your own account");
		}
		regionRepo.ensureSuperadminForMergedUsers(authUserId.orElseThrow(), keepUserId, deleteUserId);
		userRepo.mergeUsers(keepUserId, deleteUserId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Rename a user (superadmin)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/name")
	public ResponseEntity<Void> postUserRename(HttpServletRequest request,
			@RequestParam(name = "userId") int userId,
			@RequestParam(name = "firstname") String firstname,
			@RequestParam(name = "lastname") String lastname) {
		if (userId <= 0) throw new ValidationFailedException("Invalid userId=" + userId);
		if (firstname == null || firstname.isBlank()) throw new ValidationFailedException("First name is required");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		regionRepo.ensureSuperadminForUser(authUserId.orElseThrow(), userId);
		userRepo.updateUserName(userId, firstname, lastname);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Get ticks (public ascents) on logged in user as Excel file (xlsx)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/ticks", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> getUsersTicks(HttpServletRequest request) {
		var authUserId = requestContext.getAuthenticatedUserId();
		byte[] bytes = userRepo.getUserTicks(authUserId, requestContext.getLanguageId(request));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("Buldreinfo_BratteLinjer", StorageType.XLSX)))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(bytes);
	}

	@Operation(summary = "Get template (xlsx) for batch registration of ticks")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/ticks/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> getUsersTicksTemplate() {
		byte[] bytes = TickTemplate.build();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("Buldreinfo_Ticks", StorageType.XLSX)))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf(StorageType.XLSX.getMimeType()))
				.body(bytes);
	}

	@Operation(summary = "Preview (dryRun=true) or import (dryRun=false) ticks from an uploaded Excel file (xlsx)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/ticks/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TickImportResult> postUsersTicksImport(@RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
			@RequestPart("file") MultipartFile file) {
		var authUserId = requestContext.getAuthenticatedUserId().orElseThrow();
		return ResponseEntity.ok(dryRun ? tickImportService.preview(authUserId, file) : tickImportService.commit(authUserId, file));
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

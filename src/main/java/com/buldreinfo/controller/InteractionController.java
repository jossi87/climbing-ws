package com.buldreinfo.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.TickRepository;
import com.buldreinfo.dao.TodoRepository;
import com.buldreinfo.dao.TrashRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.model.Comment;
import com.buldreinfo.model.DangerousArea;
import com.buldreinfo.model.PermissionUser;
import com.buldreinfo.model.RestrictionsRegion;
import com.buldreinfo.model.Search;
import com.buldreinfo.model.SearchRequest;
import com.buldreinfo.model.Tick;
import com.buldreinfo.model.Ticks;
import com.buldreinfo.model.Todo;
import com.buldreinfo.model.Top;
import com.buldreinfo.model.Trail;
import com.buldreinfo.model.Trash;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Interaction")
@RestController
public class InteractionController {
	private final RequestContext requestContext;
	private final HierarchyRepository hierarchyRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final TickRepository tickRepo;
	private final TodoRepository todoRepo;
	private final TrashRepository trashRepo;
	private final UserRepository userRepo;

	public InteractionController(RequestContext requestContext,
			HierarchyRepository hierarchyRepo,
			ProblemRepository problemRepo,
			SectorRepository sectorRepo,
			RegionRepository regionRepo,
			TickRepository tickRepo,
			TodoRepository todoRepo,
			TrashRepository trashRepo,
			UserRepository userRepo) {
		this.requestContext = requestContext;
		this.hierarchyRepo = hierarchyRepo;
		this.problemRepo = problemRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.tickRepo = tickRepo;
		this.todoRepo = todoRepo;
		this.trashRepo = trashRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get boulders/routes marked as dangerous", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/dangerous", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<DangerousArea>> getDangerous(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getDangerous(setup, authUserId));
	}

	@Operation(summary = "Get permissions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<PermissionUser>> getPermissions(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(userRepo.getPermissions(setup, authUserId));
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/restrictions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<RestrictionsRegion>> getRestrictions(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getRestrictions(setup, authUserId));
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Ticks.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/ticks", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Ticks> getTicks(HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 1 returns first page)", required = false) @RequestParam(name = "page", defaultValue = "0") int page
			) {
		if (page < 1) throw new ValidationFailedException("Invalid page index (must be >= 1)");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(tickRepo.getTicks(authUserId, setup, page));
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Todo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Todo> getTodo(@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector
			) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(todoRepo.getTodo(authUserId, idArea, idSector));
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Top.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/top", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Top> getTop(@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector
			) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getTop(authUserId, idArea, idSector));
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/trash", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Trash>> getTrash(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(trashRepo.getTrash(authUserId, setup));
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Integer> postComments(HttpServletRequest request, @RequestBody Comment co) {
		if (co == null || co.idProblem() <= 0 || (!co.delete() && (co.comment() == null || co.comment().strip().isEmpty()))) {
			throw new ValidationFailedException("Comment payload invalid");
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(problemRepo.upsertComment(authUserId, setup, co));
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping("/permissions")
	public ResponseEntity<Void> postPermissions(HttpServletRequest request, @RequestBody PermissionUser u) {
		if (u == null || u.userId() <= 0) throw new ValidationFailedException("Invalid userId");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		userRepo.upsertPermissionUser(setup, u);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Search>> postSearch(HttpServletRequest request, @RequestBody SearchRequest sr) {
		if (sr == null || sr.value() == null || sr.value().strip().isEmpty()) throw new ValidationFailedException("Search criteria missing");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getSearch(setup, authUserId, sr.value().trim()));
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping("/ticks")
	public ResponseEntity<Void> postTicks(HttpServletRequest request, @RequestBody Tick t) {
		if (t == null || t.idProblem() <= 0) throw new ValidationFailedException("Invalid idProblem");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		tickRepo.setTick(setup, authUserId, t);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping("/todo")
	public ResponseEntity<Void> postTodo(@RequestParam(name = "idProblem") int idProblem) {
		if (idProblem <= 0) throw new ValidationFailedException("Invalid idProblem");
		var authUserId = requestContext.getAuthenticatedUserId();
		todoRepo.toggleTodo(authUserId, idProblem);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(value = "/trails", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> postTrails(@RequestBody List<Trail> trails) {
		if (trails == null || trails.isEmpty()) throw new ValidationFailedException("Trails empty");
		var authUserId = requestContext.getAuthenticatedUserId();
		sectorRepo.upsertTrails(authUserId, trails);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PutMapping("/trash")
	public ResponseEntity<Void> putTrash(HttpServletRequest request,
			@RequestParam(name = "idArea", defaultValue = "0") int idArea,
			@RequestParam(name = "idSector", defaultValue = "0") int idSector,
			@RequestParam(name = "idProblem", defaultValue = "0") int idProblem,
			@RequestParam(name = "idMedia", defaultValue = "0") int idMedia
			) {
		boolean isValid = (idArea > 0 && idSector == 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0 && idMedia > 0);
		if (!isValid) throw new ValidationFailedException("Invalid arguments");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
		trashRepo.trashRecover(idArea, idSector, idProblem, idMedia);
		return ResponseEntity.ok().build();
	}
}
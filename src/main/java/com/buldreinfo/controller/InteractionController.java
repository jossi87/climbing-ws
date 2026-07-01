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

import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.TodoRepository;
import com.buldreinfo.dao.TrashRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.ValidationFailedException;
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
import com.buldreinfo.service.TickService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Interaction")
@RestController
public class InteractionController {
	private final RequestContext requestContext;
	private final TickService tickService;
	private final HierarchyRepository hierarchyRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final TodoRepository todoRepo;
	private final TrashRepository trashRepo;
	private final UserRepository userRepo;

	public InteractionController(RequestContext requestContext,
			TickService tickService,
			HierarchyRepository hierarchyRepo,
			ProblemRepository problemRepo,
			SectorRepository sectorRepo,
			RegionRepository regionRepo,
			TodoRepository todoRepo,
			TrashRepository trashRepo,
			UserRepository userRepo) {
		this.requestContext = requestContext;
		this.tickService = tickService;
		this.hierarchyRepo = hierarchyRepo;
		this.problemRepo = problemRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.todoRepo = todoRepo;
		this.trashRepo = trashRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get boulders/routes marked as dangerous")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/dangerous", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<DangerousArea>> getDangerous(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getDangerous(setup, authUserId));
	}

	@Operation(summary = "Get permissions")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<PermissionUser>> getPermissions(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(userRepo.getPermissions(setup, authUserId));
	}

	@Operation(summary = "Get areas and sectors with restrictions")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/restrictions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<RestrictionsRegion>> getRestrictions(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getRestrictions(setup, authUserId));
	}

	@Operation(summary = "Get ticks (public ascents)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/ticks", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Ticks> getTicks(HttpServletRequest request,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		if (page < 1) throw new ValidationFailedException("Invalid page index (must be >= 1)");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(tickService.getTicks(authUserId, setup, page));
	}

	@Operation(summary = "Get todo on Area/Sector")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Todo> getTodo(@RequestParam(name = "idArea") int idArea,
			@RequestParam(name = "idSector") int idSector) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(todoRepo.getTodo(authUserId, idArea, idSector));
	}

	@Operation(summary = "Get top on Area/Sector")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/top", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Top> getTop(@RequestParam(name = "idArea") int idArea,
			@RequestParam(name = "idSector") int idSector) {
		if (idArea < 0 || idSector < 0) throw new ValidationFailedException("IDs cannot be negative");
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getTop(authUserId, idArea, idSector));
	}

	@Operation(summary = "Get trash")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/trash", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Trash>> getTrash(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		return ResponseEntity.ok(trashRepo.getTrash(authUserId, setup));
	}

	@Operation(summary = "Update comment")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Integer> postComments(HttpServletRequest request, @RequestBody Comment co) {
		if (co == null || co.idProblem() <= 0 || (!co.delete() && (co.comment() == null || co.comment().strip().isEmpty()))) {
			throw new ValidationFailedException("Comment payload invalid");
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(problemRepo.upsertComment(authUserId, setup, co));
	}

	@Operation(summary = "Update user privileges")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/permissions")
	public ResponseEntity<Void> postPermissions(HttpServletRequest request, @RequestBody PermissionUser u) {
		if (u == null || u.userId() <= 0) throw new ValidationFailedException("Invalid userId");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		userRepo.upsertPermissionUser(setup, u);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Search for area/sector/problem/user")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Search>> postSearch(HttpServletRequest request, @RequestBody SearchRequest sr) {
		if (sr == null || sr.value() == null || sr.value().strip().isEmpty()) throw new ValidationFailedException("Search criteria missing");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getSearch(setup, authUserId, sr.value().trim()));
	}

	@Operation(summary = "Update tick (public ascent)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/ticks")
	public ResponseEntity<Void> postTicks(HttpServletRequest request, @RequestBody Tick t) {
		if (t == null || t.idProblem() <= 0) throw new ValidationFailedException("Invalid idProblem");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		tickService.setTick(setup, authUserId, t);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Update todo")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping("/todo")
	public ResponseEntity<Void> postTodo(@RequestParam(name = "idProblem") int idProblem) {
		if (idProblem <= 0) throw new ValidationFailedException("Invalid idProblem");
		var authUserId = requestContext.getAuthenticatedUserId();
		todoRepo.toggleTodo(authUserId, idProblem);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Upsert trails")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/trails", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> postTrails(@RequestBody List<Trail> trails) {
		if (trails == null || trails.isEmpty()) throw new ValidationFailedException("Trails empty");
		var authUserId = requestContext.getAuthenticatedUserId();
		sectorRepo.upsertTrails(authUserId, trails);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PutMapping("/trash")
	public ResponseEntity<Void> putTrash(HttpServletRequest request,
			@RequestParam(name = "idArea", defaultValue = "0") int idArea,
			@RequestParam(name = "idSector", defaultValue = "0") int idSector,
			@RequestParam(name = "idProblem", defaultValue = "0") int idProblem,
			@RequestParam(name = "idMedia", defaultValue = "0") int idMedia) {
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

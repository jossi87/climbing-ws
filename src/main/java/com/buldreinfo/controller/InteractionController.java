package com.buldreinfo.controller;

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
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.TickRepository;
import com.buldreinfo.dao.TodoRepository;
import com.buldreinfo.dao.TrashRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.io.StorageManager;
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
public class InteractionController extends BaseController {
	private final HierarchyRepository hierarchyRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final TickRepository tickRepo;
	private final TodoRepository todoRepo;
	private final TrashRepository trashRepo;
	private final UserRepository userRepo;

	public InteractionController(StorageManager storage,
			ClimbingTransactionManager txManager,
			HierarchyRepository hierarchyRepo,
			MediaRepository mediaRepo,
			ProblemRepository problemRepo,
			SectorRepository sectorRepo,
			RegionRepository regionRepo,
			TickRepository tickRepo,
			TodoRepository todoRepo,
			TrashRepository trashRepo,
			UserRepository userRepo) {
		super(storage, txManager, mediaRepo, regionRepo, userRepo);
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
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/dangerous", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getDangerous(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> hierarchyRepo.getDangerous(setup, authUserId)));
	}

	@Operation(summary = "Get permissions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getPermissions(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			regionRepo.ensureAdminWriteRegion(setup, authUserId);
			return userRepo.getPermissions(setup, authUserId);
		}));
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/restrictions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getRestrictions(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> hierarchyRepo.getRestrictions(setup, authUserId)));
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Ticks.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/ticks", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTicks(HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 0 returns first page)", required = false) @RequestParam(name = "page", defaultValue = "0") int page
			) throws Exception {
		if (page < 1) return createBadRequestResponse("Invalid page index (must be >= 1)");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> tickRepo.getTicks(authUserId, setup, page)));
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Todo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/todo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTodo(HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector
			) throws Exception {
		if (idArea < 0 || idSector < 0) return createBadRequestResponse("IDs cannot be negative");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> todoRepo.getTodo(authUserId, setup, idArea, idSector)));
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Top.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/top", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTop(HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector
			) throws Exception {
		if (idArea < 0 || idSector < 0) return createBadRequestResponse("IDs cannot be negative");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> hierarchyRepo.getTop(authUserId, idArea, idSector)));
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/trash", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTrash(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			regionRepo.ensureAdminWriteRegion(setup, authUserId);
			return trashRepo.getTrash(authUserId, setup);
		}));
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> postComments(HttpServletRequest request, @RequestBody Comment co) throws Exception {
		if (co == null || co.idProblem() <= 0 || (!co.delete() && (co.comment() == null || co.comment().strip().isEmpty()))) {
			return createBadRequestResponse("Comment payload invalid");
		}
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> problemRepo.upsertComment(authUserId, setup, co)));
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping("/permissions")
	public ResponseEntity<?> postPermissions(HttpServletRequest request, @RequestBody PermissionUser u) throws Exception {
		if (u == null || u.userId() <= 0) return createBadRequestResponse("Invalid userId");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			regionRepo.ensureAdminWriteRegion(setup, authUserId);
			userRepo.upsertPermissionUser(setup, u);
			return null;
		}));
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> postSearch(HttpServletRequest request, @RequestBody SearchRequest sr) throws Exception {
		if (sr == null || sr.value() == null || sr.value().strip().isEmpty()) return createBadRequestResponse("Search criteria missing");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> hierarchyRepo.getSearch(setup, authUserId, sr.value().trim())));
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping("/ticks")
	public ResponseEntity<?> postTicks(HttpServletRequest request, @RequestBody Tick t) throws Exception {
		if (t == null || t.idProblem() <= 0) return createBadRequestResponse("Invalid idProblem");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			tickRepo.setTick(setup, authUserId, t);
			return null;
		}));
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping("/todo")
	public ResponseEntity<?> postTodo(HttpServletRequest request, @RequestParam(name = "idProblem") int idProblem) throws Exception {
		if (idProblem <= 0) return createBadRequestResponse("Invalid idProblem");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			todoRepo.toggleTodo(authUserId, idProblem);
			return null;
		}));
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = "/trails", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> postTrails(HttpServletRequest request, @RequestBody List<Trail> trails) throws Exception {
		if (trails == null || trails.isEmpty()) return createBadRequestResponse("Trails empty");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			sectorRepo.upsertTrails(authUserId, trails);
			return null;
		}));
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping("/trash")
	public ResponseEntity<?> putTrash(HttpServletRequest request,
			@RequestParam(name = "idArea", defaultValue = "0") int idArea,
			@RequestParam(name = "idSector", defaultValue = "0") int idSector,
			@RequestParam(name = "idProblem", defaultValue = "0") int idProblem,
			@RequestParam(name = "idMedia", defaultValue = "0") int idMedia
			) throws Exception {
		boolean isValid = (idArea > 0 && idSector == 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0 && idMedia > 0);
		if (!isValid) return createBadRequestResponse("Invalid arguments");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			regionRepo.ensureSuperadminWriteRegion(setup, authUserId);
			trashRepo.trashRecover(idArea, idSector, idProblem, idMedia);
			return null;
		}));
	}
}
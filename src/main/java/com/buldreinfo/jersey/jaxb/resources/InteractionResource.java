package com.buldreinfo.jersey.jaxb.resources;

import java.util.Collection;
import java.util.List;

import com.buldreinfo.jersey.jaxb.dao.HierarchyRepository;
import com.buldreinfo.jersey.jaxb.dao.ProblemRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.SectorRepository;
import com.buldreinfo.jersey.jaxb.dao.TickRepository;
import com.buldreinfo.jersey.jaxb.dao.TodoRepository;
import com.buldreinfo.jersey.jaxb.dao.TrashRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.DangerousArea;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.RestrictionsRegion;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Trail;
import com.buldreinfo.jersey.jaxb.model.Trash;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Interaction")
public class InteractionResource extends BaseResource {
	private final HierarchyRepository hierarchyRepo;
	private final ProblemRepository problemRepo;
	private final SectorRepository sectorRepo;
	private final TickRepository tickRepo;
	private final TodoRepository todoRepo;
	private final TrashRepository trashRepo;
	private final UserRepository userRepo;

	@Inject
	public InteractionResource(TransactionManager txManager,
			HierarchyRepository hierarchyRepo,
			ProblemRepository problemRepo,
			SectorRepository sectorRepo,			
			RegionRepository regionRepo,
			TickRepository tickRepo,
			TodoRepository todoRepo,
			TrashRepository trashRepo,
			UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.hierarchyRepo = hierarchyRepo;
		this.problemRepo = problemRepo;
		this.sectorRepo = sectorRepo;
		this.tickRepo = tickRepo;
		this.todoRepo = todoRepo;
		this.trashRepo = trashRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get boulders/routes marked as dangerous", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = DangerousArea.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/dangerous")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDangerous(@Context HttpServletRequest request) throws Exception {
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			var res = hierarchyRepo.getDangerous(setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get permissions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = PermissionUser.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@Context HttpServletRequest request) throws Exception {
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			List<PermissionUser> res = userRepo.getPermissions(setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get areas and sectors with restrictions", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RestrictionsRegion.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/restrictions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRestrictions(@Context HttpServletRequest request) throws Exception {
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			Collection<RestrictionsRegion> res = hierarchyRepo.getRestrictions(setup, authUserId);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get ticks (public ascents)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Ticks.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTicks(@Context HttpServletRequest request,
			@Parameter(description = "Page (ticks ordered descending, 0 returns first page)", required = false) @QueryParam("page") int page
			) throws Exception {
		if (page < 1) {
			return createBadRequestResponse("Invalid page index (must be >= 1)");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			Ticks res = tickRepo.getTicks(authUserId, setup, page);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get todo on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Todo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTodo(@Context HttpServletRequest request,
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) throws Exception {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			Todo res = todoRepo.getTodo(authUserId, setup, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get top on Area/Sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Top.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/top")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTop(@Context HttpServletRequest request, 
			@Parameter(description = "Area id (can be 0 if idSector>0)", required = true) @QueryParam("idArea") int idArea,
			@Parameter(description = "Sector id (can be 0 if idArea>0)", required = true) @QueryParam("idSector") int idSector
			) throws Exception {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			Top res = hierarchyRepo.getTop(authUserId, idArea, idSector);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get trash", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Trash.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/trash")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrash(@Context HttpServletRequest request) throws Exception {
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			List<Trash> res = trashRepo.getTrash(authUserId, setup);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update comment", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/comments")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postComments(@Context HttpServletRequest request, Comment co) throws Exception {
		if (co == null || co.idProblem() <= 0 || co.comment() == null || co.comment().strip().isEmpty()) {
			return createBadRequestResponse("Comment payload contains invalid fields or empty body");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			int idGuestbook = problemRepo.upsertComment(authUserId, setup, co);
			return Response.ok(idGuestbook).build();
		});
	}

	@Operation(summary = "Update user privileges", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) throws Exception {
		if (u == null || u.userId() <= 0) {
			return createBadRequestResponse("Invalid or missing userId payload");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			userRepo.upsertPermissionUser(setup, authUserId, u);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Search for area/sector/problem/user", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Search.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) throws Exception {
		if (sr == null || sr.value() == null || sr.value().strip().isEmpty()) {
			return createBadRequestResponse("Search criteria keyword is missing");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			String search = sr.value().trim();
			var res = hierarchyRepo.getSearch(setup, authUserId, search);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update tick (public ascent)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) throws Exception {
		if (t == null || t.idProblem() <= 0) {
			return createBadRequestResponse("Invalid or missing idProblem payload");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			tickRepo.setTick(setup, authUserId, t);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/todo")
	public Response postTodo(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @QueryParam("idProblem") int idProblem
			) throws Exception {
		if (idProblem <= 0) {
			return createBadRequestResponse("Invalid idProblem=" + idProblem);
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			todoRepo.toggleTodo(authUserId, idProblem);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Upsert trails", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/trails")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTrails(@Context HttpServletRequest request, List<Trail> trails) throws Exception {
		if (trails == null || trails.isEmpty()) {
			return createBadRequestResponse("Trails collection payload is missing or empty");
		}
		for (Trail t : trails) {
			if (t == null) {
				return createBadRequestResponse("Trail cannot be null");
			}
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			sectorRepo.upsertTrails(authUserId, trails);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Move Area/Sector/Problem/Media to trash (only one of the arguments must be different from 0)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PUT
	@Path("/trash")
	public Response putTrash(@Context HttpServletRequest request,
			@QueryParam("idArea") int idArea,
			@QueryParam("idSector") int idSector,
			@QueryParam("idProblem") int idProblem,
			@QueryParam("idMedia") int idMedia
			) throws Exception {
		boolean isValidSelection = (idArea > 0 && idSector == 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector > 0 && idProblem == 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem > 0 && idMedia == 0) ||
				(idArea == 0 && idSector == 0 && idProblem == 0 && idMedia > 0);
		if (!isValidSelection) {
			return createBadRequestResponse("Invalid arguments. Exactly one operational identifier target greater than zero must be specified.");
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			trashRepo.trashRecover(setup, authUserId, idArea, idSector, idProblem, idMedia);
			return Response.ok().build();
		});
	}
}

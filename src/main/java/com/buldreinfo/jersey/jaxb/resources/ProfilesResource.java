package com.buldreinfo.jersey.jaxb.resources;

import java.util.List;

import com.buldreinfo.jersey.jaxb.dao.MediaRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Profile.ProfileIdentity;
import com.buldreinfo.jersey.jaxb.model.ProfileAscent;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;

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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Profiles")
@Path("/profiles")
public class ProfilesResource extends BaseResource {
	private final MediaRepository mediaRepo;
	private final UserRepository userRepo;

	@Inject
	public ProfilesResource(TransactionManager txManager, MediaRepository mediaRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.mediaRepo = mediaRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get profile by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Profile.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfiles(@Context HttpServletRequest request, @Parameter(description = "User id", required = true) @QueryParam("id") int reqUserId) throws Exception {
		if (reqUserId <= 0) {
			return createBadRequestResponse("Invalid user id=" + reqUserId);
		}
		return executeSetupTask(request, setup -> {
			userRepo.ensureUserExists(reqUserId);
			var identity = supplyAsync(() -> userRepo.getProfileIdentity(setup, reqUserId));
			var kpis = supplyAsync(() -> userRepo.getProfileKpis(reqUserId));
			var disciplines = supplyAsync(() -> userRepo.getProfileDisciplines(setup, reqUserId));
			Profile res = new Profile(identity.join(), kpis.join(), disciplines.join());
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile ascents", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = ProfileAscent.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/ascents")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfilesAscents(@Context HttpServletRequest request, @Parameter(description = "User id", required = true) @QueryParam("id") int id) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid user id=" + id);
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			userRepo.ensureUserExists(id);
			List<ProfileAscent> res = userRepo.getProfileAscents(authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile media by user id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Media.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/media")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfilesMedia(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id,
			@Parameter(description = "FALSE = tagged media, TRUE = captured media", required = false) @QueryParam("captured") boolean captured
			) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid user id=" + id);
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.ensureUserExists(id);
			List<Media> res = mediaRepo.getProfileMedia(authUserId, id, captured);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Get profile todo", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProfileTodo.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProfilesTodo(@Context HttpServletRequest request,
			@Parameter(description = "User id", required = true) @QueryParam("id") int id) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid user id=" + id);
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			userRepo.ensureUserExists(id);
			ProfileTodo res = userRepo.getProfileTodo(authUserId, setup, id);
			return Response.ok().entity(res).build();
		});
	}

	@Operation(summary = "Update profile identity", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/identity")
	public Response postProfilesIdentity(@Context HttpServletRequest request, ProfileIdentity profile) throws Exception {
		if (profile == null) {
			return createBadRequestResponse("Profile identity payload is missing");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.setProfile(authUserId, profile);
			return Response.ok().build();
		});
	}

	@Operation(summary = "Update theme preference (light/dark)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("/theme")
	public Response postProfilesTheme(@Context HttpServletRequest request,
			@Parameter(description = "Theme preference (light or dark)", required = true) @QueryParam("themePreference") String themePreference) throws Exception {
		if (themePreference == null || (!themePreference.equals("light") && !themePreference.equals("dark"))) {
			return createBadRequestResponse("Invalid theme preference. Must be 'light' or 'dark'.");
		}
		return executeAuthenticatedTask(request, (_, authUserId) -> {
			userRepo.setThemePreference(authUserId, themePreference);
			return Response.ok().build();
		});
	}
}
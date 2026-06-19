package com.buldreinfo.jersey.jaxb.resources;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.dao.AreaRepository;
import com.buldreinfo.jersey.jaxb.dao.HierarchyRepository;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.SectorRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;

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
import jakarta.ws.rs.core.StreamingOutput;

@Tag(name = "Sectors")
@Path("/sectors")
public class SectorsResource extends BaseResource {
	private static Logger logger = LogManager.getLogger();
	private final AreaRepository areaRepo;
	private final HierarchyRepository hierarchyRepo;
	private final SectorRepository sectorRepo;

	@Inject
	public SectorsResource(TransactionManager txManager,
			AreaRepository areaRepo,
			HierarchyRepository hierarchyRepo,
			RegionRepository regionRepo,
			SectorRepository sectorRepo,
			UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.areaRepo = areaRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.sectorRepo = sectorRepo;
	}

	@Operation(summary = "Get sector by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Sector.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSectors(@Context HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @QueryParam("id") int id) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			final boolean orderByGrade = setup.isBouldering();
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			Sector s = sectorRepo.getSector(authUserId, orderByGrade, setup, id, shouldUpdateHits);
			return Response.ok().entity(s).build();
		});
	}

	@Operation(summary = "Get sector PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_PDF, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GET
	@Path("/pdf")
	@Produces(OpenApiConstants.APPLICATION_PDF)
	public Response getSectorsPdf(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @QueryParam("id") int id) throws Exception {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			final Sector sector = sectorRepo.getSector(authUserId, false, setup, id, shouldUpdateHits);
			final Collection<GradeDistribution> gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, 0, id);
			final Area area = areaRepo.getArea(setup, authUserId, sector.areaId(), shouldUpdateHits);
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) {
					try (PdfGenerator generator = new PdfGenerator(output)) {
						generator.writeArea(setup, area, gradeDistribution, List.of(sector));
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			};
			return Response.ok(stream)
					.type(OpenApiConstants.APPLICATION_PDF)
					.header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(sector.name(), "pdf")))
					.header("Access-Control-Expose-Headers", "Content-Disposition")
					.build();
		});
	}

	@Operation(summary = "Update sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSectors(@Context HttpServletRequest request, Sector s) throws Exception {
		if (s == null || s.name() == null || s.name().strip().isEmpty()) {
			return createBadRequestResponse("Sector name is missing or invalid");
		}
		if (s.areaId() <= 0) {
			return createBadRequestResponse("Invalid areaId=" + s.areaId());
		}
		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			Redirect res = sectorRepo.setSector(authUserId, setup, s);
			return Response.ok().entity(res).build();
		});
	}
}
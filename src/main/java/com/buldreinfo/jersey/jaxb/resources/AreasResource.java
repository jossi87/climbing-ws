package com.buldreinfo.jersey.jaxb.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

@Tag(name = "Areas")
@Path("/areas")
public class AreasResource extends BaseResource {
    private static final Logger logger = LogManager.getLogger();
    private final AreaRepository areaRepo;
    private final SectorRepository sectorRepo;
    private final HierarchyRepository hierarchyRepo;

    @Inject
    public AreasResource(TransactionManager txManager, RegionRepository regionRepo, UserRepository userRepo, AreaRepository areaRepo, SectorRepository sectorRepo, HierarchyRepository hierarchyRepo) {
        super(txManager, regionRepo, userRepo);
        this.areaRepo = areaRepo;
        this.sectorRepo = sectorRepo;
        this.hierarchyRepo = hierarchyRepo;
    }

    @Operation(summary = "Get areas", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Area.class)))}),
            @ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAreas(@Context HttpServletRequest request,
            @Parameter(description = "Area id", required = false) @QueryParam("id") int id) throws Exception {
        if (id < 0) {
        	return createBadRequestResponse("Invalid id=" + id);
        }
        return executeAuthenticatedTask(request, (setup, authUserId) -> {
            boolean shouldUpdateHits = isHitTrackingEnabled(request);
            var res = id > 0 ? Collections.singleton(areaRepo.getArea(setup, authUserId, id, shouldUpdateHits)) : areaRepo.getAreaList(authUserId, setup.idRegion());
            return Response.ok().entity(res).build();
        });
    }

    @Operation(summary = "Get area PDF by id", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_PDF, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
            @ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GET
    @Path("/pdf")
    @Produces(OpenApiConstants.APPLICATION_PDF)
    public Response getAreasPdf(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @QueryParam("id") int id) throws Exception {
        if (id <= 0) {
        	return createBadRequestResponse("Invalid area id=" + id);
        }
        return executeAuthenticatedTask(request, (setup, authUserId) -> {
            boolean shouldUpdateHits = isHitTrackingEnabled(request);
            Area area = areaRepo.getArea(setup, authUserId, id, shouldUpdateHits);
            Collection<GradeDistribution> gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, area.id(), 0);
            List<Sector> sectors = new ArrayList<>();
            for (Area.AreaSector sector : area.sectors()) {
                sectors.add(sectorRepo.getSector(authUserId, false, setup, sector.id(), shouldUpdateHits));
            }

            StreamingOutput stream = output -> {
                try (PdfGenerator generator = new PdfGenerator(output)) {
                    generator.writeArea(setup, area, gradeDistribution, sectors);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            };

            return Response.ok(stream)
                    .type(OpenApiConstants.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(area.name(), "pdf")))
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .build();
        });
    }

    @Operation(summary = "Update area", responses = {
            @ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Redirect.class))}),
            @ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
            @ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postAreas(@Context HttpServletRequest request, Area a) throws Exception {
        if (a == null || a.name() == null || a.name().strip().isEmpty()) {
            return createBadRequestResponse("Area name is missing or invalid");
        }
        return executeAuthenticatedTask(request, (setup, authUserId) -> {
        	var res = areaRepo.setArea(setup, authUserId, a);
        	return Response.ok().entity(res).build();
        });
    }
}
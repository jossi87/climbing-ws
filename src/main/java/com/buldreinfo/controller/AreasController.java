package com.buldreinfo.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.GradeDistribution;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Areas")
@RestController
@RequestMapping("/areas")
public class AreasController extends BaseController {
	private static final Logger logger = LogManager.getLogger();
	private final StorageManager storage;
	private final AreaRepository areaRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final HierarchyRepository hierarchyRepo;

	public AreasController(StorageManager storage, ClimbingTransactionManager txManager, RegionRepository regionRepo, AreaRepository areaRepo, SectorRepository sectorRepo, HierarchyRepository hierarchyRepo) {
		super(txManager, regionRepo);
		this.storage = storage;
		this.areaRepo = areaRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.hierarchyRepo = hierarchyRepo;
	}

	@Operation(summary = "Get areas", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Area.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<Area>> getAreas(HttpServletRequest request,
			@Parameter(description = "Area id", required = false) @RequestParam(name = "id", defaultValue = "0") int id) throws Exception {
		if (id < 0) {
			throw new ValidationFailedException("Invalid id=" + id);
		}
		return executeContextualTask(request, ctx -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			var res = id > 0 ? Collections.singleton(areaRepo.getArea(ctx.setup(), ctx.authUserId(), id, shouldUpdateHits)) : areaRepo.getAreaList(ctx.authUserId(), ctx.setup().idRegion());
			return ResponseEntity.ok(res);
		});
	}

	@Operation(summary = "Get area PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_PDF_VALUE, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<StreamingResponseBody> getAreasPdf(HttpServletRequest request, @Parameter(description = "Area id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) {
			throw new ValidationFailedException("Invalid area id=" + id);
		}
		StreamingResponseBody stream = executeContextualTask(request, ctx -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			Area area = areaRepo.getArea(ctx.setup(), ctx.authUserId(), id, shouldUpdateHits);
			Collection<GradeDistribution> gradeDistribution = hierarchyRepo.getGradeDistribution(ctx.authUserId(), area.id(), 0);
			List<Sector> sectors = new ArrayList<>();
			for (Area.AreaSector sector : area.sectors()) {
				sectors.add(sectorRepo.getSector(ctx.authUserId(), false, ctx.setup(), sector.id(), shouldUpdateHits));
			}
			return (StreamingResponseBody) output -> {
				try (PdfGenerator generator = new PdfGenerator(storage, output)) {
					generator.writeArea(ctx.setup(), area, gradeDistribution, sectors);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e.getMessage(), e);
				}
			};
		});
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"area.pdf\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_PDF)
				.body(stream);
	}

	@Operation(summary = "Update area", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Redirect> postAreas(HttpServletRequest request, @RequestBody Area a) throws Exception {
		if (a == null || a.name() == null || a.name().strip().isEmpty()) {
			throw new ValidationFailedException("Area name is missing or invalid");
		}
		return executeContextualTask(request, ctx -> {
			regionRepo.ensureAdminWriteRegion(ctx.setup(), ctx.authUserId());
			var res = areaRepo.setArea(ctx.setup(), ctx.authUserId(), a);
			return ResponseEntity.ok(res);
		});
	}
}
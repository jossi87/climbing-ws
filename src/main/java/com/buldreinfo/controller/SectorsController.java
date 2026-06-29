package com.buldreinfo.controller;

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

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Sectors")
@RestController
@RequestMapping("/sectors")
public class SectorsController {
	private final RequestContext requestContext;
	private static final Logger logger = LogManager.getLogger();
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;
	private final AreaRepository areaRepo;
	private final HierarchyRepository hierarchyRepo;
	private final SectorRepository sectorRepo;

	public SectorsController(RequestContext requestContext,
			StorageManager storage,
			LeafletPrintService leafletPrintService,
			AreaRepository areaRepo,
			HierarchyRepository hierarchyRepo,
			SectorRepository sectorRepo) {
		this.requestContext = requestContext;
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
		this.areaRepo = areaRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.sectorRepo = sectorRepo;
	}

	@Operation(summary = "Get sector by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Sector.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Sector> getSectors(HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var shouldUpdateHits = requestContext.isHitTrackingEnabled(request);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(sectorRepo.getSector(authUserId, setup.isBouldering(), setup, id, shouldUpdateHits));
	}

	@Operation(summary = "Get sector PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_PDF_VALUE)}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<StreamingResponseBody> getSectorsPdf(HttpServletRequest request, @Parameter(description = "Sector id", required = true) @RequestParam(name = "id") int id) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		var shouldUpdateHits = requestContext.isHitTrackingEnabled(request);
		final Sector sector = sectorRepo.getSector(authUserId, false, setup, id, shouldUpdateHits);
		final var gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, 0, id);
		final Area area = areaRepo.getArea(setup, authUserId, sector.areaId(), shouldUpdateHits);
		String filename = FilenameUtil.generateFilename(sector.name(), StorageType.PDF);
		StreamingResponseBody stream = output -> {
			try (PdfGenerator generator = new PdfGenerator(storage, leafletPrintService, output)) {
				generator.writeArea(setup, area, gradeDistribution, List.of(sector));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_PDF)
				.body(stream);
	}

	@Operation(summary = "Update sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Redirect> postSectors(HttpServletRequest request, @RequestBody Sector s) {
		if (s == null || s.name() == null || s.name().strip().isEmpty()) throw new ValidationFailedException("Sector name invalid");
		if (s.areaId() <= 0) throw new ValidationFailedException("Invalid areaId=" + s.areaId());
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(sectorRepo.setSector(authUserId, setup, s));
	}
}
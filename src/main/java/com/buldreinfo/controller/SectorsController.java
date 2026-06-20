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

import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;

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
public class SectorsController extends BaseController {
	private static final Logger logger = LogManager.getLogger();
	private final AreaRepository areaRepo;
	private final HierarchyRepository hierarchyRepo;
	private final SectorRepository sectorRepo;

	public SectorsController(ClimbingTransactionManager txManager,
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
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Sector.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getSectors(HttpServletRequest request,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			return sectorRepo.getSector(authUserId, setup.isBouldering(), setup, id, shouldUpdateHits);
		}));
	}

	@Operation(summary = "Get sector PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_PDF)}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/pdf", produces = OpenApiConstants.APPLICATION_PDF)
	public ResponseEntity<StreamingResponseBody> getSectorsPdf(HttpServletRequest request, 
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);

		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			final Sector sector = sectorRepo.getSector(authUserId, false, setup, id, shouldUpdateHits);
			final var gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, 0, id);
			final Area area = areaRepo.getArea(setup, authUserId, sector.areaId(), shouldUpdateHits);

			StreamingResponseBody stream = output -> {
				try (PdfGenerator generator = new PdfGenerator(output)) {
					generator.writeArea(setup, area, gradeDistribution, List.of(sector));
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e.getMessage(), e);
				}
			};

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(sector.name(), "pdf")))
					.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
					.contentType(MediaType.parseMediaType(OpenApiConstants.APPLICATION_PDF))
					.body(stream);
		});
	}

	@Operation(summary = "Update sector", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> postSectors(HttpServletRequest request, @RequestBody Sector s) throws Exception {
		if (s == null || s.name() == null || s.name().strip().isEmpty()) return createBadRequestResponse("Sector name invalid");
		if (s.areaId() <= 0) return createBadRequestResponse("Invalid areaId=" + s.areaId());

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> sectorRepo.setSector(authUserId, setup, s)));
	}
}
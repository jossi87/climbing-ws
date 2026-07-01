package com.buldreinfo.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
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
import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.pdf.PdfGenerator;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.tracking.HitTrackingListener;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Sectors")
@RestController
@RequestMapping("/sectors")
public class SectorsController {
	private static final Logger logger = LogManager.getLogger();
	private final ApplicationEventPublisher eventPublisher;
	private final RequestContext requestContext;
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;
	private final AreaRepository areaRepo;
	private final HierarchyRepository hierarchyRepo;
	private final SectorRepository sectorRepo;

	public SectorsController(
			ApplicationEventPublisher eventPublisher,
			RequestContext requestContext,
			StorageManager storage,
			LeafletPrintService leafletPrintService,
			AreaRepository areaRepo,
			HierarchyRepository hierarchyRepo,
			SectorRepository sectorRepo) {
		this.eventPublisher = eventPublisher;
		this.requestContext = requestContext;
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
		this.areaRepo = areaRepo;
		this.hierarchyRepo = hierarchyRepo;
		this.sectorRepo = sectorRepo;
	}

	@Operation(summary = "Get sector by id")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Sector> getSectors(HttpServletRequest request,
			@RequestParam(name = "id") int id) {
		if (id <= 0) {
			throw new ValidationFailedException("Invalid id=" + id);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		if (requestContext.isHitTrackingEnabled(request)) {
			eventPublisher.publishEvent(new HitTrackingListener.SectorHitEvent(id));
		}
		return ResponseEntity.ok(sectorRepo.getSector(authUserId, setup.isBouldering(), setup, id));
	}

	@Operation(summary = "Get sector PDF by id")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<StreamingResponseBody> getSectorsPdf(HttpServletRequest request, @RequestParam(name = "id") int id) {
		if (id <= 0) {
			throw new ValidationFailedException("Invalid id=" + id);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		final Sector sector = sectorRepo.getSector(authUserId, false, setup, id);
		final var gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, 0, id);
		final Area area = areaRepo.getArea(setup, authUserId, sector.areaId());
		String filename = FilenameUtil.generateFilename(sector.name(), StorageType.PDF);
		StreamingResponseBody stream = output -> {
			try (PdfGenerator generator = new PdfGenerator(storage, leafletPrintService, output)) {
				generator.writeArea(setup, area, gradeDistribution, List.of(sector));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new InternalServerErrorException("PDF generation failed", e);
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_PDF)
				.body(stream);
	}

	@Operation(summary = "Update sector")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Redirect> postSectors(HttpServletRequest request, @RequestBody Sector s) {
		if (s == null || s.name() == null || s.name().strip().isEmpty()) throw new ValidationFailedException("Sector name invalid");
		if (s.areaId() <= 0) throw new ValidationFailedException("Invalid areaId=" + s.areaId());
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(sectorRepo.setSector(authUserId, setup, s));
	}
}

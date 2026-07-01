package com.buldreinfo.controller;

import java.util.Collection;
import java.util.Collections;
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
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.GradeDistribution;
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

@Tag(name = "Areas")
@RestController
@RequestMapping("/areas")
public class AreasController {
	private static final Logger logger = LogManager.getLogger();
	private final ApplicationEventPublisher eventPublisher;
	private final RequestContext requestContext;
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;
	private final AreaRepository areaRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final HierarchyRepository hierarchyRepo;

	public AreasController(
			ApplicationEventPublisher eventPublisher,
			RequestContext requestContext,
			StorageManager storage,
			LeafletPrintService leafletPrintService,
			RegionRepository regionRepo,
			AreaRepository areaRepo,
			SectorRepository sectorRepo,
			HierarchyRepository hierarchyRepo) {
		this.eventPublisher = eventPublisher;
		this.requestContext = requestContext;
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
		this.areaRepo = areaRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.hierarchyRepo = hierarchyRepo;
	}

	@Operation(summary = "Get areas")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<Area>> getAreas(HttpServletRequest request,
			@RequestParam(name = "id", defaultValue = "0") int id) {
		if (id < 0) {
			throw new ValidationFailedException("Invalid id=" + id);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		if (id > 0 && requestContext.isHitTrackingEnabled(request)) {
			eventPublisher.publishEvent(new HitTrackingListener.AreaHitEvent(id));
		}
		var res = id > 0 ? Collections.singleton(areaRepo.getArea(setup, authUserId, id)) : areaRepo.getAreaList(authUserId, setup.idRegion());
		return ResponseEntity.ok(res);
	}

	@Operation(summary = "Get area as PDF")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<StreamingResponseBody> getAreasPdf(HttpServletRequest request,  @RequestParam(name = "id") int id) {
		if (id <= 0) {
			throw new ValidationFailedException("Invalid area id=" + id);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		Area area = areaRepo.getArea(setup, authUserId, id);
		Collection<GradeDistribution> gradeDistribution = hierarchyRepo.getGradeDistribution(authUserId, area.id(), 0);
		List<Sector> sectors = area.sectors().stream()
				.map(s -> sectorRepo.getSector(authUserId, false, setup, s.id()))
				.toList();
		String filename = FilenameUtil.generateFilename(area.name(), StorageType.PDF);
		StreamingResponseBody stream = output -> {
			try (PdfGenerator generator = new PdfGenerator(storage, leafletPrintService, output)) {
				generator.writeArea(setup, area, gradeDistribution, sectors);
			} catch (Exception e) {
				logger.error("PDF generation failed: {}", e.getMessage(), e);
				throw new InternalServerErrorException("PDF generation failed", e);
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_PDF)
				.body(stream);
	}

	@Operation(summary = "Update area")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Redirect> postAreas(HttpServletRequest request, @RequestBody Area a) {
		if (a == null || a.name() == null || a.name().strip().isEmpty()) {
			throw new ValidationFailedException("Area name is missing or invalid");
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		var res = areaRepo.setArea(setup, authUserId, a);
		return ResponseEntity.ok(res);
	}
}

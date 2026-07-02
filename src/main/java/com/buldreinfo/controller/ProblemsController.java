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
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.ProblemSearchResult;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Svg;
import com.buldreinfo.pdf.PdfGenerator;
import com.buldreinfo.service.AreaService;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.service.ProblemService;
import com.buldreinfo.service.SectorService;
import com.buldreinfo.tracking.HitTrackingListener;
import com.buldreinfo.util.FilenameUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Problems")
@RestController
@RequestMapping("/problems")
public class ProblemsController {
	private static final Logger logger = LogManager.getLogger();
	private final ApplicationEventPublisher eventPublisher;
	private final RequestContext requestContext;
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;
	private final AreaService areaService;
	private final MediaRepository mediaRepo;
	private final ProblemService problemService;
	private final SectorService sectorService;

	public ProblemsController(
			ApplicationEventPublisher eventPublisher,
			RequestContext requestContext,
			StorageManager storage,
			LeafletPrintService leafletPrintService,
			AreaService areaService,
			MediaRepository mediaRepo,
			ProblemService problemService,
			SectorService sectorService) {
		this.eventPublisher = eventPublisher;
		this.requestContext = requestContext;
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
		this.areaService = areaService;
		this.mediaRepo = mediaRepo;
		this.problemService = problemService;
		this.sectorService = sectorService;
	}

	@Operation(summary = "Get problem by id")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Problem> getProblems(HttpServletRequest request,
			@RequestParam(name = "id") int id,
			@RequestParam(name = "showHiddenMedia", defaultValue = "false") boolean showHiddenMedia) {
		if (id <= 0) throw new ValidationFailedException("Invalid id=" + id);
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		if (id > 0 && requestContext.isHitTrackingEnabled(request)) {
			eventPublisher.publishEvent(new HitTrackingListener.ProblemHitEvent(id));
		}
		return ResponseEntity.ok(problemService.getProblem(authUserId, setup, id, showHiddenMedia));
	}

	@Operation(summary = "Get problem PDF by id")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<StreamingResponseBody> getProblemsPdf(HttpServletRequest request, 
			@RequestParam(name = "id") int id) {
		if (id <= 0) {
			throw new ValidationFailedException("Invalid id=" + id);
		}
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		final var problem = problemService.getProblem(authUserId, setup, id, false);
		final var area = areaService.getArea(setup, authUserId, problem.areaId());
		final var sector = sectorService.getSector(authUserId, false, setup, problem.sectorId());
		String filename = FilenameUtil.generateFilename(problem.name(), StorageType.PDF);
		StreamingResponseBody stream = output -> {
			try (var generator = new PdfGenerator(storage, leafletPrintService, output)) {
				generator.writeProblem(setup, area, sector, problem);
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

	@Operation(summary = "Search for problem")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ProblemSearchResult>> getProblemsSearch(HttpServletRequest request,
			@RequestParam(name = "value") String value) {
		if (value == null || value.isBlank()) throw new ValidationFailedException("Search keyword is required");
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(problemService.getProblemsSearch(authUserId, setup, value));
	}

	@Operation(summary = "Update problem")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Redirect> postProblems(HttpServletRequest request, @RequestBody Problem p) {
		if (p == null || p.name() == null || p.name().strip().isEmpty()) throw new ValidationFailedException("Problem name invalid");
		if (p.sectorId() <= 0) throw new ValidationFailedException("Invalid sectorId=" + p.sectorId());
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(problemService.setProblem(authUserId, setup, p));
	}

	@Operation(summary = "Update SVG for problem media")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/svg")
	public ResponseEntity<Void> postProblemsSvg(@RequestParam(name = "problemId") int problemId,
			@RequestParam(name = "pitch") int pitch,
			@RequestParam(name = "mediaId") int mediaId,
			@RequestBody Svg svg) {
		if (problemId <= 0) throw new ValidationFailedException("Invalid problemId=" + problemId);
		if (mediaId <= 0) throw new ValidationFailedException("Invalid mediaId=" + mediaId);
		if (svg == null) throw new ValidationFailedException("Svg payload missing");
		var authUserId = requestContext.getAuthenticatedUserId();
		mediaRepo.upsertSvg(authUserId, problemId, pitch, mediaId, svg);
		return ResponseEntity.ok().build();
	}
}

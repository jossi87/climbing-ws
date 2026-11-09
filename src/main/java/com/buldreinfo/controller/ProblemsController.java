package com.buldreinfo.controller;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.config.OpenApiConfig;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.excel.ProblemTemplate;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.ProblemImportResult;
import com.buldreinfo.model.ProblemSearchResult;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Svg;
import com.buldreinfo.pdf.PdfGenerator;
import com.buldreinfo.pdf.PdfText;
import com.buldreinfo.service.AreaService;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.service.MediaService;
import com.buldreinfo.service.ProblemImportService;
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
	private final ApplicationEventPublisher eventPublisher;
	private final RequestContext requestContext;
	private final RegionRepository regionRepo;
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;
	private final AreaService areaService;
	private final MediaService mediaService;
	private final ProblemImportService problemImportService;
	private final ProblemService problemService;
	private final SectorService sectorService;

	public ProblemsController(
			ApplicationEventPublisher eventPublisher,
			RequestContext requestContext,
			RegionRepository regionRepo,
			StorageManager storage,
			LeafletPrintService leafletPrintService,
			AreaService areaService,
			MediaService mediaService,
			ProblemImportService problemImportService,
			ProblemService problemService,
			SectorService sectorService) {
		this.eventPublisher = eventPublisher;
		this.requestContext = requestContext;
		this.regionRepo = regionRepo;
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
		this.areaService = areaService;
		this.mediaService = mediaService;
		this.problemImportService = problemImportService;
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
		var pdfText = PdfText.forLanguageCode(requestContext.getLanguageCode(request));
		StreamingResponseBody stream = output -> {
			try (var generator = new PdfGenerator(storage, leafletPrintService, pdfText, output)) {
				generator.writeProblem(setup, area, sector, problem);
			} catch (Exception e) {
				throw new InternalServerErrorException("PDF generation failed", e);
			}
		};
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.APPLICATION_PDF)
				.body(stream);
	}

	@Operation(summary = "Get template (xlsx) for batch registration of problems and sectors")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> getProblemsImportTemplate(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		byte[] bytes = ProblemTemplate.build(regionRepo.getTypes(setup.idRegion(), requestContext.getLanguageId(request)));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("climbing_problem_template", StorageType.XLSX)))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf(StorageType.XLSX.getMimeType()))
				.body(bytes);
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

	@Operation(summary = "Preview (dryRun=true) or import (dryRun=false) problems and their sectors from an uploaded Excel file (xlsx)")
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SECURITY_SCHEME)
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProblemImportResult> postProblemsImport(HttpServletRequest request,
			@RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun,
			@RequestPart("file") MultipartFile file) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		regionRepo.ensureAdminWriteRegion(setup, authUserId);
		int userId = authUserId.orElseThrow();
		int idLanguage = requestContext.getLanguageId(request);
		return ResponseEntity.ok(dryRun
				? problemImportService.preview(userId, setup.idRegion(), idLanguage, file)
				: problemImportService.commit(userId, setup.idRegion(), idLanguage, file));
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
		mediaService.upsertSvg(authUserId, problemId, pitch, mediaId, svg);
		return ResponseEntity.ok().build();
	}
}

package com.buldreinfo.controller;

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
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.ProblemSearchResult;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Svg;
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

@Tag(name = "Problems")
@RestController
@RequestMapping("/problems")
public class ProblemsController extends BaseController {
	private static final Logger logger = LogManager.getLogger();
	private final AreaRepository areaRepo;
	private final MediaRepository mediaRepo;
	private final ProblemRepository problemRepo;
	private final SectorRepository sectorRepo;

	public ProblemsController(ClimbingTransactionManager txManager,
			AreaRepository areaRepo,
			MediaRepository mediaRepo,
			ProblemRepository problemRepo,
			RegionRepository regionRepo,
			SectorRepository sectorRepo,
			UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.areaRepo = areaRepo;
		this.mediaRepo = mediaRepo;
		this.problemRepo = problemRepo;
		this.sectorRepo = sectorRepo;
	}

	@Operation(summary = "Get problem by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Problem.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getProblems(HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @RequestParam(name = "id") int id,
			@Parameter(description = "Include hidden media") @RequestParam(name = "showHiddenMedia", defaultValue = "false") boolean showHiddenMedia) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			return problemRepo.getProblem(authUserId, setup, id, showHiddenMedia, shouldUpdateHits);
		}));
	}

	@Operation(summary = "Get problem PDF by id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_PDF, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/pdf", produces = OpenApiConstants.APPLICATION_PDF)
	public ResponseEntity<StreamingResponseBody> getProblemsPdf(HttpServletRequest request, 
			@Parameter(description = "Problem id", required = true) @RequestParam(name = "id") int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);

		return executeAuthenticatedTask(request, (setup, authUserId) -> {
			boolean shouldUpdateHits = isHitTrackingEnabled(request);
			final var problem = problemRepo.getProblem(authUserId, setup, id, false, shouldUpdateHits);
			final var area = areaRepo.getArea(setup, authUserId, problem.areaId(), shouldUpdateHits);
			final var sector = sectorRepo.getSector(authUserId, false, setup, problem.sectorId(), shouldUpdateHits);

			StreamingResponseBody stream = output -> {
				try (var generator = new PdfGenerator(output)) {
					generator.writeProblem(setup, area, sector, problem);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException(e.getMessage(), e);
				}
			};

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename(problem.name(), "pdf")))
					.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
					.contentType(MediaType.APPLICATION_PDF)
					.body(stream);
		});
	}

	@Operation(summary = "Search for problem", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ProblemSearchResult.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getProblemsSearch(HttpServletRequest request,
			@Parameter(description = "Search keyword", required = true) @RequestParam(name = "value") String value) throws Exception {
		if (value == null || value.isBlank()) return createBadRequestResponse("Search keyword is required");
		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> problemRepo.getProblemsSearch(authUserId, setup, value)));
	}

	@Operation(summary = "Update problem", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Redirect.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.UNAUTHORIZED_CODE, description = OpenApiConstants.UNAUTHORIZED_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.FORBIDDEN_CODE, description = OpenApiConstants.FORBIDDEN_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> postProblems(HttpServletRequest request, @RequestBody Problem p) throws Exception {
		if (p == null || p.name() == null || p.name().strip().isEmpty()) return createBadRequestResponse("Problem name invalid");
		if (p.sectorId() <= 0) return createBadRequestResponse("Invalid sectorId=" + p.sectorId());

		return ResponseEntity.ok(executeAuthenticatedTask(request, (setup, authUserId) -> problemRepo.setProblem(authUserId, setup, p)));
	}

	@PostMapping(value = "/svg")
	public ResponseEntity<?> postProblemsSvg(HttpServletRequest request,
			@RequestParam(name = "problemId") int problemId,
			@RequestParam(name = "pitch") int pitch,
			@RequestParam(name = "mediaId") int mediaId,
			@RequestBody Svg svg) throws Exception {
		if (problemId <= 0) return createBadRequestResponse("Invalid problemId=" + problemId);
		if (mediaId <= 0) return createBadRequestResponse("Invalid mediaId=" + mediaId);
		if (svg == null) return createBadRequestResponse("Svg payload missing");

		return ResponseEntity.ok(executeAuthenticatedTask(request, (_, authUserId) -> {
			mediaRepo.upsertSvg(authUserId, problemId, pitch, mediaId, svg);
			return null;
		}));
	}
}
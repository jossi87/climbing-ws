package com.buldreinfo.controller;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.excel.ExcelWorkbook;
import com.buldreinfo.helpers.GlobalFunctions;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.model.GradeDistribution;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Toc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Meta")
@RestController
public class MetaController extends BaseController {
	private final HierarchyRepository hierarchyRepo;
	private final ClimbingTransactionManager txManager;
	private final UserRepository userRepo;
	private final RegionRepository regionRepo;

	public MetaController(ClimbingTransactionManager txManager, HierarchyRepository hierarchyRepo, RegionRepository regionRepo, UserRepository userRepo) {
		super(txManager, regionRepo);
		this.txManager = txManager;
		this.hierarchyRepo = hierarchyRepo;
		this.userRepo = userRepo;
		this.regionRepo = regionRepo;
	}

	@Operation(summary = "Get grade distribution by Area Id or Sector Id", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/grade/distribution", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getGradeDistribution(HttpServletRequest request,
			@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector
			) throws Exception {
		if (idArea < 0 || idSector < 0) {
			return createBadRequestResponse("IDs cannot be negative");
		}
		if (idArea == 0 && idSector == 0) {
			return createBadRequestResponse("Either idArea or idSector must be greater than 0");
		}

		return ResponseEntity.ok(executeContextualTask(request, ctx -> 
		hierarchyRepo.getGradeDistribution(ctx.authUserId(), idArea, idSector)));
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getGraph(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeContextualTask(request, ctx -> 
		hierarchyRepo.getContentGraph(ctx.authUserId(), ctx.setup())));
	}

	@Operation(summary = "Get metadata", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Meta.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/meta", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getMeta(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeContextualTask(request, ctx -> 
		Meta.from(ctx.setup(), ctx.authUserId(), txManager, userRepo, regionRepo)));
	}

	@Operation(summary = "Get robots.txt")
	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getRobotsTxt(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executePublicTask(request, setup -> {
			var lines = List.of("User-agent: *", "Disallow: */pdf", "Sitemap: " + setup.url() + "/sitemap.txt");
			return String.join("\r\n", lines);
		}));
	}

	@Operation(summary = "Get sitemap.txt")
	@GetMapping(value = "/sitemap.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getSitemapTxt(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executePublicTask(request, setup -> hierarchyRepo.getSitemapTxt(setup)));
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Toc.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/toc", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getToc(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeContextualTask(request, ctx -> hierarchyRepo.getToc(ctx.authUserId(), ctx.setup())));
	}

	@Operation(summary = "Get table of contents as Excel (xlsx)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/toc/xlsx", produces = OpenApiConstants.APPLICATION_XLSX)
	public ResponseEntity<byte[]> getTocXlsx(HttpServletRequest request) throws Exception {
		byte[] bytes = executeContextualTask(request, ctx -> {
			var toc = hierarchyRepo.getToc(ctx.authUserId(), ctx.setup());
			var pitches = hierarchyRepo.getTocPitches(ctx.authUserId(), ctx.setup());
			try (var workbook = new ExcelWorkbook()) {
				try (var sheet = workbook.addSheet("TOC")) {
					for (var r : toc.regions()) {
						for (var a : r.areas()) {
							for (var s : a.sectors()) {
								for (var p : s.problems()) {
									sheet.incrementRow();
									sheet.writeString("REGION", r.name());
									sheet.writeHyperlink("URL", p.url());
									sheet.writeString("AREA", a.name());
									sheet.writeString("SECTOR", s.name());
									sheet.writeInt("NR", p.nr());
									sheet.writeString("NAME", p.name());
									sheet.writeString("GRADE", p.grade());
									sheet.writeInt("FA_YEAR", p.faYear());
									sheet.writeInt("LENGTH_METER", p.lengthMeter());
									sheet.writeInt("STARTING_ALTITUDE", p.startingAltitude());
									String type = p.t().type() + (p.t().subType() != null ? " (" + p.t().subType() + ")" : "");
									sheet.writeString("TYPE", type);
									if (!ctx.setup().isBouldering()) sheet.writeInt("PITCHES", p.numPitches() > 0 ? p.numPitches() : 1);
									if (ctx.setup().isBouldering()) {
										sheet.writeString("FA_USER", p.ffaUser());
										sheet.writeInt("FA_YEAR", p.ffaYear());
									} else {
										sheet.writeString("FA_USER", p.faUser());
										sheet.writeInt("FA_YEAR", p.faYear());
										sheet.writeString("FFA_USER", p.ffaUser());
										sheet.writeInt("FFA_YEAR", p.ffaYear());
									}
									sheet.writeDouble("STARS", p.stars());
									sheet.writeString("DESCRIPTION", p.description());
								}
							}
						}
					}
				}
				if (!pitches.isEmpty()) {
					try (var sheet = workbook.addSheet("TOC_MULTIPITCH_PITCHES")) {
						for (var p : pitches) {
							sheet.incrementRow();
							sheet.writeString("REGION", p.regionName());
							sheet.writeHyperlink("URL", p.url());
							sheet.writeString("AREA", p.areaName());
							sheet.writeString("SECTOR", p.sectorName());
							sheet.writeString("PROBLEM", p.problemName());
							sheet.writeInt("PITCH", p.pitch());
							sheet.writeString("GRADE", p.grade());
							sheet.writeString("DESCRIPTION", p.description());
						}
					}
				}
				try (var os = new ByteArrayOutputStream()) {
					workbook.write(os);
					return os.toByteArray();
				}
			}
		});
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(GlobalFunctions.getFilename("TOC", "xlsx")))
				.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
				.contentType(MediaType.valueOf(OpenApiConstants.APPLICATION_XLSX))
				.body(bytes);
	}
}
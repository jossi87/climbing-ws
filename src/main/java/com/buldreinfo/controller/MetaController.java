package com.buldreinfo.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.beans.StorageType;
import com.buldreinfo.dao.HierarchyRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.excel.ExcelWorkbook;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.infrastructure.RequestContext;
import com.buldreinfo.infrastructure.ValidationFailedException;
import com.buldreinfo.model.GradeDistribution;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Toc;
import com.buldreinfo.model.Toc.TocPitch;
import com.buldreinfo.util.FilenameUtil;

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
public class MetaController {
	private final RequestContext requestContext;
	private final HierarchyRepository hierarchyRepo;
	private final UserRepository userRepo;
	private final RegionRepository regionRepo;

	public MetaController(RequestContext requestContext, HierarchyRepository hierarchyRepo, RegionRepository regionRepo, UserRepository userRepo) {
		this.requestContext = requestContext;
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
	public ResponseEntity<Collection<GradeDistribution>> getGradeDistribution(@Parameter(description = "Area id", required = true) @RequestParam(name = "idArea") int idArea,
			@Parameter(description = "Sector id", required = true) @RequestParam(name = "idSector") int idSector) {
		if (idArea < 0 || idSector < 0) {
			throw new ValidationFailedException("IDs cannot be negative");
		}
		if (idArea == 0 && idSector == 0) {
			throw new ValidationFailedException("Either idArea or idSector must be greater than 0");
		}
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getGradeDistribution(authUserId, idArea, idSector));
	}

	@Operation(summary = "Get graph (number of boulders/routes grouped by grade)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = GradeDistribution.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<GradeDistribution>> getGraph(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getContentGraph(authUserId, setup));
	}

	@Operation(summary = "Get metadata", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Meta.class))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/meta", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Meta> getMeta(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(Meta.from(setup, authUserId, userRepo, regionRepo));
	}

	@Operation(summary = "Get robots.txt")
	@GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getRobotsTxt(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var lines = List.of("User-agent: *", "Disallow: */pdf", "Sitemap: " + setup.url() + "/sitemap.txt");
		return ResponseEntity.ok(String.join("\r\n", lines));
	}

	@Operation(summary = "Get sitemap.txt")
	@GetMapping(value = "/sitemap.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getSitemapTxt(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		return ResponseEntity.ok(hierarchyRepo.getSitemapTxt(setup));
	}

	@Operation(summary = "Get table of contents (all problems)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = Toc.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/toc", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Toc> getToc(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		return ResponseEntity.ok(hierarchyRepo.getToc(authUserId, setup));
	}

	@Operation(summary = "Get table of contents as Excel (xlsx)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = OpenApiConstants.APPLICATION_XLSX, array = @ArraySchema(schema = @Schema(implementation = Byte.class)))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@SecurityRequirement(name = OpenApiConstants.BEARER_AUTH)
	@GetMapping(value = "/toc/xlsx", produces = OpenApiConstants.APPLICATION_XLSX)
	public ResponseEntity<byte[]> getTocXlsx(HttpServletRequest request) {
		var setup = requestContext.getSetup(request);
		var authUserId = requestContext.getAuthenticatedUserId();
		var toc = hierarchyRepo.getToc(authUserId, setup);
		var pitches = hierarchyRepo.getTocPitches(authUserId, setup);
		try (var workbook = new ExcelWorkbook(); 
				var os = new ByteArrayOutputStream()) {
			writeSheetToc(workbook, setup, toc);
			if (!pitches.isEmpty()) {
				writeSheetTocPitches(workbook, pitches);
			}
			workbook.write(os);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FilenameUtil.generateFilename("TOC", StorageType.XLSX)))
					.header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
					.contentType(MediaType.valueOf(OpenApiConstants.APPLICATION_XLSX))
					.body(os.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private void writeSheetToc(ExcelWorkbook workbook, Setup setup, Toc toc) {
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
							if (!setup.isBouldering()) sheet.writeInt("PITCHES", p.numPitches() > 0 ? p.numPitches() : 1);
							if (setup.isBouldering()) {
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
	}

	private void writeSheetTocPitches(ExcelWorkbook workbook, List<TocPitch> pitches) {
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
}
package com.buldreinfo.controller;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.FrontpageRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.Sector;
import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Without JavaScript")
@RestController
@RequestMapping("/without-js")
public class WithoutJsController extends BaseController {
	private static final Logger logger = LogManager.getLogger();
	private static final boolean shouldUpdateHits = false;
	private final AreaRepository areaRepo;
	private final FrontpageRepository frontpageRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final SectorRepository sectorRepo;
	private final ClimbingTransactionManager txManager;
	private final UserRepository userRepo;

	public WithoutJsController(ClimbingTransactionManager txManager,
			AreaRepository areaRepo,
			FrontpageRepository frontpageRepo,
			ProblemRepository problemRepo,
			RegionRepository regionRepo,
			SectorRepository sectorRepo,
			UserRepository userRepo) {
		super(txManager, regionRepo, userRepo);
		this.txManager = txManager;
		this.areaRepo = areaRepo;
		this.frontpageRepo = frontpageRepo;
		this.problemRepo = problemRepo;
		this.regionRepo = regionRepo;
		this.sectorRepo = sectorRepo;
		this.userRepo = userRepo;
	}

	@Operation(summary = "Get Frontpage without JavaScript", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML_VALUE, schema = @Schema(implementation = String.class))})
	})
	@GetMapping(produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> getWithoutJs(HttpServletRequest request) throws Exception {
		return ResponseEntity.ok(executeSetupTask(request, setup -> {
			var meta = Meta.from(setup, Optional.empty(), txManager, userRepo, regionRepo);
			var stats = frontpageRepo.getFrontpageStats(Optional.empty(), setup);
			var randomMedia = frontpageRepo.getFrontpageRandomMedia(setup).stream().findAny().orElse(null);

			String description = "%s - %d regions, %d areas, %d %s, %d ticks".formatted(
					setup.description(), meta.regions().size(), stats.areas(), stats.problems(),
					(setup.isBouldering() ? "boulders" : "routes"), stats.ticks());

			return getHtml(setup, setup.url(), setup.title(), description,
					(randomMedia == null ? 0 : randomMedia.identity().id()),
					(randomMedia == null ? 0 : randomMedia.identity().versionStamp()),
					(randomMedia == null ? 0 : randomMedia.width()),
					(randomMedia == null ? 0 : randomMedia.height()));
		}));
	}

	@Operation(summary = "Get area by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML_VALUE, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GetMapping(value = "/area/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> getWithoutJsArea(HttpServletRequest request, @PathVariable int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);
		return ResponseEntity.ok(executeSetupTask(request, setup -> {
			Area a = areaRepo.getArea(setup, Optional.empty(), id, shouldUpdateHits);
			String description = (setup.isBouldering() ? "Bouldering in " : "Climbing in ") + a.name();
			Media m = (a.media() != null && !a.media().isEmpty()) ? a.media().getFirst() : null;
			return getHtml(setup, setup.url() + "/area/" + a.id(), a.name(), description,
					(m == null ? 0 : m.identity().id()), (m == null ? 0 : m.identity().versionStamp()),
					(m == null ? 0 : m.width()), (m == null ? 0 : m.height()));
		}));
	}

	@Operation(
			summary = "Get problem details without JavaScript",
			description = "Returns an HTML representation of a problem, suitable for embedding on platforms like Facebook. Supports optional media and pitch parameters.",
			responses = {
					@ApiResponse(
							responseCode = OpenApiConstants.OK_CODE, 
							description = OpenApiConstants.OK_DESCRIPTION, 
							content = @Content(mediaType = MediaType.TEXT_HTML_VALUE, schema = @Schema(type = "string"))
							),
					@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
					@ApiResponse(responseCode = OpenApiConstants.BAD_REQUEST_CODE, description = OpenApiConstants.BAD_REQUEST_DESCRIPTION),
					@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
			}
			)
	@GetMapping(value = {"/problem/{id}", "/problem/{id}/{mediaId}", "/problem/{id}/{mediaId}/{pitch}"}, produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> getWithoutJsProblem(
			HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathVariable int id,
			@Parameter(description = "Media id", required = false) @PathVariable(required = false) Integer mediaId,
			@Parameter(description = "Pitch number", required = false) @PathVariable(required = false) Integer pitch) throws Exception {

		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);
		int mid = (mediaId == null) ? 0 : mediaId;
		if (mid < 0) return createBadRequestResponse("Invalid mediaId=" + mid);
		if (pitch != null) logger.debug("Ignore pitch {}, just return mediaId {}", pitch, mid);

		return ResponseEntity.ok(executeSetupTask(request, setup -> {
			Problem p = problemRepo.getProblem(Optional.empty(), setup, id, false, shouldUpdateHits);
			String title = "%s [%s] (%s / %s)".formatted(p.name(), p.grade(), p.areaName(), p.sectorName());
			String description = p.comment();

			if (p.fa() != null && !p.fa().isEmpty()) {
				String fa = p.fa().stream().map(x -> x.name().trim()).collect(Collectors.joining(", "));
				description = (!Strings.isNullOrEmpty(description) ? description + " | " : "") + 
						"First ascent by " + fa + (!Strings.isNullOrEmpty(p.faDateHr()) ? " (" + p.faDate() + ")" : "");
			}

			Media m = (p.media() != null && !p.media().isEmpty()) ? 
					p.media().stream().filter(x -> !x.inherited() && (mid == 0 || x.identity().id() == mid)).findFirst().orElse(p.media().getFirst()) 
					: null;

			return getHtml(setup, setup.url() + "/problem/" + p.id(), title, description,
					(m == null ? 0 : m.identity().id()), (m == null ? 0 : m.identity().versionStamp()),
					(m == null ? 0 : m.width()), (m == null ? 0 : m.height()));
		}));
	}

	@Operation(summary = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML_VALUE, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GetMapping(value = "/sector/{id}", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> getWithoutJsSector(HttpServletRequest request, @PathVariable int id) throws Exception {
		if (id <= 0) return createBadRequestResponse("Invalid id=" + id);
		return ResponseEntity.ok(executeSetupTask(request, setup -> {
			Sector s = sectorRepo.getSector(Optional.empty(), false, setup, id, shouldUpdateHits);
			String title = "%s (%s)".formatted(s.name(), s.areaName());
			String description = "%s in %s / %s (%d %s)%s".formatted(
					(setup.isBouldering() ? "Bouldering" : "Climbing"), s.areaName(), s.name(),
					(s.problems() != null ? s.problems().size() : 0),
					(setup.isBouldering() ? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.comment()) ? " | " + s.comment() : ""));
			Media m = (s.media() != null && !s.media().isEmpty()) ? s.media().getFirst() : null;
			return getHtml(setup, setup.url() + "/sector/" + s.id(), title, description,
					(m == null ? 0 : m.identity().id()), (m == null ? 0 : m.identity().versionStamp()),
					(m == null ? 0 : m.width()), (m == null ? 0 : m.height()));
		}));
	}

	private String escapeHtml(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}

	private String getHtml(Setup setup, String pageUrl, String title, String description, int mediaId, long mediaVersionStamp, int mediaWidth, int mediaHeight) {
		String st = escapeHtml(title), sd = escapeHtml(description), spu = escapeHtml(pageUrl);
		String og = "";
		if (mediaId > 0) {
			String url = escapeHtml(setup.url() + StorageManager.getPublicUrl(S3KeyGenerator.getWebJpg(mediaId), mediaVersionStamp));
			og = """
					<meta property="og:image" content="%s" />
					<meta property="og:image:width" content="%d" />
					<meta property="og:image:height" content="%d" />
					""".formatted(url, mediaWidth, mediaHeight);
		}
		return """
				<!DOCTYPE html>
				<html lang="en">
				<head>
				    <meta charset="UTF-8">
				    <title>%s</title>
				    <meta name="description" content="%s" />
				    <meta property="og:type" content="website" />
				    <meta property="og:description" content="%s" />
				    <meta property="og:url" content="%s" />
				    <meta property="og:title" content="%s" />
				    <meta property="fb:app_id" content="275320366630912" />
				    %s
				</head>
				</html>
				""".formatted(st, sd, sd, spu, st, og);
	}
}
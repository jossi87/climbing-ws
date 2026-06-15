package com.buldreinfo.jersey.jaxb.resources;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Frontpage.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Without JavaScript")
@Path("/without-js")
public class WithoutJsResource extends BaseResource {
	private static Logger logger = LogManager.getLogger();

	@Operation(summary = "Get Frontpage without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/without-js")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJs(@Context HttpServletRequest request) {
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, _) -> {
			final Optional<Integer> authUserId = Optional.empty();
			var meta = Meta.from(dao, c, setup, authUserId);
			var stats = dao.getFrontpageRepo().getFrontpageStats(c, authUserId, setup);
			FrontpageRandomMedia frontpageRandomMedia = dao.getFrontpageRepo().getFrontpageRandomMedia(c, setup).stream()
					.findAny()
					.orElse(null);
			String description = String.format("%s - %d regions, %d areas, %d %s, %d ticks",
					setup.description(),
					meta.regions().size(),
					stats.areas(),
					stats.problems(),
					(setup.isBouldering()? "boulders" : "routes"),
					stats.ticks());
			String html = getHtml(setup,
					setup.url(),
					setup.title(),
					description,
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.identity().id()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.identity().versionStamp()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.width()),
					(frontpageRandomMedia == null? 0 : frontpageRandomMedia.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get area by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/area/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsArea(@Context HttpServletRequest request, @Parameter(description = "Area id", required = true) @PathParam("id") int id) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Area a = dao.getAreaRepo().getArea(c, setup, authUserId, id, shouldUpdateHits);
			String description = setup.isBouldering() ? "Bouldering in " + a.name() : "Climbing in " + a.name();
			Media m = a.media() != null && !a.media().isEmpty()? a.media().getFirst() : null;
			String html = getHtml(setup,
					setup.url() + "/area/" + a.id(),
					a.name(),
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/problem/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblem(@Context HttpServletRequest request, @Parameter(description = "Problem id", required = true) @PathParam("id") int id) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return getWithoutJsProblemMedia(request, id, 0);
	}

	@Operation(summary = "Get problem by id and idMedia without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/problem/{id}/{mediaId}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMedia(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		if (mediaId < 0) {
			return createBadRequestResponse("Invalid mediaId=" + mediaId);
		}
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			Problem p = dao.getProblemRepo().getProblem(c, authUserId, setup, id, false, shouldUpdateHits);
			String title = String.format("%s [%s] (%s / %s)", p.name(), p.grade(), p.areaName(), p.sectorName());
			String description = p.comment();
			if (p.fa() != null && !p.fa().isEmpty()) {
				String fa = p.fa().stream().map(x -> x.name().trim()).collect(Collectors.joining(", "));
				description = (!Strings.isNullOrEmpty(description)? description + " | " : "") + "First ascent by " + fa + (!Strings.isNullOrEmpty(p.faDateHr())? " (" + p.faDate() + ")" : "");
			}
			Media m = null;
			if (p.media() != null && !p.media().isEmpty()) {
				Optional<Media> optM = p.media()
						.stream()
						.filter(x -> !x.inherited() && (mediaId == 0 || x.identity().id() == mediaId))
						.findFirst();
				if (optM.isPresent()) {
					m = optM.get();
				}
				else {
					m = p.media().getFirst();
				}
			}
			String html = getHtml(setup,
					setup.url() + "/problem/" + p.id(),
					title,
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}

	@Operation(summary = "Get problem by id, idMedia and pitch without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/problem/{id}/{mediaId}/{pitch}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsProblemMediaPitch(@Context HttpServletRequest request,
			@Parameter(description = "Problem id", required = true) @PathParam("id") int id,
			@Parameter(description = "Media id", required = true) @PathParam("mediaId") int mediaId,
			@Parameter(description = "Pitch", required = true) @PathParam("pitch") int pitch) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		if (mediaId < 0) {
			return createBadRequestResponse("Invalid mediaId=" + mediaId);
		}
		logger.debug("Ignore pitch {}, just return mediaId {}", pitch, mediaId);
		return getWithoutJsProblemMedia(request, id, mediaId);
	}

	@Operation(summary = "Get sector by id without JavaScript (for embedding on e.g. Facebook)", responses = {
			@ApiResponse(responseCode = OpenApiConstants.OK_CODE, description = OpenApiConstants.OK_DESCRIPTION, content = {@Content(mediaType = MediaType.TEXT_HTML, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = OpenApiConstants.NOT_FOUND_CODE, description = OpenApiConstants.NOT_FOUND_DESCRIPTION),
			@ApiResponse(responseCode = OpenApiConstants.INTERNAL_SERVER_ERROR_CODE, description = OpenApiConstants.INTERNAL_SERVER_ERROR_DESCRIPTION)
	})
	@GET
	@Path("/sector/{id}")
	@Produces(MediaType.TEXT_HTML)
	public Response getWithoutJsSector(@Context HttpServletRequest request, @Parameter(description = "Sector id", required = true) @PathParam("id") int id) {
		if (id <= 0) {
			return createBadRequestResponse("Invalid id=" + id);
		}
		return DatabaseContext.buildResponseWithSql(request, (dao, c, setup, shouldUpdateHits) -> {
			final Optional<Integer> authUserId = Optional.empty();
			final boolean orderByGrade = false;
			Sector s = dao.getSectorRepo().getSector(c, authUserId, orderByGrade, setup, id, shouldUpdateHits);
			String title = String.format("%s (%s)", s.name(), s.areaName());
			String description = String.format("%s in %s / %s (%d %s)%s",
					(setup.isBouldering()? "Bouldering" : "Climbing"),
					s.areaName(),
					s.name(),
					(s.problems() != null? s.problems().size() : 0),
					(setup.isBouldering()? "boulders" : "routes"),
					(!Strings.isNullOrEmpty(s.comment())? " | " + s.comment() : ""));
			Media m = s.media() != null && !s.media().isEmpty()? s.media().getFirst() : null;
			String html = getHtml(setup,
					setup.url() + "/sector/" + s.id(),
					title,
					description,
					(m == null? 0 : m.identity().id()),
					(m == null? 0 : m.identity().versionStamp()),
					(m == null? 0 : m.width()),
					(m == null? 0 : m.height()));
			return Response.ok().entity(html).build();
		});
	}
	
	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
	
	private String getHtml(Setup setup, String pageUrl, String title, String description, int mediaId, long mediaVersionStamp, int mediaWidth, int mediaHeight) {
		String safeTitle = escapeHtml(title);
		String safeDescription = escapeHtml(description);
		String safePageUrl = escapeHtml(pageUrl);
		String ogImageTags = "";
		if (mediaId > 0) {
			String relativePath = StorageManager.getPublicUrl(S3KeyGenerator.getWebJpg(mediaId), mediaVersionStamp);
			String safeAbsoluteImageUrl = escapeHtml(setup.url() + relativePath);
			ogImageTags = """
					<meta property="og:image" content="%s" />
					<meta property="og:image:width" content="%d" />
					<meta property="og:image:height" content="%d" />
					""".formatted(safeAbsoluteImageUrl, mediaWidth, mediaHeight);
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
				""".formatted(safeTitle, safeDescription, safeDescription, safePageUrl, safeTitle, ogImageTags);
	}
}
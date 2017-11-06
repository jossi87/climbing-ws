package com.buldreinfo.jersey.jaxb;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.OpenGraphImage;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.google.common.html.HtmlEscapers;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/v1/static/")
public class V1Html {
	private class Config {
		private final int idRegion;
		private final String title;
		private final String baseUrl;
		public Config(int idRegion, String title, String baseUrl) {
			this.idRegion = idRegion;
			this.title = title;
			this.baseUrl = baseUrl;
		}
		public String getBaseUrl() {
			return baseUrl;
		}
		public int getIdRegion() {
			return idRegion;
		}
		public String getTitle() {
			return title;
		}
	}
	
	@GET
	@Path("/areas")
	public Response getAreas(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Area a = c.getBuldreinfoRepo().getArea(null, id);
			c.setSuccess();
			Config conf = getConfig(request);
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/area/" + id, conf.getTitle() + " | " + a.getName(), a.getComment())).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/frontpage")
	public Response getFrontpage(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Config conf = getConfig(request);
			Frontpage f = c.getBuldreinfoRepo().getFrontpage(null, conf.getIdRegion());
			String description = String.format("Total: %d | With coordinates: %d | Public ascents: %d | Images: %d | Ascents on video: %d", f.getNumProblems(), f.getNumProblemsWithCoordinates(), f.getNumTicks(), f.getNumImages(), f.getNumMovies());
			OpenGraphImage image = c.getBuldreinfoRepo().getImage(f.getRandomMedia().getIdMedia());
			c.setSuccess();
			return Response.ok().entity(getHtml(conf.getBaseUrl(), conf.getTitle(), description, image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems")
	public Response getProblems(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<Problem> res = c.getBuldreinfoRepo().getProblem(null, 0, id, 0);
			String name = "";
			String description = "";
			OpenGraphImage image = null;
			if (!res.isEmpty()) {
				Problem p = res.get(0);
				name = p.getName() + " (" + p.getGrade() + ")";
				description = p.getComment();
				if (p.getMedia() != null && !p.getMedia().isEmpty()) {
					image = c.getBuldreinfoRepo().getImage(p.getMedia().get(0).getId());	
				}
			}
			c.setSuccess();
			Config conf = getConfig(request);
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/problem/" + id, conf.getTitle() + " | " + name, description, image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sectors")
	public Response getSectors(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Sector s = c.getBuldreinfoRepo().getSector(null, 0, id);
			OpenGraphImage image = s.getMedia() != null && !s.getMedia().isEmpty()? c.getBuldreinfoRepo().getImage(s.getMedia().get(0).getId()) : null;
			c.setSuccess();
			Config conf = getConfig(request);
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/sector/" + id, conf.getTitle() + " | " + s.getName(), s.getComment(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private Config getConfig(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		throw new RuntimeException(url);
//	    if (url.contains("buldring.bergen-klatreklubb.no")) {
//	    	return new Config(2, "Buldring i Hordaland", "https://buldring.bergen-klatreklubb.no");
//	    }
//	    else if (url.contains("buldring.fredrikstadklatreklubb.org")) {
//	    	return new Config(3, "Buldring i Fredrikstad", "https://buldring.fredrikstadklatreklubb.org");
//	    }
//	    else if (url.contains("brattelinjer.no")) {
//	    	return new Config(4, "Bratte linjer", "https://brattelinjer.no");
//	    }
//	    return new Config(1, "buldreinfo", "https://buldreinfo.com");
	}

	private String getHtml(String url, String title, String description) {
		return getHtml(url, title, description, null);
	}

	private String getHtml(String url, String title, String description, OpenGraphImage image) {
		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html>");
		builder.append("<html prefix='og: http://ogp.me/ns#'>");
		builder.append("<head>");
		builder.append("<title>" + HtmlEscapers.htmlEscaper().escape(title) + "</title>");
		if (image != null) {
			builder.append("<meta property='og:image' content='" + image.getHttp() + "' />");
			builder.append("<meta property='og:image:width' content='" + image.getWidth() + "' />");
			builder.append("<meta property='og:image:height' content='" + image.getHeight() + "' />");
		}
		builder.append("<meta property='og:type' content='article' />");
		builder.append("<meta property='og:title' content='" + HtmlEscapers.htmlEscaper().escape(title) + "' />");
		builder.append("<meta property='og:url' content='" + url + "' />");
		if (description != null) {
			builder.append("<meta property='og:description' content='" + HtmlEscapers.htmlEscaper().escape(description) + "' />");
		}
		builder.append("<meta property='fb:app_id' content='1618301911826448' />");
		builder.append("</head>");
		builder.append("<body>OpenGraph</body>");
		builder.append("</html>");
		return builder.toString();
	}
}
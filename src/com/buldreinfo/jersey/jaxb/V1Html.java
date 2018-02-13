package com.buldreinfo.jersey.jaxb;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		@Override
		public String toString() {
			return "Config [idRegion=" + idRegion + ", title=" + title + ", baseUrl=" + baseUrl + "]";
		}
	}
	
	private static final Logger logger = LogManager.getLogger();
	
	@GET
	@Path("/areas")
	public Response getAreas(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Area a = c.getBuldreinfoRepo().getArea(null, id);
			c.setSuccess();
			Config conf = getConfig(base);
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/area/" + id, conf.getTitle() + " | " + a.getName(), a.getComment(), null)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/frontpage")
	public Response getFrontpage(@QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Config conf = getConfig(base);
			Frontpage f = c.getBuldreinfoRepo().getFrontpage(null, conf.getIdRegion());
			String description = String.format("Total: %d (%d with coordinates, %d on topo) | Public ascents: %d | Images: %d | Ascents on video: %d", 
					f.getNumProblems(), 
					f.getNumProblemsWithCoordinates(), 
					f.getNumProblemsOnTopo(),
					f.getNumTicks(), f.getNumImages(), 
					f.getNumMovies());
			OpenGraphImage image = f.getRandomMedia() == null? null : c.getBuldreinfoRepo().getImage(conf.getBaseUrl(), f.getRandomMedia().getIdMedia());
			c.setSuccess();
			return Response.ok().entity(getHtml(conf.getBaseUrl(), conf.getTitle(), description, image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems")
	public Response getProblems(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Config conf = getConfig(base);
			List<Problem> res = c.getBuldreinfoRepo().getProblem(null, conf.getIdRegion(), id, 0);
			String name = "";
			String description = "";
			OpenGraphImage image = null;
			if (!res.isEmpty()) {
				Problem p = res.get(0);
				name = p.getName() + " (" + p.getGrade() + ")";
				description = p.getComment();
				if (p.getMedia() != null && !p.getMedia().isEmpty()) {
					image = c.getBuldreinfoRepo().getImage(conf.getBaseUrl(), p.getMedia().get(0).getId());	
				}
			}
			c.setSuccess();
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/problem/" + id, conf.getTitle() + " | " + name, description, image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sectors")
	public Response getSectors(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Config conf = getConfig(base);
			Sector s = c.getBuldreinfoRepo().getSector(null, conf.getIdRegion(), id);
			OpenGraphImage image = s.getMedia() != null && !s.getMedia().isEmpty()? c.getBuldreinfoRepo().getImage(conf.getBaseUrl(), s.getMedia().get(0).getId()) : null;
			c.setSuccess();
			return Response.ok().entity(getHtml(conf.getBaseUrl() + "/sector/" + id, conf.getTitle() + " | " + s.getName(), s.getComment(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private Config getConfig(String base) {
		Config conf = null;
	    if (base.contains("buldring.bergen-klatreklubb.no")) {
	    	conf = new Config(2, "Buldring i Hordaland", "https://buldring.bergen-klatreklubb.no");
	    }
	    else if (base.contains("buldring.fredrikstadklatreklubb.org")) {
	    	conf = new Config(3, "Buldring i Fredrikstad", "https://buldring.fredrikstadklatreklubb.org");
	    }
	    else if (base.contains("brattelinjer.no")) {
	    	conf = new Config(4, "Bratte linjer", "https://brattelinjer.no");
	    }
	    else {
	    	conf = new Config(1, "buldreinfo", "https://buldreinfo.com");
	    }
	    logger.debug("getConfig(base={}) - conf={}", base, conf);
	    return conf;
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
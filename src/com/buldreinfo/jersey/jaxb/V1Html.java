package com.buldreinfo.jersey.jaxb;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Metadata;
import com.buldreinfo.jersey.jaxb.model.OpenGraphImage;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.google.common.html.HtmlEscapers;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/v1/static/")
public class V1Html {
	private final static MetaHelper metaHelper = new MetaHelper();

	@GET
	@Path("/areas")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getAreas(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Setup setup = metaHelper.getSetup(base);
			Area a = c.getBuldreinfoRepo().getArea(null, id);
			OpenGraphImage image = getLastImage(c, setup, a.getMedia());
			c.setSuccess();
			metaHelper.updateMetadata(a, setup);
			return Response.ok().entity(getHtml(setup.getUrl("/area/" + id), a.getMetadata(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/frontpage")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getFrontpage(@QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Setup setup = metaHelper.getSetup(base);
			Frontpage f = c.getBuldreinfoRepo().getFrontpage(null, setup.getIdRegion());
			OpenGraphImage image = f.getRandomMedia() == null? null : c.getBuldreinfoRepo().getImage(setup, f.getRandomMedia().getIdMedia());
			c.setSuccess();
			metaHelper.updateMetadata(f, setup);
			return Response.ok().entity(getHtml(setup.getUrl(null), f.getMetadata(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/problems")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getProblems(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Setup setup = metaHelper.getSetup(base);
			List<Problem> res = c.getBuldreinfoRepo().getProblem(null, setup.getIdRegion(), id, 0);
			Preconditions.checkArgument(!res.isEmpty());
			Problem p = res.get(0);
			OpenGraphImage image = getLastImage(c, setup, p.getMedia());
			c.setSuccess();
			metaHelper.updateMetadata(p, setup);
			return Response.ok().entity(getHtml(setup.getUrl("/problem/" + id), p.getMetadata(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getRobotsTxt(@QueryParam("base") String base) {
		Setup setup = metaHelper.getSetup(base);
		return Response.ok().entity("Sitemap: " + setup.getUrl("/sitemap.txt")).build(); 
	}

	@GET
	@Path("/sectors")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getSectors(@QueryParam("id") int id, @QueryParam("base") String base) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Setup setup = metaHelper.getSetup(base);
			Sector s = c.getBuldreinfoRepo().getSector(null, setup.getIdRegion(), id);
			OpenGraphImage image = getLastImage(c, setup, s.getMedia());
			c.setSuccess();
			metaHelper.updateMetadata(s, setup);
			return Response.ok().entity(getHtml(setup.getUrl("/sector/" + id), s.getMetadata(), image)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getSitemapTxt(@QueryParam("base") String base) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Setup setup = metaHelper.getSetup(base);
			String res = c.getBuldreinfoRepo().getSitemapTxt(setup.getIdRegion());
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private String getHtml(String url, Metadata metadata, OpenGraphImage image) {
		StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html>");
		builder.append("<html prefix='og: http://ogp.me/ns#'>");
		builder.append("<head>");
		builder.append("<title>" + HtmlEscapers.htmlEscaper().escape(metadata.getTitle()) + "</title>");
		if (image != null) {
			builder.append("<meta property='og:image' content='" + image.getHttp() + "' />");
			builder.append("<meta property='og:image:width' content='" + image.getWidth() + "' />");
			builder.append("<meta property='og:image:height' content='" + image.getHeight() + "' />");
		}
		builder.append("<meta property='og:type' content='article' />");
		builder.append("<meta property='og:title' content='" + HtmlEscapers.htmlEscaper().escape(metadata.getTitle()) + "' />");
		builder.append("<meta property='og:url' content='" + url + "' />");
		if (metadata.getDescription() != null) {
			builder.append("<meta property='og:description' content='" + HtmlEscapers.htmlEscaper().escape(metadata.getDescription()) + "' />");
		}
		builder.append("<meta property='fb:app_id' content='1618301911826448' />");
		builder.append("</head>");
		builder.append("<body>OpenGraph</body>");
		builder.append("</html>");
		return builder.toString();
	}

	private OpenGraphImage getLastImage(DbConnection c, Setup setup, List<Media> media) {
		if (media == null) {
			return null;
		}
		Optional<Media> m = media.stream().filter(x -> x.getIdType()==1).reduce((a, b) -> b);
		if (m.isPresent()) {
			return c.getBuldreinfoRepo().getImage(setup, m.get().getId());
		}
		return null;
	}
}
package com.buldreinfo.jersey.jaxb;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.app.Region;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 * Legacy code for buldreinfo-app
 */
@Path("/v1/")
public class V1 {

	@GET
	@Path("/images")
	@Produces("image/webp")
	public Response getImages(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			boolean webP = true;
			final java.nio.file.Path p = c.getBuldreinfoRepo().getImage(webP, id);
			c.setSuccess();
			return Response.ok(p.toFile()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/regions")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getRegions(@QueryParam("uniqueId") String uniqueId, @QueryParam("climbingNotBouldering") boolean climbingNotBouldering) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Collection<Region> res = c.getBuldreinfoRepo().getRegions(uniqueId, climbingNotBouldering);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
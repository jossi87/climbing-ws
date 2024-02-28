package com.buldreinfo.jersey.jaxb;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 * Legacy code for buldreinfo-app
 */
@Hidden
@Path("/v1/")
public class V1 {
	@GET
	@Path("/images")
	@Produces("image/webp")
	public Response getImages(@QueryParam("id") int id) {
		return Server.buildResponse(() -> Response.ok(Server.getDao().getImage(true, id)).build());
	}

	@GET
	@Path("/regions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRegions(@QueryParam("uniqueId") String uniqueId, @QueryParam("climbingNotBouldering") boolean climbingNotBouldering) {
		return Server.buildResponseWithSql(c -> Response.ok().entity(Server.getDao().getRegions(c, uniqueId, climbingNotBouldering)).build());
	}
}
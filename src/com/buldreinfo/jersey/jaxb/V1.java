package com.buldreinfo.jersey.jaxb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.google.common.base.Preconditions;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 * Legacy code for buldreinfo-app
 */
@Path("/v1/")
public class V1 {

	@GET
	@Path("/images")
	public Response getImages(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("targetHeight") int targetHeight, @QueryParam("targetWidth") int targetWidth) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			String acceptHeader = request.getHeader("Accept");
			boolean webP = acceptHeader != null && acceptHeader.contains("image/webp") && targetHeight == 0;
			String mimeType = webP? "image/webp" : "image/jpeg";
			final java.nio.file.Path p = c.getBuldreinfoRepo().getImage(webP, id);
			c.setSuccess();
			if (targetHeight != 0 || targetWidth != 0) {
				BufferedImage b = Preconditions.checkNotNull(ImageIO.read(p.toFile()), "Could not read " + p.toString());
				BufferedImage scaled = null;
				if (targetHeight != 0) {
					scaled = Scalr.resize(b, Scalr.Mode.FIT_TO_HEIGHT, targetHeight);
				}
				else {
					scaled = Scalr.resize(b, Scalr.Mode.FIT_TO_WIDTH, targetWidth);
				}
				b.flush();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(scaled, "jpg", baos);
				byte[] imageData = baos.toByteArray();
				baos.close();
				return Response.ok(imageData, mimeType).build();
			}
			return Response.ok(p.toFile(), mimeType).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/regions")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getRegions(@QueryParam("uniqueId") String uniqueId) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Collection<Region> res = c.getBuldreinfoRepo().getRegions(uniqueId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
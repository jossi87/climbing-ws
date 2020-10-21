package com.buldreinfo.jersey.jaxb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Cameras;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.Filter;
import com.buldreinfo.jersey.jaxb.model.FilterRequest;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Permissions;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemHse;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Sites;
import com.buldreinfo.jersey.jaxb.model.SitesRegion;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.TodoUser;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.pdf.PdfGenerator;
import com.buldreinfo.jersey.jaxb.xml.VegvesenParser;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/v2/")
public class V2 {
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private final static AuthHelper auth = new AuthHelper();
	private final static MetaHelper metaHelper = new MetaHelper();
	private static Logger logger = LogManager.getLogger();

	public V2() {
	}

	@DELETE
	@Path("/media")
	public Response deleteMedia(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(id > 0);
			c.getBuldreinfoRepo().deleteMedia(authUserId, id);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/activity")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getActivity(@Context HttpServletRequest request,
			@QueryParam("idArea") int idArea,
			@QueryParam("idSector") int idSector,
			@QueryParam("lowerGrade") int lowerGrade,
			@QueryParam("fa") boolean fa,
			@QueryParam("comments") boolean comments,
			@QueryParam("ticks") boolean ticks,
			@QueryParam("media") boolean media) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Activity> res = c.getBuldreinfoRepo().getActivity(authUserId, setup, idArea, idSector, lowerGrade, fa, comments, ticks, media);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getAreas(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Response response = null;
			try {
				Area a = c.getBuldreinfoRepo().getArea(authUserId, id);
				metaHelper.updateMetadata(c, a, setup, authUserId, requestedIdMedia);
				response = Response.ok().entity(a).build();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				String url = c.getBuldreinfoRepo().getCanonicalUrl(id, 0, 0);
				response = Response.ok().entity(url).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/areas/pdf")
	@Produces("application/pdf")
	public Response getAreasPdf(@Context final HttpServletRequest request, @QueryParam("accessToken") String accessToken, @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int requestedIdMedia = 0;
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Area area = c.getBuldreinfoRepo().getArea(authUserId, id);
			metaHelper.updateMetadata(c, area, setup, authUserId, requestedIdMedia);
			final Collection<GradeDistribution> gradeDistribution = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, area.getId(), 0);
			final List<Sector> sectors = new ArrayList<>();
			final boolean orderByGrade = false;
			for (Area.Sector sector : area.getSectors()) {
				Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, sector.getId());
				metaHelper.updateMetadata(c, s, setup, authUserId, requestedIdMedia);
				sectors.add(s);
			}
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeArea(area, gradeDistribution, sectors);
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			return Response.ok(stream).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/browse")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getBrowse(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<Area> areas = c.getBuldreinfoRepo().getAreaList(authUserId, setup.getIdRegion());
			Browse res = new Browse(areas);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/cameras")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getCameras(@Context HttpServletRequest request) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			VegvesenParser vegvesenPaser = new VegvesenParser();
			Cameras res = new Cameras(vegvesenPaser.getCameras());
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getFrontpage(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Frontpage res = c.getBuldreinfoRepo().getFrontpage(authUserId, setup);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/grade/distribution")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getGradeDistribution(@Context HttpServletRequest request, @QueryParam("idArea") int idArea, @QueryParam("idSector") int idSector) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<GradeDistribution> res = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, idArea, idSector);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/images")
	@Produces("image/jpeg")
	public Response getImages(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("minDimention") int minDimention) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			boolean webP = false;
			final java.nio.file.Path p = c.getBuldreinfoRepo().getImage(webP, id);
			final Point dimention = minDimention == 0? null : c.getBuldreinfoRepo().getMediaDimention(id);
			c.setSuccess();
			CacheControl cc = new CacheControl();
			cc.setMaxAge(2678400); // 31 days
			cc.setNoTransform(false);
			if (dimention != null) {
				BufferedImage b = Preconditions.checkNotNull(ImageIO.read(p.toFile()), "Could not read " + p.toString());
				Mode mode = dimention.getX() < dimention.getY()? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
				BufferedImage scaled = Scalr.resize(b, mode, minDimention);
				b.flush();
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					ImageIO.write(scaled, "jpg", baos);
					byte[] imageData = baos.toByteArray();
					return Response.ok(imageData).cacheControl(cc).build();
				}
			}
			return Response.ok(p.toFile()).cacheControl(cc).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getMeta(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Meta res = new Meta();
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/permissions")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getPermissions(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Permissions res = c.getBuldreinfoRepo().getPermissions(authUserId, setup.getIdRegion());
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblems(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Response response = null;
			try {
				Problem res = c.getBuldreinfoRepo().getProblem(authUserId, setup, id);
				metaHelper.updateMetadata(c, res, setup, authUserId, requestedIdMedia);
				response = Response.ok().entity(res).build();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				String url = c.getBuldreinfoRepo().getCanonicalUrl(0, 0, id);
				response = Response.ok().entity(url).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems/hse")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblemsHse(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<ProblemHse> res = c.getBuldreinfoRepo().getProblemsHse(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems/pdf")
	@Produces("application/pdf")
	public Response getProblemsPdf(@Context final HttpServletRequest request, @QueryParam("accessToken") String accessToken, @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int requestedIdMedia = 0;
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Problem problem = c.getBuldreinfoRepo().getProblem(authUserId, setup, id);
			metaHelper.updateMetadata(c, problem, setup, authUserId, requestedIdMedia);
			final Area area = c.getBuldreinfoRepo().getArea(authUserId, problem.getAreaId());
			metaHelper.updateMetadata(c, area, setup, authUserId, requestedIdMedia);
			final Sector sector = c.getBuldreinfoRepo().getSector(authUserId, false, setup, problem.getSectorId());
			metaHelper.updateMetadata(c, sector, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeProblem(area, sector, problem);
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			return Response.ok(stream).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/robots.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getRobotsTxt(@Context HttpServletRequest request, @QueryParam("base") String base) {
		final Setup setup = metaHelper.getSetup(request);
		if (setup.isSetRobotsDenyAll()) {
			return Response.ok().entity("User-agent: *\r\nDisallow: /").build(); 
		}
		List<String> lines = Lists.newArrayList(
				"Sitemap: " + setup.getUrl("/sitemap.txt"),
				"User-agent: *",
				"Disallow: /todo/*",
				"Disallow: /*pdf*");
		return Response.ok().entity(Joiner.on("\r\n").join(lines)).build(); 
	}

	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSectors(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			final boolean orderByGrade = setup.isBouldering();
			Response response = null;
			try {
				Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, id);
				metaHelper.updateMetadata(c, s, setup, authUserId, requestedIdMedia);
				response = Response.ok().entity(s).build();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
				String url = c.getBuldreinfoRepo().getCanonicalUrl(0, id, 0);
				response = Response.ok().entity(url).build();
			}
			c.setSuccess();
			return response;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sectors/pdf")
	@Produces("application/pdf")
	public Response getSectorsPdf(@Context final HttpServletRequest request, @QueryParam("accessToken") String accessToken, @QueryParam("id") int id) throws Throwable{
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int requestedIdMedia = 0;
			final int authUserId = auth.getUserId(c, request, metaHelper, accessToken);
			final Sector sector = c.getBuldreinfoRepo().getSector(authUserId, false, setup, id);
			metaHelper.updateMetadata(c, sector, setup, authUserId, requestedIdMedia);
			final Collection<GradeDistribution> gradeDistribution = c.getBuldreinfoRepo().getGradeDistribution(authUserId, setup, 0, id);
			final Area area = c.getBuldreinfoRepo().getArea(authUserId, sector.getAreaId());
			metaHelper.updateMetadata(c, area, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						try (PdfGenerator generator = new PdfGenerator(output)) {
							generator.writeArea(area, gradeDistribution, Lists.newArrayList(sector));
						}
					} catch (Throwable e) {
						e.printStackTrace();
						throw GlobalFunctions.getWebApplicationExceptionInternalError(new Exception(e.getMessage()));
					}	            	 
				}
			};
			return Response.ok(stream).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sitemap.txt")
	@Produces(MediaType.TEXT_PLAIN + "; charset=utf-8")
	public Response getSitemapTxt(@Context HttpServletRequest request, @QueryParam("base") String base) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			String res = c.getBuldreinfoRepo().getSitemapTxt(setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/sites")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSites(@Context HttpServletRequest request, @QueryParam("isBouldering") boolean isBouldering) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<SitesRegion> regions = c.getBuldreinfoRepo().getSites(isBouldering);
			Sites res = new Sites(regions, isBouldering);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStatic(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Frontpage res = c.getBuldreinfoRepo().getFrontpage(authUserId, setup);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/area/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticArea(@Context HttpServletRequest request, @PathParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Area res = c.getBuldreinfoRepo().getArea(authUserId, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/browse")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticBrowse(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Collection<Area> areas = c.getBuldreinfoRepo().getAreaList(authUserId, setup.getIdRegion());
			Browse res = new Browse(areas);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/problem/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticProblem(@Context HttpServletRequest request, @PathParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Problem res = c.getBuldreinfoRepo().getProblem(authUserId, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/sector/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticSector(@Context HttpServletRequest request, @PathParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			final boolean orderByGrade = setup.isBouldering();
			Sector res = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/sites/bouldering")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticSitesBouldering(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final boolean isBouldering = true;
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<SitesRegion> regions = c.getBuldreinfoRepo().getSites(isBouldering);
			Sites res = new Sites(regions, isBouldering);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/sites/climbing")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticSitesClimbing(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final boolean isBouldering = false;
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<SitesRegion> regions = c.getBuldreinfoRepo().getSites(isBouldering);
			Sites res = new Sites(regions, isBouldering);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/static/user/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticUser(@Context HttpServletRequest request, @PathParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			User res = c.getBuldreinfoRepo().getUser(authUserId, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res.getMetadata().toHtml()).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTicks(@Context HttpServletRequest request, @QueryParam("page") int page) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Ticks res = c.getBuldreinfoRepo().getTicks(authUserId, setup, page);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/todo")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTodo(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			TodoUser res = c.getBuldreinfoRepo().getTodo(authUserId, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsers(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			User res = c.getBuldreinfoRepo().getUser(authUserId, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, 0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersSearch(@Context HttpServletRequest request, @QueryParam("value") String value) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			List<User> res = c.getBuldreinfoRepo().getUserSearch(authUserId, value);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/users/ticks")
	@Produces(MIME_TYPE_XLSX)
	public Response getUsersTicks(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId>0, "User not logged in");
			byte[] bytes = c.getBuldreinfoRepo().getUserTicks(authUserId);
			c.setSuccess();
			final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			return Response.ok(bytes, MIME_TYPE_XLSX)
					.header("Content-Disposition", "attachment; filename=\"" + dateTime + ".xlsx\"" )
					.build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/areas")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAreas(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Area a = new Gson().fromJson(multiPart.getField("json").getValue(), Area.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			a = c.getBuldreinfoRepo().setArea(authUserId, setup.getIdRegion(), a, multiPart);
			c.setSuccess();
			return Response.ok().entity(a).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/comments")
	public Response postComments(@Context HttpServletRequest request, Comment co) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().upsertComment(authUserId, co);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/filter")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postFilter(@Context HttpServletRequest request, FilterRequest fr) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Filter> res = c.getBuldreinfoRepo().getFilter(authUserId, setup, fr);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/permissions")
	public Response postPermissions(@Context HttpServletRequest request, PermissionUser u) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().upsertPermissionUser(setup.getIdRegion(), authUserId, u);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/problems")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postProblems(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			p = c.getBuldreinfoRepo().setProblem(authUserId, setup, p, multiPart);
			c.setSuccess();
			return Response.ok().entity(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/problems/media")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postProblemsMedia(@Context HttpServletRequest request, @QueryParam("problemId") int problemId, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			c.getBuldreinfoRepo().addProblemMedia(authUserId, p, multiPart);
			c.setSuccess();
			return Response.ok().entity(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@Context HttpServletRequest request, @QueryParam("problemId") int problemId, @QueryParam("mediaId") int mediaId, Svg svg) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Preconditions.checkNotNull(svg, "Invalid svg=" + svg);
			c.getBuldreinfoRepo().upsertSvg(authUserId, problemId, mediaId, svg);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postSearch(@Context HttpServletRequest request, SearchRequest sr) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			List<Search> res = c.getBuldreinfoRepo().getSearch(authUserId, setup, sr);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/sectors")
	@Consumes(MediaType.MULTIPART_FORM_DATA + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postSectors(@Context HttpServletRequest request, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		Sector s = new Gson().fromJson(multiPart.getField("json").getValue(), Sector.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			final boolean orderByGrade = setup.isBouldering();
			s = c.getBuldreinfoRepo().setSector(authUserId, orderByGrade, setup, s, multiPart);
			c.setSuccess();
			return Response.ok().entity(s).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/ticks")
	public Response postTicks(@Context HttpServletRequest request, Tick t) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(t.getIdProblem() > 0);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setTick(authUserId, setup, t);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/todo")
	@Consumes(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postTodo(@Context HttpServletRequest request, Todo todo) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			c.getBuldreinfoRepo().upsertTodo(authUserId, todo);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/user")
	public Response postUser(@Context HttpServletRequest request, @QueryParam("useBlueNotRed") boolean useBlueNotRed) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setUser(authUserId, useBlueNotRed);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/user/regions")
	public Response postUserRegions(@Context HttpServletRequest request, @QueryParam("regionId") int regionId, @QueryParam("delete") boolean delete) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = getUserId(request);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setUserRegion(authUserId, regionId, delete);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private int getUserId(HttpServletRequest request) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = auth.getUserId(c, request, metaHelper);
			c.setSuccess();
			return authUserId;
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
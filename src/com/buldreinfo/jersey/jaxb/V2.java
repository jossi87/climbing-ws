package com.buldreinfo.jersey.jaxb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.TodoUser;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/v2/")
public class V2 {
	private static Logger logger = LogManager.getLogger();
	private static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private final static AuthHelper auth = new AuthHelper();
	private final static MetaHelper metaHelper = new MetaHelper();
	private final static ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("BuldreinfoCacheRefresher-pool-%d").setDaemon(true).build();
	private final static ExecutorService parentExecutor = Executors.newSingleThreadExecutor(threadFactory);
	private final static ListeningExecutorService executorService = MoreExecutors.listeningDecorator(parentExecutor);
	private final static LoadingCache<String, Frontpage> frontpageCache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.refreshAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<String, Frontpage>() {
				@Override
				public Frontpage load(String key) {
					String[] parts = key.split("_");
					int regionId = Integer.parseInt(parts[0]);
					int authUserId = Integer.parseInt(parts[1]);
					try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
						Setup setup = metaHelper.getSetup(regionId);
						Frontpage f = c.getBuldreinfoRepo().getFrontpage(authUserId, setup);
						metaHelper.updateMetadata(c, f, setup, authUserId, 0);
						c.setSuccess();
						return f;
					} catch (Exception e) {
						throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
					}    
				}
				@Override
				public ListenableFuture<Frontpage> reload(final String key, Frontpage oldFrontpage) throws Exception {
					ListenableFuture<Frontpage> task = executorService.submit(new Callable<Frontpage>() {
						@Override
						public Frontpage call() throws Exception {
							logger.debug("reload(key={}) initialized", key);
							return load(key);
						}
					});
					return task;
				}
			});

	public V2() {
		// Initialize cache
		if (frontpageCache.asMap().isEmpty()) {
			try {
				for (Setup s : metaHelper.getSetups()) {
					frontpageCache.get(s.getIdRegion() + "_-1");
				}
			} catch (Exception e) {
				logger.fatal(e.getMessage(), e);
			}
		}
	}

	@DELETE
	@Path("/media")
	public Response deleteMedia(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkArgument(id > 0);
			c.getBuldreinfoRepo().deleteMedia(authUserId, id);
			invalidateFrontpageCache();
			c.setSuccess();
			return Response.ok().build();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Area a = c.getBuldreinfoRepo().getArea(authUserId, id);
			metaHelper.updateMetadata(c, a, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(a).build();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getFrontpage(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Frontpage res = frontpageCache.get(setup.getIdRegion() + "_" + authUserId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/activity")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getActivity(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			List<Activity> res = c.getBuldreinfoRepo().getActivity(authUserId, setup);
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(scaled, "jpg", baos);
				byte[] imageData = baos.toByteArray();
				baos.close();
				return Response.ok(imageData).cacheControl(cc).build();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Problem res = c.getBuldreinfoRepo().getProblem(authUserId, setup, id);
			metaHelper.updateMetadata(c, res, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(res).build();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			List<ProblemHse> res = c.getBuldreinfoRepo().getProblemsHse(authUserId, setup);
			c.setSuccess();
			return Response.ok().entity(res).build();
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
		return Response.ok().entity("Sitemap: " + setup.getUrl("/sitemap.txt")).build(); 
	}

	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSectors(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("idMedia") int requestedIdMedia) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			final boolean orderByGrade = setup.isBouldering();
			Sector s = c.getBuldreinfoRepo().getSector(authUserId, orderByGrade, setup, id);
			metaHelper.updateMetadata(c, s, setup, authUserId, requestedIdMedia);
			c.setSuccess();
			return Response.ok().entity(s).build();
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
	@Path("/static")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStatic(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Frontpage res = frontpageCache.get(setup.getIdRegion() + "_" + authUserId);
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
	@Path("/static/user/{id}")
	@Produces(MediaType.TEXT_HTML + "; charset=utf-8")
	public Response getStaticUser(@Context HttpServletRequest request, @PathParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			a = c.getBuldreinfoRepo().setArea(authUserId, setup.getIdRegion(), a, multiPart);
			invalidateFrontpageCache();
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			c.getBuldreinfoRepo().upsertComment(authUserId, co);
			invalidateFrontpageCache();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			p = c.getBuldreinfoRepo().setProblem(authUserId, setup, p, multiPart);
			invalidateFrontpageCache();
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			c.getBuldreinfoRepo().addProblemMedia(authUserId, p, multiPart);
			invalidateFrontpageCache();
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			final boolean orderByGrade = setup.isBouldering();
			s = c.getBuldreinfoRepo().setSector(authUserId, orderByGrade, setup, s, multiPart);
			invalidateFrontpageCache();
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
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkArgument(t.getIdProblem() > 0);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setTick(authUserId, setup, t);
			invalidateFrontpageCache();
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
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
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(c, request, setup.getIdRegion());
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setUser(authUserId, useBlueNotRed);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private void invalidateFrontpageCache() {
		for (String key : frontpageCache.asMap().keySet()) {
			if (key.endsWith("_-1")) {
				frontpageCache.refresh(key);
			}
			else {
				frontpageCache.invalidate(key);
			}
		}
	}
}
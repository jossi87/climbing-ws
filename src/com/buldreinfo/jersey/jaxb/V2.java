package com.buldreinfo.jersey.jaxb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.Finder;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserEdit;
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
						metaHelper.updateMetadata(c, f, setup, authUserId);
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
			final int authUserId = auth.getUserId(request);
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
	public Response getAreas(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			Area a = c.getBuldreinfoRepo().getArea(authUserId, id);
			metaHelper.updateMetadata(c, a, setup, authUserId);
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
			final int authUserId = auth.getUserId(request);
			Collection<Area> areas = c.getBuldreinfoRepo().getAreaList(authUserId, setup.getIdRegion());
			Browse res = new Browse(areas);
			metaHelper.updateMetadata(c, res, setup, authUserId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/finder")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getFinder(@Context HttpServletRequest request, @QueryParam("grade") int grade) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			List<Problem> problems = c.getBuldreinfoRepo().getProblem(authUserId, setup.getIdRegion(), 0, grade);
			Finder res = new Finder(grade, GradeHelper.getGrades(setup.getIdRegion()).get(grade), problems);
			metaHelper.updateMetadata(c, res, setup, authUserId);
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
		try {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			return Response.ok().entity(frontpageCache.get(setup.getIdRegion() + "_" + authUserId)).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

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
	@Path("/meta")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getMeta(@Context HttpServletRequest request) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			Meta res = new Meta();
			metaHelper.updateMetadata(c, res, setup, authUserId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblems(@Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("grade") int grade) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			List<Problem> res = c.getBuldreinfoRepo().getProblem(authUserId, setup.getIdRegion(), id, grade);
			if (res.size() == 1) {
				metaHelper.updateMetadata(c, res.get(0), setup, authUserId);
			}
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
	public Response getSectors(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			Sector s = c.getBuldreinfoRepo().getSector(authUserId, setup.getIdRegion(), id);
			metaHelper.updateMetadata(c, s, setup, authUserId);
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
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsers(@Context HttpServletRequest request, @QueryParam("id") int id) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final Setup setup = metaHelper.getSetup(request);
			final int authUserId = auth.getUserId(request);
			User res = c.getBuldreinfoRepo().getUser(authUserId, setup.getIdRegion(), id);
			metaHelper.updateMetadata(c, res, setup, authUserId);
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
			final int authUserId = auth.getUserId(request);
			List<User> res = c.getBuldreinfoRepo().getUserSearch(authUserId, value);
			c.setSuccess();
			return Response.ok().entity(res).build();
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
			final int authUserId = auth.getUserId(request);
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			a = c.getBuldreinfoRepo().setArea(authUserId, setup.getIdRegion(), a, multiPart);
			invalidateFrontpageCache();
			c.setSuccess();
			return Response.ok().entity(a).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	/*
	function changePassword (email, newPassword, callback) {
	  var crypto = require('crypto');
	  var fetch = require('isomorphic-fetch');
	  fetch(encodeURI(`https://buldreinfo.com/com.buldreinfo.jersey.jaxb/v2/auth0/changePassword`),{
	    mode: 'cors',
	    method: 'POST',
	    credentials: 'include',
	    body: "username=" + encodeURIComponent(email) + "&newPassword=" + encodeURIComponent(crypto.createHash('md5').update(newPassword).digest("hex")),
	    headers: {
	      'Content-Type': 'application/x-www-form-urlencoded',
	      'Accept': 'application/json'
	    }
	  })
	  .then((response) => response.json())
	  .then((updated) => callback(null, updated))
	  .catch ((error) => callback(error));
	}
	 */
	@POST
	@Path("/auth0/changePassword")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAuth0ChangePassword(@FormParam("username") String username, @FormParam("newPassword") String newPassword) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			boolean res = c.getBuldreinfoRepo().changePassword(username, newPassword);
			c.setSuccess();
			return Response.ok(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	/*
	function create (user, callback) {
	  var crypto = require('crypto');
	  var fetch = require('isomorphic-fetch');
	  fetch(encodeURI(`https://buldreinfo.com/com.buldreinfo.jersey.jaxb/v2/auth0/create`),{
	    mode: 'cors',
	    method: 'POST',
	    credentials: 'include',
	    body: "email=" + encodeURIComponent(user.email) + "&username=" + encodeURIComponent(user.username) + "&password=" + encodeURIComponent(crypto.createHash('md5').update(user.password).digest("hex")) + "&firstname=" + encodeURIComponent(user.user_metadata.firstname) + "&lastname=" + encodeURIComponent(user.user_metadata.lastname),
	    headers: {
	      'Content-Type': 'application/x-www-form-urlencoded',
	      'Accept': 'application/json'
	    }
	  })
	  .then((response) => {
	    if (response.status === 409) {
	      callback(new ValidationError("user_exists", "User already exists"));
	    } else if (response.status === 201) {
	      callback(null);
	    }
	  })
	  .catch ((error) => callback(error));
	}
	 */
	@POST
	@Path("/auth0/create")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAuth0ChangePassword(
			@FormParam("email") String email,
			@FormParam("username") String username,
			@FormParam("password") String password,
			@FormParam("firstname") String firstname,
			@FormParam("lastname") String lastname) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Profile p = c.getBuldreinfoRepo().getProfile(username);
			if (p != null) {
				return Response.status(409).build();
			}
			c.getBuldreinfoRepo().createUser(email, username, password, firstname, lastname);
			c.setSuccess();
			return Response.status(201).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	/*
	function getByEmail (email, callback) {
	  var fetch = require('isomorphic-fetch');
	  fetch(encodeURI(`https://buldreinfo.com/com.buldreinfo.jersey.jaxb/v2/auth0/getByEmail`),{
	    method: 'POST',
	    body: "username=" + encodeURIComponent(email),
	    headers: {
	      'Content-Type': 'application/x-www-form-urlencoded',
	      'Accept': 'application/json'
	    }
	  })
	  .then((response) => {
	    if (response.status === 400) {
	      callback(null);
	    } else {
	      response.json().then((profile) => callback(null, profile));
	    }
	  })
	  .catch ((error) => callback(error));
	}
	 */
	@POST
	@Path("/auth0/getByEmail")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAuth0GetByEmail(@FormParam("username") String username) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Profile p = c.getBuldreinfoRepo().getProfile(username);
			c.setSuccess();
			if (p == null) {
				return Response.status(400).build();
			}
			return Response.ok(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	/*
	function login (email, password, callback) {
	  var crypto = require('crypto');
	  var fetch = require('isomorphic-fetch');
	  fetch(encodeURI(`https://buldreinfo.com/com.buldreinfo.jersey.jaxb/v2/auth0/login`),{
	    mode: 'cors',
	    method: 'POST',
	    credentials: 'include',
	    body: "username=" + encodeURIComponent(email) + "&password=" + encodeURIComponent(crypto.createHash('md5').update(password).digest("hex")),
	    headers: {
	      'Content-Type': 'application/x-www-form-urlencoded',
	      'Accept': 'application/json'
	    }
	  })
	  .then((response) => {
	    if (response.status === 401) {
	      callback(new Error("Invalid username/password"));
	    } else {
	      response.json().then((profile) => callback(null, profile));
	    }
	  })
	  .catch ((error) => callback(error));
	}
	 */
	@POST
	@Path("/auth0/login")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postAuth0Login(@FormParam("username") String username, @FormParam("password") String password) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Profile p = c.getBuldreinfoRepo().getProfile(username, password);
			c.setSuccess();
			if (p == null) {
				return Response.status(401).build();
			}
			return Response.ok(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/comments")
	public Response postComments(@Context HttpServletRequest request, Comment co) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = auth.getUserId(request);
			Preconditions.checkNotNull(Strings.emptyToNull(co.getComment()));
			c.getBuldreinfoRepo().addComment(authUserId, co);
			invalidateFrontpageCache();
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
			final int authUserId = auth.getUserId(request);
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			p = c.getBuldreinfoRepo().setProblem(authUserId, setup.getIdRegion(), p, multiPart);
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
			final int authUserId = auth.getUserId(request);
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
			final int authUserId = auth.getUserId(request);
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
			final int authUserId = auth.getUserId(request);
			List<Search> res = c.getBuldreinfoRepo().getSearch(authUserId, setup.getIdRegion(), sr);
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
			final int authUserId = auth.getUserId(request);
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			s = c.getBuldreinfoRepo().setSector(authUserId, setup.getIdRegion(), s, multiPart);
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
			final int authUserId = auth.getUserId(request);
			Preconditions.checkArgument(t.getIdProblem() > 0);
			Preconditions.checkArgument(authUserId != -1);
			c.getBuldreinfoRepo().setTick(authUserId, setup.getIdRegion(), t);
			invalidateFrontpageCache();
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/users/edit")
	public Response postUsersEdit(@Context HttpServletRequest request, UserEdit u) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			final int authUserId = auth.getUserId(request);
			c.getBuldreinfoRepo().setUser(authUserId, u);
			invalidateFrontpageCache();
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
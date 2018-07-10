package com.buldreinfo.jersey.jaxb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.imgscalr.Scalr;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.Permission;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Register;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.UserEdit;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/v1/")
public class V1 {
	private static final String COOKIE_NAME = "buldreinfo";
	
	@DELETE
	@Path("/media")
	public Response deleteMedia(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("id") int id) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(id > 0);
			c.getBuldreinfoRepo().deleteMedia(token, id);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/areas")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getAreas(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("id") int id) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Area a = c.getBuldreinfoRepo().getArea(token, id);
			c.setSuccess();
			return Response.ok().entity(a).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/areas/list")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getAreasList(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(regionId>0);
			Collection<Area> areas = c.getBuldreinfoRepo().getAreaList(token, regionId);
			c.setSuccess();
			return Response.ok().entity(areas).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/frontpage")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getFrontpage(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(regionId>0);
			Frontpage f = c.getBuldreinfoRepo().getFrontpage(token, regionId);
			c.setSuccess();
			return Response.ok().entity(f).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/grades")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getGrades(@QueryParam("regionId") int regionId) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<Grade> res = new ArrayList<>();
			Map<Integer, String> grades = GradeHelper.getGrades(regionId);
			for (int id : grades.keySet()) {
				res.add(new Grade(id, grades.get(id)));
			}
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/images")
	public Response getImages(@CookieParam(COOKIE_NAME) Cookie cookie, @Context HttpServletRequest request, @QueryParam("id") int id, @QueryParam("targetHeight") int targetHeight, @QueryParam("targetWidth") int targetWidth) throws ExecutionException, IOException {
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
	@Path("/logout")
	public Response getLogout(@CookieParam(COOKIE_NAME) Cookie cookie) {
	    if (cookie != null) {
	    	try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
				c.getBuldreinfoRepo().deleteToken(cookie.getValue());
				c.setSuccess();
			} catch (Exception e) {
				throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
			}
	        NewCookie newCookie = new NewCookie(cookie, null, 0, false);
	        return Response.ok().cookie(newCookie).build();
	    }
	    return Response.ok().build();
	}
	
	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblems(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, @QueryParam("id") int id, @QueryParam("grade") int grade) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<Problem> res = c.getBuldreinfoRepo().getProblem(token, regionId, id, grade);
			c.setSuccess();
			return Response.ok().entity(res).build();
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

	@GET
	@Path("/sectors")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getSectors(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, @QueryParam("id") int id) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Sector s = c.getBuldreinfoRepo().getSector(token, regionId, id);
			c.setSuccess();
			return Response.ok().entity(s).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@GET
	@Path("/types")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getTypes(@QueryParam("regionId") int regionId) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<Type> res = c.getBuldreinfoRepo().getTypes(regionId);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsers(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, @QueryParam("id") int id) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			User res = c.getBuldreinfoRepo().getUser(token, regionId, id);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users/edit")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersEdit(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, @QueryParam("id") int id) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(token);
			Preconditions.checkArgument(regionId>0);
			Preconditions.checkArgument(id>0);
			UserEdit res = c.getBuldreinfoRepo().getUserEdit(token, regionId, id);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users/forgotPassword")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersForgotPassword(@QueryParam("username") String username, @QueryParam("hostname") String hostname) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(username));
			c.getBuldreinfoRepo().forgotPassword(username, hostname);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users/password")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersPassword(@QueryParam("token") String token, @QueryParam("password") String password) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(token));
			Preconditions.checkNotNull(Strings.emptyToNull(password));
			c.getBuldreinfoRepo().resetPassword(token, password);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsersSearch(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("value") String value) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<User> res = c.getBuldreinfoRepo().getUserSearch(token, value);
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
	public Response postAreas(@CookieParam(COOKIE_NAME) Cookie cookie, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		Area a = new Gson().fromJson(multiPart.getField("json").getValue(), Area.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(a.getName()));
			Preconditions.checkArgument(a.getRegionId() > 0);
			a = c.getBuldreinfoRepo().setArea(token, a, multiPart);
			c.setSuccess();
			return Response.ok().entity(a).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/comments")
	public Response postComments(@CookieParam(COOKIE_NAME) Cookie cookie, Comment co) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(co.getComment()));
			c.getBuldreinfoRepo().addComment(token, co);
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
	public Response postProblems(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			// Preconditions.checkArgument(p.getAreaId() > 1); <--ZERO! Problems don't contain areaId from react-http-post
			Preconditions.checkArgument(p.getSectorId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(p.getName()));
			p = c.getBuldreinfoRepo().setProblem(token, regionId, p, multiPart);
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
	public Response postProblemsMedia(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("problemId") int problemId, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		Problem p = new Gson().fromJson(multiPart.getField("json").getValue(), Problem.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(p.getId() > 0);
			Preconditions.checkArgument(!p.getNewMedia().isEmpty());
			c.getBuldreinfoRepo().addProblemMedia(token, p, multiPart);
			c.setSuccess();
			return Response.ok().entity(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/problems/svg")
	public Response postProblemsSvg(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("problemId") int problemId, @QueryParam("mediaId") int mediaId, Svg svg) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(problemId>0, "Invalid problemId=" + problemId);
			Preconditions.checkArgument(mediaId>0, "Invalid mediaId=" + mediaId);
			Preconditions.checkNotNull(svg, "Invalid svg=" + svg);
			c.getBuldreinfoRepo().upsertSvg(token, problemId, mediaId, svg);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postSearch(@CookieParam(COOKIE_NAME) Cookie cookie, SearchRequest sr) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<Search> res = c.getBuldreinfoRepo().getSearch(token, sr);
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
	public Response postSectors(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, FormDataMultiPart multiPart) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		Sector s = new Gson().fromJson(multiPart.getField("json").getValue(), Sector.class);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(s.getAreaId() > 1);
			Preconditions.checkNotNull(Strings.emptyToNull(s.getName()));
			s = c.getBuldreinfoRepo().setSector(token, regionId, s, multiPart);
			c.setSuccess();
			return Response.ok().entity(s).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/ticks")
	public Response postTicks(@CookieParam(COOKIE_NAME) Cookie cookie, @QueryParam("regionId") int regionId, Tick t) throws ExecutionException, IOException {
		final String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(t.getIdProblem() > 0);
			Preconditions.checkNotNull(token);
			c.getBuldreinfoRepo().setTick(token, regionId, t);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/users/edit")
	public Response postUsersEdit(@CookieParam(COOKIE_NAME) Cookie cookie, UserEdit u) throws ExecutionException, IOException {
		String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(token);
			Preconditions.checkNotNull(u);
			Preconditions.checkArgument(u.getRegionId() > 0);
			final Permission p = c.getBuldreinfoRepo().setUser(token, u);
			c.setSuccess();
			if (p != null && !Strings.isNullOrEmpty(p.getToken())) { // Return new token (new password)
				return Response.ok().cookie(getBuldreinfoCookie(p.getToken())).build();
			}
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	@POST
	@Path("/users/login")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED + "; charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postUsersLogin(@CookieParam(COOKIE_NAME) Cookie cookie, @FormParam("username") String username, @FormParam("password") String password, @FormParam("regionId") int regionId) {
		String token = cookie != null? cookie.getValue() : null;
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkArgument(regionId > 0);
			Permission p = c.getBuldreinfoRepo().getPermission(token, username, password);
			c.setSuccess();
			int visibility = -1;
			NewCookie newCookie = null;
			if (p != null && !Strings.isNullOrEmpty(p.getToken())) {
				if (p.getSuperAdminRegionIds().contains(regionId)) {
					visibility = 2;
				}
				else if (p.getAdminRegionIds().contains(regionId)) {
					visibility = 1;
				}
				else {
					visibility = 0;
				}
				newCookie = getBuldreinfoCookie(p.getToken());
			}
			return Response.ok(visibility).cookie(newCookie).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/users/register")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postUsersRegister(Register r) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(r.getFirstname()));
			Preconditions.checkNotNull(Strings.emptyToNull(r.getLastname()));
			Preconditions.checkNotNull(Strings.emptyToNull(r.getUsername()));
			Preconditions.checkNotNull(Strings.emptyToNull(r.getPassword()));
			c.getBuldreinfoRepo().registerUser(r);
			c.setSuccess();
			return Response.ok().build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	private NewCookie getBuldreinfoCookie(String token) {
		String path = "/";
		String domain = null;
		String comment = "buldreinfo token";
		int version = 0; // Necessary for IE (http://coodoo.de/blog/2016/02/java-ee-7-jax-rs-und-cookies-im-response/)
		int maxAgeSeconds = 30*24*3600; // 1 month
		Date expiry = Date.from(LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant()); // Necessary for IE (http://coodoo.de/blog/2016/02/java-ee-7-jax-rs-und-cookies-im-response/)
		boolean secure = false;
		boolean httpOnly = false;
		return new NewCookie(COOKIE_NAME, token, path, domain, version, comment, maxAgeSeconds, expiry, secure, httpOnly);
	}
}
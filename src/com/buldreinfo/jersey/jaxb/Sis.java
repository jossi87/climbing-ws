package com.buldreinfo.jersey.jaxb;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.SisProblem;
import com.buldreinfo.jersey.jaxb.model.SisTick;
import com.buldreinfo.jersey.jaxb.model.SisUser;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
@Path("/sis/")
public class Sis {
	@GET
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getProblems() throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			List<SisProblem> res = c.getSisRepo().getProblems(0);
			c.setSuccess();
			return Response.ok().entity(res).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response getUsers(@QueryParam("email") String email, @QueryParam("name") String name, @QueryParam("facebookUserId") String facebookUserId) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			// Preconditions.checkArgument(!Strings.isNullOrEmpty(email)); email can now be null
			Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
			Preconditions.checkArgument(!Strings.isNullOrEmpty(facebookUserId));
			SisUser u = c.getSisRepo().getUser(facebookUserId, email, name);
			c.setSuccess();
			return Response.ok().entity(u).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/problems")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postProblems(SisProblem p) throws ExecutionException, IOException {
		Preconditions.checkArgument(p != null);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			Preconditions.checkNotNull(Strings.emptyToNull(p.getImage()));
			Preconditions.checkNotNull(Strings.emptyToNull(p.getGrade()));
			Preconditions.checkNotNull(Strings.emptyToNull(p.getType()));
			p = c.getSisRepo().setProblem(p);
			c.setSuccess();
			return Response.ok(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	@POST
	@Path("/ticks")
	@Produces(MediaType.APPLICATION_JSON + "; charset=utf-8")
	public Response postTicks(SisTick t) throws ExecutionException, IOException {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			SisProblem p = c.getSisRepo().setTick(t);
			c.setSuccess();
			return Response.ok(p).build();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
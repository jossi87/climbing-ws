package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumMedia;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumProblems;
import com.buldreinfo.jersey.jaxb.model.FrontpageNumTicks;
import com.buldreinfo.jersey.jaxb.model.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemArea;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class V2Test {

	@Test
	public void testGetActivity() throws Exception {
		V2 tester = new V2();
		Response r = tester.getActivity(getRequest(), 0, 0, 0, true, true, true, true);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
	}

	@Test
	public void testGetAreas() throws Exception {
		V2 tester = new V2();
		// All areas
		Response r = tester.getAreas(getRequest(), 0);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		@SuppressWarnings("unchecked")
		Collection<Area> areas = (Collection<Area>)r.getEntity();
		assertTrue(areas.size() > 1);
		// One area
		r = tester.getAreas(getRequest(), 7);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		@SuppressWarnings("unchecked")
		Collection<Area> area = (Collection<Area>)r.getEntity();
		assertTrue(area.size() == 1);
	}

	@Test
	public void testGetDangerous() throws Exception {
		V2 tester = new V2();
		Response r = tester.getDangerous(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
	}

	@Test
	public void testGetFrontpageNumMedia() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFrontpageNumMedia(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof FrontpageNumMedia);
		FrontpageNumMedia f = (FrontpageNumMedia)r.getEntity();
		assertTrue(f.numImages()>0);
		assertTrue(f.numMovies()>0);
	}
	
	@Test
	public void testGetFrontpageNumProblems() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFrontpageNumProblems(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof FrontpageNumProblems);
		FrontpageNumProblems f = (FrontpageNumProblems)r.getEntity();
		assertTrue(f.numProblems()>0);
		assertTrue(f.numProblemsWithCoordinates()>0);
		assertTrue(f.numProblemsWithTopo()>0);
	}
	
	@Test
	public void testGetFrontpageNumTicks() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFrontpageNumTicks(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof FrontpageNumTicks);
		FrontpageNumTicks f = (FrontpageNumTicks)r.getEntity();
		assertTrue(f.numTicks()>0);
	}
	
	@Test
	public void testGetFrontpageRandomMedia() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFrontpageRandomMedia(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof FrontpageRandomMedia);
		FrontpageRandomMedia f = (FrontpageRandomMedia)r.getEntity();
		assertTrue(f != null);
	}

	@Test
	public void testGetMeta() throws Exception {
		V2 tester = new V2();
		Response r = tester.getMeta(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Meta);
	}

	@Test
	public void testGetProblem() throws Exception {
		V2 tester = new V2();
		Response r = tester.getProblem(getRequest(), 1193, false);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Problem);
		Problem p = (Problem)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(p.getName()));			
	}

	@Test
	public void testGetProblems() throws Exception {
		V2 tester = new V2();
		Response r = tester.getProblems(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		@SuppressWarnings("unchecked")
		Collection<ProblemArea> problemAreas = (Collection<ProblemArea>)r.getEntity();
		assertTrue(problemAreas.size() > 1);
	}

	@Test
	public void testGetProfile() throws Exception {
		V2 tester = new V2();
		// User: Jostein
		Response r = tester.getProfile(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Profile);
		Profile u = (Profile)r.getEntity();
		assertTrue(u.firstname() != null);
	}

	@Test
	public void testGetSectors() throws Exception {
		V2 tester = new V2();
		Response r = tester.getSectors(getRequest(), 47);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Sector);
		Sector s = (Sector)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(s.getName()));
		assertTrue(!s.getProblems().isEmpty());
	}

	@Test
	public void testGetTicks() throws Exception {
		V2 tester = new V2();
		Response r = tester.getTicks(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Ticks);
	}

	@Test
	public void testGetTodo() throws Exception {
		V2 tester = new V2();
		// User: Jostein
		Response r = tester.getProfileTodo(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof ProfileTodo);
		ProfileTodo t = (ProfileTodo)r.getEntity();
		assertTrue(!t.areas().isEmpty());
	}

	@Test
	public void testPostSearch() throws Exception {
		V2 tester = new V2();
		Response r = tester.postSearch(getRequest(), new SearchRequest("Pan"));
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<Search> res = (List<Search>)r.getEntity();
		assertTrue(!res.isEmpty());
	}

	private HttpServletRequest getRequest() {
		HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(req.getHeader(HttpHeaders.ORIGIN)).andReturn("https://buldreinfo.com").anyTimes();
		EasyMock.expect(req.getHeader(HttpHeaders.AUTHORIZATION)).andReturn(null).anyTimes();
		EasyMock.expect(req.getServerName()).andReturn("buldreinfo.com").anyTimes();
		EasyMock.replay(req);
		return req;
	}
}
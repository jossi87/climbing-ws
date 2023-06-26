package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Filter;
import com.buldreinfo.jersey.jaxb.model.FilterRequest;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
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
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;

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
	public void testGetFrontpage() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFrontpage(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Frontpage);
		Frontpage f = (Frontpage)r.getEntity();
		assertTrue(f.getNumImages()>0);
		assertTrue(f.getRandomMedia() != null);
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
		// User: Jostein Ø
		Response r = tester.getProfile(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Profile);
		Profile u = (Profile)r.getEntity();
		assertTrue(u.getFirstname() != null);
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
		// User: Jostein Ø
		Response r = tester.getProfileTodo(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof ProfileTodo);
		ProfileTodo t = (ProfileTodo)r.getEntity();
		assertTrue(!t.getAreas().isEmpty());
	}

	@Test
	public void testPostFilter() throws Exception {
		V2 tester = new V2();
		Response r = tester.postFilter(getRequest(), new FilterRequest(Lists.newArrayList(19,20),Lists.newArrayList(1,2,3,4),null));
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<Filter> res = (List<Filter>)r.getEntity();
		assertTrue(!res.isEmpty());
	}

	@Test
	public void testPostSearch() throws Exception {
		V2 tester = new V2();
		Response r = tester.postSearch(getRequest(), new SearchRequest(1, "Pan"));
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
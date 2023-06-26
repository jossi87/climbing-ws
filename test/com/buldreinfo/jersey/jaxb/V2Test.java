package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Areas;
import com.buldreinfo.jersey.jaxb.model.Filter;
import com.buldreinfo.jersey.jaxb.model.FilterRequest;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Problems;
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
	public void testGetAreaById() throws Exception {
		V2 tester = new V2();
		Response r = tester.getAreaById(getRequest(), 7, 0);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Area);
		Area a = (Area)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(a.getName()));
		assertTrue(!a.getSectors().isEmpty());
	}
	
	@Test
	public void testGetAreas() throws Exception {
		V2 tester = new V2();
		Response r = tester.getAreas(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Areas);
		Areas b = (Areas)r.getEntity();
		assertTrue(!b.getAreas().isEmpty());
		assertTrue(b.getMetadata() != null);
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
		assertTrue(f.getMetadata() != null);
	}
	
	@Test
	public void testGetMeta() throws Exception {
		V2 tester = new V2();
		Response r = tester.getMeta(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Meta);
		Meta m = (Meta)r.getEntity();
		assertTrue(m.getMetadata() != null);
	}
	
	@Test
	public void testGetProblemById() throws Exception {
		V2 tester = new V2();
		Response r = tester.getProblemById(getRequest(), 1193, 0, false);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Problem);
		Problem p = (Problem)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(p.getName()));			
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
		Response r = tester.getSectors(getRequest(), 47, 0);
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
	public void testGetProblems() throws Exception {
		V2 tester = new V2();
		Response r = tester.getProblems(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Problems);
		Problems b = (Problems)r.getEntity();
		assertTrue(!b.getAreas().isEmpty());
		assertTrue(b.getMetadata() != null);
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
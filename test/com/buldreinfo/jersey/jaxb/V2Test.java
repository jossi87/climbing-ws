package com.buldreinfo.jersey.jaxb;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.easymock.EasyMock;
import org.junit.Test;

import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Browse;
import com.buldreinfo.jersey.jaxb.model.Finder;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

public class V2Test {
	
	@Test
	public void testGetAreas() throws Exception {
		V2 tester = new V2();
		Response r = tester.getAreas(getRequest(), 7);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Area);
		Area a = (Area)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(a.getName()));
		assertTrue(!a.getSectors().isEmpty());
	}
	
	@Test
	public void testGetBrowse() throws Exception {
		V2 tester = new V2();
		Response r = tester.getBrowse(getRequest());
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Browse);
		Browse b = (Browse)r.getEntity();
		assertTrue(!b.getAreas().isEmpty());
		assertTrue(b.getMetadata() != null);
	}
	
	@Test
	public void testGetFinder() throws Exception {
		V2 tester = new V2();
		Response r = tester.getFinder(getRequest(), 11);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Finder);
		Finder f = (Finder)r.getEntity();
		assertTrue(!f.getProblems().isEmpty());
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
	public void testGetProblems() throws Exception {
		V2 tester = new V2();
		Response r = tester.getProblems(getRequest(), 1193, 0);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		List<?> list = (List<?>)r.getEntity();
		assertTrue(!list.isEmpty());
		for (Object e : list) {
			Problem p = (Problem)e;
			assertTrue(!Strings.isNullOrEmpty(p.getName()));			
		}
		
		r = tester.getProblems(getRequest(), 0, 19);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		list = (List<?>)r.getEntity();
		assertTrue(!list.isEmpty());
		for (Object e : list) {
			Problem p = (Problem)e;
			assertTrue(!Strings.isNullOrEmpty(p.getName()));			
		}
		
		r = tester.getProblems(getRequest(), 0, -1); // SuperAdmin only!
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		list = (List<?>)r.getEntity();
		assertTrue(list.isEmpty()); // Only superadmins
	}
	
	@Test
	public void testGetRegions() throws Exception {
		int other = getRegionAreas("0000000000000000");
		int stian = getRegionAreas("d5f87f487cc3a821");
		int jostein = getRegionAreas("c1a490a9060cab5a");
		assertTrue(other > 0 && stian > 0 && jostein > 0);
		assertTrue(stian > other);
		assertTrue(jostein > stian);
	}
	
	@Test
	public void testGetSearch() throws Exception {
		V2 tester = new V2();
		Response r = tester.postSearch(getRequest(), new SearchRequest(1, "Pan"));
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<Search> res = (List<Search>)r.getEntity();
		assertTrue(!res.isEmpty());
	}
	
	@Test
	public void testGetSectors() throws Exception {
		V2 tester = new V2();
		Response r = tester.getSectors(getRequest(), 278);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Sector);
		Sector s = (Sector)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(s.getName()));
		assertTrue(!s.getProblems().isEmpty());
		assertTrue(!Strings.isNullOrEmpty(s.getProblems().get(0).getComment()));
	}
	
	@Test
	public void testGetUsers() throws Exception {
		V2 tester = new V2();
		// User: Jostein Ø
		Response r = tester.getUsers(getRequest(), 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof User);
		User u = (User)r.getEntity();
		assertTrue(!u.getTicks().isEmpty());
		assertTrue(u.getNumImagesCreated()>0);
		assertTrue(u.getNumVideosCreated()>0);
		assertTrue(u.getNumImageTags()>0);
		assertTrue(u.getNumVideoTags()>0);
		// User: jossi@jossi.org
		r = tester.getUsers(getRequest(), 1311);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof User);
		u = (User)r.getEntity();
		assertTrue(u.getTicks().isEmpty());
		assertTrue(u.getNumImagesCreated()==0);
		assertTrue(u.getNumVideosCreated()==0);
		assertTrue(u.getNumImageTags()==0);
		assertTrue(u.getNumVideoTags()==0);
	}
	
	private int getRegionAreas(String uniqueId) throws ExecutionException, IOException {
		int numAreas = 0;
		V2 tester = new V2();
		Response r = tester.getRegions(uniqueId);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		Collection<?> res = (Collection<?>) r.getEntity();
		assertTrue(!res.isEmpty());
		for (Object o : res) {
			assertTrue(o instanceof Region);
			Region region = (Region)o;
			numAreas += region.getAreas().size();
		}
		return numAreas;
	}
	
	private HttpServletRequest getRequest() {
		HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(req.getHeader(HttpHeaders.ORIGIN)).andReturn("https://buldreinfo.com").anyTimes();
		EasyMock.expect(req.getServerName()).andReturn("buldreinfo.com").anyTimes();
		EasyMock.replay(req);
		return req;
	}
}
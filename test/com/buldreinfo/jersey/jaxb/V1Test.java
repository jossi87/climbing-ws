package com.buldreinfo.jersey.jaxb;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.google.common.base.Strings;

public class V1Test {
	private final static int REGION_ID = 1;
	
	@Test
	public void testGetAreas() throws Exception {
		V1 tester = new V1();
		Response r = tester.getAreas(null, REGION_ID, 7);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Area);
		Area a = (Area)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(a.getName()));
		assertTrue(!a.getSectors().isEmpty());
	}
	
	@Test
	public void testGetAreasList() throws Exception {
		V1 tester = new V1();
		Response r = tester.getAreasList(null, REGION_ID);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		Collection<?> res = (Collection<?>) r.getEntity();
		assertTrue(!res.isEmpty());
		for (Object o : res) {
			assertTrue(o instanceof Area);
		}
	}
	
	@Test
	public void testGetFrontpage() throws Exception {
		V1 tester = new V1();
		Response r = tester.getFrontpage(null, REGION_ID);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Frontpage);
		Frontpage f = (Frontpage)r.getEntity();
		assertTrue(f.getNumImages()>0);
		assertTrue(f.getRandomMedia() != null);
	}
	
	@Test
	public void testGetGrades() throws Exception {
		V1 tester = new V1();
		Response r = tester.getGrades(REGION_ID);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		List<?> list = (List<?>)r.getEntity();
		assertTrue(!list.isEmpty());
	}
	
	@Test
	public void testGetProblems() throws Exception {
		V1 tester = new V1();
		Response r = tester.getProblems(null, 0, 1193, 0);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		List<?> list = (List<?>)r.getEntity();
		assertTrue(!list.isEmpty());
		for (Object e : list) {
			Problem p = (Problem)e;
			assertTrue(!Strings.isNullOrEmpty(p.getName()));			
		}
		
		r = tester.getProblems(null, 0, 0, 19);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		list = (List<?>)r.getEntity();
		assertTrue(!list.isEmpty());
		for (Object e : list) {
			Problem p = (Problem)e;
			assertTrue(!Strings.isNullOrEmpty(p.getName()));			
		}
		
		r = tester.getProblems(null, 0, 0, -1); // SuperAdmin only!
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
		V1 tester = new V1();
		Response r = tester.postSearch(null, new SearchRequest(REGION_ID, "Pan"));
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof List<?>);
		@SuppressWarnings("unchecked")
		List<Search> res = (List<Search>)r.getEntity();
		assertTrue(!res.isEmpty());
	}
	
	@Test
	public void testGetSectors() throws Exception {
		V1 tester = new V1();
		Response r = tester.getSectors(null, REGION_ID, 278);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Sector);
		Sector s = (Sector)r.getEntity();
		assertTrue(!Strings.isNullOrEmpty(s.getName()));
		assertTrue(!s.getProblems().isEmpty());
		assertTrue(!Strings.isNullOrEmpty(s.getProblems().get(0).getComment()));
	}
	
//	@Test
//	public void testGetMedia() throws Exception {
//		V1 tester = new V1();
//		Response r = tester.getMedia(18323);
//		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
//		assertTrue(r.getEntity() instanceof byte[]);
//	}
	
	@Test
	public void testGetUsers() throws Exception {
		V1 tester = new V1();
		// User: Jostein Ø
		Response r = tester.getUsers(null, REGION_ID, 1);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof User);
		User u = (User)r.getEntity();
		assertTrue(!u.getTicks().isEmpty());
		assertTrue(u.getNumImagesCreated()>0);
		assertTrue(u.getNumVideosCreated()>0);
		assertTrue(u.getNumImageTags()>0);
		assertTrue(u.getNumVideoTags()>0);
		// User: jossi@jossi.org
		r = tester.getUsers(null, REGION_ID, 1311);
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
		V1 tester = new V1();
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
}
package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.resources.ApiRootResource;

import jakarta.ws.rs.core.Response;

public class ApiRootResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetActivity() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, true, true, true, 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
		try (Response r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, false, false, false, 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
	}
	
	@Test
	public void testGetDangerous() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getDangerous(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetFrontpage() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getFrontpage(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetMeta() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getMeta(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Meta);
		}
	}

	@Test
	public void testGetRestrictions() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getRestrictions(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetRobotsTxt() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getRobotsTxt(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetSitemapTxt() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getSitemapTxt(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetTicks() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getTicks(getRequest(Region.buldreinfo), 1)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Ticks);
		}
	}

	@Test
	public void testGetToc() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getToc(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Toc);
		}
	}

	@Test
	public void testGetTop() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.getTop(getRequest(Region.brattelinjer), 2738, 0)) { // Dale
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Top);
		}
		try (Response r = tester.getTop(getRequest(Region.brattelinjer), 0, 2857)) { // Dale / Hovedveggen
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Top);
		}
	}

	@Test
	public void testPostSearch() throws Exception {
		var tester = new ApiRootResource();
		try (Response r = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("rock'n roll"))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
			List<?> res = (List<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
		try (Response r = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("jøssingfjord"))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
			List<?> res = (List<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
	}
}
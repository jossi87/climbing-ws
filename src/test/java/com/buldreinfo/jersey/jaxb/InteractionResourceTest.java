package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.resources.InteractionResource;

import jakarta.ws.rs.core.Response;

public class InteractionResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetDangerous() throws Exception {
		var tester = new InteractionResource();
		try (Response r = tester.getDangerous(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetRestrictions() throws Exception {
		var tester = new InteractionResource();
		try (Response r = tester.getRestrictions(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetTicks() throws Exception {
		var tester = new InteractionResource();
		try (Response r = tester.getTicks(getRequest(Region.buldreinfo), 1)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Ticks);
		}
	}

	@Test
	public void testGetTop() throws Exception {
		var tester = new InteractionResource();
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
		var tester = new InteractionResource();
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

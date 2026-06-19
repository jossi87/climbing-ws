package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.resources.WithoutJsResource;

import jakarta.ws.rs.core.Response;

public class WithoutJsResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetWithoutJs() throws Exception {
		var tester = getService(WithoutJsResource.class);
		try (Response r = tester.getWithoutJs(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetWithoutJsArea() throws Exception {
		var tester = getService(WithoutJsResource.class);
		try (Response r = tester.getWithoutJsArea(getRequest(Region.buldreinfo), BaseResourceTest.BULDREINFO_AREA_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetWithoutJsProblem() throws Exception {
		var tester = getService(WithoutJsResource.class);
		try (Response r = tester.getWithoutJsProblem(getRequest(Region.buldreinfo), BaseResourceTest.BULDREINFO_PROBLEM_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetWithoutJsProblemMedia() throws Exception {
		var tester = getService(WithoutJsResource.class);
		try (Response r = tester.getWithoutJsProblemMedia(getRequest(Region.buldreinfo), BaseResourceTest.BULDREINFO_MEDIA_ID_WITH_CHAPTERS_PROBLEM_ID, BaseResourceTest.BULDREINFO_MEDIA_ID_WITH_CHAPTERS)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetWithoutJsSector() throws Exception {
		var tester = getService(WithoutJsResource.class);
		try (Response r = tester.getWithoutJsSector(getRequest(Region.buldreinfo), BaseResourceTest.BULDREINFO_SECTOR_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
}
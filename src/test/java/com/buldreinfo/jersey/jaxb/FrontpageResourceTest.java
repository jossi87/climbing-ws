package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.resources.FrontpageResource;

import jakarta.ws.rs.core.Response;

public class FrontpageResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetFrontpage() throws Exception {
		var tester = getService(FrontpageResource.class);
		try (Response r = tester.getFrontpage(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
}
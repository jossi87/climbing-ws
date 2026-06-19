package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.resources.ActivityResource;

import jakarta.ws.rs.core.Response;

public class ActivityResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetActivity() throws Exception {
		var tester = getService(ActivityResource.class);
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
}
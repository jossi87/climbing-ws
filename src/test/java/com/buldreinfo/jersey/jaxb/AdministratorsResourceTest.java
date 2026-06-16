package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.resources.AdministratorsResource;

import jakarta.ws.rs.core.Response;

public class AdministratorsResourceTest extends BaseResourceTest {
	
	@Test
	public void getAdministrators() throws Exception {
		var tester = new AdministratorsResource();
		try (Response r = tester.getAdministrators(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> users = (Collection<?>)r.getEntity();
			assertTrue(!users.isEmpty());
		}
	}
}
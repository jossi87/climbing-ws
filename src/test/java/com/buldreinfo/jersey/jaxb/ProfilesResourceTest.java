package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.resources.ProfilesResource;

import jakarta.ws.rs.core.Response;

public class ProfilesResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetProfile() throws Exception {
		var tester = getService(ProfilesResource.class);
		try (Response r = tester.getProfiles(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Profile);
			Profile u = (Profile)r.getEntity();
			assertTrue(u.identity() != null);
		}
	}
	
	@Test
	public void testGetProfileAscents() throws Exception {
		var tester = getService(ProfilesResource.class);
		try (Response r = tester.getProfilesAscents(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
		}
	}
	
	@Test
	public void testGetProfileMedia() throws Exception {
		var tester = getService(ProfilesResource.class);
		try (Response r = tester.getProfilesMedia(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN, true)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
		}
	}

	@Test
	public void testGetProfileTodo() throws Exception {
		var tester = getService(ProfilesResource.class);
		try (Response r = tester.getProfilesTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof ProfileTodo);
		}
	}
}
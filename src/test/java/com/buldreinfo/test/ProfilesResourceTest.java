package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.ProfilesController;
import com.buldreinfo.model.Profile;
import com.buldreinfo.model.ProfileTodo;

public class ProfilesResourceTest extends BaseResourceTest {
	@Autowired private ProfilesController tester;

	@Test
	public void testGetProfile() throws Exception {
		ResponseEntity<?> r = tester.getProfiles(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		Profile u = assertInstanceOf(Profile.class, r.getBody());
		assertNotNull(u.identity());
	}

	@Test
	public void testGetProfileAscents() throws Exception {
		ResponseEntity<?> r = tester.getProfilesAscents(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(List.class, r.getBody());
	}

	@Test
	public void testGetProfileMedia() throws Exception {
		ResponseEntity<?> r = tester.getProfilesMedia(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN, true);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(List.class, r.getBody());
	}

	@Test
	public void testGetProfileTodo() throws Exception {
		ResponseEntity<?> r = tester.getProfilesTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(ProfileTodo.class, r.getBody());
	}
}
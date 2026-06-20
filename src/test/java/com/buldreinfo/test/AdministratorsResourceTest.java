package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.AdministratorsController;

public class AdministratorsResourceTest extends BaseResourceTest {
	@Autowired private AdministratorsController tester;

	@Test
	public void getAdministrators() throws Exception {
		ResponseEntity<?> r = tester.getAdministrators(getRequest(Region.buldreinfo));
		assertTrue(r.getStatusCode() == OK);
		assertTrue(r.getBody() instanceof Collection<?>);
		Collection<?> res1 = (Collection<?>) r.getBody();
		assertTrue(!res1.isEmpty());
	}
}
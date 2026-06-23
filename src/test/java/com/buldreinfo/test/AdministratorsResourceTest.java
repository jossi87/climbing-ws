package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.AdministratorsController;

public class AdministratorsResourceTest extends BaseResourceTest {
	@Autowired private AdministratorsController tester;

	@Test
	public void getAdministrators() throws Exception {
		var r = tester.getAdministrators(getRequest(Region.buldreinfo));
		assertTrue(r.getStatusCode() == OK);
		assertTrue(r.getBody() != null);
		Collection<?> res1 = r.getBody();
		assertTrue(!res1.isEmpty());
	}
}
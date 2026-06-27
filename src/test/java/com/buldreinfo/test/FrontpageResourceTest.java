package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.FrontpageController;

public class FrontpageResourceTest extends BaseResourceTest {
	@Autowired private FrontpageController tester;

	@Test
	public void testGetFrontpage() {
		var r = tester.getFrontpage(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}
}
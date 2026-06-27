package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.WithoutJsController;

public class WithoutJsResourceTest extends BaseResourceTest {
	@Autowired private WithoutJsController tester;

	@Test
	public void testGetWithoutJs() {
		var r = tester.getWithoutJs(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsArea() {
		var r = tester.getWithoutJsArea(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsProblem() {
		var r = tester.getWithoutJsProblem(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, null, null);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsProblemMedia() {
		var r = tester.getWithoutJsProblem(getRequest(Region.buldreinfo), BULDREINFO_MEDIA_ID_WITH_CHAPTERS_PROBLEM_ID, BULDREINFO_MEDIA_ID_WITH_CHAPTERS, null);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsSector() {
		var r = tester.getWithoutJsSector(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
	}
}
package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.WithoutJsController;

public class WithoutJsResourceTest extends BaseResourceTest {
	@Autowired private WithoutJsController tester;

	@Test
	public void testGetWithoutJs() throws Exception {
		var r = tester.getWithoutJs(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsArea() throws Exception {
		var r = tester.getWithoutJsArea(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsProblem() throws Exception {
		var r = tester.getWithoutJsProblem(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, null, null);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsProblemMedia() throws Exception {
		var r = tester.getWithoutJsProblem(getRequest(Region.buldreinfo), BULDREINFO_MEDIA_ID_WITH_CHAPTERS_PROBLEM_ID, BULDREINFO_MEDIA_ID_WITH_CHAPTERS, null);
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetWithoutJsSector() throws Exception {
		var r = tester.getWithoutJsSector(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
	}
}
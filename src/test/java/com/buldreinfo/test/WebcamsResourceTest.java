package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.WebcamsController;

public class WebcamsResourceTest extends BaseResourceTest {
	@Autowired private WebcamsController tester;

	@Test
	public void testGetFrontpage() {
		var r = tester.getCameras();
		assertEquals(OK, r.getStatusCode());
	}
}
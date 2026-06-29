package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.WebcamsController;

public class WebcamsResourceTest extends BaseResourceTest {
	@Autowired private WebcamsController tester;

	@Test
	public void testGetFrontpage() {
		var r = tester.getCameras();
		assertEquals(OK, r.getStatusCode());
		Collection<?> cameras = assertInstanceOf(Collection.class, r.getBody());
		assertFalse(cameras.isEmpty());
	}
}
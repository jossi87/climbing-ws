package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.FrontpageController;

public class FrontpageResourceTest extends BaseResourceTest {
	@Autowired private FrontpageController tester;

	@Test
	public void testGetFrontpage() {
		var r = tester.getFrontpage(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
		Collection<?> cameras = assertInstanceOf(Collection.class, r.getBody());
		assertFalse(cameras.isEmpty());
	}
}
package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.MetaController;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Toc;

public class MetaResourceTest extends BaseResourceTest {
	@Autowired private MetaController tester;

	@Test
	public void testGetMeta() throws Exception {
		ResponseEntity<?> r = tester.getMeta(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(Meta.class, r.getBody());
	}

	@Test
	public void testGetRobotsTxt() throws Exception {
		ResponseEntity<?> r = tester.getRobotsTxt(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetSitemapTxt() throws Exception {
		ResponseEntity<?> r = tester.getSitemapTxt(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetToc() throws Exception {
		ResponseEntity<?> r = tester.getToc(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(Toc.class, r.getBody());
	}
}
package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.MetaController;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Toc;

public class MetaResourceTest extends BaseResourceTest {
	@Autowired private MetaController tester;

	@Test
	public void testGetMeta() {
		var r = tester.getMeta(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		var meta = assertInstanceOf(Meta.class, r.getBody());
		assertFalse(meta.gradeSystems().isEmpty());
		for (var system : meta.gradeSystems()) {
			assertFalse(system.grades().isEmpty(), "No grades for grade system " + system.name());
			for (var grade : system.grades()) {
				assertFalse(grade.grade() == null || grade.grade().isBlank());
			}
		}
	}

	@Test
	public void testGetRobotsTxt() {
		var r = tester.getRobotsTxt(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetSitemapTxt() {
		var r = tester.getSitemapTxt(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetToc() {
		var r = tester.getToc(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(Toc.class, r.getBody());
	}
}

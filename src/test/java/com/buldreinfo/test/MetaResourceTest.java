package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.MetaController;
import com.buldreinfo.dao.GradeSystemRepository;
import com.buldreinfo.model.Meta;
import com.buldreinfo.model.Toc;

public class MetaResourceTest extends BaseResourceTest {
	@Autowired private GradeSystemRepository gradeSystemRepo;
	@Autowired private MetaController tester;

	@Test
	public void testGetMeta() {
		var r = tester.getMeta(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		var meta = assertInstanceOf(Meta.class, r.getBody());
		var declaredSystemIds = gradeSystemRepo.getRegionGradeSystemIds(getSetup(Region.buldreinfo).idRegion());
		assertEquals(declaredSystemIds, meta.regionGradeSystemIds());
		assertFalse(meta.allGradeSystems().isEmpty());
		assertFalse(meta.regionGradeSystemIds().isEmpty());
		var allSystemIds = meta.allGradeSystems().stream().map(system -> system.id()).toList();
		assertEquals(allSystemIds.size(), allSystemIds.stream().distinct().count(),
				"allGradeSystems contains duplicate ids");
		assertTrue(allSystemIds.containsAll(meta.regionGradeSystemIds()),
				"regionGradeSystemIds contains a system missing from allGradeSystems");
		for (var system : meta.allGradeSystems()) {
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

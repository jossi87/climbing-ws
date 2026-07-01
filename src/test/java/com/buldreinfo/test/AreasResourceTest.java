package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.AreasController;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.model.Area;

public class AreasResourceTest extends BaseResourceTest {
	@Autowired private AreasController tester;
	@Autowired private AreaRepository areaRepo;

	@Test
	public void testGetArea() {
		var r = tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
		Collection<?> area = assertInstanceOf(Collection.class, r.getBody());
		assertEquals(1, area.size());
		Area a = (Area) area.iterator().next();
		assertFalse(a.name() == null || a.name().isBlank());
		assertNull(a.redirectUrl());
	}

	@Test
	public void testGetAreaDifferentRegion() {
		var r = tester.getAreas(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_AREA_ID);
		assertEquals(OK, r.getStatusCode());
		Collection<?> area = assertInstanceOf(Collection.class, r.getBody());
		assertEquals(1, area.size());
		Area a = (Area) area.iterator().next();
		assertTrue(a.name() == null || a.name().isBlank());
		assertNotNull(a.redirectUrl());
	}

	@Test
	public void testGetAreaHidden() {
		assertThrows(NoSuchElementException.class, () -> {
			tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_AREA_ID);
		});

		var setup = getSetup(Region.buldreinfo);
		assertThrows(NoSuchElementException.class, () -> 
		areaRepo.getArea(setup, Optional.of(USER_ID_NORMAL), BULDREINFO_HIDDEN_AREA_ID));

		Area a = areaRepo.getArea(setup, Optional.of(USER_ID_SUPERADMIN), BULDREINFO_HIDDEN_AREA_ID);
		assertNotNull(a);
		assertFalse(a.name() == null || a.name().isBlank());
	}

	@Test
	public void testGetAreas() {
		var r = tester.getAreas(getRequest(Region.buldreinfo), 0);
		assertEquals(OK, r.getStatusCode());
		Collection<?> areas = assertInstanceOf(Collection.class, r.getBody());
		assertFalse(areas.isEmpty());
	}
}
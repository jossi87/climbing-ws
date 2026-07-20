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
import com.buldreinfo.model.Area;
import com.buldreinfo.service.AreaService;

public class AreasResourceTest extends BaseResourceTest {
	@Autowired private AreasController tester;
	@Autowired private AreaService areaService;

	@Test
	public void testGetArea() {
		var r = tester.getArea(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
		Area a = assertInstanceOf(Area.class, r.getBody());
		assertFalse(a.name() == null || a.name().isBlank());
		assertNull(a.redirectUrl());
	}

	@Test
	public void testGetAreaDifferentRegion() {
		var r = tester.getArea(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_AREA_ID);
		assertEquals(OK, r.getStatusCode());
		Area a = assertInstanceOf(Area.class, r.getBody());
		assertTrue(a.name() == null || a.name().isBlank());
		assertNotNull(a.redirectUrl());
	}

	@Test
	public void testGetAreaHidden() {
		assertThrows(NoSuchElementException.class, () -> {
			tester.getArea(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_AREA_ID);
		});

		var setup = getSetup(Region.buldreinfo);
		assertThrows(NoSuchElementException.class, () -> 
		areaService.getArea(setup, Optional.of(USER_ID_NORMAL), BULDREINFO_HIDDEN_AREA_ID));

		Area a = areaService.getArea(setup, Optional.of(USER_ID_SUPERADMIN), BULDREINFO_HIDDEN_AREA_ID);
		assertNotNull(a);
		assertFalse(a.name() == null || a.name().isBlank());
	}

	@Test
	public void testGetAreas() {
		var r = tester.getAreas(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
		Collection<?> areas = assertInstanceOf(Collection.class, r.getBody());
		assertFalse(areas.isEmpty());
	}
}
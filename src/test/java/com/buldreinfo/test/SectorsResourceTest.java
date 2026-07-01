package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.SectorsController;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.model.Sector;

public class SectorsResourceTest extends BaseResourceTest {
	@Autowired private SectorsController tester;
	@Autowired private SectorRepository sectorRepo;

	@Test
	public void testGetSector() {
		var r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
		Sector s = assertInstanceOf(Sector.class, r.getBody());
		assertFalse(s.name() == null || s.name().isBlank());
		assertFalse(s.problems().isEmpty());
		assertNull(s.redirectUrl());
	}

	@Test
	public void testGetSectorDifferentRegion() {
		var r = tester.getSectors(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_SECTOR_ID);
		assertEquals(OK, r.getStatusCode());
		Sector s = assertInstanceOf(Sector.class, r.getBody());
		assertTrue(s.name() == null || s.name().isBlank());
		assertNotNull(s.redirectUrl());
	}

	@Test
	public void testGetSectorHidden() {
		assertThrows(NoSuchElementException.class, () -> {
			tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID);
		});

		var setup = getSetup(Region.buldreinfo);
		assertThrows(NoSuchElementException.class, () -> 
		sectorRepo.getSector(Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID));
		Sector s = sectorRepo.getSector(Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID);
		assertNotNull(s);
		assertFalse(s.name() == null || s.name().isBlank());
	}
}
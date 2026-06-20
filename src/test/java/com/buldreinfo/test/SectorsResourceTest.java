package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.SectorsController;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.model.Sector;
import com.google.common.base.Strings;

public class SectorsResourceTest extends BaseResourceTest {
	@Autowired private SectorsController tester;
	@Autowired private SectorRepository sectorRepo;

	@Test
	public void testGetSector() throws Exception {
		ResponseEntity<?> r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE);
		assertEquals(OK, r.getStatusCode());
		Sector s = assertInstanceOf(Sector.class, r.getBody());
		assertFalse(Strings.isNullOrEmpty(s.name()));
		assertFalse(s.problems().isEmpty());
		assertNull(s.redirectUrl());
	}

	@Test
	public void testGetSectorDifferentRegion() throws Exception {
		ResponseEntity<?> r = tester.getSectors(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_SECTOR_ID);
		assertEquals(OK, r.getStatusCode());
		Sector s = assertInstanceOf(Sector.class, r.getBody());
		assertTrue(Strings.isNullOrEmpty(s.name()));
		assertNotNull(s.redirectUrl());
	}

	@Test
	public void testGetSectorHidden() throws Exception {
		assertThrows(NoSuchElementException.class, () -> {
			tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID);
		});

		var setup = getSetup(Region.buldreinfo);
		txManager.executeInTransaction(() -> {
			assertThrows(NoSuchElementException.class, () -> 
			sectorRepo.getSector(Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false));

			try {
				Sector s = sectorRepo.getSector(Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertNotNull(s);
				assertFalse(Strings.isNullOrEmpty(s.name()));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.SectorRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.resources.SectorsResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;

public class SectorsResourceTest extends BaseResourceTest {

	@Test
	public void testGetSector() throws Exception {
		var tester = getService(SectorsResource.class);
		try (Response r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Sector);
			Sector s = (Sector)r.getEntity();
			assertTrue(!Strings.isNullOrEmpty(s.name()));
			assertTrue(!s.problems().isEmpty());
			assertTrue(s.redirectUrl() == null);
		}
	}

	@Test
	public void testGetSectorDifferentRegion() throws Exception {
		var tester = getService(SectorsResource.class);
		try (Response r = tester.getSectors(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_SECTOR_ID)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Sector);
			Sector s = (Sector)r.getEntity();
			assertTrue(Strings.isNullOrEmpty(s.name()));
			assertTrue(s.redirectUrl() != null);
		}
	}

	@Test
	public void testGetSectorHidden() throws Exception {
		var tester = getService(SectorsResource.class);
		var txManager = getService(TransactionManager.class);
		var sectorRepo = getService(SectorRepository.class);
		Response r = invoke(() -> tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID));
		assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		Setup setup = getSetup(Region.buldreinfo);
		txManager.executeInTransaction(() -> {
			try {
				sectorRepo.getSector(Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		txManager.executeInTransaction(() -> {
			try {
				Sector s = sectorRepo.getSector(Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertTrue(s != null);
				assertFalse(Strings.isNullOrEmpty(s.name()));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}
}
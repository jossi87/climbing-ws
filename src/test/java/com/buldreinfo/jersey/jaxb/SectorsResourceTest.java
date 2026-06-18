package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.resources.SectorsResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;

public class SectorsResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetSector() throws Exception {
		var tester = new SectorsResource();
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
		var tester = new SectorsResource();
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
		var tester = new SectorsResource();
		try (Response r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		DatabaseContext.runSql(dao -> {
			try {
				dao.getSectorRepo().getSector(Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		DatabaseContext.runSql(dao -> {
			try {
			Sector s = dao.getSectorRepo().getSector(Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
			assertTrue(s != null);
			assertFalse(Strings.isNullOrEmpty(s.name()));
			} catch (SQLException e) {
		        throw new RuntimeException(e);
		    }
		});
	}
}
package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.AreaRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.resources.AreasResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;

public class AreasResourceTest extends BaseResourceTest {

	@Test
	public void testGetArea() throws Exception {
		var tester = getService(AreasResource.class);
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> area = (Collection<?>)r.getEntity();
			assertTrue(area.size() == 1);
			Area a = (Area) area.iterator().next();
			assertTrue(!Strings.isNullOrEmpty(a.name()));
			assertTrue(a.redirectUrl() == null);
		}
	}

	@Test
	public void testGetAreaDifferentRegion() throws Exception {
		var tester = getService(AreasResource.class);
		try (Response r = tester.getAreas(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_AREA_ID)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> area = (Collection<?>)r.getEntity();
			assertTrue(area.size() == 1);
			Area a = (Area) area.iterator().next();
			assertTrue(Strings.isNullOrEmpty(a.name()));
			assertTrue(a.redirectUrl() != null);
		}
	}

	@Test
	public void testGetAreaHidden() throws Exception {
		var tester = getService(AreasResource.class);
		Response r = invoke(() -> tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_AREA_ID));
		assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		Setup setup = getSetup(Region.buldreinfo);
		var txManager = getService(TransactionManager.class);
		var areaRepo = getService(AreaRepository.class);
		txManager.executeInTransaction(() -> {
			try {
				assertThrows(NoSuchElementException.class, () -> areaRepo.getArea(setup, Optional.of(USER_ID_NORMAL), BULDREINFO_HIDDEN_AREA_ID, false));
				Area a = areaRepo.getArea(setup, Optional.of(USER_ID_SUPERADMIN), BULDREINFO_HIDDEN_AREA_ID, false);
				assertNotNull(a);
				assertFalse(Strings.isNullOrEmpty(a.name()));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testGetAreas() throws Exception {
		var tester = getService(AreasResource.class);
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> areas = (Collection<?>)r.getEntity();
			assertTrue(!areas.isEmpty());
		}
	}
}
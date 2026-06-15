package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.resources.ApiResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;

public class ApiResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetActivity() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, true, true, true, 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
		try (Response r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, false, false, false, 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
	}
	
	@Test
	public void testGetArea() throws Exception {
		var tester = new ApiResource();
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
		var tester = new ApiResource();
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
		var tester = new ApiResource();
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_AREA_ID)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		DatabaseContext.runSql((dao, c) -> {
			try {
				dao.getAreaRepo().getArea(c, setup, Optional.of(USER_ID_NORMAL), BULDREINFO_HIDDEN_AREA_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		DatabaseContext.runSql((dao, c) -> {
			Area a = dao.getAreaRepo().getArea(c, setup, Optional.of(USER_ID_SUPERADMIN), BULDREINFO_HIDDEN_AREA_ID, false);
			assertTrue(a != null);
			assertTrue(!Strings.isNullOrEmpty(a.name()));
		});
	}

	@Test
	public void testGetAreas() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> areas = (Collection<?>)r.getEntity();
			assertTrue(!areas.isEmpty());
		}
	}

	@Test
	public void testGetDangerous() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getDangerous(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetFrontpage() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getFrontpage(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetMeta() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getMeta(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Meta);
		}
	}

	@Test
	public void testGetProfile() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getProfile(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Profile);
			Profile u = (Profile)r.getEntity();
			assertTrue(u.identity() != null);
		}
	}
	
	@Test
	public void testGetProfileAscents() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getProfileAscents(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
		}
	}
	
	@Test
	public void testGetProfileMedia() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getProfileMedia(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN, true)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
		}
	}

	@Test
	public void testGetProfileTodo() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getProfileTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof ProfileTodo);
		}
	}

	@Test
	public void testGetRestrictions() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getRestrictions(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetSector() throws Exception {
		var tester = new ApiResource();
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
		var tester = new ApiResource();
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
		var tester = new ApiResource();
		try (Response r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		DatabaseContext.runSql((dao, c) -> {
			try {
				dao.getSectorRepo().getSector(c, Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		DatabaseContext.runSql((dao, c) -> {
			Sector s = dao.getSectorRepo().getSector(c, Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
			assertTrue(s != null);
			assertTrue(!Strings.isNullOrEmpty(s.name()));
		});
	}

	@Test
	public void testGetTicks() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getTicks(getRequest(Region.buldreinfo), 1)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Ticks);
		}
	}

	@Test
	public void testGetToc() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getToc(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Toc);
		}
	}

	@Test
	public void testGetTodo() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getProfileTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof ProfileTodo);
			ProfileTodo t = (ProfileTodo)r.getEntity();
			assertTrue(!t.areas().isEmpty());
		}
	}

	@Test
	public void testGetTop() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.getTop(getRequest(Region.brattelinjer), 2738, 0)) { // Dale
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Top);
		}
		try (Response r = tester.getTop(getRequest(Region.brattelinjer), 0, 2857)) { // Dale / Hovedveggen
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Top);
		}
	}

	@Test
	public void testPostSearch() throws Exception {
		var tester = new ApiResource();
		try (Response r = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("rock'n roll"))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
			List<?> res = (List<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
		try (Response r = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("jøssingfjord"))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
			List<?> res = (List<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
	}
}
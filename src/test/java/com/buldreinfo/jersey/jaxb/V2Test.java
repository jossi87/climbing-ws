package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.FrontpageRandomMedia;
import com.buldreinfo.jersey.jaxb.model.FrontpageStats;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.SearchRequest;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

public class V2Test {
	private enum Region { buldreinfo, brattelinjer }
	private static final int USER_ID_SUPERADMIN = 1;
	private static final int USER_ID_NORMAL = 1049;
	private static final int BULDREINFO_AREA_ID_VISIBLE = 7;
	private static final int BULDREINFO_SECTOR_ID_VISIBLE = 47;
	private static final int BULDREINFO_PROBLEM_ID_VISIBLE = 1193;
	private static final int BULDREINFO_HIDDEN_AREA_ID = 3267;
	private static final int BULDREINFO_HIDDEN_SECTOR_ID = 2185;
	private static final int BULDREINFO_HIDDEN_PROBLEM_ID = 2597;
	private static final int BRATTELINJER_PROBLEM_ID_PDF = 7745;

	@Test
	public void testGetActivity() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, true, true, true, 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetAreas() throws Exception {
		V2 tester = new V2();
		// All areas
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), 0)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> areas = (Collection<?>)r.getEntity();
			assertTrue(areas.size() > 1);
		}
		// One area
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_AREA_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> area = (Collection<?>)r.getEntity();
			assertTrue(area.size() == 1);
		}
	}

	@Test
	public void testGetDangerous() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getDangerous(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetFrontpageRandomMedia() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getFrontpageRandomMedia(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>)r.getEntity();
			assertTrue(!res.isEmpty());
			assertTrue(res.iterator().next() instanceof FrontpageRandomMedia);
		}
	}

	@Test
	public void testGetFrontpageStats() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getFrontpageStats(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof FrontpageStats);
			FrontpageStats f = (FrontpageStats)r.getEntity();
			assertTrue(f.areas()>0);
			assertTrue(f.problems()>0);
			assertTrue(f.ticks()>0);
		}
	}

	@Test
	public void testGetMeta() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getMeta(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Meta);
		}
	}

	@Test
	public void testGetProblem() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getProblem(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, false)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Problem);
			Problem p = (Problem)r.getEntity();
			assertTrue(!Strings.isNullOrEmpty(p.getName()));
		}
	}
	
	@Test
	public void testGetProblemPdf() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getProblemPdf(getRequest(Region.brattelinjer), BRATTELINJER_PROBLEM_ID_PDF)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof StreamingOutput);
			var streamingOutput = (StreamingOutput)r.getEntity();
			Path p = Files.createTempFile("problemPdf", ".pdf");
			try (var fos = new FileOutputStream(p.toFile())) {
                streamingOutput.write(fos);
            }
			Files.deleteIfExists(p);
		}
	}

	@Test
	public void testGetProfile() throws Exception {
		V2 tester = new V2();
		// User: Jostein
		try (Response r = tester.getProfile(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Profile);
			Profile u = (Profile)r.getEntity();
			assertTrue(u.firstname() != null);
		}
	}

	@Test
	public void testGetSectors() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_SECTOR_ID_VISIBLE)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Sector);
			Sector s = (Sector)r.getEntity();
			assertTrue(!Strings.isNullOrEmpty(s.getName()));
			assertTrue(!s.getProblems().isEmpty());
		}
	}

	@Test
	public void testGetTicks() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getTicks(getRequest(Region.buldreinfo), 1)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Ticks);
		}
	}

	@Test
	public void testGetToc() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getToc(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Toc);
		}
	}

	@Test
	public void testGetTodo() throws Exception {
		V2 tester = new V2();
		// User: Jostein
		try (Response r = tester.getProfileTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof ProfileTodo);
			ProfileTodo t = (ProfileTodo)r.getEntity();
			assertTrue(!t.areas().isEmpty());
		}
	}

	@Test
	public void testHiddenAreaPermissions() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getAreas(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_AREA_ID)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		Server.runSql((dao, c) -> {
			try {
				dao.getArea(c, setup, Optional.of(USER_ID_NORMAL), BULDREINFO_HIDDEN_AREA_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof java.util.NoSuchElementException);
			}
		});
		Server.runSql((dao, c) -> {
			Area a = dao.getArea(c, setup, Optional.of(USER_ID_SUPERADMIN), BULDREINFO_HIDDEN_AREA_ID, false);
			assertTrue(a != null);
			assertTrue(!Strings.isNullOrEmpty(a.getName()));
		});
	}

	@Test
	public void testHiddenProblemPermissions() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getProblem(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_PROBLEM_ID, false)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		Server.runSql((dao, c) -> {
			try {
				dao.getProblem(c, Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof java.util.NoSuchElementException);
			}
		});
		Server.runSql((dao, c) -> {
			Problem p = dao.getProblem(c, Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
			assertTrue(p != null);
			assertTrue(!Strings.isNullOrEmpty(p.getName()));
		});
	}

	@Test
	public void testHiddenSectorPermissions() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.getSectors(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_SECTOR_ID)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		Server.runSql((dao, c) -> {
			try {
				dao.getSector(c, Optional.of(USER_ID_NORMAL), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof java.util.NoSuchElementException);
			}
		});
		Server.runSql((dao, c) -> {
			Sector s = dao.getSector(c, Optional.of(USER_ID_SUPERADMIN), false, setup, BULDREINFO_HIDDEN_SECTOR_ID, false);
			assertTrue(s != null);
			assertTrue(!Strings.isNullOrEmpty(s.getName()));
		});
	}

	@Test
	public void testPostSearch() throws Exception {
		V2 tester = new V2();
		try (Response r = tester.postSearch(getRequest(Region.buldreinfo), new SearchRequest("Pan"))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof List<?>);
			List<?> res = (List<?>)r.getEntity();
			assertTrue(!res.isEmpty());
		}
	}

	private HttpServletRequest getRequest(Region region) {
		String origin = switch (region) {
		case buldreinfo -> "https://buldreinfo.com";
		case brattelinjer -> "https://brattelinjer.no";
		};
		String serverName = switch (region) {
		case buldreinfo -> "buldreinfo.com";
		case brattelinjer -> "brattelinjer.no";
		};
		HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(req.getHeader(HttpHeaders.ORIGIN)).andReturn(origin).anyTimes();
		EasyMock.expect(req.getHeader(HttpHeaders.AUTHORIZATION)).andReturn(null).anyTimes();
		EasyMock.expect(req.getServerName()).andReturn(serverName).anyTimes();
		EasyMock.expect(req.getParameter("access_token")).andReturn(null).anyTimes();
		EasyMock.expect(req.getHeader(Server.HEADER_INTERNAL_REQUEST)).andReturn(Server.HEADER_INTERNAL_REQUEST_VALUE).anyTimes();
		EasyMock.replay(req);
		return req;
	}

	private Setup getSetup(Region region) {
		String domain = switch (region) {
		case buldreinfo -> "buldreinfo.com";
		case brattelinjer -> "brattelinjer.no";
		};
		return Server.getSetups().stream()
				.filter(s -> s.domain().equalsIgnoreCase(domain))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing setup for domain=" + domain));
	}
}
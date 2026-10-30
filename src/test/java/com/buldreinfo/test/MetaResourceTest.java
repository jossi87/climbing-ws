package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.buldreinfo.controller.MetaController;
import com.buldreinfo.dao.GradeSystemRepository;
import com.buldreinfo.model.ArdManifest;
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
		assertEquals(declaredSystemIds, meta.activityGradeSystemIds());
		assertTrue(allSystemIds.containsAll(meta.activityGradeSystemIds()),
				"activityGradeSystemIds contains a system missing from allGradeSystems");
		for (var system : meta.allGradeSystems()) {
			assertFalse(system.grades().isEmpty(), "No grades for grade system " + system.name());
			for (var grade : system.grades()) {
				assertFalse(grade.grade() == null || grade.grade().isBlank());
			}
		}
	}

	@Test
	public void testActivityGradeSystemsIncludeActiveRegion() {
		var setup = getSetup(Region.brattelinjer);
		var declaredSystemIds = gradeSystemRepo.getRegionGradeSystemIds(setup.idRegion());
		for (var userId : List.of(USER_ID_NORMAL, USER_ID_SUPERADMIN)) {
			var systemIds = gradeSystemRepo.getActivityGradeSystemIds(setup.idRegion(), userId);
			assertTrue(systemIds.containsAll(declaredSystemIds),
					"The feed domain must always contain the active region's systems (user " + userId + ")");
			assertEquals(systemIds.size(), systemIds.stream().distinct().count(),
					"The feed domain must not repeat a system shared by several regions (user " + userId + ")");
		}
	}

	@Test
	public void testGetRobotsTxt() {
		for (var region : List.of(Region.buldreinfo, Region.brattelinjer)) {
			var r = tester.getRobotsTxt(getRequest(region));
			assertEquals(OK, r.getStatusCode());
			var url = getSetup(region).url();
			assertTrue(r.getBody().contains("Sitemap: " + url + "/sitemap.txt"), r.getBody());
		}
	}

	@Test
	public void testGetArdJson() {
		var identifiers = new ArrayList<String>();
		for (var region : List.of(Region.buldreinfo, Region.brattelinjer)) {
			var setup = getSetup(region);
			var r = tester.getArdJson(getRequest(region));
			assertEquals(OK, r.getStatusCode());
			var manifest = assertInstanceOf(ArdManifest.class, r.getBody());
			assertEquals(1, manifest.entries().size());
			var entry = manifest.entries().getFirst();
			// ARD resolves both the URN's publisher and the url against the domain that was asked (spec 4.5.1), so
			// neither may name another site.
			assertEquals("urn:air:" + setup.domain() + ":api:climbing-api", entry.identifier());
			assertEquals(setup.url() + "/api/v3/api-docs", entry.url(), "ARD requires an absolute url");
			// Named after the site itself (the model falls back to the domain for a region without a title).
			assertTrue(entry.displayName().endsWith(" API"), entry.displayName());
			if (region == Region.buldreinfo) {
				assertFalse(entry.identifier().contains("brattelinjer"), entry.identifier());
				assertFalse(entry.description().contains("brattelinjer"), entry.description());
			}
			// The tags follow this site's own disciplines instead of a fixed list that names the other sites.
			assertEquals(setup.isBouldering(), entry.tags().contains("bouldering"), entry.tags().toString());
			assertEquals(setup.isClimbing(), entry.tags().contains("route-climbing"), entry.tags().toString());
			assertEquals(setup.isIce(), entry.tags().contains("ice-climbing"), entry.tags().toString());
			assertTrue(entry.representativeQueries().size() >= 2 && entry.representativeQueries().size() <= 5,
					"ARD wants 2-5 representative queries (spec D.2)");
			identifiers.add(entry.identifier());
		}
		// Two sites, two identifiers: the same API shape, but each entry names its own domain.
		assertEquals(2, identifiers.stream().distinct().count(), identifiers.toString());
	}

	@Test
	public void testGetSecurityTxt() {
		var canonicals = new ArrayList<String>();
		for (var region : List.of(Region.buldreinfo, Region.brattelinjer)) {
			var r = tester.getSecurityTxt(getRequest(region));
			assertEquals(OK, r.getStatusCode());
			assertEquals("public, max-age=86400", r.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
			var body = r.getBody();
			// Contact and Expires are the two fields RFC 9116 makes mandatory; a scanner treats a document
			// without them, or with an Expires in the past, as no document at all.
			assertTrue(body.contains("Contact: mailto:"), body);
			var expires = body.lines()
					.filter(line -> line.startsWith("Expires: "))
					.map(line -> line.substring("Expires: ".length()))
					.findFirst()
					.orElseThrow(() -> new AssertionError("Missing Expires field: " + body));
			var expiresAt = Instant.parse(expires);
			assertTrue(expiresAt.isAfter(Instant.now()), "Expires must be in the future: " + expires);
			assertTrue(expiresAt.isBefore(Instant.now().plus(Duration.ofDays(366))),
					"RFC 9116 caps Expires at one year: " + expires);
			// The canonical URI names the domain that was asked, like robots.txt, sitemap.txt and ard.json do.
			var canonical = "Canonical: " + getSetup(region).url() + "/.well-known/security.txt";
			assertTrue(body.contains(canonical), body);
			canonicals.add(canonical);
		}
		assertEquals(2, canonicals.stream().distinct().count(), canonicals.toString());
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

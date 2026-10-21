package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.ProfilesController;
import com.buldreinfo.model.Profile;
import com.buldreinfo.model.Profile.ProfileDiscipline;
import com.buldreinfo.model.ProfileTodo;

public class ProfilesResourceTest extends BaseResourceTest {
	@Autowired private ProfilesController tester;

	@Test
	public void testGetProfile() {
		var r = tester.getProfiles(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		Profile u = assertInstanceOf(Profile.class, r.getBody());
		assertNotNull(u.identity());
	}

	@Test
	public void testGetProfileDisciplineCharts() {
		var r = tester.getProfiles(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		Profile u = assertInstanceOf(Profile.class, r.getBody());
		var charts = u.disciplines();
		assertNotNull(charts);
		assertFalse(charts.isEmpty());
		assertEquals(charts.size(), charts.stream().map(ProfilesResourceTest::chartKey).distinct().count());
		for (var chart : charts) {
			assertNotNull(chart.type());
			assertNotNull(chart.gradeSystem());
			assertFalse(chart.gradeDistribution().isEmpty());
			if (chart.multiPitch()) {
				assertNull(chart.isBolted());
			} else {
				assertNotNull(chart.isBolted());
			}
		}
	}

	private static String chartKey(ProfileDiscipline chart) {
		return chart.type() + "/" + chart.gradeSystemId() + "/"
				+ (chart.multiPitch() ? "multi" : chart.isBolted() + "/single");
	}

	@Test
	public void testGetProfileAscents() {
		var r = tester.getProfilesAscents(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(List.class, r.getBody());
	}

	@Test
	public void testGetProfileAscentsSpansDisciplines() {
		var r = tester.getProfilesAscents(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(List.class, r.getBody());
	}

	@Test
	public void testGetProfileMedia() {
		var r = tester.getProfilesMedia(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN, true);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(List.class, r.getBody());
	}

	@Test
	public void testGetProfileTodo() {
		var r = tester.getProfilesTodo(getRequest(Region.buldreinfo), USER_ID_SUPERADMIN);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(ProfileTodo.class, r.getBody());
	}
}
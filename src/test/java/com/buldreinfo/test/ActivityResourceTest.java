package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.ActivityController;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.Activity;

public class ActivityResourceTest extends BaseResourceTest {
	@Autowired private ActivityController tester;

	@Test
	public void testGetActivity() {
		var r1 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, null, true, true, true, true, 0);
		assertTrue(r1.getStatusCode() == OK);
		assertTrue(r1.getBody() != null);
		Collection<Activity> res1 = r1.getBody();
		assertTrue(!res1.isEmpty());

		var r2 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, List.of(), true, false, false, false, 0);
		assertTrue(r2.getStatusCode() == OK);
		assertTrue(r2.getBody() != null);
		Collection<Activity> res2 = r2.getBody();
		assertTrue(!res2.isEmpty());
	}

	@Test
	public void testGetActivityRejectsInvalidGradeFilters() {
		assertThrows(ValidationFailedException.class, () -> getActivities(List.of("42")));
		assertThrows(ValidationFailedException.class, () -> getActivities(List.of("abc:5")));
		assertThrows(ValidationFailedException.class, () -> getActivities(List.of("0:5")));
		assertThrows(ValidationFailedException.class, () -> getActivities(List.of("2:0")));
	}

	private List<Activity> getActivities(List<String> gradeFilters) {
		var r = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, gradeFilters, true, true, true, true, 0);
		assertEquals(OK, r.getStatusCode());
		assertTrue(r.getBody() != null);
		return r.getBody();
	}
}
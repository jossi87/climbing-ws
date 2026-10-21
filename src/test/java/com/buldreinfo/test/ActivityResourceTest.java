package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.ActivityController;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.Activity;

public class ActivityResourceTest extends BaseResourceTest {
	private static final int NO_SUCH_GRADE_SYSTEM_ID = 999999;
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
	public void testGetActivityGradeFilterForUnknownSystemKeepsFeed() {
		// A threshold for a system the feed does not use must not hide anything. This is the regression the
		// per-system filter fixes: grades are never compared across systems.
		var unfiltered = getActivities(List.of(NO_SUCH_GRADE_SYSTEM_ID + ":1"));
		var unknownSystemFilter = getActivities(List.of(NO_SUCH_GRADE_SYSTEM_ID + ":2"));
		assertFalse(unfiltered.isEmpty());
		assertEquals(problemIds(unfiltered), problemIds(unknownSystemFilter));
	}

	@Test
	public void testGetActivityGradeFiltersApplyPerGradeSystem() {
		var unfiltered = getActivities(List.of(NO_SUCH_GRADE_SYSTEM_ID + ":1"));
		assertFalse(unfiltered.isEmpty());
		// Pick the grade system with most activity in the feed, so the test always has something to filter.
		int gradeSystemId = mostCommonGradeSystemId(unfiltered);

		var filtered = getActivities(List.of(gradeSystemId + ":" + Integer.MAX_VALUE));
		// A threshold only hides problems graded in its own system.
		assertTrue(filtered.stream().noneMatch(a -> a.getGradeSystemId() == gradeSystemId));

		// Adding a second, unrelated system must not change the result: each system is filtered independently.
		var filteredWithUnknownSystem = getActivities(List.of(gradeSystemId + ":" + Integer.MAX_VALUE, NO_SUCH_GRADE_SYSTEM_ID + ":1"));
		assertEquals(problemIds(filtered), problemIds(filteredWithUnknownSystem));
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

	private static List<Integer> problemIds(List<Activity> activities) {
		return activities.stream().map(Activity::getProblemId).sorted().toList();
	}

	private static int mostCommonGradeSystemId(List<Activity> activities) {
		Map<Integer, Long> counts = activities.stream()
				.collect(Collectors.groupingBy(Activity::getGradeSystemId, Collectors.counting()));
		return counts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.orElseThrow()
				.getKey();
	}
}

package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.InteractionController;
import com.buldreinfo.model.SearchRequest;
import com.buldreinfo.model.Ticks;
import com.buldreinfo.model.Top;

public class InteractionResourceTest extends BaseResourceTest {
	@Autowired private InteractionController tester;

	@Test
	public void testGetDangerous() throws Exception {
		ResponseEntity<?> r = tester.getDangerous(getRequest(Region.buldreinfo));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetRestrictions() throws Exception {
		ResponseEntity<?> r = tester.getRestrictions(getRequest(Region.brattelinjer));
		assertEquals(OK, r.getStatusCode());
	}

	@Test
	public void testGetTicks() throws Exception {
		ResponseEntity<?> r = tester.getTicks(getRequest(Region.buldreinfo), 1);
		assertEquals(OK, r.getStatusCode());
		assertInstanceOf(Ticks.class, r.getBody());
	}

	@Test
	public void testGetTop() throws Exception {
		ResponseEntity<?> r1 = tester.getTop(getRequest(Region.brattelinjer), 2738, 0); // Dale
		assertEquals(OK, r1.getStatusCode());
		assertInstanceOf(Top.class, r1.getBody());

		ResponseEntity<?> r2 = tester.getTop(getRequest(Region.brattelinjer), 0, 2857); // Dale / Hovedveggen
		assertEquals(OK, r2.getStatusCode());
		assertInstanceOf(Top.class, r2.getBody());
	}

	@Test
	public void testPostSearch() throws Exception {
		ResponseEntity<?> r1 = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("rock'n roll"));
		assertEquals(OK, r1.getStatusCode());
		List<?> res1 = assertInstanceOf(List.class, r1.getBody());
		assertFalse(res1.isEmpty());

		ResponseEntity<?> r2 = tester.postSearch(getRequest(Region.brattelinjer), new SearchRequest("jøssingfjord"));
		assertEquals(OK, r2.getStatusCode());
		List<?> res2 = assertInstanceOf(List.class, r2.getBody());
		assertFalse(res2.isEmpty());
	}
}
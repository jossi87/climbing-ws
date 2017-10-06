package com.buldreinfo.jersey.jaxb;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.ws.rs.core.Response;

import org.junit.Test;

public class SisTest {
	@Test
	public void testGetProblems() throws Exception {
		Sis tester = new Sis();
		Response r = tester.getProblems();
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
	}
}
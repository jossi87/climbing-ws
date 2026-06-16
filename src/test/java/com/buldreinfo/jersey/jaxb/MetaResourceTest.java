package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Toc;
import com.buldreinfo.jersey.jaxb.resources.MetaResource;

import jakarta.ws.rs.core.Response;

public class MetaResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetFrontpage() throws Exception {
		var tester = new MetaResource();
		try (Response r = tester.getFrontpage(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetMeta() throws Exception {
		var tester = new MetaResource();
		try (Response r = tester.getMeta(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Meta);
		}
	}

	@Test
	public void testGetRobotsTxt() throws Exception {
		var tester = new MetaResource();
		try (Response r = tester.getRobotsTxt(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetSitemapTxt() throws Exception {
		var tester = new MetaResource();
		try (Response r = tester.getSitemapTxt(getRequest(Region.brattelinjer))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}

	@Test
	public void testGetToc() throws Exception {
		var tester = new MetaResource();
		try (Response r = tester.getToc(getRequest(Region.buldreinfo))) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Toc);
		}
	}
}
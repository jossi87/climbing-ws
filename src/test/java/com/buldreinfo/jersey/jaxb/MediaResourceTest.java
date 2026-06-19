package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.resources.MediaResource;

import jakarta.ws.rs.core.Response;

public class MediaResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetMedia() throws Exception {
		var tester = getService(MediaResource.class);
		try (Response r = tester.getMedia(getRequest(Region.buldreinfo), BULDREINFO_MEDIA_ID_WITH_CHAPTERS)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Media);
			Media m = (Media)r.getEntity();
			assertTrue(!m.problems().isEmpty());
		}
	}
}
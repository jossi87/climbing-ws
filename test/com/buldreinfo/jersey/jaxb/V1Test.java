package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.v1.V1Region;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class V1Test {
	@Test
	public void testGetImages() throws Exception {
		try (Response r = new V1().getImages(27293)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		}
	}
	
	@Test
	public void testGetRegionAreas() throws Exception {
		int other = getRegionAreas("0000000000000000");
		int stian = getRegionAreas("d5f87f487cc3a821");
		int jostein = getRegionAreas("c1a490a9060cab5a");
		assertTrue(other > 0 && stian > 0 && jostein > 0);
		assertTrue(stian > other);
		assertTrue(jostein > stian);
	}

	private int getRegionAreas(String uniqueId) throws ExecutionException, IOException {
		try (Response r = new V1().getRegions(getRequest(), uniqueId, false)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Collection<?>);
			Collection<?> res = (Collection<?>) r.getEntity();
			assertTrue(!res.isEmpty());
			int numAreas = 0;
			for (Object o : res) {
				assertTrue(o instanceof V1Region);
				V1Region region = (V1Region)o;
				numAreas += region.areas().size();
			}
			return numAreas;
		}
	}
	
	private HttpServletRequest getRequest() {
		HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(req.getHeader(HttpHeaders.ORIGIN)).andReturn("https://buldreinfo.com").anyTimes();
		EasyMock.expect(req.getHeader(HttpHeaders.AUTHORIZATION)).andReturn(null).anyTimes();
		EasyMock.expect(req.getServerName()).andReturn("buldreinfo.com").anyTimes();
		EasyMock.replay(req);
		return req;
	}
}
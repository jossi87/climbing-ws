package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.model.v1.V1Region;

import jakarta.ws.rs.core.Response;

public class V1Test {
	@Test
	public void testGetRegions() throws Exception {
		int other = getRegionAreas("0000000000000000");
		int stian = getRegionAreas("d5f87f487cc3a821");
		int jostein = getRegionAreas("c1a490a9060cab5a");
		assertTrue(other > 0 && stian > 0 && jostein > 0);
		assertTrue(stian > other);
		assertTrue(jostein > stian);
	}
	
	private int getRegionAreas(String uniqueId) throws ExecutionException, IOException {
		int numAreas = 0;
		V1 tester = new V1();
		Response r = tester.getRegions(uniqueId, false);
		assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
		assertTrue(r.getEntity() instanceof Collection<?>);
		Collection<?> res = (Collection<?>) r.getEntity();
		assertTrue(!res.isEmpty());
		for (Object o : res) {
			assertTrue(o instanceof V1Region);
			V1Region region = (V1Region)o;
			numAreas += region.getAreas().size();
		}
		return numAreas;
	}
}
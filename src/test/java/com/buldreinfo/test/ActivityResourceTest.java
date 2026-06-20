package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.controller.ActivityController;

public class ActivityResourceTest extends BaseResourceTest {
    @Autowired private ActivityController tester;

    @Test
    public void testGetActivity() throws Exception {
        ResponseEntity<?> r1 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, true, true, true, 0);
        assertTrue(r1.getStatusCode() == OK);
        assertTrue(r1.getBody() instanceof Collection<?>);
        Collection<?> res1 = (Collection<?>) r1.getBody();
        assertTrue(!res1.isEmpty());

        ResponseEntity<?> r2 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, false, false, false, 0);
        assertTrue(r2.getStatusCode() == OK);
        assertTrue(r2.getBody() instanceof Collection<?>);
        Collection<?> res2 = (Collection<?>) r2.getBody();
        assertTrue(!res2.isEmpty());
    }
}
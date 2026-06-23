package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.ActivityController;

public class ActivityResourceTest extends BaseResourceTest {
    @Autowired private ActivityController tester;

    @Test
    public void testGetActivity() throws Exception {
        var r1 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, true, true, true, 0);
        assertTrue(r1.getStatusCode() == OK);
        assertTrue(r1.getBody() != null);
        Collection<?> res1 = r1.getBody();
        assertTrue(!res1.isEmpty());

        var r2 = tester.getActivity(getRequest(Region.buldreinfo), 0, 0, 0, true, false, false, false, 0);
        assertTrue(r2.getStatusCode() == OK);
        assertTrue(r2.getBody() != null);
        Collection<?> res2 = r2.getBody();
        assertTrue(!res2.isEmpty());
    }
}
package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.buldreinfo.controller.MediaController;
import com.buldreinfo.model.Media;

public class MediaResourceTest extends BaseResourceTest {
	@Autowired private MediaController tester;

	@Test
	public void testGetMedia() throws Exception {
		var r = tester.getMedia(getRequest(Region.buldreinfo), BULDREINFO_MEDIA_ID_WITH_CHAPTERS);
		assertEquals(OK, r.getStatusCode());
		Media m = assertInstanceOf(Media.class, r.getBody());
		assertFalse(m.problems().isEmpty());
	}
}
package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.buldreinfo.controller.ProblemsController;
import com.buldreinfo.model.Problem;
import com.buldreinfo.service.ProblemService;

public class ProblemsResourceTest extends BaseResourceTest {
	@Autowired private ProblemsController tester;
	@Autowired private ProblemService problemService;

	@Test
	public void testGetProblem() {
		var r = tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, false);
		assertEquals(OK, r.getStatusCode());
		Problem p = assertInstanceOf(Problem.class, r.getBody());
		assertFalse(p.name() == null || p.name().isBlank());
		assertNull(p.redirectUrl());
	}

	@Test
	public void testGetProblemDifferentRegion() {
		var r = tester.getProblems(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_PROBLEM_ID, false);
		assertEquals(OK, r.getStatusCode());
		Problem p = assertInstanceOf(Problem.class, r.getBody());
		assertTrue(p.name() == null || p.name().isBlank());
		assertNotNull(p.redirectUrl());
	}

	@Test
	public void testGetProblemHidden() {
		assertThrows(NoSuchElementException.class, () -> {
			tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_PROBLEM_ID, false);
		});

		var setup = getSetup(Region.buldreinfo);
		assertThrows(NoSuchElementException.class, () -> 
		problemService.getProblem(Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false));
		Problem p = problemService.getProblem(Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false);
		assertNotNull(p);
		assertFalse(p.name() == null || p.name().isBlank());
	}

	@Test
	public void testGetProblemPdf() throws IOException {
		var r = tester.getProblemsPdf(getRequest(Region.brattelinjer), BRATTELINJER_PROBLEM_ID_PDF);
		assertEquals(OK, r.getStatusCode());
		var pdf = new ByteArrayOutputStream();
		assertInstanceOf(StreamingResponseBody.class, r.getBody()).writeTo(pdf);
		try (var reader = new PdfReader(pdf.toByteArray())) {
			// Without ?lang= the sheet is English, and it declares that to the viewer.
			assertEquals("en", reader.getCatalog().getAsString(PdfName.LANG).toString());
			assertFalse(reader.getTrailer().getAsDict(PdfName.INFO).getAsString(PdfName.TITLE).toString().isBlank());
		}
	}

	/** The export is localised: ?lang=cs must reach the bundles and end up as the catalog language. */
	@Test
	public void testGetProblemPdfCzech() throws IOException {
		var r = tester.getProblemsPdf(getRequest(Region.brattelinjer, "cs"), BRATTELINJER_PROBLEM_ID_PDF);
		assertEquals(OK, r.getStatusCode());
		var pdf = new ByteArrayOutputStream();
		assertInstanceOf(StreamingResponseBody.class, r.getBody()).writeTo(pdf);
		try (var reader = new PdfReader(pdf.toByteArray())) {
			assertEquals("cs", reader.getCatalog().getAsString(PdfName.LANG).toString());
		}
	}
}	

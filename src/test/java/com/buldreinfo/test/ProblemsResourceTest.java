package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.buldreinfo.controller.ProblemsController;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.model.Problem;

public class ProblemsResourceTest extends BaseResourceTest {
	@Autowired private ProblemsController tester;
	@Autowired private ProblemRepository problemRepo;

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
		problemRepo.getProblem(Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false));
		Problem p = problemRepo.getProblem(Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
		assertNotNull(p);
		assertFalse(p.name() == null || p.name().isBlank());
	}

	@Test
	public void testGetProblemPdf() throws IOException {
		var r = tester.getProblemsPdf(getRequest(Region.brattelinjer), BRATTELINJER_PROBLEM_ID_PDF);
		assertEquals(OK, r.getStatusCode());
		StreamingResponseBody stream = assertInstanceOf(StreamingResponseBody.class, r.getBody());
		Path p = Files.createTempFile("problemPdf", ".pdf");
		try (var fos = new FileOutputStream(p.toFile())) {
			stream.writeTo(fos);
		}
		Files.deleteIfExists(p);
	}
}	
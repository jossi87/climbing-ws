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
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.buldreinfo.controller.ProblemsController;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.model.Problem;
import com.google.common.base.Strings;

public class ProblemsResourceTest extends BaseResourceTest {
	@Autowired private ProblemsController tester;
	@Autowired private ProblemRepository problemRepo;

	@Test
	public void testGetProblem() throws Exception {
		ResponseEntity<?> r = tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, false);
		assertEquals(OK, r.getStatusCode());
		Problem p = assertInstanceOf(Problem.class, r.getBody());
		assertFalse(Strings.isNullOrEmpty(p.name()));
		assertNull(p.redirectUrl());
	}

	@Test
	public void testGetProblemDifferentRegion() throws Exception {
		ResponseEntity<?> r = tester.getProblems(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_PROBLEM_ID, false);
		assertEquals(OK, r.getStatusCode());
		Problem p = assertInstanceOf(Problem.class, r.getBody());
		assertTrue(Strings.isNullOrEmpty(p.name()));
		assertNotNull(p.redirectUrl());
	}

	@Test
	public void testGetProblemHidden() throws Exception {
		assertThrows(NoSuchElementException.class, () -> {
	        tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_PROBLEM_ID, false);
	    });

		var setup = getSetup(Region.buldreinfo);
		txManager.executeInTransaction(() -> {
			assertThrows(NoSuchElementException.class, () -> 
			problemRepo.getProblem(Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false));

			try {
				Problem p = problemRepo.getProblem(Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
				assertNotNull(p);
				assertFalse(Strings.isNullOrEmpty(p.name()));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testGetProblemPdf() throws Exception {
		ResponseEntity<?> r = tester.getProblemsPdf(getRequest(Region.brattelinjer), BRATTELINJER_PROBLEM_ID_PDF);
		assertEquals(OK, r.getStatusCode());

		StreamingResponseBody stream = assertInstanceOf(StreamingResponseBody.class, r.getBody());
		Path p = Files.createTempFile("problemPdf", ".pdf");
		try (var fos = new FileOutputStream(p.toFile())) {
			stream.writeTo(fos);
		}
		Files.deleteIfExists(p);
	}
}	
package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.ProblemRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.resources.ProblemsResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

public class ProblemsResourceTest extends BaseResourceTest {

	@Test
	public void testGetProblem() throws Exception {
		var tester = getService(ProblemsResource.class);
		try (Response r = tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_PROBLEM_ID_VISIBLE, false)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Problem);
			Problem p = (Problem)r.getEntity();
			assertTrue(!Strings.isNullOrEmpty(p.name()));
			assertTrue(p.redirectUrl() == null);
		}
	}

	@Test
	public void testGetProblemDifferentRegion() throws Exception {
		var tester = getService(ProblemsResource.class);
		try (Response r = tester.getProblems(getRequest(Region.brattelinjer), BRATTELINJER_DIFFERENT_REGION_PROBLEM_ID, false)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof Problem);
			Problem p = (Problem)r.getEntity();
			assertTrue(Strings.isNullOrEmpty(p.name()));
			assertTrue(p.redirectUrl() != null);
		}
	}

	@Test
	public void testGetProblemHidden() throws Exception {
		var tester = getService(ProblemsResource.class);
		var txManager = getService(TransactionManager.class);
		var problemRepo = getService(ProblemRepository.class);
		Response r = invoke(() -> tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_PROBLEM_ID, false));
		assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		Setup setup = getSetup(Region.buldreinfo);
		txManager.executeInTransaction(() -> {
			try {
				problemRepo.getProblem(Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		txManager.executeInTransaction(() -> {
			try {
				Problem p = problemRepo.getProblem(Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
				assertTrue(p != null);
				assertFalse(Strings.isNullOrEmpty(p.name()));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testGetProblemPdf() throws Exception {
		var tester = getService(ProblemsResource.class);
		try (Response r = tester.getProblemsPdf(getRequest(Region.brattelinjer), BRATTELINJER_PROBLEM_ID_PDF)) {
			assertTrue(r.getStatus() == Response.Status.OK.getStatusCode());
			assertTrue(r.getEntity() instanceof StreamingOutput);
			var streamingOutput = (StreamingOutput)r.getEntity();
			Path p = Files.createTempFile("problemPdf", ".pdf");
			try (var fos = new FileOutputStream(p.toFile())) {
				streamingOutput.write(fos);
			}
			Files.deleteIfExists(p);
		}
	}
}
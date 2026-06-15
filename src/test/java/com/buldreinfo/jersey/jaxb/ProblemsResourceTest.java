package com.buldreinfo.jersey.jaxb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.resources.ProblemsResource;
import com.google.common.base.Strings;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

public class ProblemsResourceTest extends BaseResourceTest {
	
	@Test
	public void testGetProblem() throws Exception {
		var tester = new ProblemsResource();
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
		var tester = new ProblemsResource();
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
		var tester = new ProblemsResource();
		try (Response r = tester.getProblems(getRequest(Region.buldreinfo), BULDREINFO_HIDDEN_PROBLEM_ID, false)) {
			assertTrue(r.getStatus() == Response.Status.NOT_FOUND.getStatusCode());
		}
		Setup setup = getSetup(Region.buldreinfo);
		DatabaseContext.runSql((dao, c) -> {
			try {
				dao.getProblemRepo().getProblem(c, Optional.of(USER_ID_NORMAL), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
				assertTrue(false);
			} catch (Exception e) {
				assertTrue(e instanceof NoSuchElementException);
			}
		});
		DatabaseContext.runSql((dao, c) -> {
			Problem p = dao.getProblemRepo().getProblem(c, Optional.of(USER_ID_SUPERADMIN), setup, BULDREINFO_HIDDEN_PROBLEM_ID, false, false);
			assertTrue(p != null);
			assertTrue(!Strings.isNullOrEmpty(p.name()));
		});
	}

	@Test
	public void testGetProblemPdf() throws Exception {
		var tester = new ProblemsResource();
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
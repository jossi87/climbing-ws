package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buldreinfo.controller.UsersController;
import com.buldreinfo.dao.TickRepository;
import com.buldreinfo.excel.TickTemplate;
import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.TickImportResult;
import com.buldreinfo.model.TickImportRow.Status;

public class UsersResourceTest extends BaseResourceTest {
	private record ProblemRef(String area, String sector, String name) {
	}
	@Autowired private JdbcClient jdbcClient;
	@Autowired private TickRepository tickRepo;
	@Autowired private UsersController tester;
	private final boolean dryRun = true;

	@BeforeEach
	public void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID_NORMAL, null, List.of()));
	}

	@AfterEach
	public void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	private ProblemRef visibleProblem() {
		return jdbcClient.sql("""
						SELECT a.name area, s.name sector, p.name name
						FROM problem p
						JOIN sector s ON p.sector_id = s.id
						JOIN area a ON s.area_id = a.id
						WHERE p.id = ?
						""")
				.param(BULDREINFO_PROBLEM_ID_VISIBLE)
				.query((rs, _) -> new ProblemRef(rs.getString("area"), rs.getString("sector"), rs.getString("name")))
				.single();
	}

	@Test
	public void testGetUsersTicksTemplate() throws IOException {
		var r = tester.getUsersTicksTemplate();
		assertEquals(OK, r.getStatusCode());
		assertNotNull(r.getBody());
		try (var in = new ByteArrayInputStream(r.getBody());
				var workbook = WorkbookFactory.create(in)) {
			var sheet = assertInstanceOf(XSSFSheet.class, workbook.getSheet(TickTemplate.SHEET_TICKS));
			assertNotNull(workbook.getSheet(TickTemplate.SHEET_GUIDE), "The template must document its columns");
			for (int i = 0; i < TickTemplate.COLUMNS.size(); i++) {
				assertEquals(TickTemplate.COLUMNS.get(i), sheet.getRow(0).getCell(i).getStringCellValue());
			}
			var validations = sheet.getDataValidations();
			assertEquals(1, validations.size(), "STARS must have a dropdown");
			var regions = validations.getFirst().getRegions();
			assertEquals(TickTemplate.COLUMNS.indexOf(TickTemplate.COL_STARS), regions.getCellRangeAddress(0).getFirstColumn());
			assertEquals(1, regions.getCellRangeAddress(0).getFirstRow());
			assertArrayEquals(new String[] { "0", "1", "2", "3" }, validations.getFirst().getValidationConstraint().getExplicitListValues());
		}
	}

	@Test
	public void testPostUsersTicksImportPreviewOfExistingProblem() throws IOException {
		var problem = visibleProblem();
		var r = tester.postUsersTicksImport(dryRun, TickWorkbook.file(List.of(
				Arrays.<Object>asList(problem.area(), problem.sector(), problem.name(), LocalDate.of(2024, 5, 1), 3, "Imported"))));
		assertEquals(OK, r.getStatusCode());
		var result = assertInstanceOf(TickImportResult.class, r.getBody());
		assertFalse(result.committed());
		assertEquals(0, result.numFailed(), "A visible problem must be found by AREA, SECTOR and NAME");
		var row = result.rows().getFirst();
		assertEquals(BULDREINFO_PROBLEM_ID_VISIBLE, row.idProblem());
		assertTrue(row.status() == Status.OK || row.status() == Status.DUPLICATE, "Unexpected status=" + row.status());
		assertEquals("2024-05-01", row.date());
		assertEquals(3.0, row.stars());
	}

	@Test
	public void testPostUsersTicksImportPreviewNeverSaves() throws IOException {
		var tickedBefore = tickRepo.getTickedProblemIds(USER_ID_NORMAL, List.of(BULDREINFO_PROBLEM_ID_VISIBLE));
		var problem = visibleProblem();
		var r = tester.postUsersTicksImport(dryRun, TickWorkbook.file(List.of(
				Arrays.<Object>asList(problem.area(), problem.sector(), problem.name(), "01.05.2024", null, "Felt easy"))));
		assertEquals(OK, r.getStatusCode());
		var result = assertInstanceOf(TickImportResult.class, r.getBody());
		assertFalse(result.committed(), "A dry run must never report a commit");
		assertEquals("2024-05-01", result.rows().getFirst().date(), "A text date dd.mm.yyyy must be understood");
		assertEquals(tickedBefore, tickRepo.getTickedProblemIds(USER_ID_NORMAL, List.of(BULDREINFO_PROBLEM_ID_VISIBLE)),
				"A dry run must not create ticks");
	}

	@Test
	public void testPostUsersTicksImportPreviewOfInvalidRows() throws IOException {
		var r = tester.postUsersTicksImport(dryRun, TickWorkbook.file(List.of(
				Arrays.<Object>asList("Nowhere", "Nothing", "This problem does not exist 424242", "not a date", null, null),
				Arrays.<Object>asList("Nowhere", "Nothing", "This problem does not exist 424242", LocalDate.of(2024, 5, 1), 7, null))));
		assertEquals(OK, r.getStatusCode());
		var result = assertInstanceOf(TickImportResult.class, r.getBody());
		assertEquals(2, result.rows().size());
		assertEquals(Status.INVALID, result.rows().get(0).status());
		assertEquals("not a date", result.rows().get(0).date());
		assertEquals(Status.INVALID, result.rows().get(1).status(), "STARS=7 is outside 0-3");
		assertEquals(0, result.numSaved());
		assertEquals(2, result.numFailed());
	}

	@Test
	public void testPostUsersTicksImportPreviewOfUnknownProblem() throws IOException {
		var r = tester.postUsersTicksImport(dryRun, TickWorkbook.file(List.of(
				Arrays.<Object>asList("Nowhere", "Nothing", "This problem does not exist 424242", LocalDate.of(2024, 5, 1), 2, null))));
		assertEquals(OK, r.getStatusCode());
		var result = assertInstanceOf(TickImportResult.class, r.getBody());
		assertFalse(result.committed());
		assertEquals(1, result.rows().size());
		assertEquals(Status.NOT_FOUND, result.rows().getFirst().status());
		assertEquals(2, result.rows().getFirst().row());
		assertEquals("2024-05-01", result.rows().getFirst().date());
		assertEquals(0, result.numSaved());
		assertEquals(1, result.numFailed());
	}

	@Test
	public void testPostUsersTicksImportRejectsSheetWithoutDateColumn() throws IOException {
		var file = TickWorkbook.file(new String[] { TickTemplate.COL_NAME }, List.of(List.of("This problem does not exist 424242")));
		assertThrows(ValidationFailedException.class, () -> tester.postUsersTicksImport(true, file));
	}

	@Test
	public void testPostUsersTicksImportRejectsUnsupportedFiles() {
		var txt = new MockMultipartFile("file", "ticks.txt", "text/plain", "AREA;NAME".getBytes());
		assertThrows(ValidationFailedException.class, () -> tester.postUsersTicksImport(dryRun, txt));
		assertThrows(ValidationFailedException.class, () -> tester.postUsersTicksImport(dryRun, null));
	}
}

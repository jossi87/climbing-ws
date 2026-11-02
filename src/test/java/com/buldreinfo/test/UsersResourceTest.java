package com.buldreinfo.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buldreinfo.controller.UsersController;
import com.buldreinfo.excel.TickTemplate;

public class UsersResourceTest extends BaseResourceTest {
	@Autowired private UsersController tester;

	@BeforeEach
	public void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID_NORMAL, null, List.of()));
	}

	@AfterEach
	public void clearAuthentication() {
		SecurityContextHolder.clearContext();
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
			for (int i = 0; i < TickTemplate.HEADERS.size(); i++) {
				assertEquals(TickTemplate.HEADERS.get(i), sheet.getRow(0).getCell(i).getStringCellValue());
			}
			var validations = sheet.getDataValidations();
			assertEquals(1, validations.size(), "STARS must have a dropdown");
			var regions = validations.getFirst().getRegions();
			assertEquals(TickTemplate.COLUMNS.indexOf(TickTemplate.COL_STARS), regions.getCellRangeAddress(0).getFirstColumn());
			assertEquals(1, regions.getCellRangeAddress(0).getFirstRow());
			assertArrayEquals(new String[] { "0", "1", "2", "3" }, validations.getFirst().getValidationConstraint().getExplicitListValues());
		}
	}
}

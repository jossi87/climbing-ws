package com.buldreinfo.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

import com.buldreinfo.excel.TickTemplate;

/** Builds the spreadsheet a user would upload, using the same header labels as the downloaded template. */
public final class TickWorkbook {
	private TickWorkbook() {
	}

	public static MockMultipartFile file(List<List<Object>> rows) throws IOException {
		return file(TickTemplate.COLUMNS.toArray(String[]::new), rows);
	}

	public static MockMultipartFile file(String[] header, List<List<Object>> rows) throws IOException {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var sheet = workbook.createSheet(TickTemplate.SHEET_TICKS);
			var dateStyle = workbook.createCellStyle();
			dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
			var headerRow = sheet.createRow(0);
			for (int i = 0; i < header.length; i++) {
				headerRow.createCell(i).setCellValue(header[i]);
			}
			for (int r = 0; r < rows.size(); r++) {
				var row = sheet.createRow(r + 1);
				var values = rows.get(r);
				for (int c = 0; c < values.size(); c++) {
					var cell = row.createCell(c);
					switch (values.get(c)) {
						case String s -> cell.setCellValue(s);
						case LocalDate d -> {
							cell.setCellValue(d);
							cell.setCellStyle(dateStyle);
						}
						case Integer i -> cell.setCellValue(i);
						case null, default -> {
						}
					}
				}
			}
			workbook.write(out);
			return new MockMultipartFile("file", "ticks.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
		}
	}
}

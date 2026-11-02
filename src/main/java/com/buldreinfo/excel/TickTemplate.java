package com.buldreinfo.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Template (xlsx) for batch registration of ticks: one sheet to fill in and one sheet with the
 * guide lines. The header labels in {@link #COLUMNS} are the contract with {@code TickImportService},
 * which looks up every column by label and ignores columns it does not know.
 */
public final class TickTemplate {
	public static final String SHEET_TICKS = "TICKS";
	public static final String SHEET_GUIDE = "GUIDE";
	public static final String COL_AREA = "AREA";
	public static final String COL_SECTOR = "SECTOR";
	public static final String COL_NAME = "NAME";
	public static final String COL_DATE = "DATE";
	public static final String COL_STARS = "STARS";
	public static final String COL_COMMENT = "COMMENT";
	public static final List<String> COLUMNS = List.of(COL_AREA, COL_SECTOR, COL_NAME, COL_DATE, COL_STARS, COL_COMMENT);
	/** The STARS dropdown and the DATE format cover row 1 (header) through this row (1-based). */
	public static final int LAST_DATA_ROW = 1000;
	private static final String DATE_FORMAT = "yyyy-mm-dd";
	private static final String[] STARS = { "0", "1", "2", "3" };
	private static final List<String> GUIDE = List.of(
			"Batch registration of ticks",
			"",
			"One ascent per row in the TICKS sheet, starting on row 2. Up to 500 rows per import.",
			"Columns are matched by their header label, so extra columns you add are ignored.",
			"",
			"AREA - optional. The area the problem belongs to. Used to narrow down the search when a problem name is not unique.",
			"SECTOR - optional. The sector the problem belongs to. Used the same way as AREA.",
			"NAME - required. The problem name exactly as it appears on the site (case and leading/trailing spaces are ignored).",
			"DATE - required. The date of the ascent. Use a date cell formatted " + DATE_FORMAT + ", or text in "
					+ DATE_FORMAT + ", yyyy.mm.dd, yyyy/mm/dd or dd.mm.yyyy.",
			"STARS - optional. 0-3 stars from the dropdown. Leave the cell empty for no rating.",
			"COMMENT - optional. Free text, at most 4000 characters.",
			"",
			"Rows that cannot be matched to a problem are reported and skipped. Problems you have already ticked are",
			"reported as duplicates and never overwritten, so an accidental re-import will not change existing ticks.",
			"Nothing is saved until you confirm the preview.");

	private TickTemplate() {
	}

	public static byte[] build() {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var headerStyle = createHeaderStyle(workbook);
			var dateStyle = createDateStyle(workbook);
			writeTicksSheet(workbook, headerStyle, dateStyle);
			writeGuideSheet(workbook);
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to build the tick import template", e);
		}
	}

	private static void writeTicksSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle dateStyle) {
		var sheet = workbook.createSheet(SHEET_TICKS);
		var header = sheet.createRow(0);
		for (int i = 0; i < COLUMNS.size(); i++) {
			var cell = header.createCell(i);
			cell.setCellValue(COLUMNS.get(i));
			cell.setCellStyle(headerStyle);
		}
		sheet.createFreezePane(0, 1);
		int[] widths = { 30, 30, 45, 14, 8, 70 };
		for (int i = 0; i < widths.length; i++) {
			sheet.setColumnWidth(i, widths[i] * 256);
		}
		sheet.setDefaultColumnStyle(COLUMNS.indexOf(COL_DATE), dateStyle);
		addStarsDropdown(sheet);
	}

	private static void addStarsDropdown(Sheet sheet) {
		int column = COLUMNS.indexOf(COL_STARS);
		var helper = sheet.getDataValidationHelper();
		DataValidationConstraint constraint = helper.createExplicitListConstraint(STARS);
		var ranges = new CellRangeAddressList(1, LAST_DATA_ROW - 1, column, column);
		DataValidation validation = helper.createValidation(constraint, ranges);
		validation.setSuppressDropDownArrow(false);
		validation.setEmptyCellAllowed(true);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid stars", "Stars must be empty or a number from 0 to 3.");
		sheet.addValidationData(validation);
	}

	private static void writeGuideSheet(XSSFWorkbook workbook) {
		var sheet = workbook.createSheet(SHEET_GUIDE);
		for (int i = 0; i < GUIDE.size(); i++) {
			var cell = sheet.createRow(i).createCell(0);
			cell.setCellValue(GUIDE.get(i));
		}
		sheet.setColumnWidth(0, 120 * 256);
	}

	private static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
		var font = workbook.createFont();
		font.setBold(true);
		var style = workbook.createCellStyle();
		style.setFont(font);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	private static CellStyle createDateStyle(XSSFWorkbook workbook) {
		var style = workbook.createCellStyle();
		style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_FORMAT));
		return style;
	}
}

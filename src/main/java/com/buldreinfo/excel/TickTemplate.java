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
 * guide lines. The header labels in {@link #HEADERS} are what the user reads; the constants in
 * {@link #COLUMNS} are the same columns without the format hint, and {@code TickImportService}
 * looks up every column by them and ignores columns it does not know.
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
	/**
	 * The header row of the TICKS sheet, parallel to {@link #COLUMNS}: the hint in parentheses tells
	 * the user what to type, and {@link ExcelReader} drops it when it matches the columns.
	 */
	public static final List<String> HEADERS = List.of(
			COL_AREA + " (optional)",
			COL_SECTOR + " (optional)",
			COL_NAME + " (required)",
			COL_DATE + " (YYYY-MM-DD)",
			COL_STARS + " (0-3)",
			COL_COMMENT + " (optional)");
	/** The STARS dropdown and the DATE format cover row 1 (header) through this row (1-based). */
	public static final int LAST_DATA_ROW = 1000;
	private static final String DATE_FORMAT = "yyyy-mm-dd";
	private static final String[] STARS = { "0", "1", "2", "3" };
	/** Column widths in characters, shared by both sheets so the guide lines up with what to fill in. */
	private static final int[] COLUMN_WIDTHS = { 22, 22, 45, 20, 13, 70 };
	private static final List<String> GUIDE = List.of(
			"Batch registration of ticks",
			"",
			"Fill in one ascent per row in the TICKS sheet, starting on row 2. Up to 500 rows per import.",
			"Columns are matched by their header label, so the order does not matter and extra columns are ignored.",
			"The header row repeats what to type in parentheses, e.g. DATE (YYYY-MM-DD).",
			"",
			"AREA (optional) - the area the problem belongs to. Narrows the search when a name is not unique.",
			"SECTOR (optional) - the sector the problem belongs to. Used the same way as AREA.",
			"NAME (required) - the problem name exactly as it appears on the site. Case and extra spaces are ignored.",
			"DATE (required) - the day you climbed it, as YYYY-MM-DD (year-month-day), for instance 2024-05-08.",
			"  A real date cell is just as good, and so are 2024.05.08, 2024/05/08 and 08.05.2024.",
			"STARS (optional) - your rating: 0, 1, 2 or 3. Pick it from the dropdown. Empty cell means no rating.",
			"COMMENT (optional) - free text, at most 4000 characters.",
			"",
			"Example row 2: AREA=<area>, SECTOR=<sector>, NAME=<problem name>, DATE=2024-05-08, STARS=3",
			"Example row 3: only NAME and DATE are required - AREA, SECTOR, STARS and COMMENT can be left empty.",
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
		for (int i = 0; i < HEADERS.size(); i++) {
			var cell = header.createCell(i);
			cell.setCellValue(HEADERS.get(i));
			cell.setCellStyle(headerStyle);
		}
		sheet.createFreezePane(0, 1);
		for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
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
		// Same widths as the TICKS sheet; the text is longer than the first column and spills over the empty cells.
		for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
		}
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

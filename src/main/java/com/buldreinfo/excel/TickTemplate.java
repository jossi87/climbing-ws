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
 * Template (xlsx) for batch registration of ticks: a single sheet to fill in. The header labels in
 * {@link #HEADERS} are what the user reads; the constants in {@link #COLUMNS} are the same columns
 * without the format hint, and {@code TickImportService} looks up every column by them and ignores
 * columns it does not know.
 */
public final class TickTemplate {
	public static final String SHEET_TICKS = "TICKS";
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
	/** Column widths in characters, so the sheet reads well before anything is typed. */
	private static final int[] COLUMN_WIDTHS = { 22, 22, 45, 20, 13, 70 };

	private TickTemplate() {
	}

	public static byte[] build() {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var headerStyle = createHeaderStyle(workbook);
			var dateStyle = createDateStyle(workbook);
			writeTicksSheet(workbook, headerStyle, dateStyle);
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

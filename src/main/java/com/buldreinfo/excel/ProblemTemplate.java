package com.buldreinfo.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.buldreinfo.model.Type;

/**
 * Template (xlsx) for entering many problems and sectors at once. The header labels in {@link #HEADERS} are what the
 * user reads and {@link #COLUMNS} are the same columns without the format hint; {@code ProblemImportService} looks up
 * every column by them and ignores columns it does not know.
 */
public final class ProblemTemplate {
	public static final String SHEET_PROBLEMS = "PROBLEMS";
	public static final String COL_AREA = "AREA";
	public static final String COL_SECTOR = "SECTOR";
	public static final String COL_NR = "NR";
	public static final String COL_PROBLEM = "PROBLEM";
	public static final String COL_DESCRIPTION = "DESCRIPTION";
	public static final String COL_TYPE = "TYPE";
	public static final String COL_GRADE = "GRADE";
	public static final String COL_FA_DATE = "FA DATE";
	public static final String COL_FA_USERS = "FA USERS";
	public static final List<String> COLUMNS = List.of(COL_AREA, COL_SECTOR, COL_NR, COL_PROBLEM, COL_DESCRIPTION,
			COL_TYPE, COL_GRADE, COL_FA_DATE, COL_FA_USERS);
	public static final List<String> HEADERS = List.of(
			COL_AREA + " (required, existing)",
			COL_SECTOR + " (required)",
			COL_NR + " (required)",
			COL_PROBLEM + " (required)",
			COL_DESCRIPTION + " (optional)",
			COL_TYPE + " (required)",
			COL_GRADE + " (required)",
			COL_FA_DATE + " (YYYY-MM-DD, optional)",
			COL_FA_USERS + " (comma separated, optional)");
	/** The TYPE dropdown and the FA DATE format cover row 1 (header) through this row (1-based). */
	public static final int LAST_DATA_ROW = 1000;
	private static final String DATE_FORMAT = "yyyy-mm-dd";
	/**
	 * The type labels of the region do not fit in a dropdown (Excel stops at 255 characters), so they live on their own
	 * sheet and the TYPE column points at that range through a workbook name.
	 */
	private static final String LIST_SHEET = "LISTS";
	private static final String TYPE_RANGE = "PROBLEM_TYPES";
	private static final int[] COLUMN_WIDTHS = { 28, 28, 8, 45, 60, 30, 14, 22, 34 };

	private ProblemTemplate() {
	}

	public static byte[] build(List<Type> types) {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var headerStyle = createHeaderStyle(workbook);
			var dateStyle = createDateStyle(workbook);
			writeProblemsSheet(workbook, headerStyle, dateStyle);
			writeTypeList(workbook, types);
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to build the problem import template", e);
		}
	}

	/** What the user types in the TYPE column: the type plus the subtype when they differ. */
	public static String typeLabel(Type t) {
		var subType = t.subType() == null ? "" : t.subType().strip();
		return subType.isEmpty() || subType.equalsIgnoreCase(t.type()) ? t.type() : t.type() + " - " + subType;
	}

	private static void writeProblemsSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle dateStyle) {
		var sheet = workbook.createSheet(SHEET_PROBLEMS);
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
		sheet.setDefaultColumnStyle(COLUMNS.indexOf(COL_FA_DATE), dateStyle);
	}

	private static void writeTypeList(XSSFWorkbook workbook, List<Type> types) {
		var labels = new ArrayList<String>();
		for (var t : types) {
			var label = typeLabel(t);
			if (!labels.contains(label)) {
				labels.add(label);
			}
		}
		if (labels.isEmpty()) {
			return;
		}
		var sheet = workbook.createSheet(LIST_SHEET);
		for (int i = 0; i < labels.size(); i++) {
			sheet.createRow(i).createCell(0).setCellValue(labels.get(i));
		}
		sheet.setColumnWidth(0, 60 * 256);

		var name = workbook.createName();
		name.setNameName(TYPE_RANGE);
		name.setRefersToFormula("%s!$A$1:$A$%d".formatted(LIST_SHEET, labels.size()));

		int column = COLUMNS.indexOf(COL_TYPE);
		var helper = workbook.getSheet(SHEET_PROBLEMS).getDataValidationHelper();
		DataValidationConstraint constraint = helper.createFormulaListConstraint(TYPE_RANGE);
		var ranges = new CellRangeAddressList(1, LAST_DATA_ROW - 1, column, column);
		DataValidation validation = helper.createValidation(constraint, ranges);
		validation.setEmptyCellAllowed(true);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid type", "Pick one of the types listed on the " + LIST_SHEET + " sheet.");
		workbook.getSheet(SHEET_PROBLEMS).addValidationData(validation);

		workbook.setSheetHidden(workbook.getSheetIndex(sheet), true);
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

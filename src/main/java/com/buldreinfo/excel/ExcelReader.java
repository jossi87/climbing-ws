package com.buldreinfo.excel;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Minimal reader for uploaded spreadsheets: maps the labels in the header row to column indexes so
 * values can be read by label instead of by position, which makes extra or reordered columns
 * harmless. A header may describe the expected value in parentheses ("DATE (YYYY-MM-DD)"), which is
 * a hint for the user and not part of the label. Blank rows are skipped and cells are read by their
 * cached value, so formulas work too.
 */
public final class ExcelReader implements AutoCloseable {
	/** Trailing hint in a header, e.g. the "(YYYY-MM-DD)" of "DATE (YYYY-MM-DD)". */
	private static final Pattern LABEL_HINT = Pattern.compile("\\s*\\([^)]*\\)$");
	private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ROOT);
	private final Workbook workbook;
	private final Sheet sheet;
	private final Map<String, Integer> columns;
	private final int headerRowNum;

	private ExcelReader(Workbook workbook, Sheet sheet) {
		this.workbook = workbook;
		this.sheet = sheet;
		this.columns = new HashMap<>();
		this.headerRowNum = findHeaderRow(sheet);
		var header = sheet.getRow(headerRowNum);
		for (int i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {
			var label = textOf(header.getCell(i));
			if (label != null && !label.isBlank()) {
				columns.putIfAbsent(normalizeLabel(label), i);
			}
		}
	}

	public static ExcelReader open(InputStream in) throws IOException {
		return open(in, null);
	}

	public static ExcelReader open(InputStream in, String preferredSheetName) throws IOException {
		try (var workbook = WorkbookFactory.create(in)) {
			return new ExcelReader(workbook, pickSheet(workbook, preferredSheetName));
		}
	}

	public String sheetName() {
		return sheet.getSheetName();
	}

	public boolean hasColumn(String label) {
		return columns.containsKey(normalizeLabel(label));
	}

	/** All rows below the header row that have at least one non-blank mapped cell. */
	public List<RowReader> rows() {
		var result = new ArrayList<RowReader>();
		for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
			var row = sheet.getRow(i);
			if (row != null) {
				var reader = new RowReader(row, columns);
				if (!reader.isEmpty()) {
					result.add(reader);
				}
			}
		}
		return result;
	}

	@Override
	public void close() throws IOException {
		workbook.close();
	}


	private static Sheet pickSheet(Workbook workbook, String preferredSheetName) {
		if (preferredSheetName != null) {
			var preferred = workbook.getSheet(preferredSheetName);
			if (preferred != null) {
				return preferred;
			}
		}
		if (workbook.getNumberOfSheets() == 0) {
			throw new IllegalArgumentException("The spreadsheet has no sheets");
		}
		return workbook.getSheetAt(0);
	}

	private static int findHeaderRow(Sheet sheet) {
		for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
			var row = sheet.getRow(i);
			if (row != null && row.getLastCellNum() > row.getFirstCellNum()) {
				return i;
			}
		}
		throw new IllegalArgumentException("The spreadsheet has no header row");
	}

	private static String normalizeLabel(String label) {
		// The hint in parentheses only describes what to type, so "DATE (YYYY-MM-DD)" is the DATE column.
		var withoutHint = LABEL_HINT.matcher(label.replace('\u00A0', ' ').strip()).replaceFirst("").strip();
		return withoutHint.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
	}

	private static CellType cellType(Cell cell) {
		return cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
	}

	private static String textOf(Cell cell) {
		if (cell == null) {
			return null;
		}
		return switch (cellType(cell)) {
			case STRING -> cell.getStringCellValue();
			case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue().toLocalDate().toString() : plainNumber(cell.getNumericCellValue());
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			default -> null;
		};
	}

	private static String plainNumber(double value) {
		if (value == Math.rint(value) && Math.abs(value) < 1e15) {
			return String.valueOf((long) value);
		}
		return String.valueOf(value);
	}

	/** Accepts 2024-05-01, 2024.05.01, 2024/05/01 and 1.5.2024, all with or without leading zeros. */
	static Optional<LocalDate> parseDate(String value) {
		var normalized = value.replace('\u00A0', ' ').strip().replace('.', '-').replace('/', '-');
		for (var formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE, SHORT_DATE)) {
			try {
				return Optional.of(LocalDate.parse(normalized, formatter));
			} catch (DateTimeParseException _) {
				// try the next format
			}
		}
		return Optional.empty();
	}

	public static final class RowReader {
		private final Row row;
		private final Map<String, Integer> columns;

		private RowReader(Row row, Map<String, Integer> columns) {
			this.row = row;
			this.columns = columns;
		}

		/** 1-based row number as shown in the spreadsheet. */
		public int rowNumber() {
			return row.getRowNum() + 1;
		}

		public Optional<String> text(String label) {
			var value = textOf(cell(label));
			return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.strip());
		}

		public Optional<LocalDate> date(String label) {
			var cell = cell(label);
			if (cell == null) {
				return Optional.empty();
			}
			if (cellType(cell) == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
				return Optional.of(cell.getLocalDateTimeCellValue().toLocalDate());
			}
			return text(label).flatMap(ExcelReader::parseDate);
		}

		public boolean isEmpty() {
			for (var index : columns.values()) {
				var value = textOf(row.getCell(index));
				if (value != null && !value.isBlank()) {
					return false;
				}
			}
			return true;
		}

		private Cell cell(String label) {
			var index = columns.get(normalizeLabel(label));
			return index == null ? null : row.getCell(index);
		}
	}
}

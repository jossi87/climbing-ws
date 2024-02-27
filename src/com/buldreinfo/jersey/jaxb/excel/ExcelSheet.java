package com.buldreinfo.jersey.jaxb.excel;

import java.io.Closeable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class ExcelSheet implements Closeable {
	private final ExcelWorkbook workbook;
	private final Sheet sheet;
	private final Row headerRow;
	private final Map<String, Integer> headerLookup = new HashMap<>();
	private int rowIndex = 0;
	private int columnIndex = 0;
	
	protected ExcelSheet(ExcelWorkbook workbook, Sheet sheet) {
		this.workbook = workbook;
		this.sheet = sheet;
		this.rowIndex = 0;
		this.columnIndex = 0;
		this.headerRow = getOrCreateRow();
	}

	@Override
	public void close() {
		sheet.createFreezePane(0, 0);
		sheet.setAutoFilter(new CellRangeAddress(0, rowIndex, 0, columnIndex - 1));
		for (int columnIx : headerLookup.values()) {
			sheet.autoSizeColumn(columnIx);
		}
	}

	public void incrementRow() {
		this.rowIndex++;
	}

	public void writeDouble(String columnName, double value) {
		Cell cell = getOrCreateCell(columnName);
		cell.setCellValue(value);
	}
	
	public void writeDate(String columnName, Date date) {
		Cell cell = getOrCreateCell(columnName);
		cell.setCellValue(date);
		cell.setCellStyle(workbook.getDateStyle());
	}

	public void writeHyperlink(String columnName, String url) {
		Cell cell = getOrCreateCell(columnName);
		Hyperlink hyperlink = cell.getSheet().getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.URL);
		hyperlink.setAddress(url);
		cell.setCellValue(url);
		cell.setHyperlink(hyperlink);
		cell.setCellStyle(workbook.getHyperlinkStyle());
	}
	
	public void writeInt(String columnName, int value) {
		Cell cell = getOrCreateCell(columnName);
		cell.setCellValue(value);
	}
	
	public void writeString(String columnName, String value) {
		Cell cell = getOrCreateCell(columnName);
		cell.setCellValue(value);
	}
	
	private Cell getOrCreateCell(String columnName) {
		return getOrCreateRow().createCell(getOrCreateColumn(columnName));
	}

	private int getOrCreateColumn(String columnName) {
		Integer index = headerLookup.get(columnName);
		if (index == null) {
			index = columnIndex++;
			headerLookup.put(columnName, index);
			Cell cell = headerRow.createCell(index);
			cell.setCellStyle(workbook.getHeaderStyle());
			cell.setCellValue(columnName);
		}
		return index;
	}

	private Row getOrCreateRow() {
		Row row = sheet.getRow(rowIndex);
		return row != null ? row : sheet.createRow(rowIndex);
	}
}
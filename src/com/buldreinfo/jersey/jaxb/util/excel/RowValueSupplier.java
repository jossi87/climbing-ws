package com.buldreinfo.jersey.jaxb.util.excel;

import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFEvaluationWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.buldreinfo.jersey.jaxb.helpers.CellHelper;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.DefaultValueSupplier;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetWriter;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

/**
 * A value supplier that uses a given row as a template.
 * <p>
 * The values in the row will be copied.
 */
class RowValueSupplier implements DefaultValueSupplier {
	/**
	 * The value and cell style to supply,
	 */
	private class ValueRef {
		// Either value or formula will have a value
		public final Object value;
		public final Ptg[] formula;
		
		// We assume this is stable
		public final int sourceSheetIndex;
		public final String sourceSheetName;
		public final CellStyle style;

		// The current adjusted formula index
		private int formulaRowIndex;
		
		public ValueRef(Object value, Ptg[] formula, int sourceSheetIndex, String sourceSheetName, int sourceRowIndex, CellStyle style) {
			if (value == null && formula == null) {
				throw new IllegalArgumentException("Either value or formula must be non-null.");
			}
			this.value = value;
			this.formula = formula;
			
			this.sourceSheetIndex = sourceSheetIndex;
			this.sourceSheetName = sourceSheetName;
			this.formulaRowIndex = sourceRowIndex;
			this.style = style;
		}

		public void writeTo(SheetWriter writer, int columnIndex) {
			if (value != null) {
				// Just write the value directly
				writer.write(columnIndex, style, value);
				
			} else if (formula != null) {
				// Ensure the formula is valid before writing it
				int delta = writer.getAbsoluteRowIndex() - formulaRowIndex;
				
				// Not thread safe
				if (delta != 0) {
					FormulaShifter shifter = FormulaShifter.createForRowCopy(
							sourceSheetIndex, sourceSheetName, formulaRowIndex, 
							writer.getAbsoluteRowIndex(), delta, evaluationWorkbook.getSpreadsheetVersion());
	
					Integer sheetIndex = sheetIndexCache.computeIfAbsent(writer.getSheet(), s -> s.getWorkbook().getSheetIndex(s));
					shifter.adjustFormula(formula, sheetIndex);
					
					// Formula will now have a new base row
					formulaRowIndex = writer.getAbsoluteRowIndex();
				}
				String newFormula = FormulaRenderer.toFormulaString(renderingWorkbook, formula);
				
				try {
					// Adjusted formula
					writer.write(columnIndex, style, new ExcelReport.SheetFormula(newFormula));
				} catch (FormulaParseException e) {
					throw new RuntimeException("Unable to set formula " + newFormula, e);
				}
			}	
		}
	}
	
	private final Map<Integer, ValueRef> defaultValues = Maps.newHashMap();
	private final Map<Sheet, Integer> sheetIndexCache = new MapMaker().weakKeys().makeMap();
	
	private final FormulaParsingWorkbook evaluationWorkbook;
	private final FormulaRenderingWorkbook renderingWorkbook;
	
	/**
	 * Construct a new row value supplier for entries in the given row.
	 * @param row the row.
	 */
	protected RowValueSupplier(Workbook workbook) {		
		this.evaluationWorkbook = createParsing(workbook);
		this.renderingWorkbook = (FormulaRenderingWorkbook)evaluationWorkbook;
	}
	
	/**
	 * Construct a new row value supplier that uses the content of the given row.
	 * @param row the row.
	 * @return The corresponding row supplier.
	 */
	public static RowValueSupplier fromRow(Row row) {
		Sheet sheet = row.getSheet();
		RowValueSupplier supplier = new RowValueSupplier(sheet.getWorkbook());
		
		int sheetIndex = sheet.getWorkbook().getSheetIndex(sheet);
		String sheetName = sheet.getSheetName();
		
		for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
			Cell cell = row.getCell(i);
			
			if (cell != null) {
				supplier.addCell(sheetIndex, sheetName, cell);
			}
		}
		return supplier;
	}

	/**
	 * Add the content of the given cell as a default value that can be supplied.
	 * @param sheetIndex the sheet index.
	 * @param sheetName the sheet name.
	 * @param columnIndex the column index.
	 * @param cell the cell.
	 */
	protected void addCell(int sheetIndex, String sheetName, Cell cell) {
		CellStyle style = cell.getCellStyle();
		
		// Handle formula cells directly
		if (cell.getCellType() == CellType.FORMULA) {
			Ptg[] tokens = FormulaParser.parse(
					cell.getCellFormula(), evaluationWorkbook, FormulaType.CELL, sheetIndex);
			defaultValues.put(cell.getColumnIndex(), new ValueRef(null, tokens, sheetIndex, sheetName, cell.getRowIndex(), style));
		} else {
			Object value = CellHelper.getValue(cell);
			defaultValues.put(cell.getColumnIndex(), new ValueRef(value, null, sheetIndex, sheetName, cell.getRowIndex(), style));
		}
	}

	@Override
	public void onWriteDefault(SheetWriter writer, int columnIndex, String columnName) {
		ValueRef ref = defaultValues.get(columnIndex);
		
		if (ref != null) {
			ref.writeTo(writer, columnIndex);
		}
	}
	
	/**
	 * Create a parsing workbook from the given workbook.
	 * @param workbook the workbook.
	 * @return The parsing workbook.
	 */
	protected static FormulaParsingWorkbook createParsing(Workbook workbook) {
		if (workbook instanceof XSSFWorkbook) {
			return XSSFEvaluationWorkbook.create((XSSFWorkbook) workbook);
		} else if (workbook instanceof SXSSFWorkbook) {
			return SXSSFEvaluationWorkbook.create((SXSSFWorkbook) workbook);
		} else if (workbook instanceof HSSFWorkbook) {
			return HSSFEvaluationWorkbook.create((HSSFWorkbook) workbook);
		} else {
			throw new IllegalArgumentException("Unable to create evaluation workbook from " + workbook);
		}
	}
}

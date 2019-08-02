package com.buldreinfo.jersey.jaxb.helpers;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import com.google.common.base.Preconditions;

/**
 * Represents a class for reading the content of Apache POI cells.
 */
public final class CellHelper {
	/**
	 * Represents an error in some cell, typically due to a formula error.
	 */
	public static class ExcelError {
		private final byte errorCode;
		
		/**
		 * Construct a new Excel error.
		 * @param b the error text.
		 */
		public ExcelError(byte errorCode) {
			super();
			this.errorCode = errorCode;
		}

		/**
		 * Retrieve the error code.
		 * @return The error code.
		 */
		public byte getErrorCode() {
			return errorCode;
		}
	}
	
	private CellHelper() {
		// Sealed
	}
	
	/**
	 * Determine if the given cell contains a string.
	 * @param cell the cell. Can be NULL.
	 * @return TRUE if it does, FALSE otherwise or the cell is NULL.
	 */
	public static boolean isStringCell(Cell cell) {
		if (cell == null) {
			return false;
		}
		CellType type = getEffectiveType(cell);
		return type == CellType.STRING || type == CellType.BLANK;
	}
	
	/**
	 * Determine if the given cell contains a number.
	 * @param cell the cell. Can be NULL.
	 * @return TRUE if it does, FALSE otherwise or the cell is NULL.
	 */
	public static boolean isNumericCell(Cell cell) {
		return cell != null && getEffectiveType(cell) == CellType.NUMERIC;
	}
	
	/**
	 * Determine if the given cell contains a boolean.
	 * @param cell the cell. Can be NULL.
	 * @return TRUE if it does, FALSE otherwise or the cell is NULL.
	 */
	public static boolean isBooleanCell(Cell cell) {
		return cell != null && getEffectiveType(cell) == CellType.BOOLEAN;
	}
	
	/**
	 * Match the content of a cell.
	 * </p>
	 * Booleans will throw an exception.
	 * @param cell the cell.
	 * @param ifString applied if the cell contains a string.
	 * @param ifNumber applied if the cell contains a number.
	 * @return The resulting value.
	 */
	public static <T> T match(Cell cell, Function<String, T> ifString, DoubleFunction<T> ifNumber) {
		return match(cell, ifString, ifNumber, 
				bool -> { throw new IllegalArgumentException("Unexpected boolean cell " + cell); },
				error -> { throw createError(cell); });
	}
	
	/**
	 * Match the content of a cell, using different type functions.
	 * @param cell the cell.
	 * @param ifString applied if the cell contains a string.
	 * @param ifNumber applied if the cell contains a number.
	 * @param ifBoolean applied if the cell contains a boolean.
	 * @return The resulting value.
	 */
	public static <T> T match(Cell cell, Function<String, T> ifString, DoubleFunction<T> ifNumber, Function<Boolean, T> ifBoolean) {
		return match(cell, ifString, ifNumber, ifBoolean, error -> { throw createError(cell); });
	}
	
	/**
	 * Match the content of a cell, using different type functions.
	 * @param cell the cell. Cannot be NULL.
	 * @param ifString applied if the cell contains a string.
	 * @param ifNumber applied if the cell contains a number.
	 * @param ifBoolean applied if the cell contains a boolean.
	 * @param ifError applied if the cell contains an error value.
	 * @return The resulting value.
	 */
	public static <T> T match(Cell cell, Function<String, T> ifString, DoubleFunction<T> ifNumber, Function<Boolean, T> ifBoolean, IntFunction<T> ifError) {
		Preconditions.checkNotNull(cell, "cell cannot be NULL");
		CellType effectiveType = getEffectiveType(cell);
		
		switch (effectiveType) {
			case BLANK:
			case STRING:
				return ifString.apply(cell.getStringCellValue());
			case NUMERIC:
				return ifNumber.apply(cell.getNumericCellValue());
			case BOOLEAN:
				return ifBoolean.apply(cell.getBooleanCellValue());
			case ERROR:
				return ifError.apply(cell.getErrorCellValue());
			default:
				throw new IllegalStateException("Unknown value type: " + cell.getCellType());
		}
	}
	
	/**
	 * Retrieve the directed of computed value of the given cell as a double.
	 * @param cell the cell.
	 * @param throwError throw an error if the formula failed, or just return NaN.
	 * @return The double 
	 */
	public static double getAsDouble(Cell cell, boolean throwError) {
		return match(cell, 
				Double::parseDouble, 
				(num -> num), 
				(bool -> bool ? 1.0 : 0.0), 
				error -> { if (throwError) throw createError(cell); else return Double.NaN; });
	}
	
	/**
	 * Retrieve the direct of computed value of the given cell as a string.
	 * @param cell the cell.
	 * @param throwError throw formula errors as an exception, otherwise return NULL.
	 * @return The string content.
	 */
	public static String getAsString(Cell cell, boolean throwError) {
		Object value = getValue(cell, throwError);
		return value != null ? value.toString() : null;
	}
	
	/**
	 * Retrieve the direct or computed value of a cell.
	 * @param cell the cell.
	 * @param throwError If TRUE, throw an exception when encountering error fields. Otherwise, return NULL.
	 * @return A corresponding Java type.
	 */
	public static Object getValue(Cell cell) {
		return getValue(cell, true);
	}
	
	/**
	 * Retrieve the type of the value stored in the cell, or the value computed by its current formula.
	 * @param cell the cell.
	 * @return The type of the cell.
	 */
	private static CellType getEffectiveType(Cell cell) {
		CellType cellType = cell.getCellType();
		
		// Handle formula
		if (cellType == CellType.FORMULA) {
			cellType = cell.getCachedFormulaResultType();
		}
		return cellType;
	}
	
	/**
	 * Retrieve the direct or computed value of a cell.
	 * @param cell the cell.
	 * @param throwError If TRUE, throw an exception when encountering error fields. Otherwise, return NULL.
	 * @return A corresponding Java type.
	 */
	public static Object getValue(Cell cell, boolean throwError) {
		Preconditions.checkNotNull(cell, "cell cannot be NULL");
		CellType effectiveType = getEffectiveType(cell);
		
		switch (effectiveType) {
			case BLANK:
			case STRING:
				return cell.getStringCellValue();
			case BOOLEAN:
				return cell.getBooleanCellValue();
			case NUMERIC:
				// Return a date if it is formatted as a date
			    if (DateUtil.isCellDateFormatted(cell)) {
			    	return cell.getDateCellValue();
			    } else {
			    	double value = cell.getNumericCellValue();
			    	
			    	// Return special values directly
			    	if (Double.isInfinite(value) || Double.isNaN(value)) {
			    		return value;
			    	}
			    	// Is this a long?
			    	if (Math.floor(value) == value && value <= Long.MAX_VALUE) {
			    		return (long) value;
			    	}
			    	// Return double
			    	return value;
			    }
			case ERROR:
				if (throwError) {
					throw createError(cell);
				}
				return new ExcelError(cell.getErrorCellValue());	
			default:
				throw new IllegalStateException("Unknown value type: " + cell.getCellType());
		}
	}

	/**
	 * Create a runtime exception based on the error cell value.
	 * @param cell the cell.
	 * @return The runtime exception.
	 */
	private static RuntimeException createError(Cell cell) {
		return new RuntimeException("Cell " + cell + " contains an error: " + cell.getErrorCellValue());
	}
}
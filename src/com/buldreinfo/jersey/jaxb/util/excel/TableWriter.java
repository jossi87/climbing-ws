package com.buldreinfo.jersey.jaxb.util.excel;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a generic table writer.
 * <p>
 * The writer is initially positioned just before the first row. A caller must 
 * thus begin by calling {@link #incrementRow()} before it can write rows to the table.
 */
public interface TableWriter extends AutoCloseable {
	/**
	 * Increment the current row by one.
	 * <p>
	 * This must be invoked before writing any data.
	 */
	public void incrementRow();

	/**
	 * Determine if the table writer currently supports creating new columns.
	 * <p>
	 * This may always be FALSE as column creation is an optional operation.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean canCreateColumns();
	
	/**
	 * Retrieve the number of columns in this table writer.
	 * @return The column count.
	 */
	public int getColumnCount();
	
	/**
	 * Retrieve the name of a given column.
	 * @param columnIndex the column index, from 0 (inclusive) to column count (exclusive).
	 * @return The corresponding name, or NULL if the column could not be found.
	 */
	public String getColumnName(int columnIndex);

	/**
	 * Retrieve the index of the given column, if it exists.
	 * @param columnName name of the column.
	 * @return The corresponding column index, or -1 if not found.
	 */
	public int getColumnIndex(String columnName);
	
	/**
	 * Create a new column by the given name, if it doesn't already exists.
	 * <p>
	 * Note that column creation is an optional operation and may not be supported.
	 * @param columnName the column. Cannot be NULL.
	 * @return The created column index or the existing column index.
	 */
	public int createColumn(String columnName);
	
	/**
	 * Create every column in the given collection.
	 * <p>
	 * Note that column creation is an optional operation and may not be supported.
	 * @param columns the column collection.
	 */
	public default void createColumns(Collection<? extends String> columns) {
		for (String column : columns) {
			createColumn(column);
		}
	}
	
	/**
	 * Write the given cell value at the current row.
	 * </p>
	 * It is up to the implementation on how to handle unexpected column indices. It may 
	 * create new columns eventually when the writer is closed, or throw
	 * an exception.
	 * @param columnName column index.
	 * @param value the value to write.
	 * @return This writer, for chaining.
	 */
	public void write(int columnIndex, Object columnValue);
	
	/**
	 * Write the given cell value at the current row.
	 * </p>
	 * It is up to the implementation on how to handle unexpected column names. It may 
	 * create new columns eventually when the writer is closed, or throw
	 * an exception.
	 * @param columnName column name. Cannot be NULL.
	 * @param value the value to write.
	 * @return This writer, for chaining.
	 */
	public void write(String columnName, Object columnValue);

	/**
	 * Retrieve the current row index, relative to the current starting row.
	 * <p>
	 * Note that the initial row is immediately preceding the start row, so the row index will be -1 
	 * after the writer has been created.
	 * @return Current row index.
	 */
	public int getRowIndex();
	
	/**
	 * Write a single map to the writer. 
	 * <p>
	 * This does not change the current row index.
	 * @param map a map that represents a row.
	 */
	public default <T> void writeMap(Map<String, T> map) {
		for (Entry<String, T> entry : map.entrySet()) {
			write(entry.getKey(), entry.getValue());
		}
	}
	
}
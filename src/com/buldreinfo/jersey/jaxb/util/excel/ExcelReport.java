package com.buldreinfo.jersey.jaxb.util.excel;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.DateFormatConverter;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.buldreinfo.jersey.jaxb.function.SqlConsumer;
import com.buldreinfo.jersey.jaxb.helpers.CellHelper;
import com.buldreinfo.jersey.jaxb.helpers.CellHelper.ExcelError;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * Represents a builder for Excel reports.
 */
public class ExcelReport implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();
	
	/**
	 * Customize the creation of a new sheet writer.
	 */
	public enum SheetCreationOptions {
		/**
		 * Hide the top-most row with all the headers from view.
		 */
		HIDE_HEADER_ROW,
		
		/**
		 * Preserve any existing sheet, and all its rows, and insert the new table the end.
		 */
		PRESERVE_EXISTING
	}
	
	/**
	 * Customize how a sheet is modified.
	 */
	public enum SheetModifyOptions {
		/**
		 * Resize existing columns.
		 */
		RESIZE_EXISTING_COLUMNS,
		
		/**
		 * Ignore any existing column with the same name as a preceding column to the left.
		 */
		IGNORE_EXISTING_DUPLICATE_COLUMNS,
		
		/**
		 * Append new data at the end of the existing sheet.
		 */
		APPEND_DATA,
	}
	
	protected Workbook workbook;
	protected ReportStyle style;
	
	// Used to compute formula values
	private FormulaEvaluator evaluator;
	
	protected int tableCreateId = 1;
	
	/**
	 * Whether or not the Excel report owns the current workbook.
	 */
	private boolean ownsWorkbook;
	
	/**
	 * Sheet writers that are currently writing.
	 */
	protected final Set<SheetWriter> writers = new HashSet<>();
	
	/**
	 * Workers that will be invoked later.
	 */
	protected final List<SqlConsumer<Path>> postProcessors = new ArrayList<>();

	/**
	 * Write the given sheet to a new Excel file.
	 * @param excelFile path to the new Excel file.
	 * @param sheetName the sheet name.
	 * @param sheetContent the sheet content.
	 * @throws IOException If the write failed.
	 */
	public static <T> void writeTo(Path excelFile, String sheetName, Table<Integer, String, T> sheetContent) throws IOException {
		try (ExcelReport report = new ExcelReport()) {
			report.addCustomSheet(sheetName, sheetContent);
			report.writeExcel(excelFile);
		}
	}
	
	/**
	 * Write the given sheets to a new Excel file.
	 * @param excelFile path to the new Excel file.
	 * @param sheets mapping of sheet name and sheet content.
	 * @throws IOException If the write failed.
	 */
	public static void writeTo(Path excelFile, Map<String, Table<Integer, String, String>> sheets) throws IOException {
		try (ExcelReport report = new ExcelReport()) {
			for (Entry<String, Table<Integer, String, String>> entry : sheets.entrySet()) {
				report.addCustomSheet(entry.getKey(), entry.getValue());
			}
			report.writeExcel(excelFile);
		}
	}
		
	/**
	 * Construct a new Excel report.
	 */
	public ExcelReport() {
		this(false);
	}
	
	/**
	 * Construct a new Excel report.
	 * <p>
	 * Set <i>streaming</i> to TRUE in order to reduce the amount of memory used by this report generator. Note that this might
	 * reduce performance.
	 * @param streaming TRUE to use streaming while outputting data, FALSE otherwise.
	 */
	public ExcelReport(boolean streaming) {
		createWorkbook(null, streaming);
		
		if (workbook == null) {
			throw new IllegalStateException("createWorkbook should create a workbook!");
		}
		this.ownsWorkbook = true;
		this.style = createStyle();
	}
	
	/**
	 * Construct a new Excel report that writes to the given workbook.
	 * <p>
	 * The caller is the owner of the workbook and thus responsible for closing it.
	 * @param target the target workbook.
	 */
	public ExcelReport(Workbook target) {
		this(target, false, false);
	}
	
	/**
	 * Construct a new Excel report that writes to the given workbook.
	 * @param target the target workbook.
	 * @param ownsWorkbook TRUE if the excel report owns the workbook instance, FALSE otherwise.
	 * @param streaming TRUE to enable streaming, FALSE otherwise.
	 */
	protected ExcelReport(Workbook target, boolean ownsWorkbook, boolean streaming) {
		createWorkbook(Preconditions.checkNotNull(target, "workbook cannot be NULL"), streaming);
		this.ownsWorkbook = false;
		this.style = createStyle();
	}
	
	/**
	 * Construct a new excel report that uses the given file as a template.
	 * @param templateFile the template file.
	 * @param streaming TRUE to stream the data to the output, FALSE otherwise.
	 * @return The corresponding excel report.
	 */
	public static ExcelReport fromTemplate(Path templateFile, boolean streaming) throws IOException {
		try {
			if (Objects.equal(templateFile.getFileSystem(), FileSystems.getDefault())) {
				return new ExcelReport(WorkbookFactory.create(templateFile.toFile()), true, streaming);
			} else {
				try (InputStream input = Files.newInputStream(templateFile)) {
					Workbook workbook = WorkbookFactory.create(input);
					return new ExcelReport(workbook);
				}
			}
		} catch (EncryptedDocumentException e) {
			throw new IOException("Unable to load workbook.", e);
		}
	}
	
	/**
	 * Invoked when we are ready to create the workbook.
	 * @param workbook any existing workbook given by the constructor.
	 * @param streaming TRUE if we should create a streaming workbook.
	 */
	protected Workbook createWorkbook(Workbook existing, boolean streaming) {
		if (existing != null) {
			return workbook = existing;
		} else {
			return workbook = streaming ? new SXSSFWorkbook() : new XSSFWorkbook();
		}
	}
	
	/**
	 * Create shared styles.
	 * </p>
	 * This is invoked after we have created the workbook.
	 */
	protected ReportStyle createStyle() {
		CellStyle headerStyle = createHeaderStyle(workbook);
		CellStyle nullStyle = createBackgroundColorStyle(workbook, new Color(234, 234, 255));
		CellStyle timestampStyle = createTimestampStyle(workbook);
		CellStyle dateStyle = createDateStyle(workbook);
		CellStyle timeStyle = createTimeStyle(workbook);
		CellStyle hyperlinkStyle = createHyperlinkStyle(workbook);
		return new ReportStyle(workbook, headerStyle, null, null, timestampStyle, dateStyle, timeStyle, nullStyle, hyperlinkStyle);
	}

	/**
	 * Invalidate the cache used to compute formula results.
	 * <p>
	 * This must be called if cells have been modified outside of SheetWriter.
	 */
	public void invalidateFormulaCache() {
		if (evaluator != null) {
			evaluator.clearAllCachedResultValues();
		}
	}
	
	/**
	 * Retrieve or create the current formula evaluator.
	 * @return The evaluator.
	 */
	protected FormulaEvaluator getOrCreateEvaluator() {
		// Disable formula evaluation in streaming workbooks
		if (isStreaming()) {
			return null;
		}
		if (evaluator == null) {
			evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		}
		return evaluator;
	}
	
	/**
	 * Determine if the current report is streaming the current content.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isStreaming() {
		return workbook instanceof SXSSFWorkbook;
	}

	/**
	 * Retrieve the current report style.
	 * @return Current default style.
	 */
	public ReportStyle getStyle() {
		return style;
	}
	
	/**
	 * Add a new custom sheet to the output Excel file.
	 * @param sheetName the sheet name.
	 * @param cells the cells to add.
	 * @return This report, for chaining.
	 */
	public <T> ExcelReport addCustomSheet(String sheetName, Table<Integer, String, T> cells) {
		// Map must be ordered	
		try (SheetWriter writer = addSheet(sheetName)) {
			writer.writeCells(cells);
		}
		return this;
	}
	
	/**
	 * Write the current report to an Excel file.
	 * <p>
	 * Any open sheet writers will be automatically closed in order to write the report to file.
	 * @param file the file.
	 * @throws IOException If anything went wrong.
	 */
	public void writeExcel(Path file) throws IOException {
		closeWriters();
		
		try (OutputStream output = Files.newOutputStream(file)) {
			workbook.write(output);
		}
		invokePostProcessors(file);
	}
	
	/**
	 * Write the current Excel report to an output stream.
	 * <p>
	 * Any open sheet writers will be automatically closed in order to write the report to file.
	 * @param stream the output stream.
	 * @throws IOException If anything went wrong.
	 */
	public void writeExcel(OutputStream stream) throws IOException {
		closeWriters();
		
		// Can we write directly to the stream?
		if (postProcessors.size() == 0) {
			workbook.write(stream);
			return;
		}
		// Otherwise, we have processors that assume a file in the file system - create that temporary file
		Path temporary = Files.createTempFile("report", workbook instanceof XSSFWorkbook ? "xlsx" : "xlx");
		
		// Use temporary file, and then write to the output stream
		writeExcel(temporary);
		Files.copy(temporary, stream);
		Files.deleteIfExists(temporary);
	}
	
	/**
	 * Close all pending writers.
	 */
	private void closeWriters() {
		if (writers.size() > 0) {
			for (SheetWriter writer : writers.toArray(new SheetWriter[0])) {
				writer.close();
			}
		}
	}
	
	/**
	 * Invoke all post-processors.
	 * @param file the file.
	 */
	protected void invokePostProcessors(Path file) throws IOException {
		if (postProcessors.size() == 0) {
			return;
		}
		Exception sibling = null;
		Stopwatch watch = Stopwatch.createStarted();
		
		try {
			for (SqlConsumer<Path> consumer : postProcessors) {
				try {
					logger.info("Invoking post-processor " + consumer);
					consumer.apply(file);
				} catch (Exception e) {
					// Let the next processor execute
					sibling = addNewSibling(sibling, e);
				}
			}
			watch.stop();
			logger.info("Post-processors elapsed: " + watch);
			
		} catch (Exception e) {
			sibling = addNewSibling(sibling, e);
		} finally {
			// Throw exception?
			if (sibling != null) {
				Throwables.propagateIfPossible(sibling, IOException.class);
				throw new RuntimeException(sibling);
			}
		}
	}
	
	/**
	 * Add the existing siblings to the new exception.
	 * @param existingSiblings existing siblings, or NULL for none.
	 * @param newSibling the new exception.
	 * @return The new exception.
	 */
	protected Exception addNewSibling(Exception existingSiblings, Exception newException) {
		if (existingSiblings != null) {
			newException.addSuppressed(existingSiblings);
		}
		return newException;
	}
	
	/**
	 * Create a default value supplier based on the given row.
	 * <p>
	 * The value supplier will use the cells in the given row to supply column values where nothing has been written by a SheetWriter.
	 * @param sheetName the name of the sheet that contains the row.
	 * @param rowIndex index of the row.
	 * @return The default value supplier.
	 */
	public DefaultValueSupplier createValueSupplier(String sheetName, int rowIndex) {
		Sheet sheet = workbook.getSheet(sheetName);
		
		if (sheet == null) {
			throw new IllegalArgumentException("Unable to find sheet " + sheetName);
		}
		return RowValueSupplier.fromRow(sheet.getRow(rowIndex));
	}
	
	/**
	 * Create a default value supplier based on the given row.
	 * <p>
	 * The value supplier will use the cells in the given row to supply column values where nothing has been written by a SheetWriter.
	 * @param sheetIndex index of the sheet that contains the row.
	 * @param rowIndex index of the row.
	 * @return The default value supplier.
	 */
	public DefaultValueSupplier createValueSupplier(int sheetIndex, int rowIndex) {
		return RowValueSupplier.fromRow(workbook.getSheetAt(sheetIndex).getRow(rowIndex));
	}

	/**
	 * Modify an existing sheet with headers and existing data.
	 * <p>
	 * To insert a new table under an existing table, use {@link #addSheet(String, SheetCreationOptions...)} with {@link SheetCreationOptions#PRESERVE_EXISTING}.
	 * @param sheetName the name of the sheet we are modifying.
	 * @param headerRow the index of the row (from 0) that will or may contain headers. 
	 * @param defaultValueSupplier a default value supplier for columns that haven't been written, or NULL to just default to empty strings.
	 * @param options additional modification options.
	 * @return A sheet writer. 
	 */
	public SheetWriter modifySheet(String sheetName, int headerRow, DefaultValueSupplier defaultValueSupplier, SheetModifyOptions... options) {
		Sheet sheet = workbook.getSheet(sheetName);
		
		if (sheet == null) {
			throw new IllegalArgumentException("Unable to find sheet " + sheetName);
		}
		return modifySheet(sheet, headerRow, defaultValueSupplier, options);
	}
	
	/**
	 * Modify an existing sheet with headers and existing data.
	 * <p>
	 * To insert a new table under an existing table, use {@link #addSheet(String, SheetCreationOptions...)} with {@link SheetCreationOptions#PRESERVE_EXISTING}.
	 * @param sheetIndex the index of the sheet we are modifying.
	 * @param headerRow the index of the row (from 0) that will or may contain headers. 
	 * @param defaultValueSupplier a default value supplier for columns that haven't been written, or NULL to just default to empty strings.
	 * @param options additional modification options.
	 * @return A sheet writer. 
	 */
	public SheetWriter modifySheet(int sheetIndex, int headerRow, DefaultValueSupplier defaultValueSupplier, SheetModifyOptions... options) {
		return modifySheet(workbook.getSheetAt(sheetIndex), headerRow, defaultValueSupplier, options);
	}
	
	/**
	 * Modify an existing sheet with headers and existing data.
	 * <p>
	 * To insert a new table under an existing table, use {@link #addSheet(String, SheetCreationOptions...)} with {@link SheetCreationOptions#PRESERVE_EXISTING}.
	 * @param sheet the sheet we are modifying.
	 * @param headerRow the index of the row (from 0) that will or may contain headers. 
	 * @param defaultValueSupplier a default value supplier for columns that haven't been written, or NULL to just default to empty strings.
	 * @param options additional modification options.
	 * @return A sheet writer. 
	 */
	protected SheetWriter modifySheet(Sheet sheet, int headerRow, DefaultValueSupplier defaultValueSupplier, SheetModifyOptions... options) {
		// Creation options
		Set<SheetModifyOptions> lookup = options != null ? 
			Sets.newEnumSet(Arrays.asList(options), SheetModifyOptions.class) : 
			EnumSet.noneOf(SheetModifyOptions.class);
		
		SheetWriter writer = new SheetWriter(sheet, headerRow, 
				lookup.contains(SheetModifyOptions.APPEND_DATA) ? sheet.getLastRowNum() + 1 : headerRow + 1, 0, true, false, 
				defaultValueSupplier, 
				EnumSet.of(SheetCreationOptions.PRESERVE_EXISTING));
		
		// Ensure existing headers are taken into account
		writer.readHeaders(lookup.contains(SheetModifyOptions.RESIZE_EXISTING_COLUMNS), 
						   lookup.contains(SheetModifyOptions.IGNORE_EXISTING_DUPLICATE_COLUMNS));
		return writer;
	}
	
	/**
	 * Create a new sheet in the worksheet, and return a writer that will be used to fill said sheet.
	 * <p>
	 * Remember to call {@link SheetWriter#incrementRow()} before using the writer.
	 * <p>
	 * It may be necessary to call {@link SheetWriter#createColumns(Collection)} with every column name in use if the current
	 * report generator is streaming, as it may not be possible to create these columns in the middle of the output.
	 * <p>
	 * To insert a new table into an existing sheet at the end, supply {@link SheetCreationOptions#PRESERVE_EXISTING}. 
	 * @param sheetName name of the sheet.
	 * @param options sheet writer options.
	 * @return A sheet writer.
	 */
	public SheetWriter addSheet(String sheetName, SheetCreationOptions... options) {
		return addSheet(sheetName, true, true, (DefaultValueSupplier)null, options);
	}
	
	/**
	 * Create a new sheet in the worksheet, and return a writer that will be used to fill said sheet.
	 * <p>
	 * To insert a new table into an existing sheet at the end, supply {@link SheetCreationOptions#PRESERVE_EXISTING}. 
	 * @param sheetName name of the sheet.
	 * @param autoSizeColumns TRUE if we should automatically resize columns, FALSE otherwise.
	 * @param filterFirstRow TRUE if we should automatically add filter on header row, FALSE otherwise.
	 * @param options sheet writer options.
	 * @return A sheet writer.
	 */
	public SheetWriter addSheet(String sheetName, boolean autoSizeColumns, boolean filterFirstRow, SheetCreationOptions... options) {
		return addSheet(sheetName, autoSizeColumns, filterFirstRow, (DefaultValueSupplier)null, options);
	}
	
	/**
	 * Create a new sheet in the worksheet, and return a writer that will be used to fill said sheet.
	 * <p>
	 * To insert a new table into an existing sheet at the end, supply {@link SheetCreationOptions#PRESERVE_EXISTING}. 
	 * @param sheetName name of the sheet.
	 * @param autoSizeColumns TRUE if we should automatically resize columns, FALSE otherwise.
	 * @param filterFirstRow TRUE if we should automatically add filter on header row, FALSE otherwise.
	 * @param defaultValueSupplier a default value supplier for columns that haven't been written, or NULL to just default to empty strings.
	 * @param options sheet writer options.
	 * @return A sheet writer.
	 */
	public SheetWriter addSheet(String sheetName, boolean autoSizeColumns, boolean filterFirstRow, DefaultValueSupplier defaultValueSupplier, SheetCreationOptions... options) {		
		return addSheet(sheetName, autoSizeColumns, filterFirstRow, defaultValueSupplier, 0, 1, options);
	}
	
	/**
	 * Create a new sheet in the worksheet, and return a writer that will be used to fill said sheet.
	 * <p>
	 * To insert a new table into an existing sheet at the end, supply {@link SheetCreationOptions#PRESERVE_EXISTING}. 
	 * @param sheetName name of the sheet.
	 * @param autoSizeColumns TRUE if we should automatically resize columns, FALSE otherwise.
	 * @param filterFirstRow TRUE if we should automatically add filter on header row, FALSE otherwise.
	 * @param defaultValueSupplier a default value supplier for columns that haven't been written, or NULL to just default to empty strings.
	 * @param offsetFirst the starting row offset of this table, if this table is first in the sheet. This is relative to the first row in the sheet.
	 * @param offsetNext the starting row offset of this table, if this table is next after an existing table in the sheet. This is relative to the first row AFTER the previous table.
	 * @param options sheet writer options.
	 * @return A sheet writer.
	 */
	public SheetWriter addSheet(String sheetName, boolean autoSizeColumns, boolean filterFirstRow, DefaultValueSupplier defaultValueSupplier, int offsetFirst, int offsetNext, SheetCreationOptions... options) {		
		// Creation options
		Set<SheetCreationOptions> lookup = options != null ? 
			Sets.newEnumSet(Arrays.asList(options), SheetCreationOptions.class) : 
			EnumSet.noneOf(SheetCreationOptions.class);
		
		Sheet sheet = null;
		int startingRow = offsetFirst;
		
		// Preserve existing sheet
		if (lookup.contains(SheetCreationOptions.PRESERVE_EXISTING)) {
			sheet = workbook.getSheet(sheetName);
		
			// Set the starting row
			if (sheet != null) {
				startingRow = sheet.getLastRowNum() + 1 + offsetNext;
			}
		}
		
		// Create sheet if PRESERVE_EXISTING is missing or the sheet was not found
		if (sheet == null) {
			try {
				sheet = workbook.createSheet(sheetName);
			} catch (IllegalArgumentException e) {
				if (workbook.getSheet(sheetName) != null) {
					throw new IllegalArgumentException("A sheet named '" + sheetName + "' already exists - "
							+ "please use SheetCreationOptions.PRERSERVE_EXISTING to append to the end instead.");
					}
				throw e;
			}
		}
		return new SheetWriter(sheet, 
				startingRow, 
				startingRow + (!lookup.contains(SheetCreationOptions.HIDE_HEADER_ROW) ? 1 : 0), 0, 
				autoSizeColumns, filterFirstRow, defaultValueSupplier, lookup);
	}

	/**
	 * Invoked when a sheet writer has been created in this report.
	 * @param sheetWriter the writer.
	 */
	protected void onSheetWriterCreated(SheetWriter sheetWriter) {
		writers.add(sheetWriter);
	}
	
	/**
	 * Invoked when a sheet writer created in this report has been closed.
	 * @param sheetWriter the writer that has been closed.
	 */
	protected void onSheetWriterClosed(SheetWriter sheetWriter) {
		writers.remove(sheetWriter);
	}
	
	/**
	 * Dynamically set the cell value, depending on the type of the input.
	 * @param row the row.
	 * @param index the cell index.
	 * @param style the cell style.
	 * @param value the cell value.
	 * @return The cell that was updated.
	 */
	protected Cell setCellValue(Row row, int index, CellStyle style, Object value) {
		return setCellValue(row.createCell(index), style, value);
	}
	
	/**
	 * Dynamically set the cell value, depending on the type of the input.
	 * @param cell the cell.
	 * @param style report style.
	 * @param value the cell value.
	 * @return The cell that was updated.
	 */
	protected Cell setCellValue(Cell cell, CellStyle style, Object value) {
		// No need to create an evaluator eagerly, and then invalidate its content.
		boolean invalidateCache = evaluator != null;
		
		// Update style
		cell.setCellStyle(style);
		
		// Update value
		if (value instanceof Date) {
			cell.setCellValue((Date) value);
		} else if (value instanceof TemporalAccessor) {
			Calendar calendar = getCalendar((TemporalAccessor) value);
			cell.setCellValue(calendar);
		} else if (value instanceof Calendar) {
			cell.setCellValue((Calendar) value);
		} else if (value instanceof Number) {
			cell.setCellValue(((Number) value).doubleValue());
		} else if (value instanceof SheetEntity) {
			((SheetEntity) value).writeTo(cell, evaluator);
			invalidateCache = false;
		} else if (value instanceof Hyperlink) {
			cell.setHyperlink((Hyperlink) value);
		} else if (value != null) {
			cell.setCellValue(value.toString());
		} else {
			cell.setCellValue("");
		}

		if (invalidateCache) {
			evaluator.notifyUpdateCell(cell);
		}
		return cell;
	}
	
	/**
     * Retrieve a calendar from a temporal accessor.
     * @param accessor the accessor.
     * @return The corresponding calendar.
     */
    private static Calendar getCalendar(TemporalAccessor accessor) {
        Instant instant = Instant.from(accessor);
        ZoneId zone = accessor.query(TemporalQueries.zone());

        Calendar calendar = zone != null ?
                Calendar.getInstance(TimeZone.getTimeZone(zone)) :
                Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(instant.toEpochMilli());
        return calendar;
    }
	
	/**
	 * Create the cell style for headers.
	 * @param book current workbook.
	 * @return Cell style.
	 */
	protected CellStyle createHeaderStyle(Workbook book) {
		Font font = book.createFont();
		CellStyle style = book.createCellStyle();
		font.setBold(true);
		style.setFont(font);
		return style;
	}
	
	/**
	 * Create the style for a timestamp.
	 * @param book the workbook.
	 * @return Corresponding style.
	 */
	protected CellStyle createTimestampStyle(Workbook book) {
		CellStyle style = book.createCellStyle();
		DataFormat poiFormat = book.createDataFormat();
		String excelFormatPattern = DateFormatConverter.convert(Locale.ENGLISH, "yyyy.MM.dd HH:mm:ss");
		
		style.setDataFormat(poiFormat.getFormat(excelFormatPattern));
		return style;
	}
	
	/**
	 * Create the style for a date.
	 * @param book the workbook.
	 * @return Corresponding style.
	 */
	protected CellStyle createDateStyle(Workbook book) {
		CellStyle style = book.createCellStyle();
		DataFormat poiFormat = book.createDataFormat();
		String excelFormatPattern = DateFormatConverter.convert(Locale.ENGLISH, "dd-MM-yyyy");
		
		style.setDataFormat(poiFormat.getFormat(excelFormatPattern));
		return style;
	}
	
	/**
	 * Create the style for a time cell.
	 * @param book the workbook.
	 * @return Corresponding style.
	 */
	protected CellStyle createTimeStyle(Workbook book) {
		CellStyle style = book.createCellStyle();
		DataFormat poiFormat = book.createDataFormat();
		String excelFormatPattern = DateFormatConverter.convert(Locale.ENGLISH, "HH:mm:ss");
		
		style.setDataFormat(poiFormat.getFormat(excelFormatPattern));
		return style;
	}
	
	/**
	 * Create the style for a hyperlink cell.
	 * @param book the workbook.
	 * @return Corresponding style.
	 */
	protected CellStyle createHyperlinkStyle(Workbook book) {
        CellStyle hyperlinkStyle = book.createCellStyle();
        Font hyperlinkFont = book.createFont();
        
        hyperlinkFont.setUnderline(Font.U_SINGLE);
        hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
        hyperlinkStyle.setFont(hyperlinkFont);
		
		return hyperlinkStyle;
	}
	
	/**
	 * Create a new cell style with a given background color.
	 * @param book the workbook.
	 * @param backgroundColor the background color.
	 * @return The cell style.
	 */
	protected static CellStyle createBackgroundColorStyle(Workbook book, Color backgroundColor) {
		return createBackgroundColorStyle(book, backgroundColor, null);
	}
	
	/**
	 * Create a new cell style with a given background color.
	 * @param book the workbook.
	 * @param backgroundColor the background color.
	 * @param template optional template.
	 * @return The cell style.
	 */
	protected static CellStyle createBackgroundColorStyle(Workbook book, Color backgroundColor, CellStyle template) {
		XSSFCellStyle style = (XSSFCellStyle) book.createCellStyle();
		XSSFColor color = new XSSFColor(backgroundColor);
		
		if (template != null) {
			style.cloneStyleFrom(template);
		}
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setFillForegroundColor(color);
		return style;
	}
	
	/**
	 * Create a new cell style with a given border color.
	 * @param book the workbook.
	 * @param borderStyle the border style.
	 * @param borderColor the border color.
	 * @param template optional template.
	 * @return The cell style.
	 */
	protected static CellStyle createBorderColorStyle(Workbook book, BorderStyle borderStyle, Color borderColor, CellStyle template) {
		XSSFCellStyle style = (XSSFCellStyle) book.createCellStyle();
		XSSFColor color = new XSSFColor(borderColor);
		
		if (template != null) {
			style.cloneStyleFrom(template);
		}
		style.setBorderLeft(borderStyle);
		style.setBorderRight(borderStyle);
		style.setBorderTop(borderStyle);
		style.setBorderBottom(borderStyle);
		style.setLeftBorderColor(color);
		style.setRightBorderColor(color);
		style.setTopBorderColor(color);
		style.setBottomBorderColor(color);
		return style;
	}
	
	/**
	 * Retrieve a copy of the given style, but with the new font.
	 * @param book the parent book. Cannot be NULL.
	 * @param template the template style. Can be NULL.
	 * @param newFont the font to set in the style. Can be NULL.
	 * @return A new style similar to the template, but with a new font.
	 */
	protected static CellStyle createStyleWithFont(Workbook book, CellStyle template, Font newFont) {
		Preconditions.checkNotNull(book, "book cannot be NULL");
		
		// Safely retrieve the current font
		Font currentFont = template != null && template.getFontIndexAsInt() >= 0 ? 
				book.getFontAt(template.getFontIndexAsInt()) : null;
		
		// See if we actually need to change the style
		if (Objects.equal(currentFont, newFont)) {
			return template;
		}
		CellStyle copy = book.createCellStyle();
		
		if (template != null) {
			copy.cloneStyleFrom(template);
		}
		copy.setFont(newFont);
		return copy;
	}
	
	@Override
	public void close() throws IOException {
		if (ownsWorkbook) {
			workbook.close();
		}
	}
	
	/**
	 * A sheet formula that is not validated.
	 */
	public static class SheetFormula implements SheetEntity {
		private final String formula;
		
		/**
		 * Construct a sheet formula.
		 * @param formula the formula.
		 */
		protected SheetFormula(String formula) {
			this.formula = Preconditions.checkNotNull(formula, "formula cannot be NULL");
		}

		/**
		 * Retrieve a sheet formula using the given formula.
		 * @param formula the formula.
		 * @return The corresponding sheet formula.
		 */
		public static SheetFormula fromFormula(String formula) {
			// Performance can be improved by parsing formulas, and implementing format in terms of a parser
			return new SheetFormula(formula);
		}
		
		/**
		 * Retrieve a sheet formula from the given formula format.
		 * <p>
		 * This uses a syntax similar to {@link String#format(String, Object...)}.
		 * @param format the formula format. Cannot be NULL.
		 * @param args formulas that will be substituted in the format string.
		 * @return Corresponding sheet formula.
		 */
		public static SheetFormula fromFormat(String format, SheetFormula... args) {
			// Performance can be improved by parsing formulas, and implementing format in terms of a parser
			if (args == null || args.length == 0) {
				return new SheetFormula(format);
			} else {
				return new SheetFormula(String.format(
					format, 
					Arrays.stream(args).map(SheetFormula::getFormula).toArray(Object[]::new)
				));
			}
		}
		
		/**
		 * Retrieve a formula that references the cell that the writer will use to write to the given column.
		 * @param writer the writer.
		 * @param columnName name of the given column.
		 * @return A formula statically referencing the cell.
		 */
		public static SheetFormula fromReference(SheetWriter writer, String columnName) {
			return new SheetFormula(
				new CellReference(writer.getAbsoluteRowIndex(), writer.getColumnIndex(columnName)).formatAsString()
			);
		}
		
		/**
		 * Retrieve the underlying formula.
		 * @return The formula.
		 */
		public String getFormula() {
			return formula;
		}
		
		@Override
		public int hashCode() {
			return formula.hashCode();
		}

		@Override
		public CellStyle getDefaultStyle(ReportStyle reportStyle) {
			// May need to be customisable
			return reportStyle.getTextStyle();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof SheetFormula) {
				SheetFormula other = (SheetFormula) obj;
				return Objects.equal(formula, other.getFormula());
			}
			return false;
		}
		
		@Override
		public String toString() {
			return formula;
		}

		@Override
		public void writeTo(Cell cell, FormulaEvaluator evaluator) {
			cell.setCellFormula(formula);
			
			if (evaluator != null) {
				evaluator.notifySetFormula(cell);
			}
		}
	}
	
	/**
	 * Represents a hyperlink in a sheet.
	 */
	public static class SheetHyperlink implements SheetEntity {
		private final String address;
		private final String label;
		
		/**
		 * Construct a new sheet hyperlink.
		 * @param address the address. Cannot be NULL.
		 * @param label the text to display. Cannot be NULL.
		 */
		protected SheetHyperlink(String address, String label) {
			this.address = Preconditions.checkNotNull(address, "address cannot be NULL");
			this.label = Preconditions.checkNotNull(label, "label cannot be NULL");
		}
		
		/**
		 * Construct a new sheet hyperlink that displays the address as text.
		 * @param address the address. Cannot be NULL.
		 */
		public static SheetHyperlink of(String address) {
			return new SheetHyperlink(address, address);
		}
		
		/**
		 * Construct a new sheet hyperlink.
		 * @param address the address. Cannot be NULL.
		 * @param label the text to display. Cannot be NULL.
		 */
		public static SheetHyperlink of(String address, String label) {
			return new SheetHyperlink(address, label);
		}

		/**
		 * Retrieve the address associated with this hyperlink.
		 * @return The address.
		 */
		public String getAddress() {
			return address;
		}

		/**
		 * Retrieve the label associated with this hyperlink.
		 * @return The text label.
		 */
		public String getLabel() {
			return label;
		}

		@Override
		public CellStyle getDefaultStyle(ReportStyle reportStyle) {
			return reportStyle.getHyperlinkStyle();
		}
		
		@Override
		public boolean equals(final Object other) {
			if (!(other instanceof SheetHyperlink)) {
				return false;
			}
			SheetHyperlink castOther = (SheetHyperlink) other;
			return Objects.equal(address, castOther.address) && 
				   Objects.equal(label, castOther.label);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(address, label);
		}

		@Override
		public String toString() {
			return "SheetHyperlink [address=" + address + ", label=" + label + "]";
		}

		@Override
		public void writeTo(Cell cell, FormulaEvaluator evaluator) {
			Workbook wb = cell.getSheet().getWorkbook();
			Hyperlink link = wb.getCreationHelper().createHyperlink(address.startsWith("http")? HyperlinkType.URL : HyperlinkType.FILE);
			
			link.setAddress(address);
			cell.setCellValue(label);
			cell.setHyperlink(link);
			
			if (evaluator != null) {
				evaluator.notifyUpdateCell(cell);
			}
		}
	}
	
	/**
	 * Represents a default column value supplier.
	 */
	public interface DefaultValueSupplier {
		/**
		 * Invoked when a column must be supplied with its default value.
		 * @param writer the writer.
		 * @param columnIndex the column index.
		 * @param columnName the column value.
		 */
		public void onWriteDefault(SheetWriter writer, int columnIndex, String columnName);
	}
	
	/**
	 * Represents an entity that can be written to a cell in a sheet.
	 */
	public interface SheetEntity {
		/**
		 * Write the current entity to an Apache POI cell.
		 * @param cell the cell.
		 * @param reportStyle the current report style that must be 
		 * @param evaluator an evaluator that should be updated if the cell value changes. May be NULL.
		 */
		public void writeTo(Cell cell, FormulaEvaluator evaluator);

		/**
		 * Retrieve the default cell style associated with sheet entities of this type.
		 * @param reportStyle the report style.
		 * @return The default cell style.
		 */
		public CellStyle getDefaultStyle(ReportStyle reportStyle);

	}
	
	/**
	 * Represents a sheet writer. 
	 * <p>
	 * A writer always starts before the first row, so it is necessary to call {@link #incrementRow()} before writing anything.
	 * <p>
	 * Note that this is not thread safe.
	 */
	public class SheetWriter implements TableWriter, AutoCloseable, Closeable {
		/**
		 * Index of columns that are suppressed.
		 */
		public static final int SUPPRESSED_COLUMN = Integer.MAX_VALUE;
		
		// Target sheet
		private final Sheet sheet;
		
		// Current headers
		private final Row headerRow;
		private final BiMap<String, Integer> headerLookup = HashBiMap.create();
		private final Map<Integer, ColumnSizeTracker> sizeTrackers = Maps.newHashMap();
		
		// Columns that have been written to the output
		private final BitSet writtenColumns = new BitSet();
		
		// Columns that will not be written to the Excel file
		private Set<String> suppressedColumns;
		private Set<String> hiddenColumns;
		
		// Row indexes
		private final int startingRowIndex;
		private final int startingColumnIndex;
		private final int headerRowIndex;
		
		// Current index
		private int columnIndex;
		private int rowIndex;
		
		// Current row
		private Row currentRow;
		
		// Whether or not to create columns
		private boolean lockColumns;
		private boolean closed;
		
		// TRUE to set (or replace) header row as frozen, FALSE to only remove existing frozen header
		private boolean freezeHeaderRow;
		
		// Index of the first column to auto size
		private int indexAutoSizeColumns;
		
		// TRUE if we should automatically add filter on header row
		private boolean filterFirstRow;
		
		// Order of the sheet after it has been created
		private int sheetOrder = -1;
		
		// A supplier of default vales
		private final DefaultValueSupplier defaultValueSupplier;
		
		// Options passed during sheet writer creation
		private final Set<SheetCreationOptions> creationOptions;

		/**
		 * Construct a new sheet writer.
		 * @param sheet the sheet.
		 * @param autoSizeColumns TRUE to automatically fit the width of every column to the largest content.
		 * @param filterFirstRow TRUE if we should automatically add filter on header row, FALSE otherwise.
		 */
		protected SheetWriter(Sheet sheet, boolean autoSizeColumns, boolean filterFirstRow) {
			this(sheet, 0, 1, 0, autoSizeColumns, filterFirstRow, null, EnumSet.noneOf(SheetCreationOptions.class));
		}
		
		/**
		 * Construct a new sheet writer.
		 * @param sheet the sheet.
		 * @param headerRowIndex row index of the header.
		 * @param startingRow index of the first data row. The writer will initially start at the previous row.
		 * @param startingColumn index of the next available column.
		 * @param autoSizeColumns TRUE to automatically fit the width of every column to the largest content.
		 * @param filterFirstRow TRUE if we should automatically add filter on header row, FALSE otherwise.
		 * @param options a set of creation options. Cannot be NULL.
		 */
		protected SheetWriter(
					Sheet sheet, int headerRowIndex, int startingRow, int startingColumn, 
					boolean autoSizeColumns, boolean filterFirstRow, DefaultValueSupplier defaultValueSupplier, Set<SheetCreationOptions> creationOptions) {
			this.sheet = sheet;
			this.rowIndex = startingRow - 1;
			this.startingRowIndex = startingRow;
			this.startingColumnIndex = startingColumn;
			this.columnIndex = startingColumn;
			this.headerRow = getOrCreateRow(headerRowIndex);
			this.headerRowIndex = headerRowIndex;
			this.indexAutoSizeColumns = autoSizeColumns ? 0 : Integer.MAX_VALUE;
			this.filterFirstRow = filterFirstRow;
			this.defaultValueSupplier = defaultValueSupplier;
			this.freezeHeaderRow = !creationOptions.contains(SheetCreationOptions.PRESERVE_EXISTING);
			this.creationOptions = Preconditions.checkNotNull(creationOptions, "creationOptions cannot be NULL");
			
			onSheetWriterCreated(this);
		}
		
		/**
		 * Read any existing headers in the current header row.
		 * @param autoSizeExistingColumns TRUE to resize existing columns if resizing is enabled, FALSE otherwise.
		 * @param ignoreExistingDupColumns TRUE to ignore existing duplicate columns, FALSE to throw an exception in such cases.
		 */
		protected void readHeaders(boolean autoSizeExistingColumns, boolean ignoreExistingDupColumns) {
			Row headers = sheet.getRow(headerRowIndex);
			
			if (headers != null) {
				// Last cell is exclusive
				for (int i = headers.getFirstCellNum(); i < headers.getLastCellNum(); i++) {
					Cell cell = headers.getCell(i);
					Object value = cell != null ? CellHelper.getValue(cell, false) : null;
					
					// Skip errors
					if (value instanceof ExcelError || value == null) {
						continue;
					}
					String headerName = value.toString();
					
					if (autoSizeExistingColumns) {
						onCellWritten(i, cell, cell.getCellStyle(), headerName);
					}
					if (Strings.isNullOrEmpty(headerName)) {
						continue;
					}
					// Ensure column is known
					columnIndex = i + 1;
					
					if (headerLookup.containsKey(headerName)) {
						// Throw exception by default, and let the calleer figure out how to handle duplicate column names.
						if (!ignoreExistingDupColumns) {
							throw new IllegalArgumentException("Encountered duplicate column " + headerName + " at " + i);
						}
					} else {
						updateColumnIndex(headerName, i);
					}
				} 
			}
			if (!autoSizeExistingColumns) {
				indexAutoSizeColumns = Math.max(columnIndex, indexAutoSizeColumns);
			}
		}
		
		/**
		 * Clear the content of all remaining data rows.
		 */
		public void clearDataRows() {
			int lastRowNum = sheet.getLastRowNum();
			int firstRowNum = Math.max(startingRowIndex, rowIndex);
			
			for (int i = lastRowNum; i >= firstRowNum; i--) {
				sheet.removeRow(sheet.getRow(i));
			}
		}
				
		/**
		 * Write the cells of an entire table to this writer.
		 * <p>
		 * The table will start at the current index of the writer.
		 * @param table the source table.
		 */
		public <T> void writeCells(Table<Integer, String, T> table) {
			writeCells(table, this::writeMap);
		}
		
		/**
		 * Write a single map to the writer. 
		 * <p>
		 * This does not change the current row index.
		 * @param map a map that represents a row.
		 */
		@Override
		public <T> void writeMap(Map<String, T> map) {
			for (Entry<String, T> cell : map.entrySet()) {
				write(cell.getKey(), style, cell.getValue());
			}
		}
		
		/**
		 * Write the cells of an entire table to this writer.
		 * <p>
		 * The table will start at the current row of the writer. 
		 * <p>
		 * Note that the rowWriter should not increment the current row index.
		 * @param table the source table.
		 * @param a custom writer of rows. 
		 */
		public <T> void writeCells(Table<Integer, String, T> table, Consumer<Map<String, T>> rowWriter) {
			if (table.size() > 0) {
				// This may not actually require any sorting if the backing map is sorted by rows
				int[] sortedRows = table.rowKeySet().stream().
						mapToInt(x -> x).
						sorted().
						toArray();
				
				// Initial source row
				int sourceStart = sortedRows[0];
				// Last written relative source row (writer starts at -1)
				int lastRelativeRow = -1; 
				
				// Sheet writer might be backing a streaming worksheet, so we have to write the rows in order
				for (int row : sortedRows) {
					// An iterator might be faster, but we cannot guarantee the table is sorted
					Map<String, T> entries = table.row(row);
					int sourceRelative = row - sourceStart;
					int delta = sourceRelative - lastRelativeRow;
					
					// Check for impossible negative delta
					if (delta < 0) {
						throw new IllegalStateException("Unexpected delta: " + delta);
					}
					incrementRow(delta);
					rowWriter.accept(entries);
					
					// In order to support delta
					lastRelativeRow = sourceRelative;
				}
			}
		}

		/**
		 * Determine if the writer is positioned at the initial row, which is the row immediately preceding the starting row.
		 * <p>
		 * Any attempt to write 
		 * @return TRUE if it is, FALSE otherwise.
		 */
		public boolean atStart() {
			return rowIndex <= startingRowIndex - 1;
		}
		
		/**
		 * Determine if column creation has been locked.
		 * @return TRUE if it has, FALSE otherwise.
		 */
		public boolean isLockColumns() {
			return lockColumns;
		}
		
		/**
		 * Retrieve the order of appearance (0 based) of the sheet in the workbook.
		 * <p>
		 * A negative order is interpreted as <code>numberOfSheetsAfterInsertion - Abs(sheetOrder)</code>.
		 * @return The sheet order. Default is -1, which places the sheet at the end.
		 */
		public int getSheetOrder() {
			return sheetOrder;
		}
		
		/**
		 * Set the order of appearance (0 based) of the sheet in the workbook.
		 * <p>
		 * A negative order is interpreted as <code>numberOfSheetsAfterInsertion - Abs(sheetOrder)</code>.
		 * @param sheetOrder the sheet order. Negative orders are placed at the end of the sheet list.
		 */
		public void setSheetOrder(int sheetOrder) {
			this.sheetOrder = sheetOrder;
			
			// Update sheet order immediately
			workbook.setSheetOrder(sheet.getSheetName(), sheetOrder >= 0 ? 
					sheetOrder : workbook.getNumberOfSheets() + sheetOrder);
		}
		
		/**
		 * Set whether or not to lock column creation.
		 * <p/>
		 * During a lock, all attempts to create new or modify existing columns will throw an exception.
		 * @param lockColumns TRUE to lock/prevent column creation, FALSE otherwise.
		 */
		public void setLockColumns(boolean lockColumns) {
			checkClosed();
			this.lockColumns = lockColumns;
		}
		
		/**
		 * Get or create a row in the sheet.
		 * @param rowIndex the row index.
		 * @return The corresponding row.
		 */
		protected Row getOrCreateRow(int rowIndex) {
			Row row = sheet.getRow(rowIndex);
			return row != null ? row : sheet.createRow(rowIndex);
		}
		
		/**
		 * Retrieve the name of a given column.
		 * @param columnIndex the column index.
		 * @return The corresponding name, or NULL if the column could not be found.
		 */
		@Override
		public String getColumnName(int columnIndex) {
			return headerLookup.inverse().get(columnIndex);
		}
		
		/**
		 * Retrieve the number of columns in this table writer.
		 * @return The column count.
		 */
		public int getColumnCount() {
			return columnIndex;
		}
		
		/**
		 * Retrieve the index of the given column, if it exists.
		 * @param columnName name of the column.
		 * @return The corresponding column index, or -1 if not found.
		 */
		@Override
		public int getColumnIndex(String columnName) {
			Integer index = headerLookup.get(columnName);
			return index != null ? index : -1;
		}
		
		/**
		 * Set the suppressed state of a given column.
		 * <p/>
		 * Suppressed columns will not be written to the output Excel file.
		 * This can only be set when the writer is at the start of the file, and before
		 * the column has been created. 
		 * <p/>
		 * It is best to set this before any columns have been created, and data has been written.
		 * @param column the column.
		 * @param suppressed TRUE if the column is suppressed, FALSE otherwise.
		 */
		public void setColumnSuppressed(String column, boolean suppressed) {
			checkClosed();
			checkLockState(column);
			
			if (!atStart()) {
				throw new IllegalStateException("Cannot change suppressed state after the start.");
			}
			if (suppressed) {
				if (headerLookup.containsKey(column)) {
					throw new IllegalStateException("Cannot suppress column " + column + ": It has already been created.");
				}
				if (suppressedColumns == null) {
					suppressedColumns = Sets.newHashSet();
				}
				suppressedColumns.add(column);
				
			} else if (suppressedColumns != null) {
				suppressedColumns.remove(column);
			}
		}
		
		/**
		 * Determine if the given column is suppressed.
		 * @param column the column name.
		 * @return TRUE if it is, FALSE otherwise.
		 * @see {@link #setColumnSuppressed(String, boolean)}
		 */
		public boolean isColumnSuppressed(String column) {
			return suppressedColumns != null && suppressedColumns.contains(column);
		}
		
		/**
		 * Set whether or not the given column is hidden, hiding both the header and the data.
		 * <p>
		 * This can be set before a column is created.
		 * @param column the column to hide.
		 * @param hidden TRUE to hide the column, FALSE to show it.
		 * @return TRUE if the column was hidden previously, FALSE otherwise.
		 */
		public boolean setColumnHidden(String column, boolean hidden) {
			checkClosed();
			
			if (hidden) {
				if (hiddenColumns == null) {
					hiddenColumns = Sets.newHashSet();
				}
				// If add is TRUE, it did not yet contain this column
				return !hiddenColumns.add(column);
				
			} else if (hiddenColumns != null) {
				// If remove is TRUE, it DID contain this column
				return hiddenColumns.remove(column);
				
			} else {
				// No column has been hidden yet
				return false;
			}
		}
		
		/**
		 * Determine if the given column exists and is hidden.
		 * @param column the column to check.
		 * @return TRUE if this column is hidden, FALSE otherwise.
		 */
		public boolean isColumnHidden(String column) {
			return hiddenColumns != null && hiddenColumns.contains(column);
		}
		
		/**
		 * Create a new column by the given name.
		 * @param columnName the column. Cannot be NULL.
		 * @return The created column index, the existing column index, or {@link #SUPPRESSED_COLUMN} if it is suppressed.
		 */
		public int createColumn(String columnName) {
			Preconditions.checkNotNull(columnName, "columnName cannot be NULL");
			
			// Do not create suppressed columns
			if (isColumnSuppressed(columnName)) {
				return SUPPRESSED_COLUMN;
			}
			Integer index = headerLookup.get(columnName);
			checkClosed();
			
			if (index == null) {
				index = columnIndex++;
				updateColumnIndex(columnName, index);
				
				if (isHeaderRowVisible()) {
					Cell cell = setCellValue(headerRow, index, style.getHeaderStyle(), columnName);
					onCellWritten(index, cell, style.getHeaderStyle(), columnName);
				}
			}
			return index;
		}
		
		@Override
		public boolean canCreateColumns() {
			if (closed || lockColumns) {
				return false;
			}
			// Ensure header row is within range (might not be in the case of streaming sheets)
			return rowIndex < 0 || sheet.getRow(headerRowIndex) != null;
		}

		/**
		 * Notify the writer of a new column at the given index.
		 * @param columnName the column name.
		 */
		private void updateColumnIndex(String columnName, int index) {
			// Check the lock state
			checkLockState(columnName);
			headerLookup.put(columnName, index);
		}
		
		/**
		 * Determine if the header row should be visible.
		 * @return TRUE if it is, FALSE otherwise.
		 */
		protected boolean isHeaderRowVisible() {
			return !creationOptions.contains(SheetCreationOptions.HIDE_HEADER_ROW);
		}
		
		/**
		 * Pre-create the given list of columns.
		 * @param columns the columns.
		 */
		public void createColumns(Collection<? extends String> columns) {
			checkClosed();
			
			for (String column : columns) {
				createColumn(column);
			}
		}
		
		/**
		 * Retrieve the underlying sheet.
		 * @return The sheet.
		 */
		public Sheet getSheet() {
			return sheet;
		}

		/**
		 * Retrieve the current default style.
		 * @return The default style.
		 */
		public ReportStyle getStyle() {
			return style;
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing.
		 * @param columnName column name. Cannot be NULL.
		 * @param value the value to write.
		 */
		public void write(String columnName, Object value) {
			write(columnName, style, value);
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing. 
		 * @param columnIndex column index, starting at 0. 
		 * @param cellStyle the cell style.
		 * @param value the value to write.
		 */
		@Override
		public void write(int columnIndex, Object value) {
			write(columnIndex, style, value);
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing.
		 * @param columnName column name. Cannot be NULL.
		 * @param style the report style.
		 * @param value the value to write.
		 */
		public void write(String columnName, ReportStyle style, Object value) {
			write(createColumn(columnName), style, value);
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing. 
		 * @param columnIndex column index, starting at 0. 
		 * @param style the report style.
		 * @param value the value to write.
		 */
		public void write(int columnIndex, ReportStyle style, Object value) {
			CellStyle cellStyle = style.forValue(value);
			write(columnIndex, cellStyle, value);
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing. 
		 * @param columnName column name. Cannot be NULL.
		 * @param cellStyle the cell style.
		 * @param value the value to write.
		 */
		public void write(String columnName, CellStyle cellStyle, Object value) {
			write(createColumn(columnName), cellStyle, value);
		}
		
		/**
		 * Write the given value using the current row.
		 * </p>
		 * Columns will automatically be created if missing. 
		 * @param columnIndex column index, starting at 0. 
		 * @param cellStyle the cell style.
		 * @param value the value to write.
		 */
		public void write(int columnIndex, CellStyle cellStyle, Object value) {
			// Cannot write to the initial row
			if (atStart()) {
				throw new IllegalStateException("Call incrementRow() before writing data to a writer.");
			}

			// Ignore suppressed columns
			if (columnIndex != SUPPRESSED_COLUMN) {
				// Record that this data column is being written
				writtenColumns.set(columnIndex);

				Cell cell = setCellValue(getRow(), columnIndex, cellStyle, value);
				onCellWritten(columnIndex, cell, cellStyle, value);
			}
		}
		
		/**
		 * Invoked before a cell is being written, either with data or a header.
		 * @param columnIndex column index, starting at 0.
		 * @param columnCell column cell in the sheet.
		 * @param style the style.
		 * @param value the value that is being written.
		 */
		protected void onCellWritten(int columnIndex, Cell columnCell, CellStyle style, Object value) {
			checkClosed();
						
			if (columnIndex >= indexAutoSizeColumns) {
				// Register size of cell
				sizeTrackers.computeIfAbsent(columnIndex, this::createSizeTracker).
					registerCell(columnCell, style, value);
			}
		}

		/**
		 * Create a size tracker for the given column.
		 * @param columnIndex the index.
		 * @return The corresponding size tracker.
		 */
		private ColumnSizeTracker createSizeTracker(int columnIndex) {
			ColumnSizeTracker tracker = new ColumnSizeTracker(() -> getOrCreateEvaluator());
			
			// If we are appending, we'll just assume the column width is the minimum correct width
			if (creationOptions.contains(SheetCreationOptions.PRESERVE_EXISTING)) {
				tracker.setMinWidth(sheet.getColumnWidth(columnIndex));
			}
			return tracker;
		}

		/**
		 * Retrieve the absolute starting row index.
		 * <p>
		 * This is the index of the first writable data row. 
		 * @return Starting row index.
		 */
		public int getAbsoluteStartingRow() {
			return startingRowIndex;
		}
		
		/**
		 * Retrieve the absolute row index of the writer.
		 * <p>
		 * This is initially one less than the {@link #getAbsoluteStartingRow()}.
		 * @return Absolute row index.
		 */
		public int getAbsoluteRowIndex() {
			return rowIndex;
		}
		
		/**
		 * Retrieve the current row.
		 * @return Current row.
		 */
		public Row getRow() {
			Row result = currentRow;
			
			if (result == null) {
				result = currentRow = getOrCreateRow(rowIndex);
			}
			return result;
		}
		
		/**
		 * Retrieve the current row index, relative to the current starting row.
		 * <p>
		 * Note that the initial row is immediately preceding the start row, so the row index will be -1 
		 * after the writer has been created.
		 * @return Current row index.
		 */
		public int getRowIndex() {
			return rowIndex - startingRowIndex;
		}
		
		/**
		 * Retrieve the number of written columns.
		 * @return Number of written columns.
		 */
		public int getWrittenColumns() {
			return writtenColumns.cardinality();
		}
				
		/**
		 * Determine if the writer will automatically freeze the header row (replacing any existing frozen row) when it is closed.
		 * @return TRUE if the header row will be frozen, FALSE otherwise.
		 */
		public boolean isFreezeHeaderRow() {
			return freezeHeaderRow;
		}
		
		/**
		 * Set whether or not the writer will automatically freeze the header row (replacing any existing frozen row) when it is closed.
		 * @param freezeHeaderRow TRUE to set the header as frozen, FALSE otherwise.
		 */
		public void setFreezeHeaderRow(boolean freezeHeaderRow) {
			this.freezeHeaderRow = freezeHeaderRow;
		}
		
		/**
		 * Determine if the given column has a written value (even NULL) in the current row.
		 * <p/>
		 * This will always be FALSE if the column does not exists.
		 * @param columnName the column name.
		 * @return TRUE if it does, FALSE otherwise.
		 */
		public boolean isColumnWritten(String columnName) {
			int columnIndex = getColumnIndex(columnName);
			return columnIndex >= 0 && isColumnWritten(columnIndex);
		}
		
		/**
		 * Determine if the column with the given index has a written value (even NULL) in the current row.
		 * @param columnIndex the column index. Cannot be negative.
		 * @return TRUE if it does, FALSE otherwise.
		 */
		public boolean isColumnWritten(int columnIndex) {
			if (columnIndex < 0) {
				throw new IllegalArgumentException("columnIndex cannot be negative.");
			}
			return writtenColumns.get(columnIndex);
		}
				
		/**
		 * Determine if the current row is empty.
		 * <p/>
		 * A row is empty if no values have been written to the row.
		 * @return TRUE if it is empty, FALSE otherwise.
		 */
		public boolean isEmptyRow() {
			// Initial row is not writable, so emptiness does not make sense
			if (atStart()) {
				throw new IllegalStateException("Call incrementRow() before testing for empty rows.");
			}
			return writtenColumns.isEmpty();
		}
		
		/**
		 * Retrieve an unmodifiable view of the options used when creating this sheet writer.
		 * @return The sheet options.
		 */
		public Set<SheetCreationOptions> getCreationOptions() {
			return Collections.unmodifiableSet(creationOptions);
		}
		
		/**
		 * Set the new row index, relative to the current starting row.
		 * @param rowIndex new row index. Cannot be negative.
		 */
		public void setRowIndex(int rowIndex) {
			if (rowIndex < 0) {
				throw new IllegalArgumentException("rowIndex cannot be negative.");
			}
			int absoluteRow = rowIndex + startingRowIndex;
			checkClosed();
			
			if (this.rowIndex != absoluteRow) {
				this.rowIndex = absoluteRow;
				this.currentRow = null;
				this.writtenColumns.clear();
			}
		}
		
		/**
		 * Increment the current row by one.
		 * <p>
		 * This must be invoked before writing any data.
		 */
		public void incrementRow() {
			incrementRow(1);
		}
		
		/**
		 * Increment the current row index by the given delta.
		 * @param delta the row delta.
		 * @return This writer, for chaining.
		 */
		public void incrementRow(int delta) {
			checkClosed();
			
			if (delta != 0) {
				// The very first row (-1) should be ignored
				if (rowIndex >= startingRowIndex) {
					onRowDone();
				}
				this.rowIndex += delta;
				this.currentRow = null;
				this.writtenColumns.clear();
			}
		}
		
		/**
		 * Invoked when the current row is done writing.
		 */
		protected void onRowDone() {
			if (defaultValueSupplier != null) {
				for (int i = startingColumnIndex; i < columnIndex; i++) {
					if (!writtenColumns.get(i)) {
						defaultValueSupplier.onWriteDefault(this, i, getColumnName(i));
					}
				}
			}
		}

		/**
		 * Determine if the current writer is closed.
		 */
		protected void checkClosed() {
			if (closed) {
				throw new IllegalStateException("Cannot write to a closed writer.");
			}
		}
		
		/**
		 * Determine if the column creation/modification has been locked.
		 * @param columnName the column that was attempted modified/created.
		 */
		protected void checkLockState(String columnName) {
			if (lockColumns) {
				throw new IllegalStateException("Unable to create/modify column '" + 
						columnName + "': Column modification has been locked.");
			}
		}
		
		/**
		 * Close the current writer.
		 */
		public void close() {
			if (closed) {
				return;
			}
			// Close the last row
			if (rowIndex >= startingRowIndex) {
				onRowDone();
			}
			// Writer has now been closed
			closed = true;
			
			try {
				// Freeze header section
				if (freezeHeaderRow) {
					sheet.createFreezePane(startingColumnIndex, startingRowIndex);
				} else {
					sheet.createFreezePane(0, 0);
				}
				
				// Auto size columns
				for (Entry<Integer, ColumnSizeTracker> entry : sizeTrackers.entrySet()) {
					// Width in character unit
					double width = entry.getValue().computeMaxWidth(workbook);
					
					if (width != -1) {
						sheet.setColumnWidth(entry.getKey(), (int) (256 * width));
					}
				}
				
				// Filter first row?
				if (filterFirstRow && rowIndex>=headerRowIndex && (columnIndex-1)>=startingColumnIndex) {
					sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, rowIndex, startingColumnIndex, columnIndex - 1));
				}
				
				// Set hidden columns?
				if (hiddenColumns != null) {
					for (String column : hiddenColumns) {
						int index = getColumnIndex(column);
						
						if (index >= 0) {
							sheet.setColumnHidden(index, true);
						}
					}
				}
			} finally {
				onSheetWriterClosed(this);
			}
		}
	}

	/**
	 * Represents an Excel report style.
	 */
	public static class ReportStyle {
		/**
		 * Represents a report style type.
		 */
		public enum ReportStyleType {
			HEADER,
			TEXT,
			NUMBER,
			TIMESTAMP,
			DATE,
			TIME,
			NULL,
			HYPERLINK
		}
		
		/**
		 * A set that contains all report style types.
		 */
		private static final Set<ReportStyleType> ALL_TYPES = EnumSet.allOf(ReportStyleType.class);
		
		private final Workbook workbook;
		private CellStyle headerStyle;
		private final CellStyle textStyle;
		private final CellStyle numberStyle;
		private final CellStyle timestampStyle;
		private final CellStyle dateStyle;
		private final CellStyle timeStyle;
		private final CellStyle nullStyle;
		private final CellStyle hyperlinkStyle;
		
		/**
		 * Retrieve a report style that uses the given style in all cases.
		 * @param workbook the parent workbook.
		 * @param style the style.
		 * @return Corresponding report style.
		 */
		public static ReportStyle all(Workbook workbook, CellStyle style) {
			return new ReportStyle(workbook, style, style, style, style, style, style, style);
		}
		
		/**
		 * Construct a new report style.
		 * <p/>
		 * The styles must belong to the same workbook. Styles can be NULL, this will be treated as no style.
		 * @param workbook the parent workbook.
		 * @param headerStyle style of header cells.
		 * @param textStyle text cell style.
		 * @param numberStyle number cell style.
		 * @param timestampStyle timestamp cell style.
		 * @param dateStyle date cell style.
		 * @param timeStyle time cell style.
		 * @param nullStyle null cell style.
		 */
		public ReportStyle(
				Workbook workbook,
				CellStyle headerStyle, CellStyle textStyle, CellStyle numberStyle, 
				CellStyle timestampStyle, CellStyle dateStyle, CellStyle timeStyle, CellStyle nullStyle) {
			
			this(workbook, headerStyle, textStyle, nullStyle, timestampStyle, dateStyle, timeStyle, nullStyle, null);
		}
		
		/**
		 * Construct a new report style.
		 * <p/>
		 * The styles must belong to the same workbook. Styles can be NULL, this will be treated as no style.
		 * @param workbook the parent workbook. Must be NON-NULL and match any non-null styles.
		 * @param headerStyle style of header cells.
		 * @param textStyle text cell style.
		 * @param numberStyle number cell style.
		 * @param timestampStyle timestamp cell style.
		 * @param dateStyle date cell style.
		 * @param timeStyle time cell style.
		 * @param nullStyle null cell style.
		 * @param hyperlinkStyle the hyperlink style.
		 */
		public ReportStyle(
				Workbook workbook,
				CellStyle headerStyle, CellStyle textStyle, CellStyle numberStyle, 
				CellStyle timestampStyle, CellStyle dateStyle, CellStyle timeStyle, CellStyle nullStyle, 
				CellStyle hyperlinkStyle) {
			
			super();
			this.workbook = Preconditions.checkNotNull(workbook, "workbook cannot be NULL");
			this.headerStyle = headerStyle;
			this.textStyle = textStyle;
			this.numberStyle = numberStyle;
			this.timestampStyle = timestampStyle;
			this.dateStyle = dateStyle;
			this.timeStyle = timeStyle;
			this.nullStyle = nullStyle;
			this.hyperlinkStyle = hyperlinkStyle;
		}
		
		/**
		 * Retrieve the style associated with the given style type.
		 * @param type the report style type.
		 * @return Corresponding cell style.
		 */
		public CellStyle getStyle(ReportStyleType type) {
			switch (type) {
			case TIMESTAMP: return getTimestampStyle();
			case DATE: return getDateStyle();
			case HEADER: return getHeaderStyle();
			case HYPERLINK: return getHyperlinkStyle();
			case NULL: return getNullStyle();
			case NUMBER: return getNumberStyle();
			case TEXT: return getTextStyle();
			case TIME: return getTimeStyle(); 
			default:
				throw new IllegalArgumentException("Unknown style type: " + type);
			}
		}
		
		/**
		 * Retrieve the style of header cells.
		 * @return Header cell style.
		 */
		public CellStyle getHeaderStyle() {
			return headerStyle;
		}

		/**
		 * Retrieve the style of text cells.
		 * @return Text cell style.
		 */
		public CellStyle getTextStyle() {
			return textStyle;
		}

		/**
		 * Retrieve the style of number cells.
		 * @return Number cell style.
		 */
		public CellStyle getNumberStyle() {
			return numberStyle;
		}

		/**
		 * Retrieve the style of date cells.
		 * @return Date cell style.
		 */
		public CellStyle getDateStyle() {
			return dateStyle;
		}

		/**
		 * Retrieve the style of timestamp cells.
		 * @return Date cell style.
		 */
		public CellStyle getTimestampStyle() {
			return timestampStyle;
		}

		/**
		 * Retrieve the style of time cells.
		 * @return Time cells.
		 */
		public CellStyle getTimeStyle() {
			return timeStyle;
		}
		
		/**
		 * Retrieve the style of null/empty cells.
		 * @return Null/empty cell style.
		 */
		public CellStyle getNullStyle() {
			return nullStyle;
		}

		/**
		 * Retrieve the style that will be applied to hyperlinks.
		 * @return The hyperlink style.
		 */
		public CellStyle getHyperlinkStyle() {
			return hyperlinkStyle;
		}
		
		/**
		 * Retrieve an equivalent report style where the background color is replaced with the given background color.
		 * @param color the color.
		 * @param modifyTypes report types where the background color should be changed, or NULL/empty if this should apply to ALL types.
		 * @return An equivalent report style.
		 */
		public ReportStyle withBackgroundColor(Color color, ReportStyleType... modifyTypes) {
			Set<ReportStyleType> lookup = createLookup(modifyTypes);
			
			return withMapping((type, style) -> 
				lookup.contains(type) ? createBackgroundColorStyle(workbook, color, style) : style
			);
		}
		
		/**
		 * Retrieve an equivalent report style with border
		 * @param borderStyle the style.
		 * @param color the color.
		 * @param modifyTypes report types where the background color should be changed, or NULL/empty if this should apply to ALL types.
		 * @return An equivalent report style.
		 */
		public ReportStyle withBorderColor(BorderStyle borderStyle, Color color, ReportStyleType... modifyTypes) {
			Set<ReportStyleType> lookup = createLookup(modifyTypes);
			
			return withMapping((type, style) -> 
				lookup.contains(type) ? createBorderColorStyle(workbook, borderStyle, color, style) : style
			);
		}
		
		/**
		 * Retrieve an equivalent report style where the font is replaced with the given font.
		 * @param font the font.
		 * @param modifyTypes report types where the font should be changed, or NULL/empty if this should apply to ALL types.
		 * @return An equivalent report style.
		 */
		public ReportStyle withFont(Font font, ReportStyleType... modifyTypes) {
			Set<ReportStyleType> lookup = createLookup(modifyTypes);
			
			return withMapping((type, style) -> 
				lookup.contains(type) ? createStyleWithFont(workbook, style, font) : style
			);
		}
		
		/**
		 * Retrieve an equivalent report style where cell types of this report is mapped according to a function.
		 * @param the cell type mapping.
		 * @return An equivalent report style.
		 */
		protected ReportStyle withMapping(BiFunction<ReportStyleType, CellStyle, CellStyle> mapping) {
			return new ReportStyle(
				workbook,
				mapping.apply(ReportStyleType.HEADER, headerStyle),
				mapping.apply(ReportStyleType.TEXT, textStyle),
				mapping.apply(ReportStyleType.NUMBER, numberStyle),
				mapping.apply(ReportStyleType.TIMESTAMP, timestampStyle),
				mapping.apply(ReportStyleType.DATE, dateStyle),
				mapping.apply(ReportStyleType.TIME, timeStyle),
				mapping.apply(ReportStyleType.NULL, nullStyle),
				mapping.apply(ReportStyleType.HYPERLINK, hyperlinkStyle)
			);
		}
		
		/**
		 * Create a lookup for report style types.
		 * @param types the report style types.
		 * @return Corresponding lookup set.
		 */
		private Set<ReportStyleType> createLookup(ReportStyleType[] types) {
			if (types == null || types.length == 0) {
				return ALL_TYPES;
			}
			EnumSet<ReportStyleType> result = EnumSet.noneOf(ReportStyleType.class);
			
			for (ReportStyleType type : types) {
				result.add(type);
			}
			return result;
		}
				
		/**
		 * Retrieve the appropriate cell style for a given value.
		 * @param value the value.
		 * @return Corresponding cell style.
		 */
		public CellStyle forValue(Object value) {
			if (value instanceof Timestamp) {
				return getTimestampStyle();
			} else if (value instanceof Date) {
				return getDateStyle();
			} else if (value instanceof Calendar) {
				return getDateStyle();
			} else if (value instanceof LocalTime || value instanceof OffsetTime) {
				return getTimeStyle();
			} else if (value instanceof TemporalAccessor) {
				return getDateStyle();
			} else if (value instanceof Number) {
				return getNumberStyle();
			} else if (value instanceof SheetEntity) {
				return ((SheetEntity) value).getDefaultStyle(this);
			} else if (value != null) {
				return getTextStyle();
			} else {
				return getNullStyle();
			}
		}
		
		@Override
		public int hashCode() {
			return Objects.hashCode(headerStyle, textStyle, numberStyle, dateStyle, timeStyle, nullStyle, hyperlinkStyle);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} else if (obj instanceof ReportStyle) {
				ReportStyle other = (ReportStyle) obj;
				return Objects.equal(getHeaderStyle(), other.getHeaderStyle()) &&
					   Objects.equal(getTextStyle(), other.getTextStyle()) &&
					   Objects.equal(getNumberStyle(), other.getNumberStyle()) &&
					   Objects.equal(getDateStyle(), other.getDateStyle()) &&
					   Objects.equal(getTimeStyle(), other.getTimeStyle()) &&
					   Objects.equal(getNullStyle(), other.getNullStyle()) &&
					   Objects.equal(getHyperlinkStyle(), other.getHyperlinkStyle());
			}
			return false;
		}
	}
	
	public Workbook getWorkbook() {
		return workbook;
	}
}

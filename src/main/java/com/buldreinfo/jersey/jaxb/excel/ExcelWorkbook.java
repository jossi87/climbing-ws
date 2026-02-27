package com.buldreinfo.jersey.jaxb.excel;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWorkbook implements AutoCloseable {
	private final Workbook workbook;
	private CellStyle headerStyle;
	private CellStyle hyperlinkStyle;
	private CellStyle dateStyle;

	public ExcelWorkbook() {
		this.workbook = new XSSFWorkbook();
	}

	public ExcelSheet addSheet(String sheetName) {
		return new ExcelSheet(this, workbook.createSheet(sheetName));
	}

	@Override
	public void close() throws IOException {
		workbook.close();
	}

	public void write(OutputStream stream) throws IOException {
		workbook.write(stream);
	}

	protected CellStyle getDateStyle() {
		if (dateStyle == null) {
			dateStyle = workbook.createCellStyle();
			dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy.mm.dd"));
		}
		return dateStyle;
	}
	
	protected CellStyle getHeaderStyle() {
		if (headerStyle == null) {
			Font font = workbook.createFont();
			headerStyle = workbook.createCellStyle();
			font.setBold(true);
			headerStyle.setFont(font);
		}
		return headerStyle;
	}
	
	protected Hyperlink getHyperlink(String url) {
		Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
		hyperlink.setAddress(url);
		return hyperlink;
	}

	protected CellStyle getHyperlinkStyle() {
		if (hyperlinkStyle == null) {
			hyperlinkStyle = workbook.createCellStyle();
			Font hyperlinkFont = workbook.createFont();
			hyperlinkFont.setUnderline(Font.U_SINGLE);
			hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
			hyperlinkStyle.setFont(hyperlinkFont);
		}
		return hyperlinkStyle;
	}
}
package com.buldreinfo.jersey.jaxb.pdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.batik.transcoder.TranscoderException;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Sector.Problem;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

public class PdfGenerator {
	private static Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
	private static Font FONT_CHAPTER = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
	private static Font FONT_REGULAR = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
	private static Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
	private final PdfWriter writer;
	private final Document document;
	private final Area area;
	private final List<Sector> sectors;

	public PdfGenerator(OutputStream output, Area area, List<Sector> sectors) throws DocumentException, IOException, TranscoderException, TransformerException {
		Preconditions.checkArgument(area != null && !sectors.isEmpty());
		this.area = area;
		this.sectors = sectors;
		this.document = new Document();
		this.writer = PdfWriter.getInstance(document, output);
		FooterPageEvent event = new FooterPageEvent();
		writer.setPageEvent(event);
		document.open();
		addMetaData();
		if (!Strings.isNullOrEmpty(area.getComment()) || (area.getMedia() != null && area.getMedia().isEmpty())) {
			writeFrontpage();
		}
		document.newPage();
		writeSectors();
		document.close();
	}

	private void addMetaData() {
		document.addTitle(area.getName());
		document.addSubject(area.getName());
		document.addKeywords(area.getName());
		document.addAuthor("Jostein Øygarden (buldreinfo.com / brattelinjer.no");
		document.addCreator("Jostein Øygarden (buldreinfo.com / brattelinjer.no");
	}

	private void writeFrontpage() throws DocumentException, IOException, TranscoderException, TransformerException {
		document.add(new Paragraph(area.getName(), FONT_HEADER));
		if (!Strings.isNullOrEmpty(area.getComment())) {
			document.add(new Paragraph(" "));
			try (InputStream is = new ByteArrayInputStream(("<p style=\"font-size:12px;\">"+area.getComment()+"</p>").getBytes())) {
				XMLWorkerHelper.getInstance().parseXHtml(writer, document, is);
			}
		}
		if (area.getMedia() != null && !area.getMedia().isEmpty()) {
			for (Media m : area.getMedia()) {
				URL url = new URL(GlobalFunctions.getUrlJpgToImage(m.getId()));
				Image img = Image.getInstance(url);
				img.scaleToFit(527, 527);
				document.add(img);
			}
		}
	}

	private void writeSectors() throws DocumentException, IOException, TranscoderException, TransformerException {
		for (int i = 0; i < sectors.size(); i++) {
			Sector s = sectors.get(i);
			Anchor anchor = new Anchor(s.getName(), FONT_CHAPTER);
			anchor.setName(s.getName() + " (" + s.getAreaName() + ")");
			Chapter chapter = new Chapter(new Paragraph(anchor), (i+1));
			if (s.getMedia() != null && !s.getMedia().isEmpty()) {
				writeSectorTopo(chapter, s);
			}
			writeSectorTable(chapter, s);
			document.add(chapter);
		}
	}

	private void writeSectorTopo(Section section, Sector s) throws BadElementException, IOException, TranscoderException, TransformerException {
		for (Media m : s.getMedia()
				.stream()
				.filter(x -> x.getSvgs() != null && !x.getSvgs().isEmpty())
				.collect(Collectors.toList())) {
			Path topo = TopoGenerator.generateTopo(m);
			Image img = Image.getInstance(topo.toString());
			img.scaleToFit(527, 527);
			section.add(img);
		}
	}

	private void writeSectorTable(Section section, Sector s) {
		float[] relativeWidths = s.getMetadata().isBouldering()? new float[]{1, 5, 2, 5} : new float[]{1, 5, 2, 2, 5};
		PdfPTable table = new PdfPTable(relativeWidths);
		table.setWidthPercentage(100);
		addTableCell(table, true, "#");
		addTableCell(table, true, "Name");
		addTableCell(table, true, "Grade");
		if (!s.getMetadata().isBouldering()) {
			addTableCell(table, true, "Type");
		}
		addTableCell(table, true, "FA");
		for (Problem p : s.getProblems()) {
			addTableCell(table, false, String.valueOf(p.getNr()));
			addTableCell(table, false, p.getName());
			addTableCell(table, false, p.getGrade());
			if (!s.getMetadata().isBouldering()) {
				addTableCell(table, false, p.getT().getSubType());
			}
			addTableCell(table, false, p.getFa());
		}
		section.add(new Paragraph(" "));
		section.add(table);
	}

	private void addTableCell(PdfPTable table, boolean isHeader, String str) {
		PdfPCell cell = new PdfPCell(new Phrase(str, isHeader? FONT_BOLD : FONT_REGULAR));
		table.addCell(cell);
	}
}
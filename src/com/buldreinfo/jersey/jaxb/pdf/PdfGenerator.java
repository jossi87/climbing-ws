package com.buldreinfo.jersey.jaxb.pdf;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.apache.batik.transcoder.TranscoderException;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Sector.Problem;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

public class PdfGenerator {
	class MyFooter extends PdfPageEventHelper {
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase header = new Phrase("\u00A9 buldreinfo.com & brattelinjer.no", FONT_SMALL);
            Phrase footer = new Phrase("Page " + document.getPageNumber(), FONT_SMALL);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    header,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.top() + 10, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10, 0);
        }
    }
	private static Font FONT_SMALL = new Font(Font.FontFamily.UNDEFINED, 5, Font.ITALIC);
	private static Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
	private static Font FONT_CHAPTER = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
	private static Font FONT_REGULAR = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
	private static Font FONT_REGULAR_LINK = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLUE);
	private static Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
	private final PdfWriter writer;
	private final Document document;
	private final Area area;
	private final List<Sector> sectors;
	private final Set<Integer> mediaIdProcessed = Sets.newHashSet();
	
	public static void main(String[] args) throws IOException, DocumentException, TranscoderException, TransformerException {
		int areaId = 2780;
		Path dst = Paths.get("c:/users/jostein/desktop/test.pdf");
		try (FileOutputStream fos = new FileOutputStream(dst.toFile())) {
			Gson gson = new Gson();
			Area area = null;
			List<Sector> sectors = new ArrayList<>();
			URL obj = new URL("https://brattelinjer.no/com.buldreinfo.jersey.jaxb/v2/areas?id=" + areaId);
			HttpURLConnection con = (HttpURLConnection)obj.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				area = gson.fromJson(new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")), Area.class);
			}
			for (Area.Sector s : area.getSectors()) {
				obj = new URL("https://brattelinjer.no/com.buldreinfo.jersey.jaxb/v2/sectors?id=" + s.getId());
				con = (HttpURLConnection)obj.openConnection();
				con.setRequestMethod("GET");
				responseCode = con.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					sectors.add(gson.fromJson(new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")), Sector.class));
				}
			}
			new PdfGenerator(fos, area, sectors);
		}
	}

	public PdfGenerator(OutputStream output, Area area, List<Sector> sectors) throws DocumentException, IOException, TranscoderException, TransformerException {
		Preconditions.checkArgument(area != null && !sectors.isEmpty());
		this.area = area;
		this.sectors = sectors;
		this.document = new Document();
		this.writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new MyFooter());
		document.open();
		addMetaData();
		if (!Strings.isNullOrEmpty(area.getComment()) || (area.getMedia() != null && area.getMedia().isEmpty())) {
			writeFrontpage();
		}
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
				mediaIdProcessed.add(m.getId());
				URL url = new URL(GlobalFunctions.getUrlJpgToImage(m.getId()));
				Image img = Image.getInstance(url);
				scaleImage(img);
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
			if (!Strings.isNullOrEmpty(s.getComment())) {
				chapter.add(new Paragraph(s.getComment()));
			}
			writeSectorTable(chapter, s);
			if (s.getMedia() != null) {
				writeSectorTopo(chapter, s);
			}
			document.add(chapter);
		}
	}

	private void writeSectorTopo(Section section, Sector s) throws BadElementException, IOException, TranscoderException, TransformerException {
		for (Media m : s.getMedia()) {
			if (m.getSvgs() != null && !m.getSvgs().isEmpty()) {
				Path topo = TopoGenerator.generateTopo(m);
				Image img = Image.getInstance(topo.toString());
				scaleImage(img);
				section.add(img);
			}
			else if (mediaIdProcessed.add(m.getId())) {
				URL url = new URL(GlobalFunctions.getUrlJpgToImage(m.getId()));
				Image img = Image.getInstance(url);
				scaleImage(img);
				section.add(img);
			}
		}
	}
	
	private void scaleImage(Image img) {
		img.scaleToFit(527, 350);
	}

	private void writeSectorTable(Section section, Sector s) {
		float[] relativeWidths = s.getMetadata().isBouldering()? new float[]{1, 5, 2, 5, 5} : new float[]{1, 5, 2, 2, 5, 5};
		PdfPTable table = new PdfPTable(relativeWidths);
		table.setWidthPercentage(100);
		addTableCell(table, FONT_BOLD, "#");
		addTableCell(table, FONT_BOLD, "Name");
		addTableCell(table, FONT_BOLD, "Grade");
		if (!s.getMetadata().isBouldering()) {
			addTableCell(table, FONT_BOLD, "Type");
		}
		addTableCell(table, FONT_BOLD, "FA");
		addTableCell(table, FONT_BOLD, "Note");
		for (Problem p : s.getProblems()) {
			addTableCell(table, FONT_REGULAR, String.valueOf(p.getNr()));
			String url = s.getMetadata().getCanonical();
			url = url.substring(0, url.indexOf("/sector"));
			url += "/problem/" + p.getId();
			addTableCell(table, FONT_REGULAR_LINK, p.getName(), url);
			addTableCell(table, FONT_REGULAR, p.getGrade());
			if (!s.getMetadata().isBouldering()) {
				addTableCell(table, FONT_REGULAR, p.getT().getSubType());
			}
			addTableCell(table, FONT_REGULAR, p.getFa());
			addTableCell(table, FONT_REGULAR, p.getComment());
		}
		section.add(new Paragraph(" "));
		section.add(table);
	}
	
	private void addTableCell(PdfPTable table, Font font, String str) {
		addTableCell(table, font, str, null);
	}
	
	private void addTableCell(PdfPTable table, Font font, String str, String url) {
		PdfPCell cell = new PdfPCell(new Phrase(str, font));
		if (url != null) {
			cell.setCellEvent(new LinkInCell(url));
		}
		table.addCell(cell);
	}
}
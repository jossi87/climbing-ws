package com.buldreinfo.jersey.jaxb.pdf;

import java.awt.Color;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openpdf.text.Anchor;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfAction;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfOutline;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPCellEvent;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import com.buldreinfo.jersey.jaxb.jfreechart.GradeDistributionGenerator;
import com.buldreinfo.jersey.jaxb.leafletprint.LeafletPrintGenerator;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.IconType;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Marker;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Outline;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.PrintSlope;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.Meta;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.ProblemComment;
import com.buldreinfo.jersey.jaxb.model.ProblemSection;
import com.buldreinfo.jersey.jaxb.model.ProblemTick;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.TickRepeat;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class PdfGenerator implements AutoCloseable {

	private class PageHeaderFooter extends PdfPageEventHelper {
		private PdfTemplate totalPages;
		private BaseFont baseFont;
		private String headerText = "";
		private final String copyrightText = "\u00A9 buldreinfo.com & brattelinjer.no";

		@Override
		public void onCloseDocument(PdfWriter writer, @SuppressWarnings("unused") Document document) {
			totalPages.beginText();
			totalPages.setFontAndSize(baseFont, 8);
			totalPages.setColorFill(Color.DARK_GRAY);
			totalPages.setTextMatrix(0, 0);
			totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
			totalPages.endText();
		}

		@Override
		public void onEndPage(PdfWriter writer, Document document) {
			PdfContentByte cb = writer.getDirectContent();

			cb.saveState();
			cb.beginText();
			cb.setFontAndSize(baseFont, 8);
			cb.setColorFill(Color.DARK_GRAY);

			cb.showTextAligned(Element.ALIGN_LEFT, headerText, document.left(), document.top() + 10, 0);
			cb.showTextAligned(Element.ALIGN_RIGHT, copyrightText, document.right(), document.top() + 10, 0);

			cb.endText();
			cb.restoreState();

			cb.saveState();
			cb.beginText();
			cb.setFontAndSize(baseFont, 8);
			cb.setColorFill(Color.DARK_GRAY);
			String text = "Page " + writer.getPageNumber() + " of ";
			float textWidth = baseFont.getWidthPoint(text, 8);
			float totalWidth = textWidth + baseFont.getWidthPoint("999", 8);
			float footerX = (document.right() - document.left()) / 2 + document.leftMargin() - (totalWidth / 2);
			float footerY = document.bottom() - 10;

			cb.setTextMatrix(footerX, footerY);
			cb.showText(text);
			cb.endText();
			cb.addTemplate(totalPages, footerX + textWidth, footerY);
			cb.restoreState();
		}

		@Override
		public void onOpenDocument(PdfWriter writer, @SuppressWarnings("unused") Document document) {
			totalPages = writer.getDirectContent().createTemplate(30, 12);
			try {
				baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
			} catch (Exception e) {
				logger.error("Error creating font for header/footer", e);
			}
		}

		public void setHeaderText(String text) {
			this.headerText = text;
		}
	}
	private class WatermarkedCell implements PdfPCellEvent {
		private final String watermark;
		public WatermarkedCell(String watermark) {
			this.watermark = watermark;
		}
		@Override
		public void cellLayout(@SuppressWarnings("unused") PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
			if (watermarkFont == null) {
				return;
			}
			PdfContentByte cb = canvases[PdfPTable.TEXTCANVAS];
			cb.saveState();
			try {
				cb.beginText();
				cb.setFontAndSize(watermarkFont, 6);
				cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL_STROKE);
				cb.setLineWidth(0.3f);
				cb.setColorStroke(Color.BLACK);
				cb.setColorFill(Color.WHITE);
				float x = (position.getLeft() + position.getRight()) / 2;
				float y = position.getBottom() + 4;
				cb.showTextAligned(Element.ALIGN_CENTER, watermark, x, y, 0);
				cb.endText();
			} catch (Exception e) {
				logger.error("Error drawing watermark", e);
			}
			cb.restoreState();
		}
	}
	private static final Logger logger = LogManager.getLogger();
	private static final Font FONT_H1 = new Font(Font.HELVETICA, 16, Font.BOLD);
	private static final Font FONT_H2 = new Font(Font.HELVETICA, 11, Font.BOLD);
	private static final Font FONT_REG = new Font(Font.HELVETICA, 8, Font.NORMAL);
	private static final Font FONT_REG_LINK = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLUE);
	private static final Font FONT_ITALIC = new Font(Font.HELVETICA, 9, Font.ITALIC);
	private static final Font FONT_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);
	private static final Font FONT_LINK = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLUE);
	private static final Font FONT_SMALL = new Font(Font.HELVETICA, 5, Font.ITALIC);

	private final static int IMAGE_STAR_SIZE = 7;
	private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
	private final Document document;
	private final PdfWriter writer;
	private final Set<Integer> mediaIdProcessed = new HashSet<>();

	private final PageHeaderFooter pageEvent;

	private BaseFont watermarkFont;

	public PdfGenerator(OutputStream output) {
		this.document = new Document(PageSize.A4, 30, 30, 30, 30);
		this.writer = PdfWriter.getInstance(document, output);

		this.writer.setViewerPreferences(PdfWriter.PageModeUseOutlines);
		this.pageEvent = new PageHeaderFooter();
		this.writer.setPageEvent(this.pageEvent);

		document.open();
		try {
			watermarkFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
		} catch (Exception e) {
			logger.error("Could not create font", e);
		}
	}

	@Override
	public void close() throws Exception {
		document.close();
	}

	public void writeArea(Meta meta, Area area, Collection<GradeDistribution> gradeDistribution, List<Sector> sectors) throws Exception {
		mediaIdProcessed.clear();
		pageEvent.setHeaderText(area.getName());
		document.add(new Paragraph(area.getName(), FONT_H1));
		writeMapArea(area, sectors);

		PdfPTable info = new PdfPTable(new float[]{1.5f, 8.5f});
		info.setWidthPercentage(100);
		info.setSpacingBefore(15f);
		info.setSpacingAfter(15f);

		info.addCell(createKeyCell("Generated"));
		info.addCell(createValueCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));

		if (!Strings.isNullOrEmpty(area.getCanonical())) {
			info.addCell(createKeyCell("URL"));
			Anchor anchor = new Anchor(area.getCanonical(), FONT_LINK);
			anchor.setReference(area.getCanonical());
			PdfPCell urlCell = new PdfPCell(anchor);
			urlCell.setBorder(Rectangle.BOTTOM);
			urlCell.setBorderColor(Color.LIGHT_GRAY);
			urlCell.setPadding(6f);
			urlCell.setVerticalAlignment(Element.ALIGN_TOP);
			info.addCell(urlCell);
		}

		if (!Strings.isNullOrEmpty(area.getAccessClosed()) || !Strings.isNullOrEmpty(area.getAccessInfo()) || !Strings.isNullOrEmpty(area.getComment())) {
			StringBuilder areaInfo = new StringBuilder();
			if (!Strings.isNullOrEmpty(area.getAccessClosed())) {
				areaInfo.append("Access closed: ").append(area.getAccessClosed());
			}
			if (!Strings.isNullOrEmpty(area.getAccessInfo())) {
				if (areaInfo.length() > 0) {
					areaInfo.append("\n");
				}
				areaInfo.append("Access info: ").append(area.getAccessInfo());
			}
			if (!Strings.isNullOrEmpty(area.getComment())) {
				if (areaInfo.length() > 0) {
					areaInfo.append("\n");
				}
				areaInfo.append(area.getComment());
			}
			info.addCell(createKeyCell("Description"));
			info.addCell(createValueCell(areaInfo.toString()));
		}

		if (info.getRows().size() > 0) {
			document.add(info);
		}

		if (gradeDistribution != null && !gradeDistribution.isEmpty()) {
			byte[] png = GradeDistributionGenerator.write(gradeDistribution);
			Image img = Image.getInstance(png);
			img.scaleToFit(150, 150);
			document.add(img);
		}

		if (area.getMedia() != null && !area.getMedia().isEmpty()) {
			writeMediaGrid(area.getMedia(), 0, 3, 600);
		}

		writeSectors(meta, sectors);
	}

	public void writeProblem(Area area, Sector sector, Problem problem) throws Exception {
		mediaIdProcessed.clear();

		String headerTitle = String.format("%s / %s / #%d %s [%s]", area.getName(), sector.getName(), problem.getNr(), problem.getName(), problem.getGrade());
		pageEvent.setHeaderText(headerTitle);

		document.add(new Paragraph(headerTitle, FONT_H1));
		writeMapProblem(area, sector, problem);

		PdfPTable info = new PdfPTable(new float[]{1.5f, 8.5f});
		info.setWidthPercentage(100);
		info.setSpacingBefore(15f);
		info.setSpacingAfter(15f);

		info.addCell(createKeyCell("Generated"));
		info.addCell(createValueCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));

		if (!Strings.isNullOrEmpty(problem.getCanonical())) {
			info.addCell(createKeyCell("URL"));
			Anchor anchor = new Anchor(problem.getCanonical(), FONT_LINK);
			anchor.setReference(problem.getCanonical());
			PdfPCell urlCell = new PdfPCell(anchor);
			urlCell.setBorder(Rectangle.BOTTOM);
			urlCell.setBorderColor(Color.LIGHT_GRAY);
			urlCell.setPadding(6f);
			urlCell.setVerticalAlignment(Element.ALIGN_TOP);
			info.addCell(urlCell);
		}

		StringBuilder areaInfo = new StringBuilder();
		if (!Strings.isNullOrEmpty(area.getAccessClosed())) {
			areaInfo.append("Access closed: ").append(area.getAccessClosed());
		}
		if (!Strings.isNullOrEmpty(area.getAccessInfo())) {
			if (areaInfo.length() > 0) areaInfo.append("\n");
			areaInfo.append("Access info: ").append(area.getAccessInfo());
		}
		if (!Strings.isNullOrEmpty(area.getComment())) {
			if (areaInfo.length() > 0) areaInfo.append("\n");
			areaInfo.append(area.getComment());
		}
		if (!areaInfo.isEmpty()) {
			info.addCell(createKeyCell("Area"));
			info.addCell(createValueCell(areaInfo.toString()));
		}

		StringBuilder sectorInfo = new StringBuilder();
		if (!Strings.isNullOrEmpty(sector.getAccessClosed())) {
			sectorInfo.append("Access closed: ").append(sector.getAccessClosed());
		}
		if (!Strings.isNullOrEmpty(sector.getAccessInfo())) {
			if (sectorInfo.length() > 0) sectorInfo.append("\n");
			sectorInfo.append("Access info: ").append(sector.getAccessInfo());
		}
		if (!Strings.isNullOrEmpty(sector.getComment())) {
			if (sectorInfo.length() > 0) sectorInfo.append("\n");
			sectorInfo.append(sector.getComment());
		}
		if (!sectorInfo.isEmpty()) {
			info.addCell(createKeyCell("Sector"));
			info.addCell(createValueCell(sectorInfo.toString()));
		}

		if (problem.getFaAid() != null) {
			String aide = problem.getFaAid().users().stream().map(User::name).collect(Collectors.joining(", "));
			info.addCell(createKeyCell("FA (Aid)"));
			info.addCell(createValueCell(aide + " (" + problem.getFaAid().dateHr() + ")"));
		}

		String fa = problem.getFa().stream().map(User::name).collect(Collectors.joining(", "));
		if (!fa.isEmpty()) {
			info.addCell(createKeyCell("First Ascent"));
			info.addCell(createValueCell(fa + " (" + problem.getFaDateHr() + ")"));
		}

		if (!Strings.isNullOrEmpty(problem.getComment())) {
			info.addCell(createKeyCell("Description"));
			info.addCell(createValueCell(problem.getComment()));
		}
		document.add(info);

		List<Media> combinedMedia = new ArrayList<>();
		if (area.getMedia() != null) combinedMedia.addAll(area.getMedia());
		if (sector.getMedia() != null) combinedMedia.addAll(sector.getMedia());
		if (problem.getMedia() != null) {
			for (Media m : problem.getMedia()) {
				boolean isPitchMedia = m.svgs() != null && m.svgs().stream().anyMatch(s -> s.problemId() == problem.getId() && s.pitch() > 0);
				if (!isPitchMedia) {
					combinedMedia.add(m);
				}
			}
		}
		writeMediaGrid(combinedMedia, problem.getId(), 3, 600);

		if (problem.getSections() != null && !problem.getSections().isEmpty()) {
			PdfOutline rootOutline = writer.getRootOutline();

			for (ProblemSection section : problem.getSections()) {
				PdfPTable pitchWrapper = new PdfPTable(1);
				pitchWrapper.setWidthPercentage(100);
				pitchWrapper.setSpacingBefore(10f);

				PdfPCell pitchCell = new PdfPCell();
				pitchCell.setBorder(Rectangle.LEFT);
				pitchCell.setBorderWidthLeft(1.5f);
				pitchCell.setBorderColorLeft(Color.LIGHT_GRAY);
				pitchCell.setPaddingLeft(10f);
				pitchCell.setPaddingBottom(10f);
				pitchCell.setBorderWidthRight(0);
				pitchCell.setBorderWidthTop(0);
				pitchCell.setBorderWidthBottom(0);

				Paragraph p = new Paragraph(8);

				String destName = "pitch_" + problem.getId() + "_" + section.nr();
				Chunk pitchTitle = new Chunk("Pitch " + section.nr() + " (" + section.grade() + "): ", FONT_BOLD);
				pitchTitle.setLocalDestination(destName);
				p.add(pitchTitle);

				p.add(new Chunk(section.description(), FONT_REG));
				pitchCell.addElement(p);

				new PdfOutline(rootOutline, PdfAction.gotoLocalPage(destName, false), "Pitch " + section.nr());

				PdfPTable grid = createPitchGrid(problem, section);
				pitchCell.addElement(grid);

				pitchWrapper.addCell(pitchCell);
				document.add(pitchWrapper);
			}
		}
		writeTicksAndComments(problem);
	}

	private void addDummyCell(PdfPTable table) {
		PdfPCell c = new PdfPCell(); 
		c.setBorder(Rectangle.NO_BORDER); 
		table.addCell(c);
	}

	private void addImageCell(PdfPTable table, byte[] data, String desc) {
		if (data == null) {
			return;
		}
		try {
			Image img = Image.getInstance(data);
			PdfPCell cell = new PdfPCell(img, true);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPadding(3f);
			if (!Strings.isNullOrEmpty(desc)) cell.setCellEvent(new WatermarkedCell(desc));
			table.addCell(cell);
		} catch (Exception e) {
			logger.error("Failed to add image cell", e);
		}
	}

	private void addTableCell(PdfPTable table, Font font, String str, String url, boolean greenBackground) {
		Phrase phrase = new Phrase();
		if (str == null) str = "";

		if (url != null && !url.isEmpty()) {
			Anchor anchor = new Anchor(str, font);
			anchor.setReference(url);
			phrase.add(anchor);
		} else {
			phrase.add(new Chunk(str, font));
		}

		PdfPCell cell = new PdfPCell(phrase);
		cell.setPadding(4f);
		cell.setBorderColor(Color.LIGHT_GRAY);
		if (greenBackground) {
			cell.setBackgroundColor(new Color(220, 255, 220));
		}
		table.addCell(cell);
	}

	private void addTextWithLinks(Phrase p, String text, Font textFont) {
		if (Strings.isNullOrEmpty(text)) {
			return;
		}
		Matcher m = URL_PATTERN.matcher(text);
		int lastEnd = 0;
		while (m.find()) {
			if (m.start() > lastEnd) {
				p.add(new Chunk(text.substring(lastEnd, m.start()), textFont));
			}
			String url = m.group(1);
			Anchor a = new Anchor(url, FONT_LINK);
			a.setReference(url);
			p.add(a);
			lastEnd = m.end();
		}
		if (lastEnd < text.length()) {
			p.add(new Chunk(text.substring(lastEnd), textFont));
		}
	}

	private void appendStarIcons(Phrase phrase, double stars) throws Exception {
		for (int i = 0; i < 3; i++) {
			Image star = Image.getInstance(PdfGenerator.class.getResource(stars >= i + 1 ? "filled-star.png" : (stars >= i + 0.5 ? "star-half-empty.png" : "star.png")));
			star.scaleAbsolute(IMAGE_STAR_SIZE, IMAGE_STAR_SIZE);
			phrase.add(new Chunk(star, 0, 0));
		}
	}

	private PdfPCell createKeyCell(String text) {
		PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(Color.LIGHT_GRAY);
		cell.setPadding(6f);
		cell.setPaddingLeft(0f);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		return cell;
	}

	private PdfPTable createPitchGrid(Problem problem, ProblemSection section) throws Exception {
		int cols = 3;
		PdfPTable mainTable = new PdfPTable(cols);
		mainTable.setWidthPercentage(100);
		mainTable.setSpacingBefore(5f);

		List<PdfPTable> columnTables = new ArrayList<>();
		for (int i = 0; i < cols; i++) {
			PdfPTable colTable = new PdfPTable(1);
			colTable.setWidthPercentage(100);
			columnTables.add(colTable);
		}

		List<CompletableFuture<byte[]>> futures = new ArrayList<>();
		for (Media m : problem.getMedia()) {
			if (m.svgs() == null) continue;
			List<Svg> pitchSvgs = m.svgs().stream().filter(s -> s.problemId() == problem.getId() && s.pitch() == section.nr()).collect(Collectors.toList());
			if (!pitchSvgs.isEmpty()) {
				futures.add(CompletableFuture.supplyAsync(() -> {
					PdfMediaScaler.MediaRegion reg = null;
					try {
						reg = PdfMediaScaler.calculateMediaRegion(pitchSvgs.get(0).path(), m.width(), m.height());
					} catch (Exception e) {
						logger.warn(e.getMessage(), e);
					}
					return safeGenerateTopo(m.id(), m.width(), m.height(), m.mediaSvgs(), m.svgs(), reg, 1200, problem.getId());
				}));
			}
		}

		int[] colCounts = new int[cols];
		int imageIndex = 0;

		for (CompletableFuture<byte[]> f : futures) {
			byte[] data = f.get();
			if (data != null) {
				int targetCol = imageIndex % cols;
				addImageCell(columnTables.get(targetCol), data, "");
				colCounts[targetCol]++;
				imageIndex++;
			}
		}

		if (section.media() != null) {
			List<CompletableFuture<byte[]>> sectionFutures = new ArrayList<>();
			List<Media> toProcess = new ArrayList<>();
			for (Media m : section.media()) {
				if (!mediaIdProcessed.contains(m.id())) {
					toProcess.add(m);
					sectionFutures.add(CompletableFuture.supplyAsync(() -> safeGenerateTopo(m.id(), m.width(), m.height(), m.mediaSvgs(), null, null, 600, 0)));
					mediaIdProcessed.add(m.id());
				}
			}
			for (int i = 0; i < sectionFutures.size(); i++) {
				byte[] data = sectionFutures.get(i).get();
				if (data != null) {
					int targetCol = imageIndex % cols;
					addImageCell(columnTables.get(targetCol), data, toProcess.get(i).mediaMetadata().description());
					colCounts[targetCol]++;
					imageIndex++;
				}
			}
		}

		for (int i = 0; i < cols; i++) {
			PdfPTable colTable = columnTables.get(i);
			if (colCounts[i] == 0) addDummyCell(colTable);

			PdfPCell colWrapper = new PdfPCell();
			colWrapper.setBorder(Rectangle.NO_BORDER);
			colWrapper.setPadding(0);
			colWrapper.addElement(colTable);
			mainTable.addCell(colWrapper);
		}

		return mainTable;
	}

	private PdfPCell createValueCell(String text) {
		Paragraph p = new Paragraph();
		addTextWithLinks(p, text, FONT_REG);
		PdfPCell cell = new PdfPCell(p);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(Color.LIGHT_GRAY);
		cell.setPadding(6f);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		return cell;
	}

	private String removeIllegalChars(String name) {
		if (name != null) {
			return name.replaceAll("[^ÆØÅæøåa-zA-Z0-9]", " ");
		}
		return name;
	}

	private byte[] safeGenerateTopo(int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int targetWidth, int probId) {
		try {
			return TopoGenerator.generateTopo(mediaId, width, height, mediaSvgs, svgs, region, targetWidth, probId);
		} catch (Exception e) {
			logger.error("Failed to generate topo for media " + mediaId, e);
			return null;
		}
	}

	private void writeMapArea(Area area, List<Sector> sectors) {
		try {
			List<Marker> markers = new ArrayList<>();
			List<Outline> outlines = new ArrayList<>();
			List<PrintSlope> slopes = new ArrayList<>();
			LatLng defaultCenter = null;
			if (area.getCoordinates() != null && area.getCoordinates().getLatitude() > 0 && area.getCoordinates().getLongitude() > 0) {
				defaultCenter = new LatLng(area.getCoordinates().getLatitude(), area.getCoordinates().getLongitude());
			}
			int defaultZoom = 14;

			boolean useLegend = sectors.size()>1;
			List<String> legends = new ArrayList<>();
			for (Sector sector : sectors) {
				if (sector.getParking() != null && sector.getParking().getLatitude() > 0 && sector.getParking().getLongitude() > 0) {
					markers.add(new Marker(sector.getParking().getLatitude(), sector.getParking().getLongitude(), IconType.PARKING, null));
				}
				if (sector.getApproach() != null && !sector.getApproach().coordinates().isEmpty()) {
					slopes.add(new PrintSlope(sector.getApproach(), "lime"));
				}
				if (sector.getDescent() != null && !sector.getDescent().coordinates().isEmpty()) {
					slopes.add(new PrintSlope(sector.getDescent(), "purple"));
				}
				if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
					final String name = removeIllegalChars(sector.getName());
					String label = null;
					if (useLegend) {
						label = String.valueOf(legends.size() + 1);
						legends.add(label + ": " + name);
					}
					else {
						label = name;
					}
					String polygonCoords = sector.getOutline().stream().map(o -> o.getLatitude() + "," + o.getLongitude()).collect(Collectors.joining(";"));
					outlines.add(new Outline(label, polygonCoords));
				}
			}

			if (!markers.isEmpty() || !outlines.isEmpty() || !slopes.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, legends, defaultCenter, defaultZoom, false);
				Optional<byte[]> optSnapshot = LeafletPrintGenerator.takeSnapshot(leaflet);
				if (optSnapshot.isPresent()) {
					PdfPTable table = new PdfPTable(1);
					table.setWidthPercentage(100);
					Image img = Image.getInstance(optSnapshot.get());
					PdfPCell cell = new PdfPCell(img, true);
					cell.setColspan(table.getNumberOfColumns());
					table.addCell(cell);
					document.add(new Paragraph(" "));
					document.add(table);
				}
			}
		} catch (Exception | Error e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private void writeMapProblem(Area area, Sector sector, Problem problem) {
		try {
			List<Marker> markers = new ArrayList<>();
			List<Outline> outlines = new ArrayList<>();
			List<PrintSlope> slopes = new ArrayList<>();
			LatLng defaultCenter = null;
			if (problem.getCoordinates() != null && problem.getCoordinates().getLatitude() > 0 && problem.getCoordinates().getLongitude() > 0) {
				defaultCenter = new LatLng(problem.getCoordinates().getLatitude(), problem.getCoordinates().getLongitude());
			}
			else if (sector.getParking() != null && sector.getParking().getLatitude() > 0 && sector.getParking().getLongitude() > 0) {
				defaultCenter = new LatLng(sector.getParking().getLatitude(), sector.getParking().getLongitude());
			}
			else if (area.getCoordinates() != null && area.getCoordinates().getLatitude() > 0 && area.getCoordinates().getLongitude() > 0) {
				defaultCenter = new LatLng(area.getCoordinates().getLatitude(), area.getCoordinates().getLongitude());
			}
			int defaultZoom = 15;

			if (sector.getParking() != null && sector.getParking().getLatitude() > 0 && sector.getParking().getLongitude() > 0) {
				markers.add(new Marker(sector.getParking().getLatitude(), sector.getParking().getLongitude(), IconType.PARKING, null));
			}
			if (problem.getCoordinates() != null && problem.getCoordinates().getLatitude() > 0 && problem.getCoordinates().getLongitude() > 0) {
				String name = removeIllegalChars(problem.getName());
				markers.add(new Marker(problem.getCoordinates().getLatitude(), problem.getCoordinates().getLongitude(), IconType.DEFAULT, name));
			}
			if (sector.getApproach() != null && !sector.getApproach().coordinates().isEmpty()) {
				slopes.add(new PrintSlope(sector.getApproach(), "lime"));
			}
			if (sector.getDescent() != null && !sector.getDescent().coordinates().isEmpty()) {
				slopes.add(new PrintSlope(sector.getDescent(), "purple"));
			}
			if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
				String label = removeIllegalChars(sector.getName());
				String polygonCoords = sector.getOutline().stream().map(o -> o.getLatitude() + "," + o.getLongitude()).collect(Collectors.joining(";"));
				outlines.add(new Outline(label, polygonCoords));
			}

			if (!markers.isEmpty() || !outlines.isEmpty() || !slopes.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, null, defaultCenter, defaultZoom, false);
				Optional<byte[]> optSnapshot = LeafletPrintGenerator.takeSnapshot(leaflet);
				if (optSnapshot.isPresent()) {
					PdfPTable table = new PdfPTable(1);
					table.setWidthPercentage(100);
					Image img = Image.getInstance(optSnapshot.get());
					PdfPCell cell = new PdfPCell(img, true);
					cell.setColspan(table.getNumberOfColumns());
					table.addCell(cell);

					// Also append photo map
					markers = markers.stream().filter(m -> !m.iconType().equals(IconType.PARKING)).collect(Collectors.toList());
					if (!markers.isEmpty()) {
						outlines.clear();
						slopes.clear();
						leaflet = new Leaflet(markers, outlines, slopes, null, defaultCenter, defaultZoom, true);
						optSnapshot = LeafletPrintGenerator.takeSnapshot(leaflet);
						if (optSnapshot.isPresent()) {
							img = Image.getInstance(optSnapshot.get());
							cell = new PdfPCell(img, true);
							cell.setColspan(table.getNumberOfColumns());
							table.addCell(cell);
						}
					}

					document.add(new Paragraph(" "));
					document.add(table);
				}
			}
		} catch (Exception | Error e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private void writeMapSector(Sector sector) {
		try {
			List<Outline> outlines = new ArrayList<>();
			List<PrintSlope> slopes = new ArrayList<>();
			List<Marker> markers = new ArrayList<>();
			LatLng defaultCenter = null;
			if (sector.getParking() != null && sector.getParking().getLatitude() > 0 && sector.getParking().getLongitude() > 0) {
				defaultCenter = new LatLng(sector.getParking().getLatitude(), sector.getParking().getLongitude());
			}
			int defaultZoom = 14;
			List<String> legends = new ArrayList<>();

			Multimap<String, SectorProblem> problemsWithCoordinatesGroupedByRock = ArrayListMultimap.create();
			List<SectorProblem> problemsWithoutRock = new ArrayList<>();
			for (SectorProblem p : sector.getProblems()) {
				if (p.coordinates() != null && p.coordinates().getLatitude() > 0 && p.coordinates().getLongitude() > 0) {
					if (p.rock() != null) {
						problemsWithCoordinatesGroupedByRock.put(p.rock(), p);
					}
					else {
						problemsWithoutRock.add(p);
					}
				}
			}
			for (String rock : problemsWithCoordinatesGroupedByRock.keySet()) {
				Collection<SectorProblem> problems = problemsWithCoordinatesGroupedByRock.get(rock);
				LatLng latLng = LeafletPrintGenerator.getCenter(problems);
				markers.add(new Marker(latLng.lat(), latLng.lng(), IconType.ROCK, rock));
			}
			for (SectorProblem p : problemsWithoutRock) {
				markers.add(new Marker(p.coordinates().getLatitude(), p.coordinates().getLongitude(), IconType.DEFAULT, String.valueOf(p.nr())));
			}
			if (markers.size() >= 1 && markers.size() <= 3) {
				if (sector.getParking() != null && sector.getParking().getLatitude() > 0 && sector.getParking().getLongitude() > 0) {
					markers.add(new Marker(sector.getParking().getLatitude(), sector.getParking().getLongitude(), IconType.PARKING, null));
				}
				if (sector.getApproach() != null && !sector.getApproach().coordinates().isEmpty()) {
					slopes.add(new PrintSlope(sector.getApproach(), "lime"));
				}
				if (sector.getDescent() != null && !sector.getDescent().coordinates().isEmpty()) {
					slopes.add(new PrintSlope(sector.getDescent(), "purple"));
				}
				if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
					final String label = removeIllegalChars(sector.getName());
					String polygonCoords = sector.getOutline().stream().map(o -> o.getLatitude() + "," + o.getLongitude()).collect(Collectors.joining(";"));
					outlines.add(new Outline(label, polygonCoords));
				}
			}

			if (!markers.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, legends, defaultCenter, defaultZoom, true);
				Optional<byte[]> optSnapshot = LeafletPrintGenerator.takeSnapshot(leaflet);
				if (optSnapshot.isPresent()) {
					PdfPTable table = new PdfPTable(1);
					table.setWidthPercentage(100);
					Image img = Image.getInstance(optSnapshot.get());
					PdfPCell cell = new PdfPCell(img, true);
					cell.setColspan(table.getNumberOfColumns());
					table.addCell(cell);
					document.add(new Paragraph(" "));
					document.add(table);
				}
			}
		} catch (Exception | Error e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private void writeMediaGrid(List<Media> media, int probId, int cols, int targetWidth) throws Exception {
		if (media == null || media.isEmpty()) {
			return;
		}

		PdfPTable mainTable = new PdfPTable(cols);
		mainTable.setWidthPercentage(100);

		List<PdfPTable> columnTables = new ArrayList<>();
		for (int i = 0; i < cols; i++) {
			PdfPTable colTable = new PdfPTable(1);
			colTable.setWidthPercentage(100);
			columnTables.add(colTable);
		}

		List<CompletableFuture<byte[]>> futures = new ArrayList<>();
		List<Media> toProcess = new ArrayList<>();
		for (Media m : media) {
			if (mediaIdProcessed.contains(m.id())) continue;
			toProcess.add(m);
			List<Svg> svgs = m.svgs() != null ? m.svgs().stream().filter(s -> probId <= 0 || s.problemId() == probId).collect(Collectors.toList()) : null;
			futures.add(CompletableFuture.supplyAsync(() -> safeGenerateTopo(m.id(), m.width(), m.height(), m.mediaSvgs(), svgs, null, targetWidth, probId)));
			mediaIdProcessed.add(m.id());
		}

		int[] colCounts = new int[cols];
		int imageIndex = 0;

		for (int i = 0; i < futures.size(); i++) {
			byte[] data = futures.get(i).get();
			if (data != null) {
				Media m = toProcess.get(i);
				long uniqueProblems = (m.svgs() == null || probId != 0) ? 0 : m.svgs().stream().map(Svg::problemId).distinct().count();
				if (uniqueProblems > 5) {
					Image img = Image.getInstance(data);
					PdfPCell cell = new PdfPCell(img, true);
					cell.setBorder(Rectangle.NO_BORDER);
					cell.setColspan(cols);
					mainTable.addCell(cell);
				} else {
					int targetCol = imageIndex % cols;
					addImageCell(columnTables.get(targetCol), data, m.mediaMetadata().description());
					colCounts[targetCol]++;
					imageIndex++;
				}
			}
		}

		for (int i = 0; i < cols; i++) {
			PdfPTable colTable = columnTables.get(i);
			if (colCounts[i] == 0) addDummyCell(colTable);

			PdfPCell colWrapper = new PdfPCell();
			colWrapper.setBorder(Rectangle.NO_BORDER);
			colWrapper.setPadding(0);
			colWrapper.addElement(colTable);
			mainTable.addCell(colWrapper);
		}

		document.add(mainTable);
	}

	private void writeSectors(Meta meta, List<Sector> sectors) throws Exception {
		for (Sector s : sectors) {
			final boolean showType = meta.isClimbing();
			document.newPage();
			document.add(new Paragraph(s.getName(), FONT_H2));
			if (!Strings.isNullOrEmpty(s.getAccessInfo())) {
				document.add(new Phrase(s.getAccessInfo(), FONT_BOLD));
			}
			if (!Strings.isNullOrEmpty(s.getComment())) {
				document.add(new Phrase(s.getComment(), FONT_REG));
			}
			writeMapSector(s);

			// Table
			float[] relativeWidths = showType ? new float[]{1, 4, 2, 2, 4, 7} : new float[]{1, 3, 1, 3, 8};
			PdfPTable table = new PdfPTable(relativeWidths);
			table.setWidthPercentage(100);

			addTableCell(table, FONT_BOLD, "#", null, false);
			addTableCell(table, FONT_BOLD, "Name", null, false);
			addTableCell(table, FONT_BOLD, "Grade", null, false);
			if (showType) {
				addTableCell(table, FONT_BOLD, "Type", null, false);
			}
			addTableCell(table, FONT_BOLD, "FA", null, false);
			addTableCell(table, FONT_BOLD, "Note", null, false);

			for (SectorProblem p : s.getProblems()) {
				String description = Strings.emptyToNull(p.comment());
				if (!Strings.isNullOrEmpty(p.rock())) {
					if (description == null) {
						description = "Rock: " + p.rock();
					} else {
						description = "Rock: " + p.rock() + ". " + description;
					}
				}
				if (!Strings.isNullOrEmpty(p.broken())) {
					if (description == null) {
						description = p.broken();
					} else {
						description = p.broken() + ". " + description;
					}
				}

				addTableCell(table, FONT_REG, String.valueOf(p.nr()), null, p.ticked());
				String url = meta.url() + "/problem/" + p.id();
				addTableCell(table, FONT_REG_LINK, p.name(), url, p.ticked());
				addTableCell(table, FONT_REG, p.grade(), null, p.ticked());
				if (showType) {
					addTableCell(table, FONT_REG, p.t().subType(), null, p.ticked());
				}
				addTableCell(table, FONT_REG, p.fa(), null, p.ticked());

				Phrase note = new Phrase();
				if (p.numTicks() > 0) {
					appendStarIcons(note, p.stars());
					note.add(new Chunk(" " + p.numTicks() + " ascent" + (p.numTicks() == 1 ? "" : "s"), FONT_REG));
				}
				if (description != null) {
					note.add(new Chunk((p.numTicks() > 0 ? " - " : "") + description, FONT_ITALIC));
				}

				PdfPCell cell = new PdfPCell(note);
				cell.setPadding(4f);
				cell.setBorderColor(Color.LIGHT_GRAY);
				if (p.ticked()) {
					cell.setBackgroundColor(new Color(220, 255, 220));
				}
				table.addCell(cell);
			}
			document.add(new Paragraph(" "));
			document.add(table);

			if (s.getMedia() != null && !s.getMedia().isEmpty()) {
				writeMediaGrid(s.getMedia(), 0, 3, 600);
			}
		}
	}

	private void writeTicksAndComments(Problem p) throws Exception {
		if (p.getTicks() != null && !p.getTicks().isEmpty()) {
			document.add(new Paragraph("Ascents", FONT_H2));
			PdfPTable table = new PdfPTable(new float[]{1.0f, 1.3f, 4.5f});
			table.setWidthPercentage(100);
			table.setSpacingBefore(5f);

			for (ProblemTick t : p.getTicks()) {
				PdfPCell dateCell = new PdfPCell(new Phrase(t.getDate(), FONT_REG));
				dateCell.setPadding(4f);
				dateCell.setBorderColor(Color.LIGHT_GRAY);
				table.addCell(dateCell);

				PdfPCell nameCell = new PdfPCell(new Phrase(t.getName(), FONT_REG));
				nameCell.setPadding(4f);
				nameCell.setBorderColor(Color.LIGHT_GRAY);
				table.addCell(nameCell);

				Phrase pStars = new Phrase(8);
				appendStarIcons(pStars, t.getStars());
				if (!Strings.isNullOrEmpty(t.getComment())) {
					pStars.add(new Chunk(" ", FONT_REG));
					addTextWithLinks(pStars, t.getComment(), FONT_REG);
				}

				if (t.getRepeats() != null) {
					for (TickRepeat r : t.getRepeats()) {
						pStars.add(new Chunk("\n \u2022 " + r.date() + ": ", FONT_SMALL));
						if (!Strings.isNullOrEmpty(r.comment())) {
							addTextWithLinks(pStars, r.comment(), FONT_SMALL);
						}
					}
				}
				PdfPCell commentCell = new PdfPCell(pStars);
				commentCell.setPadding(4f);
				commentCell.setBorderColor(Color.LIGHT_GRAY);
				table.addCell(commentCell);
			}
			document.add(table);
		}

		if (p.getComments() != null && !p.getComments().isEmpty()) {
			document.add(new Paragraph("Comments", FONT_H2));
			PdfPTable cTable = new PdfPTable(new float[]{1.0f, 1.3f, 4.5f});
			cTable.setWidthPercentage(100);
			cTable.setSpacingBefore(5f);

			for (ProblemComment c : p.getComments()) {
				PdfPCell dateCell = new PdfPCell(new Phrase(c.getDate(), FONT_REG));
				dateCell.setPadding(4f);
				dateCell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(dateCell);

				PdfPCell nameCell = new PdfPCell(new Phrase(c.getName(), FONT_REG));
				nameCell.setPadding(4f);
				nameCell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(nameCell);

				Paragraph para = new Paragraph(8);
				addTextWithLinks(para, c.getMessage(), FONT_REG);
				PdfPCell cell = new PdfPCell(para);
				cell.setPadding(4f);
				cell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(cell);
			}
			document.add(cTable);
		}
	}
}
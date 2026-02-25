package com.buldreinfo.jersey.jaxb.pdf;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openpdf.text.Anchor;
import org.openpdf.text.BadElementException;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfDestination;
import org.openpdf.text.pdf.PdfOutline;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPCellEvent;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.jfreechart.GradeDistributionGenerator;
import com.buldreinfo.jersey.jaxb.leafletprint.LeafletPrintGenerator;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.IconType;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Leaflet;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Marker;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.Outline;
import com.buldreinfo.jersey.jaxb.leafletprint.beans.PrintSlope;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.FaAid;
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
import com.buldreinfo.jersey.jaxb.model.Slope;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class PdfGenerator implements AutoCloseable {
	private class MyFooter extends PdfPageEventHelper {
		@Override
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
	private class WatermarkedCell implements PdfPCellEvent {
		private final String watermark;
		public WatermarkedCell(String watermark) {
			this.watermark = watermark;
		}
		@Override
		public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
			var textCanvas = canvases[PdfPTable.TEXTCANVAS];
			var phrase = new Phrase(watermark, FONT_WATERMARK);
			var x = (position.getLeft() + position.getRight()) / 2;
			var y = position.getTop() - phrase.getFont().getSize();
			// Draw outline (white)
			textCanvas.saveState();
			textCanvas.setColorStroke(Color.BLACK);
			textCanvas.setLineWidth(0.6f);
			textCanvas.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
			ColumnText.showTextAligned(textCanvas, Element.ALIGN_CENTER, phrase, x, y, 0);
			textCanvas.restoreState();
			// Draw the main text (black) on top of the outline
			textCanvas.saveState();
			textCanvas.setColorFill(Color.WHITE);
			textCanvas.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
			ColumnText.showTextAligned(textCanvas, Element.ALIGN_CENTER, phrase, x, y, 0);
			textCanvas.restoreState();
		}
	}
	private static Logger logger = LogManager.getLogger();
	private static Font FONT_SMALL = new Font(Font.UNDEFINED, 5, Font.ITALIC);
	private static Font FONT_WATERMARK = new Font(Font.UNDEFINED, 6, Font.NORMAL);
	private static Font FONT_H1 = new Font(Font.HELVETICA, 18, Font.BOLD);
	private static Font FONT_H2 = new Font(Font.HELVETICA, 12, Font.BOLD);
	private static Font FONT_REGULAR = new Font(Font.HELVETICA, 9, Font.NORMAL);
	private static Font FONT_ITALIC = new Font(Font.HELVETICA, 9, Font.ITALIC);
	private static Font FONT_REGULAR_LINK = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLUE);
	private static Font FONT_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD);
	private final static int IMAGE_STAR_SIZE = 9;
	private final PdfWriter writer;
	private final Document document;
	private final Set<Integer> mediaIdProcessed = Sets.newHashSet();
	private Image imageStarFilled;
	private Image imageStarHalf;
	private Image imageStarEmpty;
	public PdfGenerator(OutputStream output) {
		this.document = new Document();
		this.writer = PdfWriter.getInstance(document, output);
		writer.setStrictImageSequence(true);
		writer.setPageEvent(new MyFooter());
		document.open();
	}

	@Override
	public void close() throws Exception {
		document.close();		
	}

	public void writeArea(Meta meta, Area area, Collection<GradeDistribution> gradeDistribution, List<Sector> sectors) throws IOException, DocumentException, TranscoderException, TransformerException {
		Preconditions.checkArgument(area != null && !sectors.isEmpty());
		String title = area.getName();
		addMetaData(title);
		document.add(new Paragraph(title, FONT_H1));
		writeMapArea(area, sectors);
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk("URL: ", FONT_BOLD));
		Anchor anchor = new Anchor(area.getCanonical(), FONT_REGULAR_LINK);
		anchor.setReference(area.getCanonical());
		paragraph.add(anchor);
		document.add(paragraph);
		paragraph = new Paragraph();
		paragraph.add(new Chunk("PDF generated: ", FONT_BOLD));
		paragraph.add(new Chunk(String.valueOf(LocalDateTime.now()), FONT_REGULAR));
		document.add(paragraph);
		if (!Strings.isNullOrEmpty(area.getComment())) {
			document.add(new Paragraph(area.getComment()));
		}
		if ( (gradeDistribution != null && !gradeDistribution.isEmpty()) || (area.getMedia() != null && !area.getMedia().isEmpty()) ) {
			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			boolean addDummyCell = false;
			if (gradeDistribution != null && !gradeDistribution.isEmpty()) {
				byte[] png = GradeDistributionGenerator.write(gradeDistribution);
				Image img = Image.getInstance(png);
				PdfPCell cell = new PdfPCell(img, true);
				cell.setBorder(0);
				table.addCell(cell);
				addDummyCell = !addDummyCell;
			}
			if (area.getMedia() != null && !area.getMedia().isEmpty()) {
				for (Media m : area.getMedia()) {
					writeMediaCell(table, 1, m.id(), m.width(), m.height(), m.mediaMetadata().description(), m.mediaSvgs(), m.svgs());
					addDummyCell = !addDummyCell;
				}
			}
			if (addDummyCell) {
				addDummyCell(table);
			}
			document.add(table);
		}
		writeSectors(meta, sectors);
	}

	public void writeProblem(Area area, Sector sector, Problem problem) throws DocumentException, IOException, TranscoderException, TransformerException {
		Preconditions.checkArgument(area != null && sector != null && problem != null);
		String title = String.format("%s (%s / %s)", problem.getName(), area.getName(), sector.getName());
		addMetaData(title);
		document.add(new Paragraph(title, FONT_H1));
		writeMapProblem(area, sector, problem);
		if (!Strings.isNullOrEmpty(problem.getBroken())) {
			document.add(new Phrase(problem.getBroken(), FONT_BOLD));
		}
		if (!Strings.isNullOrEmpty(problem.getStartingAltitude())) {
			document.add(new Paragraph("Starting altitude: " + problem.getStartingAltitude(), FONT_REGULAR));
		}
		if (!Strings.isNullOrEmpty(problem.getAspect())) {
			document.add(new Paragraph("Aspect: " + problem.getAspect(), FONT_REGULAR));
		}
		if (!Strings.isNullOrEmpty(problem.getRouteLength())) {
			document.add(new Paragraph("Route length: " + problem.getRouteLength(), FONT_REGULAR));
		}
		if (!Strings.isNullOrEmpty(problem.getDescent())) {
			document.add(new Paragraph("Descent: " + problem.getDescent(), FONT_REGULAR));
		}
		if (!Strings.isNullOrEmpty(sector.getAccessInfo())) {
			document.add(new Phrase(sector.getAccessInfo(), FONT_BOLD));
		}
		if (!Strings.isNullOrEmpty(area.getComment())) {
			document.add(new Paragraph(area.getComment(), FONT_ITALIC));
		}
		if (!Strings.isNullOrEmpty(sector.getComment())) {
			document.add(new Paragraph(sector.getComment(), FONT_REGULAR));
		}

		// Route/Problem info
		Paragraph paragraph = new Paragraph();
		paragraph.add(new Chunk("URL: ", FONT_BOLD));
		Anchor anchor = new Anchor(problem.getCanonical(), FONT_REGULAR_LINK);
		anchor.setReference(problem.getCanonical());
		paragraph.add(anchor);
		document.add(paragraph);
		paragraph = new Paragraph();
		paragraph.add(new Chunk("PDF generated: ", FONT_BOLD));
		paragraph.add(new Chunk(String.valueOf(LocalDateTime.now()), FONT_REGULAR));
		document.add(paragraph);
		paragraph = new Paragraph();
		paragraph.add(new Chunk("Nr: ", FONT_BOLD));
		paragraph.add(new Chunk(String.valueOf(problem.getNr()), FONT_REGULAR));
		document.add(paragraph);
		paragraph = new Paragraph();
		paragraph.add(new Chunk("Type: ", FONT_BOLD));
		String type = problem.getT().subType() != null? problem.getT().type() + " - " + problem.getT().subType() : problem.getT().type();
		paragraph.add(new Chunk(type, FONT_REGULAR));
		document.add(paragraph);
		paragraph = new Paragraph();
		paragraph.add(new Chunk("Grade: ", FONT_BOLD));
		paragraph.add(new Chunk(problem.getGrade(), FONT_REGULAR));
		document.add(paragraph);
		if (problem.getFaAid() != null) {
			FaAid faAid = problem.getFaAid();
			paragraph = new Paragraph();
			paragraph.add(new Chunk("First ascent (Aid): ", FONT_BOLD));
			String faUsers = faAid.users() == null || faAid.users().isEmpty()? null : faAid.users().stream().map(User::name).collect(Collectors.joining(", "));
			if (!Strings.isNullOrEmpty(faUsers) && !Strings.isNullOrEmpty(faAid.dateHr())) {
				paragraph.add(new Chunk(faUsers + " (" + faAid.dateHr() + "). ", FONT_REGULAR));
			}
			else if (!Strings.isNullOrEmpty(faUsers)) {
				paragraph.add(new Chunk(faUsers + ". ", FONT_REGULAR));
			}
			else if (!Strings.isNullOrEmpty(faAid.dateHr())) {
				paragraph.add(new Chunk(faAid.dateHr() + ". ", FONT_REGULAR));
			}
			if (!Strings.isNullOrEmpty(faAid.description())) {
				paragraph.add(new Chunk(faAid.description(), FONT_ITALIC));
			}
			document.add(paragraph);
		}
		paragraph = new Paragraph();
		paragraph.add(new Chunk(problem.getFaAid() != null? "First free ascent (FFA): ": "First ascent: ", FONT_BOLD));
		String faUsers = problem.getFa() == null || problem.getFa().isEmpty()? null : problem.getFa().stream().map(User::name).collect(Collectors.joining(", "));
		if (!Strings.isNullOrEmpty(faUsers) && !Strings.isNullOrEmpty(problem.getFaDateHr())) {
			paragraph.add(new Chunk(faUsers + " (" + problem.getFaDateHr() + "). ", FONT_REGULAR));
		}
		else if (!Strings.isNullOrEmpty(faUsers)) {
			paragraph.add(new Chunk(faUsers + ". ", FONT_REGULAR));
		}
		else if (!Strings.isNullOrEmpty(problem.getFaDateHr())) {
			paragraph.add(new Chunk(problem.getFaDateHr() + ". ", FONT_REGULAR));
		}
		if (!Strings.isNullOrEmpty(problem.getComment())) {
			paragraph.add(new Chunk(problem.getComment(), FONT_ITALIC));
		}
		document.add(paragraph);


		// Pitches
		if (problem.getSections() != null && !problem.getSections().isEmpty()) {
			document.add(new Paragraph(" "));
			PdfPTable table = new PdfPTable(new float[] {1, 1, 10});
			table.setWidthPercentage(100);
			addTableCell(table, FONT_BOLD, "#");
			addTableCell(table, FONT_BOLD, "Grade");
			addTableCell(table, FONT_BOLD, "Description");
			for (ProblemSection section : problem.getSections()) {
				addTableCell(table, FONT_REGULAR, String.valueOf(section.nr()));
				addTableCell(table, FONT_REGULAR, section.grade());
				addTableCell(table, FONT_REGULAR, section.description());
			}
			document.add(table);
		}

		// Public ascents
		if (problem.getTicks() != null && !problem.getTicks().isEmpty()) {
			document.add(new Paragraph(" "));
			PdfPTable table = new PdfPTable(new float[] {1, 1, 1, 3});
			table.setWidthPercentage(100);
			addTableCell(table, FONT_BOLD, "Date");
			addTableCell(table, FONT_BOLD, "Grade");
			addTableCell(table, FONT_BOLD, "Name");
			addTableCell(table, FONT_BOLD, "Comment");
			for (ProblemTick tick : problem.getTicks()) {
				addTableCell(table, FONT_REGULAR, tick.getDate());
				Phrase grade = new Phrase(tick.getSuggestedGrade(), FONT_REGULAR);
				appendStarIcons(grade, tick.getStars(), true);
				table.addCell(new PdfPCell(grade));
				addTableCell(table, FONT_REGULAR, tick.getName());
				addTableCell(table, FONT_REGULAR, tick.getComment());
			}
			document.add(table);
		}

		// Comments
		if (problem.getComments() != null && !problem.getComments().isEmpty()) {
			document.add(new Paragraph(" "));
			PdfPTable table = new PdfPTable(new float[] {1, 1, 4});
			table.setWidthPercentage(100);
			addTableCell(table, FONT_BOLD, "When");
			addTableCell(table, FONT_BOLD, "Name");
			addTableCell(table, FONT_BOLD, "Message");
			for (ProblemComment comment : problem.getComments()) {
				addTableCell(table, FONT_REGULAR, comment.getDate());
				addTableCell(table, FONT_REGULAR, comment.getName());
				String url = isValidUrl(comment.getMessage())? comment.getMessage() : null;
				addTableCell(table, url != null? FONT_REGULAR_LINK : FONT_REGULAR, comment.getMessage(), url, false);
			}
			document.add(table);
		}

		// Media
		List<Media> media = Lists.newArrayList();
		if (area.getMedia() != null) {
			media.addAll(area.getMedia().stream()
					.filter(m -> m.idType() == 1)
					.toList());
		}
		if (problem.getMedia() != null) {
			media.addAll(problem.getMedia().stream()
					.filter(m -> m.idType() == 1)
					.toList());
		}
		if (problem.getSections() != null) {
			for (ProblemSection s : problem.getSections()) {
				if (s.media() != null) {
					media.addAll(s.media().stream()
							.filter(m -> m.idType() == 1)
							.toList());
				}
			}
		}
		if (!media.isEmpty()) {
			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			for (Media m : media) {
				List<Svg> svgs = m.svgs() == null? null : m.svgs().stream().filter(x -> x.problemId() == problem.getId()).collect(Collectors.toList());
				String txt = m.pitch() > 0? "Pitch " + m.pitch() : null;
				if (!Strings.isNullOrEmpty(m.mediaMetadata().description())) {
					if (txt != null) {
						txt += " - " + m.mediaMetadata().description();
					}
					else {
						txt = m.mediaMetadata().description();
					}
				}
				writeMediaCell(table, 1, m.id(), m.width(), m.height(), txt, m.mediaSvgs(), svgs);
			}
			if (media.size() > 1 && media.size() % 2 == 1) {
				addDummyCell(table);
			}
			document.add(table);
		}
	}

	/**
	 * Needed if colspan=2 and written cells is an odd number
	 */
	private void addDummyCell(PdfPTable table) {
		PdfPCell dummyCell = new PdfPCell();
		dummyCell.setBorder(0);
		table.addCell(dummyCell); // Add dummy, last image is not visible without this
	}

	private void addMetaData(String title) {
		document.addTitle(title);
		document.addSubject(title);
		document.addKeywords(title);
		document.addAuthor("Jostein Oeygarden (buldreinfo.com / brattelinjer.no");
		document.addCreator("Jostein Oeygarden (buldreinfo.com / brattelinjer.no");
	}

	private void addTableCell(PdfPTable table, Font font, String str) {
		addTableCell(table, font, str, null, false);
	}

	private void addTableCell(PdfPTable table, Font font, String str, String url, boolean greenBackground) {
		PdfPCell cell = new PdfPCell(new Phrase(str, font));
		if (url != null) {
			cell.setCellEvent(new LinkInCell(url));
		}
		if (greenBackground) {
			cell.setBackgroundColor(Color.GREEN);
		}
		table.addCell(cell);
	} 

	private void appendStarIcons(Phrase phrase, double stars, boolean includeNoRating) throws BadElementException, MalformedURLException, IOException {
		if (includeNoRating && stars == -1) {
			phrase.add("No rating");
		}
		else if (stars == 0) {
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
		}
		else if (stars == 0.5) {
			phrase.add(new Chunk(getImageStarHalf(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
		}
		else if (stars == 1) {
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
		}
		else if (stars == 1.5) {
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarHalf(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
		}
		else if (stars == 2) {
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarEmpty(), 0, 0));
		}
		else if (stars == 2.5) {
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarHalf(), 0, 0));
		}
		else if (stars == 3) {
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
			phrase.add(new Chunk(getImageStarFilled(), 0, 0));
		}
	}

	private String convertFromApproachToPolyline(List<Coordinates> approach) {
		return approach.stream().map(a -> a.getLatitude() + "," + a.getLongitude()).collect(Collectors.joining(";"));
	}

	private String getDistance(Slope a) {
		long meter = a.distance();
		if (meter > 1000) {
			return meter/1000 + " km";
		}
		return meter + " meter";
	}

	private Image getImageStarEmpty() throws BadElementException, MalformedURLException, IOException {
		if (imageStarEmpty == null) {
			imageStarEmpty = Image.getInstance(PdfGenerator.class.getResource("star.png"));
			imageStarEmpty.scaleAbsolute(IMAGE_STAR_SIZE, IMAGE_STAR_SIZE);
		}
		return imageStarEmpty;
	}

	private Image getImageStarFilled() throws BadElementException, MalformedURLException, IOException {
		if (imageStarFilled == null) {
			imageStarFilled = Image.getInstance(PdfGenerator.class.getResource("filled-star.png"));
			imageStarFilled.scaleAbsolute(IMAGE_STAR_SIZE, IMAGE_STAR_SIZE);
		}
		return imageStarFilled;
	}

	private Image getImageStarHalf() throws BadElementException, MalformedURLException, IOException {
		if (imageStarHalf == null) {
			imageStarHalf = Image.getInstance(PdfGenerator.class.getResource("star-half-empty.png"));
			imageStarHalf.scaleAbsolute(IMAGE_STAR_SIZE, IMAGE_STAR_SIZE);
		}
		return imageStarHalf;
	}

	private boolean isValidUrl(String url)  {
		/* Try creating a valid URL */
		try { 
			URI.create(url); 
			return true; 
		} catch (Exception e) { 
			logger.debug(e.getMessage(), e);
			return false; 
		} 
	}

	private String removeIllegalChars(String name) {
		if (name != null) {
			return name.replaceAll("[^ÆØÅæøåa-zA-Z0-9]", " ");
		}
		return name;
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
				String distance = null;
				if (sector.getApproach() != null && sector.getApproach().coordinates() != null && !sector.getApproach().coordinates().isEmpty()) {
					var polyline = convertFromApproachToPolyline(sector.getApproach().coordinates());
					slopes.add(new PrintSlope(polyline, getDistance(sector.getApproach()), "lime"));
				}
				if (sector.getDescent() != null && sector.getDescent().coordinates() != null && !sector.getDescent().coordinates().isEmpty()) {
					var polyline = convertFromApproachToPolyline(sector.getDescent().coordinates());
					slopes.add(new PrintSlope(polyline, getDistance(sector.getDescent()), "purple"));
				}
				if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
					final String name = removeIllegalChars(sector.getName()) + (!Strings.isNullOrEmpty(distance)? " (" + distance + ")" : "");
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
			String distance = null;
			if (sector.getApproach() != null && sector.getApproach().coordinates() != null && !sector.getApproach().coordinates().isEmpty()) {
				var polyline = convertFromApproachToPolyline(sector.getApproach().coordinates());
				slopes.add(new PrintSlope(polyline, getDistance(sector.getApproach()), "lime"));
			}
			if (sector.getDescent() != null && sector.getDescent().coordinates() != null && !sector.getDescent().coordinates().isEmpty()) {
				var polyline = convertFromApproachToPolyline(sector.getDescent().coordinates());
				slopes.add(new PrintSlope(polyline, getDistance(sector.getDescent()), "purple"));
			}
			if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
				String label = removeIllegalChars(sector.getName()) + (!Strings.isNullOrEmpty(distance)? " (" + distance + ")" : "");
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
				String distance = null;
				if (sector.getApproach() != null && sector.getApproach().coordinates() != null && !sector.getApproach().coordinates().isEmpty()) {
					var polyline = convertFromApproachToPolyline(sector.getApproach().coordinates());
					slopes.add(new PrintSlope(polyline, getDistance(sector.getApproach()), "lime"));
				}
				if (sector.getDescent() != null && sector.getDescent().coordinates() != null && !sector.getDescent().coordinates().isEmpty()) {
					var polyline = convertFromApproachToPolyline(sector.getDescent().coordinates());
					slopes.add(new PrintSlope(polyline, getDistance(sector.getDescent()), "purple"));
				}
				if (sector.getOutline() != null && !sector.getOutline().isEmpty()) {
					final String label = removeIllegalChars(sector.getName()) + (!Strings.isNullOrEmpty(distance)? " (" + distance + ")" : "");
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

	private void writeMediaCell(PdfPTable table, int colSpan, int mediaId, int width, int height, String txt, List<MediaSvgElement> mediaSvgs, List<Svg> svgs) throws MalformedURLException, IOException, DocumentException, TranscoderException, TransformerException {
		Image img = null;
		if ((mediaSvgs == null || mediaSvgs.isEmpty()) && (svgs == null || svgs.isEmpty())) {
			if (mediaIdProcessed.add(mediaId)) {
				URL url = URI.create(StorageManager.getPublicUrl(S3KeyGenerator.getWebJpg(mediaId), 0l)).toURL();
				img = Image.getInstance(url);
			}
		}
		else {
			byte[] topo = TopoGenerator.generateTopo(mediaId, width, height, mediaSvgs, svgs);
			img = Image.getInstance(topo);
		}
		if (img != null) {
			PdfPCell cell = new PdfPCell(img, true);
			if (!Strings.isNullOrEmpty(txt)) {
				cell.setCellEvent(new WatermarkedCell(txt));
			}
			cell.setColspan(colSpan);
			cell.setBorder(0);
			table.addCell(cell);
		}
	}

	private void writeSectors(Meta meta, List<Sector> sectors) throws DocumentException, IOException, TranscoderException, TransformerException {
		for (Sector s : sectors) {
			final boolean showType = meta.isClimbing();
			document.newPage();
			new PdfOutline(writer.getRootOutline(), new PdfDestination(PdfDestination.FITH, writer.getVerticalPosition(true)), s.getName(), true);
			document.add(new Paragraph(s.getName(), FONT_H2));
			if (!Strings.isNullOrEmpty(s.getAccessInfo())) {
				document.add(new Phrase(s.getAccessInfo(), FONT_BOLD));
			}
			if (!Strings.isNullOrEmpty(s.getComment())) {
				document.add(new Phrase(s.getComment(), FONT_REGULAR));
			}
			writeMapSector(s);
			// Table
			float[] relativeWidths = showType? new float[]{1, 4, 2, 2, 4, 7} : new float[]{1, 3, 1, 3, 8};
			PdfPTable table = new PdfPTable(relativeWidths);
			table.setWidthPercentage(100);
			addTableCell(table, FONT_BOLD, "#");
			addTableCell(table, FONT_BOLD, "Name");
			addTableCell(table, FONT_BOLD, "Grade");
			if (showType) {
				addTableCell(table, FONT_BOLD, "Type");
			}
			addTableCell(table, FONT_BOLD, "FA");
			addTableCell(table, FONT_BOLD, "Note");
			for (SectorProblem p : s.getProblems()) {
				String description = Strings.emptyToNull(p.comment());
				if (!Strings.isNullOrEmpty(p.rock())) {
					if (description == null) {
						description = "Rock: " + p.rock();
					}
					else {
						description = "Rock: " + p.rock() + ". " + description;
					}
				}
				if (!Strings.isNullOrEmpty(p.broken())) {
					if (description == null) {
						description = p.broken();
					}
					else {
						description = p.broken() + ". " + description;
					}
				}
				addTableCell(table, FONT_REGULAR, String.valueOf(p.nr()), null, p.ticked());
				String url = meta.url() + "/problem/" + p.id();
				addTableCell(table, FONT_REGULAR_LINK, p.name(), url, p.ticked());
				addTableCell(table, FONT_REGULAR, p.grade(), null, p.ticked());
				if (showType) {
					addTableCell(table, FONT_REGULAR, p.t().subType(), null, p.ticked());
				}
				addTableCell(table, FONT_REGULAR, p.fa(), null, p.ticked());
				Phrase note = new Phrase();
				if (p.numTicks() > 0) {
					appendStarIcons(note, p.stars(), false);
					note.add(new Chunk(" " + p.numTicks() + " ascent" + (p.numTicks()==1? "" : "s"), FONT_REGULAR));
				}
				if (description != null) {
					note.add(new Chunk((p.numTicks() > 0? " - " : "") + description, FONT_ITALIC));
				}
				PdfPCell cell = new PdfPCell(note);
				if (p.ticked()) {
					cell.setBackgroundColor(Color.GREEN);
				}
				table.addCell(cell);
			}
			document.add(new Paragraph(" "));
			document.add(table);
			if (s.getMedia() != null) {
				int columns = 1;
				if (s.getMedia().stream().filter(m -> m.svgs() != null && m.svgs().size() > 5).findAny().isPresent()) {
					columns = 2;
				}
				table = new PdfPTable(columns);
				table.setWidthPercentage(100);
				for (Media m : s.getMedia()) {
					writeMediaCell(table, 1, m.id(), m.width(), m.height(), m.mediaMetadata().description(), m.mediaSvgs(), m.svgs());
				}
				if (columns == 2 && s.getMedia().size() % 2 == 1) {
					addDummyCell(table);
				}
				document.add(table);
			}
		}
	}
}
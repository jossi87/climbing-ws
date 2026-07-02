package com.buldreinfo.pdf;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfAction;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfOutline;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPCellEvent;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.leafletprint.beans.IconType;
import com.buldreinfo.leafletprint.beans.Leaflet;
import com.buldreinfo.leafletprint.beans.Marker;
import com.buldreinfo.leafletprint.beans.Outline;
import com.buldreinfo.leafletprint.beans.PrintSlope;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.GradeDistribution;
import com.buldreinfo.model.LatLng;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.MediaSvgElement;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.ProblemComment;
import com.buldreinfo.model.ProblemSection;
import com.buldreinfo.model.ProblemTick;
import com.buldreinfo.model.Sector;
import com.buldreinfo.model.Sector.SectorProblem;
import com.buldreinfo.model.Svg;
import com.buldreinfo.model.Tick.TickRepeat;
import com.buldreinfo.model.User;
import com.buldreinfo.service.LeafletPrintService;
import com.buldreinfo.util.FilenameUtil;

public class PdfGenerator implements AutoCloseable {

	private class PageHeaderFooter extends PdfPageEventHelper {
		private BaseFont baseFont;
		private final String copyrightText = "\u00A9 buldreinfo.com & brattelinjer.no";
		private String headerText = "";
		private PdfTemplate totalPages;

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
	private static final Font FONT_BOLD = new Font(Font.HELVETICA, 8, Font.BOLD);
	private static final Font FONT_H1 = new Font(Font.HELVETICA, 16, Font.BOLD);
	private static final Font FONT_H2 = new Font(Font.HELVETICA, 11, Font.BOLD);
	private static final Font FONT_ITALIC = new Font(Font.HELVETICA, 9, Font.ITALIC);
	private static final Font FONT_LINK = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLUE);
	private static final Font FONT_REG = new Font(Font.HELVETICA, 8, Font.NORMAL);
	private static final Font FONT_REG_LINK = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLUE);
	private static final Font FONT_SMALL = new Font(Font.HELVETICA, 5, Font.ITALIC);
	private final static int IMAGE_STAR_SIZE = 7;

	private static final Logger logger = LogManager.getLogger();
	private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
	private final Document document;
	private final Set<Integer> mediaIdProcessed = new HashSet<>();
	private final PageHeaderFooter pageEvent;
	private final Map<String, byte[]> preRenderedPitchAssets = new HashMap<>();
	private final StorageManager storage;
	private final LeafletPrintService leafletPrintService;

	private BaseFont watermarkFont;

	private final PdfWriter writer;

	public PdfGenerator(StorageManager storage, LeafletPrintService leafletPrintService, OutputStream output) {
		this.storage = storage;
		this.leafletPrintService = leafletPrintService;
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
	public void close() {
		document.close();
	}

	public void writeArea(Setup setup, Area area, Collection<GradeDistribution> gradeDistribution, List<Sector> sectors) {
		try {
			mediaIdProcessed.clear();
			pageEvent.setHeaderText(area.name());
			document.add(new Paragraph(area.name(), FONT_H1));
			writeMapArea(area, sectors);

			PdfPTable info = new PdfPTable(new float[]{1.5f, 8.5f});
			info.setWidthPercentage(100);
			info.setSpacingBefore(15f);
			info.setSpacingAfter(15f);

			info.addCell(createKeyCell("Generated"));
			info.addCell(createValueCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));

			String areaUrl = setup.url() + "/area/" + area.id();
			info.addCell(createKeyCell("URL"));
			Anchor anchor = new Anchor(areaUrl, FONT_LINK);
			anchor.setReference(areaUrl);
			PdfPCell urlCell = new PdfPCell(anchor);
			urlCell.setBorder(Rectangle.BOTTOM);
			urlCell.setBorderColor(Color.LIGHT_GRAY);
			urlCell.setPadding(6f);
			urlCell.setVerticalAlignment(Element.ALIGN_TOP);
			info.addCell(urlCell);

			if ((area.accessClosed() != null && !area.accessClosed().isBlank()) || (area.accessInfo() != null && !area.accessInfo().isBlank()) || (area.comment() != null && !area.comment().isBlank())) {
				StringBuilder areaInfo = new StringBuilder();
				if (area.accessClosed() != null && !area.accessClosed().isBlank()) {
					areaInfo.append("Access closed: ").append(area.accessClosed());
				}
				if (area.accessInfo() != null && !area.accessInfo().isBlank()) {
					if (areaInfo.length() > 0) {
						areaInfo.append("\n");
					}
					areaInfo.append("Access info: ").append(area.accessInfo());
				}
				if (area.comment() != null && !area.comment().isBlank()) {
					if (areaInfo.length() > 0) {
						areaInfo.append("\n");
					}
					areaInfo.append(area.comment());
				}
				info.addCell(createKeyCell("Description"));
				info.addCell(createValueCell(areaInfo.toString()));
			}

			if (gradeDistribution != null && !gradeDistribution.isEmpty()) {
				info.addCell(createKeyCell("Grade Dist."));
				PdfTemplate template = getChartTemplate(gradeDistribution, writer.getDirectContent(), 400f, 150f);
				PdfPCell chartCell = new PdfPCell(Image.getInstance(template));
				chartCell.setBorder(Rectangle.BOTTOM);
				chartCell.setBorderColor(Color.LIGHT_GRAY);
				chartCell.setPadding(6f);
				info.addCell(chartCell);
			}

			if (info.getRows().size() > 0) {
				document.add(info);
			}

			if (area.media() != null && !area.media().isEmpty()) {
				writeMediaGrid(area.media(), 0, 3, 600);
			}

			writeSectors(setup, sectors);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeProblem(Setup setup, Area area, Sector sector, Problem problem) {
		try {
			mediaIdProcessed.clear();

			String headerTitle = String.format("%s / %s / #%d %s [%s]", area.name(), sector.name(), problem.nr(), problem.name(), problem.grade());
			pageEvent.setHeaderText(headerTitle);

			document.add(new Paragraph(headerTitle, FONT_H1));
			writeMapProblem(area, sector, problem);

			PdfPTable info = new PdfPTable(new float[]{1.5f, 8.5f});
			info.setWidthPercentage(100);
			info.setSpacingBefore(15f);
			info.setSpacingAfter(15f);

			info.addCell(createKeyCell("Generated"));
			info.addCell(createValueCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));

			String problemUrl = setup.url() + "/problem/" + problem.id();
			info.addCell(createKeyCell("URL"));
			Anchor anchor = new Anchor(problemUrl, FONT_LINK);
			anchor.setReference(problemUrl);
			PdfPCell urlCell = new PdfPCell(anchor);
			urlCell.setBorder(Rectangle.BOTTOM);
			urlCell.setBorderColor(Color.LIGHT_GRAY);
			urlCell.setPadding(6f);
			urlCell.setVerticalAlignment(Element.ALIGN_TOP);
			info.addCell(urlCell);

			StringBuilder areaInfo = new StringBuilder();
			if (area.accessClosed() != null && !area.accessClosed().isEmpty()) {
				areaInfo.append("Access closed: ").append(area.accessClosed());
			}
			if (area.accessInfo() != null && !area.accessInfo().isEmpty()) {
				if (areaInfo.length() > 0) areaInfo.append("\n");
				areaInfo.append("Access info: ").append(area.accessInfo());
			}
			if (area.comment() != null && !area.comment().isEmpty()) {
				if (areaInfo.length() > 0) areaInfo.append("\n");
				areaInfo.append(area.comment());
			}
			if (!areaInfo.isEmpty()) {
				info.addCell(createKeyCell("Area"));
				info.addCell(createValueCell(areaInfo.toString()));
			}

			StringBuilder sectorInfo = new StringBuilder();
			if (sector.accessClosed() != null && !sector.accessClosed().isEmpty()) {
				sectorInfo.append("Access closed: ").append(sector.accessClosed());
			}
			if (sector.accessInfo() != null && !sector.accessInfo().isEmpty()) {
				if (sectorInfo.length() > 0) sectorInfo.append("\n");
				sectorInfo.append("Access info: ").append(sector.accessInfo());
			}
			if (sector.comment() != null && !sector.comment().isEmpty()) {
				if (sectorInfo.length() > 0) sectorInfo.append("\n");
				sectorInfo.append(sector.comment());
			}
			if (!sectorInfo.isEmpty()) {
				info.addCell(createKeyCell("Sector"));
				info.addCell(createValueCell(sectorInfo.toString()));
			}

			if (problem.faAid() != null) {
				String aide = problem.faAid().users().stream().map(User::name).collect(Collectors.joining(", "));
				info.addCell(createKeyCell("FA (Aid)"));
				info.addCell(createValueCell(aide + " (" + problem.faAid().dateHr() + ")"));
			}

			String fa = problem.fa().stream().map(User::name).collect(Collectors.joining(", "));
			if (!fa.isEmpty()) {
				info.addCell(createKeyCell("First Ascent"));
				info.addCell(createValueCell(fa + " (" + problem.faDateHr() + ")"));
			}

			if (problem.comment() != null && !problem.comment().isEmpty()) {
				info.addCell(createKeyCell("Description"));
				info.addCell(createValueCell(problem.comment()));
			}
			if (problem.lengthMeter() != 0) {
				info.addCell(createKeyCell("Length"));
				info.addCell(createValueCell(problem.lengthMeter() + "m"));
			}
			document.add(info);

			List<Media> combinedMedia = new ArrayList<>();
			if (area.media() != null) combinedMedia.addAll(area.media());
			if (sector.media() != null) combinedMedia.addAll(sector.media());
			if (sector.trails() != null) {
				for (var trail : sector.trails()) {
					if (trail.media() != null) {
						combinedMedia.addAll(trail.media());
					}
				}
			}
			if (problem.media() != null) {
				for (Media m : problem.media()) {
					boolean isPitchMedia = m.svgs() != null && m.svgs().stream().anyMatch(s -> s.problemId() == problem.id() && s.pitch() > 0);
					if (!isPitchMedia) {
						combinedMedia.add(m);
					}
				}
			}
			writeMediaGrid(combinedMedia, problem.id(), 3, 600);

			if (problem.sections() != null && !problem.sections().isEmpty()) {
				PdfOutline rootOutline = writer.getRootOutline();
				List<CompletableFuture<Void>> preFlightFutures = new ArrayList<>();
				for (ProblemSection section : problem.sections()) {
					for (Media m : problem.media()) {
						if (m.svgs() == null) continue;
						List<Svg> pitchSvgs = m.svgs().stream()
								.filter(s -> s.problemId() == problem.id() && s.pitch() == section.nr())
								.toList();

						if (!pitchSvgs.isEmpty()) {
							String cacheKey = section.nr() + "_" + m.identity().id();
							preFlightFutures.add(CompletableFuture.supplyAsync(() -> {
								PdfMediaScaler.MediaRegion reg = null;
								try {
									reg = PdfMediaScaler.calculateMediaRegion(pitchSvgs.get(0).path(), m.width(), m.height());
								} catch (Exception e) {
									logger.warn(e.getMessage(), e);
								}
								return safeGenerateTopo(m.identity().id(), m.width(), m.height(), m.mediaSvgs(), m.svgs(), reg, 1200, problem.id(), section.nr());
							}).thenAccept(bytes -> {
								if (bytes != null) {
									synchronized(preRenderedPitchAssets) {
										preRenderedPitchAssets.put(cacheKey, bytes);
									}
								}
							}));
						}
					}
				}
				try {
					CompletableFuture.allOf(preFlightFutures.toArray(new CompletableFuture[0])).get();
				} catch (Exception e) {
					logger.error("Error during multi-pitch parallel execution pre-flight", e);
				}

				for (ProblemSection section : problem.sections()) {
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

					String destName = "pitch_" + problem.id() + "_" + section.nr();
					Chunk pitchTitle = new Chunk("Pitch " + section.nr() + " (" + section.grade() + "): ", FONT_BOLD);
					pitchTitle.setLocalDestination(destName);
					p.add(pitchTitle);

					p.add(new Chunk(section.description() == null ? "" : section.description(), FONT_REG));
					pitchCell.addElement(p);

					new PdfOutline(rootOutline, PdfAction.gotoLocalPage(destName, false), "Pitch " + section.nr());

					PdfPTable grid = createPitchGrid(problem, section);
					pitchCell.addElement(grid);

					pitchWrapper.addCell(pitchCell);
					document.add(pitchWrapper);
				}
				preRenderedPitchAssets.clear();
			}
			writeTicksAndComments(problem);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
			if (desc != null && !desc.isBlank()) cell.setCellEvent(new WatermarkedCell(desc));
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
		if (text == null || text.isBlank()) {
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

	private void appendStarIcons(Phrase phrase, double stars) throws BadElementException, IOException {
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

	private PdfPTable createPitchGrid(Problem problem, ProblemSection section) throws InterruptedException, ExecutionException {
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

		int[] colCounts = new int[cols];
		int imageIndex = 0;

		for (Media m : problem.media()) {
			if (m.svgs() == null) continue;
			String cacheKey = section.nr() + "_" + m.identity().id();
			byte[] data = preRenderedPitchAssets.get(cacheKey);

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
				if (!mediaIdProcessed.contains(m.identity().id())) {
					toProcess.add(m);

					boolean hasOverlays = (m.mediaSvgs() != null && !m.mediaSvgs().isEmpty()) || (m.svgs() != null && !m.svgs().isEmpty());

					if (hasOverlays) {
						sectionFutures.add(CompletableFuture.supplyAsync(() -> 
						safeGenerateTopo(m.identity().id(), m.width(), m.height(), m.mediaSvgs(), m.svgs(), null, 600, 0, 0)
								));
					} else {
						sectionFutures.add(CompletableFuture.supplyAsync(() -> {
							try {
								String s3Key = S3KeyGenerator.getWebJpg(m.identity().id());
								return storage.downloadBytes(s3Key);
							} catch (Exception e) {
								logger.error("Failed to fetch image via storage manager for media " + m.identity().id(), e);
								return null;
							}
						}));
					}
					mediaIdProcessed.add(m.identity().id());
				}
			}
			for (int i = 0; i < sectionFutures.size(); i++) {
				byte[] data = sectionFutures.get(i).get();
				if (data != null) {
					int targetCol = imageIndex % cols;
					addImageCell(columnTables.get(targetCol), data, toProcess.get(i).description());
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

	private PdfTemplate getChartTemplate(Collection<GradeDistribution> gradeDistribution, PdfContentByte cb, float plotWidth, float plotHeight) throws DocumentException, IOException {
		PdfTemplate template = cb.createTemplate(plotWidth + 40f, plotHeight + 50f);
		if (gradeDistribution == null || gradeDistribution.isEmpty()) return template;
		int maxCount = gradeDistribution.stream().mapToInt(GradeDistribution::getNum).max().orElse(1);
		float stepX = plotWidth / gradeDistribution.size();
		float barWidth = stepX * 0.8f; 
		float spacing = stepX * 0.2f;
		PdfGState transparentState = new PdfGState();
		transparentState.setFillOpacity(0.35f);
		PdfGState opaqueState = new PdfGState();
		opaqueState.setFillOpacity(1.0f);
		BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		template.setFontAndSize(bf, 8);
		int i = 0;
		for (GradeDistribution d : gradeDistribution) {
			float x = 20f + (i * stepX) + (spacing / 2f);
			float hTot = ((float) d.getNum() / maxCount) * plotHeight;
			float hPrim = ((float) d.getPrim() / maxCount) * plotHeight;

			if (d.getNum() > 0) {
				Color c = Color.decode(d.getColor());
				template.setRGBColorFill(c.getRed(), c.getGreen(), c.getBlue());

				template.setGState(transparentState);
				template.rectangle(x, 20f, barWidth, hTot);
				template.fill();
				template.setGState(opaqueState);

				template.rectangle(x, 20f, barWidth, hPrim);
				template.fill();

				template.beginText();
				template.setRGBColorFill(0, 0, 0);
				template.showTextAligned(PdfContentByte.ALIGN_CENTER, String.valueOf(d.getNum()), x + (barWidth / 2f), 20f + hTot + 2f, 0);
				template.endText();
			}

			template.beginText();
			template.setRGBColorFill(0, 0, 0);
			template.showTextAligned(PdfContentByte.ALIGN_CENTER, d.getGrade(), x + (barWidth / 2f), 5f, 0);
			template.endText();
			i++;
		}
		return template;
	}

	private byte[] safeGenerateTopo(int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int targetWidth, int highlightProbId, int highlightPitch) {
		try {
			return TopoGenerator.generateTopo(storage, mediaId, width, height, mediaSvgs, svgs, region, targetWidth, highlightProbId, highlightPitch);
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
			if (area.coordinates() != null && area.coordinates().latitude() > 0 && area.coordinates().longitude() > 0) {
				defaultCenter = new LatLng(area.coordinates().latitude(), area.coordinates().longitude());
			}
			int defaultZoom = 14;

			boolean useLegend = sectors.size()>1;
			List<String> legends = new ArrayList<>();
			for (Sector sector : sectors) {
				if (sector.parking() != null && sector.parking().latitude() > 0 && sector.parking().longitude() > 0) {
					markers.add(new Marker(sector.parking().latitude(), sector.parking().longitude(), IconType.PARKING, null));
				}
				if (sector.trails() != null && !sector.trails().isEmpty()) {
					sector.trails().forEach(t -> slopes.add(PrintSlope.of(t)));
				}
				if (sector.outline() != null && !sector.outline().isEmpty()) {
					final String name = FilenameUtil.sanitize(sector.name());
					String label = null;
					if (useLegend) {
						label = String.valueOf(legends.size() + 1);
						legends.add(label + ": " + name);
					}
					else {
						label = name;
					}
					String polygonCoords = sector.outline().stream().map(o -> o.latitude() + "," + o.longitude()).collect(Collectors.joining(";"));
					outlines.add(new Outline(label, polygonCoords));
				}
			}

			if (!markers.isEmpty() || !outlines.isEmpty() || !slopes.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, legends, defaultCenter, defaultZoom, false);
				Optional<byte[]> optSnapshot = leafletPrintService.takeSnapshot(leaflet);
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
			if (problem.coordinates() != null && problem.coordinates().latitude() > 0 && problem.coordinates().longitude() > 0) {
				defaultCenter = new LatLng(problem.coordinates().latitude(), problem.coordinates().longitude());
			}
			else if (sector.parking() != null && sector.parking().latitude() > 0 && sector.parking().longitude() > 0) {
				defaultCenter = new LatLng(sector.parking().latitude(), sector.parking().longitude());
			}
			else if (area.coordinates() != null && area.coordinates().latitude() > 0 && area.coordinates().longitude() > 0) {
				defaultCenter = new LatLng(area.coordinates().latitude(), area.coordinates().longitude());
			}
			int defaultZoom = 15;

			if (sector.parking() != null && sector.parking().latitude() > 0 && sector.parking().longitude() > 0) {
				markers.add(new Marker(sector.parking().latitude(), sector.parking().longitude(), IconType.PARKING, null));
			}
			if (problem.coordinates() != null && problem.coordinates().latitude() > 0 && problem.coordinates().longitude() > 0) {
				String name = FilenameUtil.sanitize(problem.name());
				markers.add(new Marker(problem.coordinates().latitude(), problem.coordinates().longitude(), IconType.DEFAULT, name));
			}
			if (sector.trails() != null && !sector.trails().isEmpty()) {
				sector.trails().forEach(t -> slopes.add(PrintSlope.of(t)));
			}
			if (sector.outline() != null && !sector.outline().isEmpty()) {
				String label = FilenameUtil.sanitize(sector.name());
				String polygonCoords = sector.outline().stream().map(o -> o.latitude() + "," + o.longitude()).collect(Collectors.joining(";"));
				outlines.add(new Outline(label, polygonCoords));
			}

			if (!markers.isEmpty() || !outlines.isEmpty() || !slopes.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, null, defaultCenter, defaultZoom, false);
				Optional<byte[]> optSnapshot = leafletPrintService.takeSnapshot(leaflet);
				if (optSnapshot.isPresent()) {
					PdfPTable table = new PdfPTable(1);
					table.setWidthPercentage(100);
					Image img = Image.getInstance(optSnapshot.get());
					PdfPCell cell = new PdfPCell(img, true);
					cell.setColspan(table.getNumberOfColumns());
					table.addCell(cell);

					// Also append photo map
					markers = markers.stream().filter(m -> !m.iconType().equals(IconType.PARKING)).toList();
					if (!markers.isEmpty()) {
						outlines.clear();
						slopes.clear();
						leaflet = new Leaflet(markers, outlines, slopes, null, defaultCenter, defaultZoom, true);
						optSnapshot = leafletPrintService.takeSnapshot(leaflet);
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
			if (sector.parking() != null && sector.parking().latitude() > 0 && sector.parking().longitude() > 0) {
				defaultCenter = new LatLng(sector.parking().latitude(), sector.parking().longitude());
			}
			int defaultZoom = 14;
			List<String> legends = new ArrayList<>();

			Map<String, List<SectorProblem>> problemsWithCoordinatesGroupedByRock = new HashMap<>();
			List<SectorProblem> problemsWithoutRock = new ArrayList<>();
			for (SectorProblem p : sector.problems()) {
				if (p.coordinates() != null && p.coordinates().latitude() > 0 && p.coordinates().longitude() > 0) {
					if (p.rock() != null) {
						problemsWithCoordinatesGroupedByRock.computeIfAbsent(p.rock(), _ -> new ArrayList<>()).add(p);
					}
					else {
						problemsWithoutRock.add(p);
					}
				}
			}
			for (String rock : problemsWithCoordinatesGroupedByRock.keySet()) {
				List<SectorProblem> problems = problemsWithCoordinatesGroupedByRock.get(rock);
				LatLng latLng = leafletPrintService.getCenter(problems);
				markers.add(new Marker(latLng.lat(), latLng.lng(), IconType.ROCK, rock));
			}
			for (SectorProblem p : problemsWithoutRock) {
				markers.add(new Marker(p.coordinates().latitude(), p.coordinates().longitude(), IconType.DEFAULT, String.valueOf(p.nr())));
			}
			if (markers.size() >= 1 && markers.size() <= 3) {
				if (sector.parking() != null && sector.parking().latitude() > 0 && sector.parking().longitude() > 0) {
					markers.add(new Marker(sector.parking().latitude(), sector.parking().longitude(), IconType.PARKING, null));
				}
				if (sector.trails() != null && !sector.trails().isEmpty()) {
					sector.trails().forEach(t -> slopes.add(PrintSlope.of(t)));
				}
				if (sector.outline() != null && !sector.outline().isEmpty()) {
					final String label = FilenameUtil.sanitize(sector.name());
					String polygonCoords = sector.outline().stream()
						    .map(o -> o.latitude() + "," + o.longitude())
						    .collect(Collectors.joining(";"));
					outlines.add(new Outline(label, polygonCoords));
				}
			}

			if (!markers.isEmpty()) {
				Leaflet leaflet = new Leaflet(markers, outlines, slopes, legends, defaultCenter, defaultZoom, true);
				Optional<byte[]> optSnapshot = leafletPrintService.takeSnapshot(leaflet);
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

	private void writeMediaGrid(List<Media> media, int probId, int cols, int targetWidth) throws InterruptedException, ExecutionException, BadElementException, IOException {
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
			if (mediaIdProcessed.contains(m.identity().id())) continue;
			toProcess.add(m);
			List<Svg> svgs = m.svgs() != null ? m.svgs().stream().filter(s -> probId <= 0 || s.problemId() == probId).toList() : null;
			boolean hasOverlays = (svgs != null && !svgs.isEmpty()) || (m.mediaSvgs() != null && !m.mediaSvgs().isEmpty());
			if (hasOverlays) {
				futures.add(CompletableFuture.supplyAsync(() -> safeGenerateTopo(m.identity().id(), m.width(), m.height(), m.mediaSvgs(), svgs, null, targetWidth, probId, 0)));
			}
			else {
				futures.add(CompletableFuture.supplyAsync(() -> {
					try {
						String s3Key = S3KeyGenerator.getWebJpg(m.identity().id());
						return storage.downloadBytes(s3Key);
					} catch (Exception e) {
						logger.error("Failed to fetch image via storage manager for media " + m.identity().id(), e);
						return null;
					}
				}));
			}
			mediaIdProcessed.add(m.identity().id());
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
					addImageCell(columnTables.get(targetCol), data, m.description());
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

	private void writeSectors(Setup setup, List<Sector> sectors) throws BadElementException, IOException, InterruptedException, ExecutionException {
		for (Sector s : sectors) {
			final boolean showType = s.problems().stream().filter(p -> p.t().subType() != null).findAny().isPresent();
			document.newPage();
			document.add(new Paragraph(s.name(), FONT_H2));
			if (s.accessInfo() != null && !s.accessInfo().isBlank()) {
				document.add(new Phrase(s.accessInfo(), FONT_BOLD));
			}
			if (s.comment() != null && !s.comment().isBlank()) {
				document.add(new Phrase(s.comment(), FONT_REG));
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

			for (SectorProblem p : s.problems()) {
				String description = (p.comment() == null || p.comment().isBlank()) ? null : p.comment();
				if (p.rock() != null && !p.rock().isBlank()) {
					if (description == null) {
						description = "Rock: " + p.rock();
					} else {
						description = "Rock: " + p.rock() + ". " + description;
					}
				}
				if (p.broken() != null && !p.broken().isBlank()) {
					if (description == null) {
						description = p.broken();
					} else {
						description = p.broken() + ". " + description;
					}
				}

				addTableCell(table, FONT_REG, String.valueOf(p.nr()), null, p.ticked());
				String url = setup.url() + "/problem/" + p.id();
				addTableCell(table, FONT_REG_LINK, p.name(), url, p.ticked());
				addTableCell(table, FONT_REG, p.grade(), null, p.ticked());
				if (showType) {
					addTableCell(table, FONT_REG, p.t().subType(), null, p.ticked());
				}
				List<String> parts = new ArrayList<>();

				if (p.faUser() != null && !p.faUser().isEmpty()) {
					String faPart = p.faUser();
					if (p.faYear() > 0) {
						faPart += " (" + p.faYear() + ")";
					}
					parts.add(faPart);
				}

				if (p.ffaUser() != null && !p.ffaUser().isEmpty()) {
					String ffaPart = p.ffaUser();
					if (p.ffaYear() > 0) {
						ffaPart += " (" + p.ffaYear() + ")";
					}

					if (!parts.isEmpty()) {
						parts.add("FFA: " + ffaPart);
					} else {
						parts.add(ffaPart);
					}
				}
				String fa = String.join(", ", parts);
				addTableCell(table, FONT_REG, fa, null, p.ticked());

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

			if (s.media() != null && !s.media().isEmpty()) {
				writeMediaGrid(s.media(), 0, 3, 600);
			}
		}
	}

	private void writeTicksAndComments(Problem p) throws BadElementException, IOException {
		if (p.ticks() != null && !p.ticks().isEmpty()) {
			document.add(new Paragraph("Ascents", FONT_H2));
			PdfPTable table = new PdfPTable(new float[]{1.0f, 1.3f, 4.5f});
			table.setWidthPercentage(100);
			table.setSpacingBefore(5f);

			for (ProblemTick t : p.ticks()) {
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
				if (t.getComment() != null && !t.getComment().isBlank()) {
					pStars.add(new Chunk(" ", FONT_REG));
					addTextWithLinks(pStars, t.getComment(), FONT_REG);
				}

				if (t.getRepeats() != null) {
					for (TickRepeat r : t.getRepeats()) {
						pStars.add(new Chunk("\n \u2022 " + r.date() + ": ", FONT_SMALL));
						if (r.comment() != null && !r.comment().isBlank()) {
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

		if (p.comments() != null && !p.comments().isEmpty()) {
			document.add(new Paragraph("Comments", FONT_H2));
			PdfPTable cTable = new PdfPTable(new float[]{1.0f, 1.3f, 4.5f});
			cTable.setWidthPercentage(100);
			cTable.setSpacingBefore(5f);

			for (ProblemComment c : p.comments()) {
				PdfPCell dateCell = new PdfPCell(new Phrase(c.date(), FONT_REG));
				dateCell.setPadding(4f);
				dateCell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(dateCell);

				PdfPCell nameCell = new PdfPCell(new Phrase(c.name(), FONT_REG));
				nameCell.setPadding(4f);
				nameCell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(nameCell);

				Paragraph para = new Paragraph(8);
				addTextWithLinks(para, c.message(), FONT_REG);
				PdfPCell cell = new PdfPCell(para);
				cell.setPadding(4f);
				cell.setBorderColor(Color.LIGHT_GRAY);
				cTable.addCell(cell);
			}
			document.add(cTable);
		}
	}
}
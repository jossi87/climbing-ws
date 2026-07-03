package com.buldreinfo.pdf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buldreinfo.beans.S3KeyGenerator;
import com.buldreinfo.io.StorageManager;
import com.buldreinfo.model.MediaSvgElement;
import com.buldreinfo.model.Svg;

public class TopoGenerator {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final static Pattern COORD_PATTERN = Pattern.compile("(-?\\d+\\.?\\d*)\\s*,?\\s*(-?\\d+\\.?\\d*)");
	private final static Pattern PATH_TOKEN_PATTERN = Pattern.compile("([a-zA-Z])|([-+]?(?:\\d*\\.\\d+|\\d+)(?:[eE][-+]?\\d+)?)");

	private static Stroke createStroke(double width, String dash) {
		float[] dashArray = null;
		if (dash != null) {
			dashArray = new float[]{Float.parseFloat(dash)};
		}
		return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashArray, 0.0f);
	}

	private static void drawAnchor(Graphics2D g2d, double scale, float x, float y, String colorHex, boolean isHighlight) {
		double opacity = isHighlight ? 1.0 : 0.5;
		double r = 9 * scale * (isHighlight ? 1.2 : 0.6);

		Ellipse2D.Double circle = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);

		g2d.setColor(getColor(colorHex, opacity));
		g2d.fill(circle);

		g2d.setStroke(new BasicStroke((float) (2.5 * scale)));
		g2d.setColor(getColor("#000000", opacity));
		g2d.draw(circle);
	}

	private static void drawReactHaloPath(Graphics2D g2d, double scale, String d, String colorHex, boolean isHighlight, String dash) {
		Path2D.Double path = parsePath(d);
		double opacity = isHighlight ? 1.0 : 0.5;

		g2d.setStroke(createStroke(8 * scale, null));
		g2d.setColor(getColor("#FFFFFF", 0.4 * opacity));
		g2d.draw(path);

		g2d.setStroke(createStroke(5 * scale, dash));
		g2d.setColor(getColor("#000000", opacity));
		g2d.draw(path);

		double coreWidth = 2.5 * scale * (isHighlight ? 1.4 : 0.9);
		g2d.setStroke(createStroke(coreWidth, dash));
		g2d.setColor(getColor(colorHex, opacity));
		g2d.draw(path);
	}

	private static void drawText(Graphics2D g2d, double scale, float x, float y, String txt, boolean isHighlight, boolean ticked, boolean todo, boolean dangerous) {
		double opacity = isHighlight ? 1.0 : 0.5;
		double fontSize = (isHighlight ? 26 : 16) * scale;
		double haloWidth = (isHighlight ? 5 : 2) * scale;

		String textColor = "#FFFFFF"; 
		if (ticked) textColor = "#21ba45"; 
		else if (todo) textColor = "#659DBD"; 
		else if (dangerous) textColor = "#FF0000"; 

		Font font = new Font("Arial", Font.BOLD, (int) fontSize);
		g2d.setFont(font);

		FontRenderContext frc = g2d.getFontRenderContext();
		GlyphVector gv = font.createGlyphVector(frc, txt);
		Rectangle2D bounds = gv.getVisualBounds();

		float drawX = x - (float) (bounds.getWidth() / 2.0) - (float) bounds.getX();
		float drawY = y - (float) (bounds.getHeight() / 2.0) - (float) bounds.getY();

		Shape textShape = gv.getOutline(drawX, drawY);

		g2d.setStroke(new BasicStroke((float) haloWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setColor(getColor("#000000", opacity));
		g2d.draw(textShape);

		g2d.setColor(getColor(textColor, opacity));
		g2d.fill(textShape);
	}

	private static Color getColor(String hex, double opacity) {
		Color c;
		try {
			c = Color.decode(hex);
		} catch (NumberFormatException e) {
			logger.warn(e.getMessage(), e);
			c = Color.BLACK;
		}
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * opacity));
	}

	private static Path2D.Double parsePath(String d) {
		Path2D.Double path = new Path2D.Double();
		Matcher m = PATH_TOKEN_PATTERN.matcher(d);
		char cmd = 'M';
		List<Double> args = new ArrayList<>();
		double cx = 0, cy = 0;

		while (m.find()) {
			String token = m.group();
			if (Character.isLetter(token.charAt(0))) {
				if (!args.isEmpty()) {
					processCmd(path, cmd, args, cx, cy);
					if (path.getCurrentPoint() != null) { cx = path.getCurrentPoint().getX(); cy = path.getCurrentPoint().getY(); }
					args.clear();
				}
				cmd = token.charAt(0);
				if (Character.toLowerCase(cmd) == 'z') {
					path.closePath();
				}
			} else {
				args.add(Double.parseDouble(token));
				int req = reqArgs(cmd);
				if (req > 0 && args.size() >= req) {
					processCmd(path, cmd, args.subList(0, req), cx, cy);
					if (path.getCurrentPoint() != null) { cx = path.getCurrentPoint().getX(); cy = path.getCurrentPoint().getY(); }
					args.subList(0, req).clear();
					if (cmd == 'M') cmd = 'L';
					if (cmd == 'm') cmd = 'l';
				}
			}
		}
		if (!args.isEmpty()) {
			processCmd(path, cmd, args, cx, cy);
		}
		return path;
	}

	private static void processCmd(Path2D.Double path, char cmd, List<Double> args, double cx, double cy) {
		if (args.isEmpty()) return;
		boolean rel = Character.isLowerCase(cmd);
		switch (Character.toLowerCase(cmd)) {
		case 'm' -> path.moveTo(args.get(0) + (rel ? cx : 0), args.get(1) + (rel ? cy : 0));
		case 'l' -> path.lineTo(args.get(0) + (rel ? cx : 0), args.get(1) + (rel ? cy : 0));
		case 'h' -> path.lineTo(args.get(0) + (rel ? cx : 0), cy);
		case 'v' -> path.lineTo(cx, args.get(0) + (rel ? cy : 0));
		case 'c' -> path.curveTo(
				args.get(0) + (rel ? cx : 0), args.get(1) + (rel ? cy : 0),
				args.get(2) + (rel ? cx : 0), args.get(3) + (rel ? cy : 0),
				args.get(4) + (rel ? cx : 0), args.get(5) + (rel ? cy : 0)
				);
		case 'q' -> path.quadTo(
				args.get(0) + (rel ? cx : 0), args.get(1) + (rel ? cy : 0),
				args.get(2) + (rel ? cx : 0), args.get(3) + (rel ? cy : 0)
				);
		}
	}

	private static int reqArgs(char cmd) {
		return switch (Character.toLowerCase(cmd)) {
		case 'm', 'l', 't' -> 2;
		case 'q', 's' -> 4;
		case 'c' -> 6;
		case 'h', 'v' -> 1;
		case 'a' -> 7;
		default -> 0;
		};
	}

	protected static byte[] generateTopo(StorageManager storage, int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int targetRes, int highlightProbId, int highlightPitch) throws IOException {
		int finalWidth = region != null ? region.width() : width;
		float scale = Math.max(1.0f, (float) targetRes / finalWidth);
		int exportWidth = (int) (finalWidth * scale);
		int exportHeight = (int) ((region != null ? region.height() : height) * scale);

		String s3Key = (region != null) 
				? S3KeyGenerator.getWebJpgRegion(mediaId, region.x(), region.y(), region.width(), region.height())
						: S3KeyGenerator.getWebJpg(mediaId);

		byte[] rawImageBytes = storage.downloadBytes(s3Key);
		BufferedImage bgImage = ImageIO.read(new ByteArrayInputStream(rawImageBytes));

		BufferedImage img = new BufferedImage(exportWidth, exportHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.scale(scale, scale);

		int viewW = region != null ? region.width() : width;
		int viewH = region != null ? region.height() : height;

		g2d.drawImage(bgImage, 0, 0, viewW, viewH, null);

		final double elemScale = Math.max(viewW / 1920.0, viewH / 1440.0);

		if (mediaSvgs != null) {
			for (MediaSvgElement mSvg : mediaSvgs) {
				if (mSvg.path() != null) {
					drawReactHaloPath(g2d, elemScale, PdfMediaScaler.scalePath(mSvg.path(), region), "#FFFFFF", false, null);
				}
			}
		}

		if (svgs != null) {
			List<Runnable> textDrawCalls = new ArrayList<>();
			for (Svg svg : svgs.stream()
					.sorted((a, b) -> {
						int cmp = Boolean.compare(b.problemId() == highlightProbId, a.problemId() == highlightProbId);
						if (cmp != 0) return cmp;
						cmp = Boolean.compare(b.pitch() == highlightPitch, a.pitch() == highlightPitch);
						if (cmp != 0) return cmp;
						cmp = Integer.compare(a.nr(), b.nr());
						if (cmp != 0) return cmp;
						return Integer.compare(a.pitch(), b.pitch());
					}).toList()) {

				String d = PdfMediaScaler.scalePath(svg.path(), region);

				float xNr = 0, maxY = -Float.MAX_VALUE; 
				float xAnchor = 0, minY = Float.MAX_VALUE;
				Matcher m = COORD_PATTERN.matcher(d);
				while (m.find()) {
					float cx = Float.parseFloat(m.group(1));
					float cy = Float.parseFloat(m.group(2));
					if (cy > maxY) {
						maxY = cy;
						xNr = cx;
					}
					if (cy < minY) {
						minY = cy;
						xAnchor = cx;
					}
				}

				boolean isTargetProblem = (highlightProbId <= 0) || (svg.problemId() == highlightProbId);
				boolean isTargetPitch = (highlightPitch <= 0) || (svg.pitch() == highlightPitch);
				boolean isHighlight = isTargetProblem && isTargetPitch;

				String lineColor = svg.problemGradeColor();
				String dash = (svg.problemSubtype() == null || svg.problemSubtype().equalsIgnoreCase("bolt")) ? String.valueOf(10 * elemScale) : null;

				drawReactHaloPath(g2d, elemScale, d, lineColor, isHighlight, dash);

				if (svg.hasAnchor()) {
					drawAnchor(g2d, elemScale, xAnchor, minY, lineColor, isHighlight);
				}

				String nrStr = (svg.pitch() != 0) ? svg.nr() + "-" + svg.pitch() : String.valueOf(svg.nr());

				float finalXNr = xNr;
				float finalMaxY = maxY;
				textDrawCalls.add(() -> drawText(g2d, elemScale, finalXNr, finalMaxY, nrStr, isHighlight, svg.ticked(), svg.todo(), svg.dangerous()));
			}

			for (Runnable drawCall : textDrawCalls) {
				drawCall.run();
			}
		}

		g2d.dispose();

		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG ImageWriter found");
		}
		ImageWriter writer = writers.next();

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

			writer.setOutput(ios);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.85f);
			}

			writer.write(null, new IIOImage(img, null, null), param);

			return baos.toByteArray();
		} finally {
			writer.dispose();
		}
	}
}
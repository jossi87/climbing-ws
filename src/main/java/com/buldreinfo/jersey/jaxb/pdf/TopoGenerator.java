package com.buldreinfo.jersey.jaxb.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.google.common.collect.Lists;

public class TopoGenerator {
	private final static String xmlns = "http://www.w3.org/2000/svg";
	private final static String xlinkns = "http://www.w3.org/1999/xlink";
	private final static Pattern COORD_PATTERN = Pattern.compile("(-?\\d+\\.?\\d*)\\s*,?\\s*(-?\\d+\\.?\\d*)");

	protected static byte[] generateTopo(int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int targetRes, int highlightProbId) throws IOException, TranscoderException, TransformerException {
		int finalWidth = region != null ? region.width() : width;
		float scale = Math.max(1.0f, (float)targetRes / finalWidth);
		int exportWidth = (int)(finalWidth * scale);
		int exportHeight = (int)((region != null ? region.height() : height) * scale);
		try (Reader reader = new StringReader(generateDocument(mediaId, width, height, mediaSvgs, svgs, region, highlightProbId))) {
			TranscoderInput ti = new TranscoderInput(reader);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				TranscoderOutput to = new TranscoderOutput(baos);
				JPEGTranscoder t = new JPEGTranscoder();
				t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 0.85f);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float)exportWidth);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float)exportHeight);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, true);
				t.transcode(ti, to);
				return baos.toByteArray();
			}
		}
	}

	private static String generateDocument(int mediaId, int origWidth, int origHeight, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int highlightProbId) throws TransformerException {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		Document doc = impl.createDocument(xmlns, "svg", null);
		Element svgRoot = doc.getDocumentElement();
		int viewW = region != null ? region.width() : origWidth;
		int viewH = region != null ? region.height() : origHeight;
		svgRoot.setAttributeNS(null, "viewBox", "0 0 " + viewW + " " + viewH);
		
		Element image = doc.createElementNS(xmlns, "image");
		String url = (region != null) 
				? StorageManager.getDirectStorageUrl(S3KeyGenerator.getWebJpgRegion(mediaId, region.x(), region.y(), region.width(), region.height()))
				: StorageManager.getDirectStorageUrl(S3KeyGenerator.getWebJpg(mediaId));
		image.setAttributeNS(xlinkns, "xlink:href", url);
		image.setAttributeNS(null, "href", url);
		image.setAttributeNS(null, "width", "100%");
		image.setAttributeNS(null, "height", "100%");
		svgRoot.appendChild(image);

		final double scale = Math.max(viewW / 1920.0, viewH / 1440.0);

		// 1. RESTORED: Draw Media SVGs (Descents/Background)
		if (mediaSvgs != null) {
			for (MediaSvgElement mSvg : mediaSvgs) {
				if (mSvg.path() != null) {
					addEnvironmentPath(doc, scale, svgRoot, PdfMediaScaler.scalePath(mSvg.path(), region));
				}
			}
		}

		// 2. Draw Route SVGs
		if (svgs != null) {
			List<Element> textElements = Lists.newArrayList();
			for (Svg svg : svgs) {
				String d = PdfMediaScaler.scalePath(svg.path(), region);
				
				// Coordinate parsing for minY/maxY
				float xNr = 0, maxY = -Float.MAX_VALUE; 
				float xAnchor = 0, minY = Float.MAX_VALUE;
				Matcher m = COORD_PATTERN.matcher(d);
				while (m.find()) {
					float cx = Float.parseFloat(m.group(1));
					float cy = Float.parseFloat(m.group(2));
					if (cy > maxY) { maxY = cy; xNr = cx; }
					if (cy < minY) { minY = cy; xAnchor = cx; }
				}

				boolean isHighlight = (highlightProbId <= 0) || (svg.problemId() == highlightProbId);
				String groupColor = getGroupColor(svg.problemGradeGroup());
				String dash = svg.primary() ? String.valueOf(10 * scale) : null;
				
				addPath(doc, scale, svgRoot, d, groupColor, isHighlight, dash);

				if (svg.hasAnchor()) {
					addAnchor(doc, scale, svgRoot, xAnchor, minY, groupColor, isHighlight);
				}

				String nrStr = (svg.pitch() != 0) ? svg.nr() + "-" + svg.pitch() : String.valueOf(svg.nr());
				addText(doc, textElements, scale, xNr, maxY, nrStr, isHighlight, svg.ticked(), svg.todo(), svg.dangerous());
			}
			// Text elements drawn last to stay on top
			for (Element t : textElements) svgRoot.appendChild(t);
		}

		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(writer));
		return writer.toString();
	}

	private static void addText(Document doc, List<Element> container, double scale, float x, float y, String txt, boolean isHighlight, boolean ticked, boolean todo, boolean dangerous) {
		double fontSize = (isHighlight ? 25 : 20) * scale;
		double haloWidth = 4 * scale;
		
		String textColor = "#FFFFFF"; 
		if (ticked) textColor = "#21ba45"; 
		else if (todo) textColor = "#659DBD"; 
		else if (dangerous) textColor = "#FF0000"; 

		// Halo (Black outline)
		Element halo = doc.createElementNS(xmlns, "text");
		halo.setAttributeNS(null, "x", String.valueOf(x));
		halo.setAttributeNS(null, "y", String.valueOf(y));
		halo.setAttributeNS(null, "text-anchor", "middle");
		halo.setAttributeNS(null, "dominant-baseline", "central");
		halo.setAttributeNS(null, "font-size", String.valueOf(fontSize));
		halo.setAttributeNS(null, "font-family", "Arial, sans-serif");
		halo.setAttributeNS(null, "font-weight", isHighlight ? "bolder" : "normal");
		halo.setAttributeNS(null, "fill", "none");
		halo.setAttributeNS(null, "stroke", "#000000");
		halo.setAttributeNS(null, "stroke-width", String.valueOf(haloWidth));
		halo.appendChild(doc.createTextNode(txt));
		container.add(halo);

		// Colored Text (On top)
		Element main = doc.createElementNS(xmlns, "text");
		main.setAttributeNS(null, "x", String.valueOf(x));
		main.setAttributeNS(null, "y", String.valueOf(y));
		main.setAttributeNS(null, "text-anchor", "middle");
		main.setAttributeNS(null, "dominant-baseline", "central");
		main.setAttributeNS(null, "font-size", String.valueOf(fontSize));
		main.setAttributeNS(null, "font-family", "Arial, sans-serif");
		main.setAttributeNS(null, "font-weight", isHighlight ? "bolder" : "normal");
		main.setAttributeNS(null, "fill", textColor);
		main.appendChild(doc.createTextNode(txt));
		container.add(main);
	}

	private static void addPath(Document doc, double scale, Element parent, String d, String color, boolean isHighlight, String dash) {
		Element p1 = doc.createElementNS(xmlns, "path");
		p1.setAttributeNS(null, "d", d);
		p1.setAttributeNS(null, "style", "fill:none; stroke:black; stroke-linecap:round; stroke-width:" + (5 * scale));
		parent.appendChild(p1);

		Element p2 = doc.createElementNS(xmlns, "path");
		p2.setAttributeNS(null, "d", d);
		String s = "fill:none; stroke-linecap:round; stroke:" + color + "; stroke-width:" + (2 * scale * (isHighlight ? 1.2 : 1.0));
		if (dash != null) s += "; stroke-dasharray:" + dash;
		p2.setAttributeNS(null, "style", s);
		parent.appendChild(p2);
	}

	private static void addEnvironmentPath(Document doc, double scale, Element parent, String d) {
		Element p = doc.createElementNS(xmlns, "path");
		p.setAttributeNS(null, "d", d);
		p.setAttributeNS(null, "style", "fill:none; stroke:white; stroke-linecap:round; stroke-width:" + (2 * scale));
		parent.appendChild(p);
	}

	private static void addAnchor(Document doc, double scale, Element parent, float x, float y, String color, boolean isHighlight) {
		double r = 8 * scale * (isHighlight ? 1.2 : 1.0);
		Element c = doc.createElementNS(xmlns, "circle");
		c.setAttributeNS(null, "cx", String.valueOf(x));
		c.setAttributeNS(null, "cy", String.valueOf(y));
		c.setAttributeNS(null, "r", String.valueOf(r));
		c.setAttributeNS(null, "fill", color);
		c.setAttributeNS(null, "stroke", "black");
		c.setAttributeNS(null, "stroke-width", String.valueOf(2 * scale));
		parent.appendChild(c);
	}

	private static String getGroupColor(int group) {
		return switch (group) {
			case 1 -> "#00FF00";
			case 2 -> "#0000FF";
			case 3 -> "#FFFF00";
			case 4 -> "#FF0000";
			case 5 -> "#FF00FF";
			default -> "#FFFFFF";
		};
	}
}
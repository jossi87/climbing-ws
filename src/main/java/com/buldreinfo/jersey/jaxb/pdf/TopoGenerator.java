package com.buldreinfo.jersey.jaxb.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

public class TopoGenerator {
	private static final Logger logger = LogManager.getLogger();
	private final static String xmlns = "http://www.w3.org/2000/svg";
	private final static String xlinkns = "http://www.w3.org/1999/xlink";
	private final static Pattern COORD_PATTERN = Pattern.compile("(-?\\d+\\.?\\d*)\\s*,?\\s*(-?\\d+\\.?\\d*)");

	protected static byte[] generateTopo(StorageManager storage, int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int targetRes, int highlightProbId, int highlightPitch) throws IOException, TranscoderException, TransformerException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		int finalWidth = region != null ? region.width() : width;
		float scale = Math.max(1.0f, (float)targetRes / finalWidth);
		int exportWidth = (int)(finalWidth * scale);
		int exportHeight = (int)((region != null ? region.height() : height) * scale);
		
		String s3Key = (region != null) 
				? S3KeyGenerator.getWebJpgRegion(mediaId, region.x(), region.y(), region.width(), region.height())
				: S3KeyGenerator.getWebJpg(mediaId);
		
		byte[] rawImageBytes = storage.downloadBytes(s3Key);
		String base64Image = Base64.getEncoder().encodeToString(rawImageBytes);
		String dataUri = "data:image/jpeg;base64," + base64Image;

		try (Reader reader = new StringReader(generateDocument(width, height, mediaSvgs, svgs, region, highlightProbId, highlightPitch, dataUri))) {
			TranscoderInput ti = new TranscoderInput(reader);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				TranscoderOutput to = new TranscoderOutput(baos);
				JPEGTranscoder t = new JPEGTranscoder();
				t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 0.85f);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float)exportWidth);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float)exportHeight);
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, false);
				t.transcode(ti, to);
				logger.debug("generateTopo(mediaId={}, width={}, height={}, mediaSvgs.size()={}, svgs.size()={}, region={}, targetRes={}, highlightProbId={}, highlightPitch={}) - duration={}", mediaId, width, height, mediaSvgs == null ? 0 : mediaSvgs.size(), svgs == null ? 0 : svgs.size(), region, targetRes, highlightProbId, highlightPitch, stopwatch);
				return baos.toByteArray();
			}
		}
	}

	private static String generateDocument(int origWidth, int origHeight, List<MediaSvgElement> mediaSvgs, List<Svg> svgs, PdfMediaScaler.MediaRegion region, int highlightProbId, int highlightPitch, String dataUri) throws TransformerException {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		Document doc = impl.createDocument(xmlns, "svg", null);
		Element svgRoot = doc.getDocumentElement();
		int viewW = region != null ? region.width() : origWidth;
		int viewH = region != null ? region.height() : origHeight;
		svgRoot.setAttributeNS(null, "viewBox", "0 0 " + viewW + " " + viewH);

		Element image = doc.createElementNS(xmlns, "image");
		image.setAttributeNS(xlinkns, "xlink:href", dataUri);
		image.setAttributeNS(null, "href", dataUri);
		image.setAttributeNS(null, "width", "100%");
		image.setAttributeNS(null, "height", "100%");
		svgRoot.appendChild(image);

		final double scale = Math.max(viewW / 1920.0, viewH / 1440.0);

		if (mediaSvgs != null) {
			for (MediaSvgElement mSvg : mediaSvgs) {
				if (mSvg.path() != null) {
					addReactHaloPath(doc, scale, svgRoot, PdfMediaScaler.scalePath(mSvg.path(), region), "#FFFFFF", false, null);
				}
			}
		}

		if (svgs != null) {
			List<Element> textElements = Lists.newArrayList();
			for (Svg svg : svgs.stream()
					.sorted((a, b) -> ComparisonChain.start()
							.compareFalseFirst(a.problemId() == highlightProbId, b.problemId() == highlightProbId)
							.compareFalseFirst(a.pitch() == highlightPitch, b.pitch() == highlightPitch)
							.compare(a.nr(), b.nr())
							.compare(a.pitch(), b.pitch())
							.result())
					.toList()) {
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
				String dash = (svg.problemSubtype() == null || svg.problemSubtype().equalsIgnoreCase("bolt")) ? String.valueOf(10 * scale) : null;

				addReactHaloPath(doc, scale, svgRoot, d, lineColor, isHighlight, dash);

				if (svg.hasAnchor()) {
					addAnchor(doc, scale, svgRoot, xAnchor, minY, lineColor, isHighlight);
				}

				String nrStr = (svg.pitch() != 0) ? svg.nr() + "-" + svg.pitch() : String.valueOf(svg.nr());
				addText(doc, textElements, scale, xNr, maxY, nrStr, isHighlight, svg.ticked(), svg.todo(), svg.dangerous());
			}
			for (Element t : textElements) svgRoot.appendChild(t);
		}

		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(writer));
		return writer.toString();
	}

	private static void addReactHaloPath(Document doc, double scale, Element parent, String d, String color, boolean isHighlight, String dash) {
		double opacity = isHighlight ? 1.0 : 0.5;

		Element pHalo = doc.createElementNS(xmlns, "path");
		pHalo.setAttributeNS(null, "d", d);
		pHalo.setAttributeNS(null, "style", "fill:none; stroke:white; stroke-opacity:" + (0.4 * opacity) + "; stroke-linecap:round; stroke-linejoin:round; stroke-width:" + (8 * scale));
		parent.appendChild(pHalo);

		Element pBorder = doc.createElementNS(xmlns, "path");
		pBorder.setAttributeNS(null, "d", d);
		String borderStyle = "fill:none; stroke:black; stroke-opacity:" + opacity + "; stroke-linecap:round; stroke-linejoin:round; stroke-width:" + (5 * scale);
		if (dash != null) borderStyle += "; stroke-dasharray:" + dash;
		pBorder.setAttributeNS(null, "style", borderStyle);
		parent.appendChild(pBorder);

		Element pCore = doc.createElementNS(xmlns, "path");
		pCore.setAttributeNS(null, "d", d);
		double coreWidth = 2.5 * scale * (isHighlight ? 1.4 : 0.9);
		String coreStyle = "fill:none; stroke-linecap:round; stroke-linejoin:round; stroke:" + color + "; stroke-opacity:" + opacity + "; stroke-width:" + coreWidth;
		if (dash != null) coreStyle += "; stroke-dasharray:" + dash;
		pCore.setAttributeNS(null, "style", coreStyle);
		parent.appendChild(pCore);
	}

	private static void addText(Document doc, List<Element> container, double scale, float x, float y, String txt, boolean isHighlight, boolean ticked, boolean todo, boolean dangerous) {
		double opacity = isHighlight ? 1.0 : 0.5;
		double fontSize = (isHighlight ? 26 : 16) * scale;
		double haloWidth = (isHighlight ? 5 : 2) * scale;

		String textColor = "#FFFFFF"; 
		if (ticked) textColor = "#21ba45"; 
		else if (todo) textColor = "#659DBD"; 
		else if (dangerous) textColor = "#FF0000"; 

		Element halo = doc.createElementNS(xmlns, "text");
		halo.setAttributeNS(null, "x", String.valueOf(x));
		halo.setAttributeNS(null, "y", String.valueOf(y));
		halo.setAttributeNS(null, "text-anchor", "middle");
		halo.setAttributeNS(null, "dominant-baseline", "central");
		halo.setAttributeNS(null, "font-size", String.valueOf(fontSize));
		halo.setAttributeNS(null, "font-family", "Arial, sans-serif");
		halo.setAttributeNS(null, "font-weight", "900");
		halo.setAttributeNS(null, "fill", "none");
		halo.setAttributeNS(null, "stroke", "#000000");
		halo.setAttributeNS(null, "stroke-opacity", String.valueOf(opacity));
		halo.setAttributeNS(null, "stroke-width", String.valueOf(haloWidth));
		halo.appendChild(doc.createTextNode(txt));
		container.add(halo);

		Element main = doc.createElementNS(xmlns, "text");
		main.setAttributeNS(null, "x", String.valueOf(x));
		main.setAttributeNS(null, "y", String.valueOf(y));
		main.setAttributeNS(null, "text-anchor", "middle");
		main.setAttributeNS(null, "dominant-baseline", "central");
		main.setAttributeNS(null, "font-size", String.valueOf(fontSize));
		main.setAttributeNS(null, "font-family", "Arial, sans-serif");
		main.setAttributeNS(null, "font-weight", "900");
		main.setAttributeNS(null, "fill", textColor);
		main.setAttributeNS(null, "fill-opacity", String.valueOf(opacity));
		main.appendChild(doc.createTextNode(txt));
		container.add(main);
	}

	private static void addAnchor(Document doc, double scale, Element parent, float x, float y, String color, boolean isHighlight) {
		double opacity = isHighlight ? 1.0 : 0.5;
		double r = 9 * scale * (isHighlight ? 1.2 : 0.6);
		Element c = doc.createElementNS(xmlns, "circle");
		c.setAttributeNS(null, "cx", String.valueOf(x));
		c.setAttributeNS(null, "cy", String.valueOf(y));
		c.setAttributeNS(null, "r", String.valueOf(r));
		c.setAttributeNS(null, "fill", color);
		c.setAttributeNS(null, "fill-opacity", String.valueOf(opacity));
		c.setAttributeNS(null, "stroke", "black");
		c.setAttributeNS(null, "stroke-opacity", String.valueOf(opacity));
		c.setAttributeNS(null, "stroke-width", String.valueOf(2.5 * scale));
		parent.appendChild(c);
	}
}
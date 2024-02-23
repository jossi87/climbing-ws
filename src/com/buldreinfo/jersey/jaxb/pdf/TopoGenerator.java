package com.buldreinfo.jersey.jaxb.pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Transformer;
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

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.IOHelper;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.SvgAnchor;
import com.buldreinfo.jersey.jaxb.model.SvgText;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TopoGenerator {
	private final static String xmlns = "http://www.w3.org/2000/svg";
	private final static String COLOR_WHITE = "#FFFFFF";
	
	public static Path generateTopo(int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs) throws FileNotFoundException, IOException, TranscoderException, TransformerException {
		Path dst = IOHelper.getPathTemp().resolve("topo").resolve(System.currentTimeMillis() + "_" + UUID.randomUUID() + ".jpg");
		IOHelper.createDirectories(dst.getParent());
		try (Reader reader = new StringReader(generateDocument(mediaId, width, height, mediaSvgs, svgs))) {
			TranscoderInput ti = new TranscoderInput(reader);
			try (OutputStream os = new FileOutputStream(dst.toString())) {
				TranscoderOutput to = new TranscoderOutput(os);
				JPEGTranscoder t = new JPEGTranscoder();
				t.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, true);
				t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 1f);
				t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, (float)width);
				t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, (float)height);
				t.addTranscodingHint(JPEGTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "*");
				t.addTranscodingHint(JPEGTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, true);
				t.addTranscodingHint(JPEGTranscoder.KEY_EXECUTE_ONLOAD, true);
				t.transcode(ti, to);
			}
		}
		return dst;
	}
	
	private static void addCircle(Document doc, Element parent, String fill, String x, String y, String strokeWidth, String r) {
		Element circle = doc.createElementNS(xmlns, "circle");
		circle.setAttributeNS(null, "stroke-linecap", "round");
		circle.setAttributeNS(null, "fill", fill);
		circle.setAttributeNS(null, "cx", x);
		circle.setAttributeNS(null, "cy", y); 
		if (strokeWidth != null) {
			circle.setAttributeNS(null, "stroke-width", strokeWidth);
		}
		circle.setAttributeNS(null, "r", r);
		parent.appendChild(circle);
	}
	
	private static void addLine(Document doc, Element parent, String x1, String y1, String x2, String y2, String strokeWidth, String stroke) {
		Element line = doc.createElementNS(xmlns, "line");
		line.setAttributeNS(null, "stroke-linecap", "round");
		line.setAttributeNS(null, "x1", x1);
		line.setAttributeNS(null, "y1", y1);
		line.setAttributeNS(null, "x2", x2);
		line.setAttributeNS(null, "y2", y2);
		line.setAttributeNS(null, "stroke-width", strokeWidth);
		line.setAttributeNS(null, "stroke", stroke);
		parent.appendChild(line);
	}
	
	private static void addPath(Document doc, int imgMax, Element parent, String svgPath, boolean dashedPath, String stroke) {
		Element path = doc.createElementNS(xmlns, "path");
		path.setAttributeNS(null, "style", "fill: none; stroke: #000000;");
		path.setAttributeNS(null, "d", svgPath);
		path.setAttributeNS(null, "stroke-width", String.valueOf(0.003 * imgMax));
		if (dashedPath) {
			path.setAttributeNS(null, "stroke-dasharray", String.valueOf(0.006 * imgMax));
		}
		path.setAttributeNS(null, "stroke-linecap", "round");
		parent.appendChild(path);
		path = doc.createElementNS(xmlns, "path");
		path.setAttributeNS(null, "style", "fill: none; stroke: " + stroke + ";");
		path.setAttributeNS(null, "d", svgPath);
		path.setAttributeNS(null, "stroke-width", String.valueOf(0.0015 * imgMax));
		if (dashedPath) {
			path.setAttributeNS(null, "stroke-dasharray", String.valueOf(0.006 * imgMax));
		}
		path.setAttributeNS(null, "stroke-linecap", "round");
		parent.appendChild(path);
	}
	
	private static void addRappel(Document doc, int imgMax, Element parent, MediaSvgElement mediaSvg) {
		final String strokeWidth = String.valueOf(0.0015*imgMax);
		final double r = 0.005*imgMax;
		final double x = mediaSvg.getRappelX();
		final double y = mediaSvg.getRappelY();
		Element g = doc.createElementNS(xmlns, "g");
		g.setAttributeNS(null, "opacity", "0.9");
		if (mediaSvg.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED)) {
			addCircle(doc, g, "none", String.valueOf(x), String.valueOf(y), strokeWidth, String.valueOf(r));
		}
		else {
			addLine(doc, g, String.valueOf(x-r), String.valueOf(y-r), String.valueOf(x+r), String.valueOf(y-r), strokeWidth, COLOR_WHITE);
			addLine(doc, g, String.valueOf(x-r), String.valueOf(y-r), String.valueOf(x), String.valueOf(y+(r*0.8)), strokeWidth, COLOR_WHITE);
			addLine(doc, g, String.valueOf(x+r), String.valueOf(y-r), String.valueOf(x), String.valueOf(y+(r*0.8)), strokeWidth, COLOR_WHITE);
		}
		addLine(doc, g, String.valueOf(x), String.valueOf(y+r), String.valueOf(x), String.valueOf(y+r+r+r), strokeWidth, COLOR_WHITE);
		addLine(doc, g, String.valueOf(x-r), String.valueOf(y+r+r), String.valueOf(x), String.valueOf(y+r+r+r), strokeWidth, COLOR_WHITE);
		addLine(doc, g, String.valueOf(x+r), String.valueOf(y+r+r), String.valueOf(x), String.valueOf(y+r+r+r), strokeWidth, COLOR_WHITE);
		parent.appendChild(g);
	}

	private static String generateDocument(int mediaId, int width, int height, List<MediaSvgElement> mediaSvgs, List<Svg> svgs) throws TransformerException {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
		Document doc = impl.createDocument(svgNS, "svg", null);
		Element svgRoot = doc.getDocumentElement();
		svgRoot.setAttributeNS(null, "overflow", "visible");
		svgRoot.setAttributeNS(null, "viewBox", "0 0 " + width + " " + height);
		svgRoot.setAttributeNS(null, "preserveAspectRatio", "xMidYMid meet");

		// Image
		Element image = doc.createElementNS(xmlns, "image");
		String url = GlobalFunctions.getUrlJpgToImage(mediaId);
		image.setAttributeNS(null, "xlink:href", url);
		image.setAttributeNS(null, "href", url);
		image.setAttributeNS(null, "height", "100%");
		image.setAttributeNS(null, "width", "100%");
		svgRoot.appendChild(image);
		
		final int imgMax = Math.max(width, height);

		if (mediaSvgs != null && !mediaSvgs.isEmpty()) {
			for (MediaSvgElement mediaSvg : mediaSvgs) {
				if (mediaSvg.getT().equals(MediaSvgElement.TYPE.PATH)) {
					addPath(doc, imgMax, svgRoot, mediaSvg.getPath(), true, COLOR_WHITE);
				}
				else if (mediaSvg.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED) || mediaSvg.getT().equals(MediaSvgElement.TYPE.RAPPEL_NOT_BOLTED)) {
					addRappel(doc, imgMax, svgRoot, mediaSvg);
				}
			}
		}
		if (svgs != null && !svgs.isEmpty()) {
			List<Element> texts = Lists.newArrayList(); // Text always on top
			for (Svg svg : svgs) {
				List<String> parts = Lists.newArrayList(Splitter.on("L").omitEmptyStrings().trimResults().split(svg.getPath().replace("M", "L").replace("C", "L")));
				float x0 = Float.parseFloat(parts.get(0).split(" ")[0]);
				float y0 = Float.parseFloat(parts.get(0).split(" ")[1]);
				String[] lastParts = parts.get(parts.size()-1).split(" ");
				float x1 = Float.parseFloat(lastParts[lastParts.length-2]);
				float y1 = Float.parseFloat(lastParts[lastParts.length-1]);
				boolean firstIsLowest = y0 > y1;
				float xMin = firstIsLowest? x0 : x1;
				float yMin = firstIsLowest? y0 : y1;
				float xMax = firstIsLowest? x1 : x0;
				float yMax = firstIsLowest? y1 : y0;
				// Init colors
				String groupColor = null;
				switch (svg.getProblemGradeGroup()) {
				case 0: groupColor = COLOR_WHITE; break;
				case 1: groupColor = "#00FF00"; break;
				case 2: groupColor = "#0000FF"; break;
				case 3: groupColor = "#FFFF00"; break;
				case 4: groupColor = "#FF0000"; break;
				case 5: groupColor = "#FF00FF"; break;
				default: groupColor = "#000000"; break;
				}
				String textColor = COLOR_WHITE;
				if (svg.isTicked()) {
					textColor = "#21ba45";
				}
				else if (svg.isTodo()) {
					textColor = "#659DBD";
				}
				else if (svg.isDangerous()) {
					textColor = "#FF0000";
				}
				// Path
				addPath(doc, imgMax, svgRoot, svg.getPath(), svg.isPrimary(), groupColor);
				// Anchor-circle
				if (svg.isHasAnchor()) {
					addCircle(doc, svgRoot, "#000000", String.valueOf(xMax), String.valueOf(yMax), null, String.valueOf(0.005 * imgMax));
					addCircle(doc, svgRoot, groupColor, String.valueOf(xMax), String.valueOf(yMax), null, String.valueOf(0.004 * imgMax));
				}
				// Nr
				final double r = 0.01*imgMax;
				Element rect = doc.createElementNS(xmlns, "rect");
				rect.setAttributeNS(null, "fill", "#000000");
				rect.setAttributeNS(null, "x", String.valueOf(xMin-r));
				rect.setAttributeNS(null, "y", String.valueOf(yMin-r));
				rect.setAttributeNS(null, "width", String.valueOf(r*2));
				rect.setAttributeNS(null, "height", String.valueOf(r*1.7));
				rect.setAttributeNS(null, "rx", String.valueOf(r/3));
				svgRoot.appendChild(rect);
				Element text = doc.createElementNS(xmlns, "text");
				text.setAttributeNS(null, "text-anchor", "middle");
				text.setAttributeNS(null, "font-size", String.valueOf(0.017 * imgMax));
				text.setAttributeNS(null, "font-weight", "bolder");
				text.setAttributeNS(null, "fill", textColor);
				text.setAttributeNS(null, "x", String.valueOf(xMin));
				text.setAttributeNS(null, "y", String.valueOf(yMin));
				text.setAttributeNS(null, "dy", String.valueOf(r/3));
				text.appendChild(doc.createTextNode(String.valueOf(svg.getNr())));
				texts.add(text);

				Gson gson = new Gson();
				// Texts
				if (!Strings.isNullOrEmpty(svg.getTexts())) {
					List<SvgText> svgTexts = gson.fromJson(svg.getTexts(), new TypeToken<ArrayList<SvgText>>(){}.getType());
					for (SvgText svgText : svgTexts) {
						text = doc.createElementNS(xmlns, "text");
						text.setAttributeNS(null, "style", "fill: #FF0000;");
						text.setAttributeNS(null, "x", String.valueOf(svgText.getX()));
						text.setAttributeNS(null, "y", String.valueOf(svgText.getY()));
						text.setAttributeNS(null, "dy", ".3em");
						text.setAttributeNS(null, "font-size", "5em");
						text.appendChild(doc.createTextNode(svgText.getTxt()));
					}
					texts.add(text);
				}
				// Anchors
				if (!Strings.isNullOrEmpty(svg.getAnchors())) {
					List<SvgAnchor> svgAnchors = gson.fromJson(svg.getAnchors(), new TypeToken<ArrayList<SvgAnchor>>(){}.getType());
					for (SvgAnchor svgAnchor : svgAnchors) {
						addCircle(doc, svgRoot, "#000000", String.valueOf(svgAnchor.getX()), String.valueOf(svgAnchor.getY()), null, String.valueOf(0.005 * imgMax));
						addCircle(doc, svgRoot, groupColor, String.valueOf(svgAnchor.getX()), String.valueOf(svgAnchor.getY()), null, String.valueOf(0.004 * imgMax));
					}
				}
			}
			for (Element text : texts) {
				svgRoot.appendChild(text);
			}
		}

		DOMSource domSource = new DOMSource(doc);
		Writer writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		String res = writer.toString();
		return res;
	}
}
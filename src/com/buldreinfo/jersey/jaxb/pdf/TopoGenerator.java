package com.buldreinfo.jersey.jaxb.pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.SvgAnchor;
import com.buldreinfo.jersey.jaxb.model.SvgText;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TopoGenerator {
	public static Path generateTopo(int mediaId, int width, int height, List<Svg> svgs) throws FileNotFoundException, IOException, TranscoderException, TransformerException {
		Path dst = Files.createTempFile("topo", "jpg");
		try (Reader reader = new StringReader(generateDocument(mediaId, width, height, svgs))) {
			TranscoderInput ti = new TranscoderInput(reader);
			try (OutputStream os = new FileOutputStream(dst.toString())) {
				TranscoderOutput to = new TranscoderOutput(os);
				JPEGTranscoder t = new JPEGTranscoder();
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

	private static String generateDocument(int mediaId, int width, int height, List<Svg> svgs) throws TransformerException {
		final String xmlns = "http://www.w3.org/2000/svg";
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

		List<Element> texts = Lists.newArrayList(); // Text always on top
		for (Svg svg : svgs) {
			final int imgMax = Math.max(width, height);
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
			case 0: groupColor = "#FFFFFF"; break;
			case 1: groupColor = "#00FF00"; break;
			case 2: groupColor = "#0000FF"; break;
			case 3: groupColor = "#FFFF00"; break;
			case 4: groupColor = "#FF0000"; break;
			case 5: groupColor = "#FF00FF"; break;
			default: groupColor = "#000000"; break;
			}
			String textColor = "#FFFFFF";
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
			Element path = doc.createElementNS(xmlns, "path");
			path.setAttributeNS(null, "style", "fill: none; stroke: #000000;");
			path.setAttributeNS(null, "d", svg.getPath());
			path.setAttributeNS(null, "stroke-width", String.valueOf(0.003 * imgMax));
			if (svg.isPrimary()) {
				path.setAttributeNS(null, "stroke-dasharray", String.valueOf(0.006 * imgMax));
			}
			path.setAttributeNS(null, "stroke-linecap", "round");
			svgRoot.appendChild(path);
			path = doc.createElementNS(xmlns, "path");
			path.setAttributeNS(null, "style", "fill: none; stroke: " + groupColor + ";");
			path.setAttributeNS(null, "d", svg.getPath());
			path.setAttributeNS(null, "stroke-width", String.valueOf(0.0015 * imgMax)); 
			path.setAttributeNS(null, "stroke-dasharray", String.valueOf(0.006 * imgMax));
			path.setAttributeNS(null, "stroke-linecap", "round");
			svgRoot.appendChild(path);
			// Anchor-circle
			if (svg.isHasAnchor()) {
				Element circle = doc.createElementNS(xmlns, "circle");
				circle.setAttributeNS(null, "fill", "#000000");
				circle.setAttributeNS(null, "cx", String.valueOf(xMax));
				circle.setAttributeNS(null, "cy", String.valueOf(yMax)); 
				circle.setAttributeNS(null, "r", String.valueOf(0.005 * imgMax));
				svgRoot.appendChild(circle);
				circle = doc.createElementNS(xmlns, "circle");
				circle.setAttributeNS(null, "fill", groupColor);
				circle.setAttributeNS(null, "cx", String.valueOf(xMax));
				circle.setAttributeNS(null, "cy", String.valueOf(yMax)); 
				circle.setAttributeNS(null, "r", String.valueOf(0.004 * imgMax));
				svgRoot.appendChild(circle);
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
					Element circle = doc.createElementNS(xmlns, "circle");
					circle.setAttributeNS(null, "fill", groupColor);
					circle.setAttributeNS(null, "cx", String.valueOf(svgAnchor.getX()));
					circle.setAttributeNS(null, "cy", String.valueOf(svgAnchor.getY())); 
					circle.setAttributeNS(null, "r", String.valueOf(0.006 * imgMax));
					svgRoot.appendChild(circle);
				}
			}
		}
		for (Element text : texts) {
			svgRoot.appendChild(text);
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
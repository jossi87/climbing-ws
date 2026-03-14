package com.buldreinfo.jersey.jaxb.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
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
		int svgViewWidth = region != null ? region.width() : origWidth;
		int svgViewHeight = region != null ? region.height() : origHeight;
		svgRoot.setAttributeNS(null, "viewBox", "0 0 " + svgViewWidth + " " + svgViewHeight);
		Element image = doc.createElementNS(xmlns, "image");
		String url = (region != null) 
				? StorageManager.getDirectStorageUrl(S3KeyGenerator.getWebJpgRegion(mediaId, region.x(), region.y(), region.width(), region.height()))
						: StorageManager.getDirectStorageUrl(S3KeyGenerator.getWebJpg(mediaId));
		image.setAttributeNS(xlinkns, "xlink:href", url);
		image.setAttributeNS(null, "href", url);
		image.setAttributeNS(null, "x", "0");
		image.setAttributeNS(null, "y", "0");
		image.setAttributeNS(null, "width", String.valueOf(svgViewWidth));
		image.setAttributeNS(null, "height", String.valueOf(svgViewHeight));
		svgRoot.appendChild(image);
		final int imgMax = Math.max(svgViewWidth, svgViewHeight);
		if (mediaSvgs != null) {
			for (MediaSvgElement mSvg : mediaSvgs) {
				if (mSvg.t().name().equals("PATH")) {
					if (region == null || PdfMediaScaler.isPathVisible(mSvg.path(), region)) {
						addPath(doc, imgMax, svgRoot, PdfMediaScaler.scalePath(mSvg.path(), region), "#FFFFFF", false);
					}
				}
			}
		}
		if (svgs != null) {
			List<Element> texts = Lists.newArrayList();
			for (Svg svg : svgs) {
				String path = PdfMediaScaler.scalePath(svg.path(), region);
				List<String> pts = Pattern.compile("L").splitAsStream(path.replace("M", "L").replace("C", "L")).map(String::trim).filter(s -> !s.isEmpty()).toList();
				if (!pts.isEmpty()) {
					float x = Float.parseFloat(pts.get(0).split(" ")[0]);
					float y = Float.parseFloat(pts.get(0).split(" ")[1]);
					boolean isHighlight = (highlightProbId <= 0) || (svg.problemId() == highlightProbId);
					String strokeColor = isHighlight ? "#FFFFFF" : "#AAAAAA"; 
					addPath(doc, imgMax, svgRoot, path, strokeColor, isHighlight);
					String label = svg.pitch() > 0 ? svg.nr() + "-" + svg.pitch() : String.valueOf(svg.nr());
					addHaloText(doc, texts, imgMax, x, y, label, isHighlight);
				}
			}
			for (Element t : texts) {
				svgRoot.appendChild(t);
			}
		}
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(domSource, new StreamResult(writer));
		return writer.toString();
	}

	private static void addHaloText(Document doc, List<Element> container, int imgMax, float x, float y, String txt, boolean isHighlight) {
		double fontSize = isHighlight ? 0.025 : 0.015;
		double strokeWidth = isHighlight ? 0.009 : 0.005;
		Element halo = doc.createElementNS(xmlns, "text");
		halo.setAttributeNS(null, "text-anchor", "middle");
		halo.setAttributeNS(null, "font-size", String.valueOf(fontSize * imgMax));
		halo.setAttributeNS(null, "font-weight", "bold");
		halo.setAttributeNS(null, "fill", "none");
		halo.setAttributeNS(null, "stroke", "#000000");
		halo.setAttributeNS(null, "stroke-width", String.valueOf(strokeWidth * imgMax));
		halo.setAttributeNS(null, "x", String.valueOf(x));
		halo.setAttributeNS(null, "y", String.valueOf(y));
		halo.appendChild(doc.createTextNode(txt));
		container.add(halo);
		Element main = doc.createElementNS(xmlns, "text");
		main.setAttributeNS(null, "text-anchor", "middle");
		main.setAttributeNS(null, "font-size", String.valueOf(fontSize * imgMax));
		main.setAttributeNS(null, "font-weight", "bold");
		main.setAttributeNS(null, "fill", isHighlight ? "#FFFFFF" : "#DDDDDD");
		main.setAttributeNS(null, "x", String.valueOf(x));
		main.setAttributeNS(null, "y", String.valueOf(y));
		main.appendChild(doc.createTextNode(txt));
		container.add(main);
	}

	private static void addPath(Document doc, int imgMax, Element parent, String svgPath, String stroke, boolean isHighlight) {
		double baseWidth = isHighlight ? 0.005 : 0.003;
		double topWidth = isHighlight ? 0.002 : 0.001;
		Element p = doc.createElementNS(xmlns, "path");
		p.setAttributeNS(null, "style", "fill: none; stroke: #000000; stroke-width: " + (baseWidth * imgMax) + ";");
		p.setAttributeNS(null, "d", svgPath);
		parent.appendChild(p);
		Element p2 = doc.createElementNS(xmlns, "path");
		p2.setAttributeNS(null, "style", "fill: none; stroke: " + stroke + "; stroke-width: " + (topWidth * imgMax) + ";");
		p2.setAttributeNS(null, "d", svgPath);
		parent.appendChild(p2);
	}
}
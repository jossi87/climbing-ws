package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SvgParser {
	class Svg {
		private String nr;
		private String nrTransform;
		private String route;
		private final Map<Float, String> rings = new TreeMap<>();
		public Svg() {
		}
		public String getNr() {
			return nr;
		}
		public void setNr(String nr) {
			this.nr = nr;
		}
		public String getRoute() {
			return route;
		}
		public void setRoute(String route) {
			this.route = route;
		}
		public Map<Float, String> getRings() {
			return rings;
		}
		public String getNrTransform() {
			return nrTransform;
		}
		public void setNrTransform(String nrTransform) {
			this.nrTransform = nrTransform;
		}
	}
	
	private static Logger logger = LogManager.getLogger();
	private static float originalW = 3072;
	private static float originalH = 2048;
	
	public SvgParser(Path svg) throws Exception {
		List<Svg> svgs = new ArrayList<>();
		float deltaW = 0;
		float deltaH = 0;
		List<String> lines = Files.readAllLines(svg);
		Svg x = null;
		for (int i = 0; i < lines.size(); i++) {
			String l = lines.get(i);
			/**
			 *  viewBox
			 */
			if (deltaW == 0 && deltaH == 0) {
				int ix = l.indexOf("viewBox=\"0 0 ");
				if (ix>0) {
					String[] str = l.substring(ix+13, l.indexOf("\" style")).split(" ");
					float w = Float.parseFloat(str[0]);
					float h = Float.parseFloat(str[1]);
					deltaW = originalW / w;
					deltaH = originalH / h;
					logger.debug("deltaW={}, deltaH={}", deltaW, deltaH);
				}
			}
			else if (l.contains("<g>")) {
				x = new Svg();
			}
			else {
				/**
				 * Route
				 */
				int ix = l.indexOf("<path class=\"route\" d=");
				if (ix > 0) {
					svgs.add(x);
					String r = l.substring(ix+24);
					int diff = 1;
					while (!r.endsWith("/>")) {
						r += lines.get(i+(diff++)).trim();
					}
					r = r.replace("\"/>", "");
					x.setRoute(scaleD(deltaW, deltaH, r).getValue());
				}
				/**
				 * Text
				 */
				ix = l.indexOf("<text transform=\"");
				if (ix > 0 && !l.contains("> <")) {
					String t = l.substring(ix+17);
					int diff = 1;
					while (!t.endsWith("</text>")) {
						t += lines.get(i+(diff++)).trim();
					}
					t = t.replace("</text>", "");
					// nr
					ix = t.indexOf("\">");
					x.setNr(t.substring(ix+2));
					// nr transform
					t = t.substring(0, t.indexOf("\" class=\"routenr"));
					String transform = "";
					boolean isW = true;
					String temp = "";
					for (int j = 0; j < t.length(); j++) {
						if (j > 14 && Character.isDigit(t.charAt(j)) || t.charAt(j)=='.') {
							temp += t.charAt(j);
						}
						else {
							if (temp.length()>0) {
								float f = Float.parseFloat(temp);
								f *= (isW? deltaW : deltaH);
								isW = !isW;
								transform += String.valueOf(f);
								temp = "";
							}
							transform += t.charAt(j);
						}
					}
					x.setNrTransform(transform);
				}
				/**
				 * Ring
				 */
				ix = l.indexOf("<path class=\"ring\" d=");
				if (ix > 0) {
					String r = l.substring(ix+23);
					int diff = 1;
					while (!r.endsWith("/>")) {
						r += lines.get(i+(diff++)).trim();
					}
					r = r.replace("\"/>", "");
					Entry<Float, String> ring = scaleD(deltaW, deltaH, r);
					x.getRings().put(ring.getKey(), ring.getValue());
				}
			}
		}
		Collections.sort(svgs, (a, b) -> a.getNr().compareTo(b.getNr()));
		for (Svg y : svgs) {
			System.out.println(String.format("nr=%s, nrTransform=%s, route=%s, rings=%s", y.getNr(), y.getNrTransform(), y.getRoute(), y.getRings()));
		}
	}
	
	private Entry<Float, String> scaleD(float deltaW, float deltaH, String d) {
		String m = "M";
		float minY = Float.MAX_VALUE;
		boolean isW = true;
		String temp = "";
		for (int j = 0; j < d.length(); j++) {
			if (Character.isDigit(d.charAt(j)) || d.charAt(j)=='.' || (j==0 && d.charAt(j)=='-')) {
				temp += d.charAt(j);
			}
			else {
				if (temp.length()>0) {
					float f = Float.parseFloat(temp);
					f *= (isW? deltaW : deltaH);
					m += String.valueOf(f < 0? 0 : f);
					temp = "";
					if (minY == Float.MAX_VALUE && !isW) {
						minY = f;
					}
					isW = !isW;
				}
				m += d.charAt(j);
			}
		}
		if (temp.length()>0) {
			float f = Float.parseFloat(temp);
			f *= (isW? deltaW : deltaH);
			isW = !isW;
			m += String.valueOf(f < 0? 0 : f);
			temp = "";
		}
		return new AbstractMap.SimpleEntry<>(minY, m);
	}

	public static void main(String[] args) throws Exception {
		Path svg = Paths.get("C:/Users/jostein/Dropbox/klatrefører/Ålgård/ålgård10.svg");
		new SvgParser(svg);
	}
}

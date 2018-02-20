package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jersey.repackaged.com.google.common.base.Preconditions;

public class SvgParser {
	public static void main(String[] args) throws Exception {
		Path svg = Paths.get("C:/Users/jostein/Dropbox/klatrefører/Bersagel/nedre gullvegg.svg");
		new SvgParser(svg);
	}
	
	public SvgParser(Path svg) throws Exception {
		List<com.buldreinfo.jersey.jaxb.batch.svg.Path> paths = new ArrayList<>();
		List<String> lines = Files.readAllLines(svg);
		double[][] matrix = null;
		for (int i = 0; i < lines.size(); i++) {
			String l = lines.get(i);
			if (l.contains("image style") && l.contains("matrix(")) {
				Preconditions.checkArgument(matrix == null);
				matrix = new double[2][3];
				String m = l.substring(l.indexOf("matrix(") + 7);
				m = m.replace(")\">", "");
				String[] parts = m.split(" ");
				Preconditions.checkArgument(parts.length == 6, "Invalid matrix: " + m);
				matrix[0][0] = Double.parseDouble(parts[0]); // a
				matrix[1][0] = Double.parseDouble(parts[1]); // b
				matrix[0][1] = Double.parseDouble(parts[2]); // c
				matrix[1][1] = Double.parseDouble(parts[3]); // d
				matrix[0][2] = Double.parseDouble(parts[4]); // e
				matrix[1][2] = Double.parseDouble(parts[5]); // f
			}
			int ix = l.indexOf("<path ");
			if (ix > -1) {
				String r = l.substring(l.indexOf(" d=\"")+4);
				int diff = 1;
				while (!r.endsWith("/>")) {
					r += lines.get(i+(diff++)).trim();
				}
				r = r.replace("\"/>", "");
				paths.add(new com.buldreinfo.jersey.jaxb.batch.svg.Path(r, matrix));
			}
			ix = l.indexOf("<polyline ");
			if (ix > -1) {
				String r = l.substring(l.indexOf(" points=\"")+9);
				int diff = 1;
				while (!r.endsWith("/>")) {
					r += lines.get(i+(diff++)).trim();
				}
				r = r.replace("\"/>", "");
				paths.add(new com.buldreinfo.jersey.jaxb.batch.svg.Path("M" + r, matrix));
			}
		}
		Collections.sort(paths, (p1, p2) -> Double.compare(p1.getMinX(), p2.getMinX()));
		for (com.buldreinfo.jersey.jaxb.batch.svg.Path p : paths) {
			System.out.println(p);
		}
	}
}

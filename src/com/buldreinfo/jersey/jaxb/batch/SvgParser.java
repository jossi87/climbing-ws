package com.buldreinfo.jersey.jaxb.batch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SvgParser {
	public SvgParser(Path svg) throws Exception {
		List<com.buldreinfo.jersey.jaxb.batch.svg.Path> paths = new ArrayList<>();
		List<String> lines = Files.readAllLines(svg);
		for (int i = 0; i < lines.size(); i++) {
			String l = lines.get(i);
			int ix = l.indexOf("<path ");
			if (ix > -1) {
				String r = l.substring(l.indexOf(" d=\"")+4);
				int diff = 1;
				while (!r.endsWith("/>")) {
					r += lines.get(i+(diff++)).trim();
				}
				r = r.replace("\"/>", "");
				
				double[][] matrix = new double[2][3];
				matrix[0][0] = 0.1532; // a
				matrix[1][0] = 0; // b
				matrix[0][1] = 0; // c
				matrix[1][1] = 0.1532; // d
				matrix[0][2] = 58; // e
				matrix[1][2] = -17.6689; // f
				paths.add(new com.buldreinfo.jersey.jaxb.batch.svg.Path(r, matrix));
			}
		}
		Collections.sort(paths, (p1, p2) -> Double.compare(p1.getMinX(), p2.getMinX()));
		for (com.buldreinfo.jersey.jaxb.batch.svg.Path p : paths) {
			System.out.println(p);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Path svg = Paths.get("C:/Users/jostein/Dropbox/klatrefører/Bersagel/Storveggen.svg");
		new SvgParser(svg);
	}
}

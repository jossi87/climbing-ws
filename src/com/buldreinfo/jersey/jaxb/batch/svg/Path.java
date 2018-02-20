package com.buldreinfo.jersey.jaxb.batch.svg;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import jersey.repackaged.com.google.common.base.Preconditions;

public class Path {
	private final String path;
	private final List<Element> elements = new ArrayList<>();
	private final double[][] matrix;
	
	public Path(String path, double[][] matrix) {
		this.matrix = matrix;
		path = path.replaceAll(",", " ");
		path = path.replaceAll("-", " -");
		this.path = path;
		String temp = "";
		for (int i = 0; i < path.length(); i++) {
			if (temp.length() > 0 && Character.isLetter(path.charAt(i))) {
				addElement(temp);
				temp = "";
			}
			temp += path.charAt(i);
		}
		addElement(temp);
	}

	public double getMinX() {
		return elements.stream().min((a, b) -> Double.compare(a.getX(), b.getX())).get().getX();
	}
	
	@Override
	public String toString() {
		for (Element e : elements) {
			e.applyMatrix(matrix);
		}
		return Joiner.on(" ").join(elements).replaceAll(",", ".");
	}

	private void addElement(String temp) {
		switch (temp.charAt(0)) {
		case 'M':
			String[] parts = temp.substring(1).trim().split(" ");
			for (int i = 0; i < parts.length; i+=2) {
				double x = Double.parseDouble(parts[i]);
				double y = Double.parseDouble(parts[i+1]);
				elements.add(new M(x, y));
			}
			break;
		case 'l':
			parts = temp.substring(1).trim().split(" ");
			for (int i = 0; i < parts.length; i+=2) {
				double startX = elements.get(elements.size()-1).getX();
				double startY = elements.get(elements.size()-1).getY();
				double x = startX + Double.parseDouble(parts[i]);
				double y = startY + Double.parseDouble(parts[i+1]);
				elements.add(new L(x, y));
			}
			break;
		case 'c':
			parts = temp.substring(1).trim().split(" ");
			for (int i = 0; i < parts.length; i+=6) {
				double startX = elements.get(elements.size()-1).getX();
				double startY = elements.get(elements.size()-1).getY();
				double x1 = startX + Double.parseDouble(parts[i]);
				double y1 = startY + Double.parseDouble(parts[i+1]);
				double x2 = startX + Double.parseDouble(parts[i+2]);
				double y2 = startY + Double.parseDouble(parts[i+3]);
				double x = startX + Double.parseDouble(parts[i+4]);
				double y = startY + Double.parseDouble(parts[i+5]);
				elements.add(new C(x1, y1, x2, y2, x, y));
			}
			break;
		case 's':
			parts = temp.substring(1).trim().split(" ");
			for (int i = 0; i < parts.length; i+=4) {
				Element prevElement = elements.get(elements.size()-1);
				double startX = prevElement.getX();
				double startY = prevElement.getY();
				double x2 = startX + Double.parseDouble(parts[i]);
				double y2 = startY + Double.parseDouble(parts[i+1]);
				double x = startX + Double.parseDouble(parts[i+2]);
				double y = startY + Double.parseDouble(parts[i+3]);
				double x1 = 0;
				double y1 = 0;
					x1 = x;
					y1 = y;
				
				elements.add(new C(x1, y1, x2, y2, x, y));
			}
			break;
		default:
			throw new RuntimeException("Invalid temp=" + temp);
		}
	}
}
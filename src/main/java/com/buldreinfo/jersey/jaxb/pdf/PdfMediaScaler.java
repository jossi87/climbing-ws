package com.buldreinfo.jersey.jaxb.pdf;

import java.util.ArrayList;
import java.util.List;

public class PdfMediaScaler {
	protected record MediaRegion(int x, int y, int width, int height) {}

	protected static MediaRegion calculateMediaRegion(String path, int mediaWidth, int mediaHeight) {
		if (path == null || path.isBlank()) {
			return null;
		}
		String[] pathLst = path.replace("  ", " ").trim().split(" ");
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = 0;
		float maxY = 0;
		for (int i = 0; i < pathLst.length; i++) {
			String part = pathLst[i];
			switch (part) {
			case "M", "L" -> {
				float x = Float.parseFloat(pathLst[i + 1]);
				float y = Float.parseFloat(pathLst[i + 2]);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				i += 2;
			}
			case "C" -> {
				float x = Float.parseFloat(pathLst[i + 5]);
				float y = Float.parseFloat(pathLst[i + 6]);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				i += 6;
			}
			}
		}
		int margin = 360;
		minX = Math.max(minX - margin, 0);
		minY = Math.max(minY - margin, 0);
		maxX = Math.min(maxX + margin, mediaWidth);
		maxY = Math.min(maxY + margin, mediaHeight);
		int width = Math.min(Math.max((int)(maxX - minX), 1920), mediaWidth);
		int height = Math.min(Math.max((int)(maxY - minY), 1080), mediaHeight);
		int finalMinX = Math.max(0, Math.min((int)minX - (width - (int)(maxX - minX)) / 2, mediaWidth - width));
		int finalMinY = Math.max(0, Math.min((int)minY - (height - (int)(maxY - minY)) / 2, mediaHeight - height));
		return new MediaRegion(finalMinX, finalMinY, width, height);
	}

	protected static boolean isPathVisible(String path, MediaRegion mediaRegion) {
		if (mediaRegion == null || path == null || path.isBlank()) {
			return true;
		}
		int regionX1 = mediaRegion.x();
		int regionY1 = mediaRegion.y();
		int regionX2 = mediaRegion.x() + mediaRegion.width();
		int regionY2 = mediaRegion.y() + mediaRegion.height();
		String[] pathLst = path.replace("  ", " ").trim().split(" ");
		float x1 = Float.parseFloat(pathLst[1]);
		float y1 = Float.parseFloat(pathLst[2]);
		float x2 = Float.parseFloat(pathLst[pathLst.length - 2]);
		float y2 = Float.parseFloat(pathLst[pathLst.length - 1]);
		return (x1 >= regionX1 && x1 <= regionX2 && y1 >= regionY1 && y1 <= regionY2) ||
				(x2 >= regionX1 && x2 <= regionX2 && y2 >= regionY1 && y2 <= regionY2);
	}

	protected static String scalePath(String path, MediaRegion mediaRegion) {
		if (mediaRegion == null || path == null || path.isBlank()) {
			return path;
		}
		String[] pathLst = path.replace("  ", " ").trim().split(" ");
		List<String> newPathLst = new ArrayList<>();
		for (int i = 0; i < pathLst.length; i++) {
			String part = pathLst[i];
			if (part.matches("[MLC]")) {
				newPathLst.add(part);
			}
			else {
				float valX = Float.parseFloat(pathLst[i++]);
				float valY = Float.parseFloat(pathLst[i]);
				newPathLst.add(String.valueOf(Math.round(valX - mediaRegion.x())));
				newPathLst.add(String.valueOf(Math.round(valY - mediaRegion.y())));
			}
		}
		return String.join(" ", newPathLst);
	}
}
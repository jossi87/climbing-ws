package com.buldreinfo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.buldreinfo.beans.StorageType;

public class FilenameUtil {
	private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	public static String generateFilename(String rawName, StorageType type) {
		String date = LocalDateTime.now().format(FILE_DATE_FORMAT);
		String safeName = sanitize(rawName);
		return String.format("%s_%s.%s", date, safeName, type.getExtension());
	}

	public static String sanitize(String name) {
		if (name == null) {
			return "download";
		}
		// Allows alphanumeric characters, Norwegian chars, dots, and hyphens
		return name.replaceAll("[^ÆØÅæøåa-zA-Z0-9.-]", "_").trim();
	}
}
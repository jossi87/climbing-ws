package com.buldreinfo.jersey.jaxb.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.base.Strings;

public class GlobalFunctions {
	public static String getFilename(String purpose, String ext) {
		purpose = removeIllegalCharacters(purpose);
		final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return String.format("%s_Buldreinfo_BratteLinjer_%s.%s", dateTime, purpose, ext);
	}
	
	public static Path getPathLeafletPrint() throws IOException {
		Path res = Paths.get("/var/lib/jenkins/workspace/climbing-web/leaflet-puppeteer-print/index.js");
		if (!Files.exists(res)) {
			throw new RuntimeException(res.toString() + " does not exists");
		}
		return res;
	}

	public static String getUrlJpgToImage(int id) {
		return "https://brattelinjer.no/buldreinfo_media/jpg/" + String.valueOf(id / 100 * 100) + "/" + id + ".jpg";
	}

	public static String stripString(String str) {
		if (str == null) {
			return null;
		}
		return Strings.emptyToNull(str.strip());
	}

	private static String removeIllegalCharacters(String str) {
		return str.trim().replaceAll("[\\\\/:*?\"<>|] ", "_");
	}
}
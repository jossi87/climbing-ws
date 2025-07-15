package com.buldreinfo.jersey.jaxb.helpers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.SystemUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;

public class GlobalFunctions {
	public static String getFilename(String purpose, String ext) {
		final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return String.format("%s_Buldreinfo_BratteLinjer_%s.%s", dateTime, removeIllegalCharacters(purpose), ext);
	}

	public static Path getPathLeafletPrint() {
		String path = SystemUtils.IS_OS_WINDOWS ?
				"C:/Users/JosteinØygarden/git/climbing-web/leaflet-puppeteer-print/index.js" :
					"/var/lib/jenkins/workspace/climbing-web/leaflet-puppeteer-print/index.js";
		Path res = Paths.get(path);
		Preconditions.checkArgument(Files.exists(res), res.toString() + " does not exist");
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

	public static boolean requestAcceptsWebp(HttpServletRequest request) {
		final String acceptHeader = request.getHeader("Accept");
		return acceptHeader != null && acceptHeader.contains("image/webp");
	}
}
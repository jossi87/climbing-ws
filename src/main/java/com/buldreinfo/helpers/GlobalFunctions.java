package com.buldreinfo.helpers;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;

public class GlobalFunctions {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	public static String getFilename(String purpose, String ext) {
		final String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return String.format("%s_Buldreinfo_BratteLinjer_%s.%s", dateTime, removeIllegalCharacters(purpose), ext);
	}

	public static boolean requestAcceptsWebm(HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader != null && acceptHeader.contains("video/webm")) {
			return true;
		}
		
		String userAgent = request.getHeader("User-Agent");
		if (userAgent != null) {
			if (userAgent.contains("Chrome") || userAgent.contains("Firefox") || userAgent.contains("Edg/")) {
				return !userAgent.contains("Like Mac OS X");
			}
		}
		return false;
	}

	public static boolean requestAcceptsWebp(HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader != null && acceptHeader.contains("image/webp")) {
			return true;
		}

		String userAgent = request.getHeader("User-Agent");
		if (userAgent != null && userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
			int idx = userAgent.indexOf("Version/");
			if (idx >= 0 && idx + 8 < userAgent.length()) {
				String versionPart = userAgent.substring(idx + 8);
				
				int spaceIdx = versionPart.indexOf(' ');
				if (spaceIdx >= 0) {
					versionPart = versionPart.substring(0, spaceIdx);
				}
				
				int dotIdx = versionPart.indexOf('.');
				String majorStr = dotIdx >= 0 ? versionPart.substring(0, dotIdx) : versionPart;
				try {
					return Integer.parseInt(majorStr) >= 14;
				} catch (NumberFormatException _) {
					return false;
				}
			}
		}
		return false;
	}

	public static String stripString(String str) {
		if (str == null) {
			return null;
		}
		return Strings.emptyToNull(str.strip());
	}
	
	private static String removeIllegalCharacters(String str) {
		if (str == null) {
			return "";
		}
		
		String cleaned = str.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
		return cleaned.replaceAll("[^\\u0000-\\uFFFF]", "").strip();
	}
}
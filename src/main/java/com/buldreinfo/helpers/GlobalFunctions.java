package com.buldreinfo.helpers;

import java.net.http.HttpClient;

import jakarta.servlet.http.HttpServletRequest;

public class GlobalFunctions {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

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
		var stripped = str.strip();
		return stripped.isBlank() ? null : stripped;
	}
}
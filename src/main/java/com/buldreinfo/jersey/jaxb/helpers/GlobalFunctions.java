package com.buldreinfo.jersey.jaxb.helpers;

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
	    String userAgent = request.getHeader("User-Agent");
	    // 1. Explicit Support: Browser says "I want Webm"
	    if (acceptHeader != null && acceptHeader.contains("video/webm")) {
	        return true;
	    }
	    // 2. Implicit Support: If it's Chrome, Firefox, or Edge, 
	    // we know they've supported Webm for a decade, even if they send */*
	    if (userAgent != null) {
	        if (userAgent.contains("Chrome") || userAgent.contains("Firefox") || userAgent.contains("Edg/")) {
	            // Check to exclude iPhones/iPads if you want to be 100% safe, 
	            // as they often report "Chrome" but use the WebKit engine.
	            return !userAgent.contains("Like Mac OS X");
	        }
	    }
	    // 3. Fallback: Default to MP4 for everything else (Safari, unknown, etc.)
	    return false;
	}

	public static boolean requestAcceptsWebp(HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader != null && acceptHeader.contains("image/webp")) {
			return true;
		}
		// Safari 14+ supports WebP but may not advertise it in Accept header
		// Detect Safari (not Chrome) and assume WebP support for recent versions
		String userAgent = request.getHeader("User-Agent");
		if (userAgent != null && userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
			// Safari 14+ on macOS/iOS supports WebP; Safari on older OS versions may not
			// Version detection: "Version/14" or higher
			int idx = userAgent.indexOf("Version/");
			if (idx >= 0) {
				String versionPart = userAgent.substring(idx + 8);
				int dotIdx = versionPart.indexOf('.');
				String majorStr = dotIdx >= 0 ? versionPart.substring(0, dotIdx) : versionPart;
				try {
					int majorVersion = Integer.parseInt(majorStr);
					return majorVersion >= 14;
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
		if (str == null) return "";
	    
	    // 1. Remove OS-illegal characters (the ones you had before)
	    String cleaned = str.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
	    
	    // 2. Remove Emojis and high-range characters
	    // This targets characters outside the standard Unicode plane
	    return cleaned.replaceAll("[^\\u0000-\\uFFFF]", "").strip();
	}
}
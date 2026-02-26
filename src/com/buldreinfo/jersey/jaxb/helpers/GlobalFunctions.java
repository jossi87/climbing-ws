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
		final String acceptHeader = request.getHeader("Accept");
		return acceptHeader != null && acceptHeader.contains("image/webp");
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
package com.buldreinfo.jersey.jaxb.helpers;

import java.text.DecimalFormat;

public class HitsFormatter {
	public static String formatHits(long hits) {
		if (hits == 0) {
			return null;
		}
		if (hits >= 1_000_000_000) {
			return formatNumber(hits, 1_000_000_000, "B");
		}
		if (hits >= 1_000_000) {
			return formatNumber(hits, 1_000_000, "M");
		}
		if (hits >= 1_000) {
			return formatNumber(hits, 1_000, "K");
		}
		return String.valueOf(hits);
	}

	private static String formatNumber(long num, long divisor, String suffix) {
		DecimalFormat df = new DecimalFormat("#.#"); // Ensures one decimal place if needed
		double result = (double) num / divisor;
		String formatted = df.format(result);

		// Remove ".0" if it's an even number
		if (formatted.endsWith(".0")) {
			return formatted.substring(0, formatted.length() - 2) + suffix;
		}
		return formatted + suffix;
	}
}

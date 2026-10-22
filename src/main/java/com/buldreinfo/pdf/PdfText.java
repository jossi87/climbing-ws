package com.buldreinfo.pdf;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * User-visible text of the generated PDFs (labels, headings, plurals), resolved from
 * {@code i18n/pdf*.properties} for the language of the request that triggered the export.
 *
 * <p>The base bundle is English, so a missing key - or an absent/unknown {@code lang} parameter -
 * falls back to English instead of failing. Callers pass the raw language code, exactly the value
 * the rest of the API already accepts; no translation logic belongs outside the bundles.
 */
public final class PdfText {
	private static final String BUNDLE_NAME = "i18n.pdf";
	/** No system-locale fallback: an unknown language must land on the base (English) bundle. */
	private static final ResourceBundle.Control CONTROL = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

	private final Locale locale;
	private final ResourceBundle bundle;

	private PdfText(Locale locale) {
		this.locale = locale;
		this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, PdfText.class.getClassLoader(), CONTROL);
	}

	/**
	 * @param languageCode the {@code lang} of the request ({@code cs}, {@code en}, {@code nb}, ...) or
	 *                     {@code null} when the request has none. Resource bundles are cached by the JDK,
	 *                     so constructing this per export is cheap.
	 */
	public static PdfText forLanguageCode(String languageCode) {
		if (languageCode == null || languageCode.isBlank()) {
			return new PdfText(Locale.ENGLISH);
		}
		return new PdfText(Locale.forLanguageTag(languageCode.strip()));
	}

	/**
	 * The language really rendered into the PDF, as a BCP 47 tag ({@code cs}, {@code en}, ...). A
	 * request for an untranslated language lands on the base bundle, which has no locale of its own
	 * and is English. Used for the {@code /Lang} entry of the PDF catalog, so viewers and screen
	 * readers know how to read the sheet.
	 */
	public String languageTag() {
		Locale resolved = bundle.getLocale();
		return (resolved.getLanguage().isEmpty() ? Locale.ENGLISH : resolved).toLanguageTag();
	}

	public String accessClosed(String reason) {
		return get("pdf.accessClosed", reason);
	}

	public String accessInfo(String info) {
		return get("pdf.accessInfo", info);
	}

	public String area() {
		return get("pdf.area");
	}

	public String ascentCount(int count) {
		return get("pdf.ascentCount", count);
	}

	public String ascents() {
		return get("pdf.ascents");
	}

	public String columnFirstAscent() {
		return get("pdf.columnFirstAscent");
	}

	public String columnGrade() {
		return get("pdf.columnGrade");
	}

	public String columnName() {
		return get("pdf.columnName");
	}

	public String columnNote() {
		return get("pdf.columnNote");
	}

	public String columnType() {
		return get("pdf.columnType");
	}

	public String comments() {
		return get("pdf.comments");
	}

	public String description() {
		return get("pdf.description");
	}

	public String firstAscent() {
		return get("pdf.firstAscent");
	}

	public String firstAscentAid() {
		return get("pdf.firstAscentAid");
	}

	/** "Page 3 of" - the total number of pages is appended by the generator. */
	public String footerPage(int page) {
		return get("pdf.footer.page", page);
	}

	public String generated() {
		return get("pdf.generated");
	}

	public String gradeDistribution() {
		return get("pdf.gradeDistribution");
	}

	public String length() {
		return get("pdf.length");
	}

	public String pitch(int nr) {
		return get("pdf.pitch", nr);
	}

	public String rock(String rock) {
		return get("pdf.rock", rock);
	}

	public String sector() {
		return get("pdf.sector");
	}

	public String url() {
		return get("pdf.url");
	}

	private String get(String key) {
		return bundle.getString(key);
	}

	/** Only parameterised keys go through MessageFormat, so plain labels never need quote escaping. */
	private String get(String key, Object... args) {
		return new MessageFormat(bundle.getString(key), locale).format(args);
	}
}

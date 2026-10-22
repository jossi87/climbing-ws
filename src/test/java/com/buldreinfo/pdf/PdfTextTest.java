package com.buldreinfo.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.BaseFont;

/**
 * Guards the PDF text bundles: a language must translate every key, translations must survive the
 * UTF-8 property file, and the embedded font must contain every glyph the Czech labels need.
 */
public class PdfTextTest {
	private static final String BUNDLE_NAME = "i18n.pdf";
	private static final String[] FONTS = { "LiberationSans-Regular.ttf", "LiberationSans-Bold.ttf", "LiberationSans-Italic.ttf" };

	private static Set<String> keys(String languageTag) {
		return new TreeSet<>(ResourceBundle.getBundle(BUNDLE_NAME, Locale.forLanguageTag(languageTag)).keySet());
	}

	@Test
	public void englishIsTheDefault() {
		for (String code : new String[] { null, "", "   ", "xx", "de" }) {
			var text = PdfText.forLanguageCode(code);
			assertEquals("Generated", text.generated(), "lang=" + code);
			assertEquals("Page 3 of", text.footerPage(3), "lang=" + code);
			assertEquals("Rock: granite", text.rock("granite"), "lang=" + code);
		}
	}

	/**
	 * The tag becomes {@code /Lang} in the PDF catalog; it must name the language that was really
	 * used, which is English whenever the request language has no bundle.
	 */
	@Test
	public void languageTagFollowsTheResolvedBundle() {
		assertEquals("cs", PdfText.forLanguageCode("cs").languageTag());
		assertEquals("cs", PdfText.forLanguageCode("cs-CZ").languageTag());
		assertEquals("en", PdfText.forLanguageCode("en").languageTag());
		assertEquals("en", PdfText.forLanguageCode(null).languageTag());
		assertEquals("en", PdfText.forLanguageCode("de").languageTag());
	}

	@Test
	public void czechIsTranslated() {
		var text = PdfText.forLanguageCode("cs");
		assertEquals("Vygenerováno", text.generated());
		assertEquals("Popis", text.description());
		assertEquals("Obtížnost", text.columnGrade());
		assertEquals("Název", text.columnName());
		assertEquals("Skála: pískovec", text.rock("pískovec"));
		assertEquals("Přelezy", text.ascents());
		assertEquals("Přístup uzavřen: hnízdění", text.accessClosed("hnízdění"));
		assertEquals("Strana 1 z", text.footerPage(1));
		assertEquals("Délka 2", text.pitch(2));
	}

	/** Czech has three plural forms; the bundled ChoiceFormat must pick them without any Java logic. */
	@Test
	public void czechPluralForms() {
		var text = PdfText.forLanguageCode("cs");
		assertEquals("0 přelezů", text.ascentCount(0));
		assertEquals("1 přelez", text.ascentCount(1));
		assertEquals("3 přelezy", text.ascentCount(3));
		assertEquals("7 přelezů", text.ascentCount(7));
	}

	@Test
	public void englishPluralForms() {
		var text = PdfText.forLanguageCode("en");
		assertEquals("1 ascent", text.ascentCount(1));
		assertEquals("5 ascents", text.ascentCount(5));
	}

	/** A language file that misses a key silently falls back to English; this keeps that from happening. */
	@Test
	public void everyLanguageTranslatesEveryKey() {
		Set<String> english = keys("en");
		assertFalse(english.isEmpty());
		assertEquals(english, keys("cs"));
		for (String key : english) {
			assertFalse(ResourceBundle.getBundle(BUNDLE_NAME, Locale.forLanguageTag("cs")).getString(key).isBlank(), key);
		}
	}

	/** Reading the property file as Latin-1 would turn "Přelezy" into "Pøelezy"; assert it does not. */
	@Test
	public void bundleIsReadAsUtf8() {
		assertTrue(PdfText.forLanguageCode("cs").comments().contains("Komentáře"));
	}

	/** Every character of every Czech label must exist in the embedded fonts, or pdf viewers drop it. */
	@Test
	public void embeddedFontsCoverTheCzechLabels() throws Exception {
		var czech = PdfText.forLanguageCode("cs");
		String labels = String.join(" ",
				czech.generated(), czech.url(), czech.description(), czech.gradeDistribution(),
				czech.accessClosed("x"), czech.accessInfo("x"), czech.area(), czech.sector(),
				czech.firstAscentAid(), czech.firstAscent(), czech.length(), czech.pitch(1),
				czech.columnName(), czech.columnGrade(), czech.columnType(), czech.columnFirstAscent(),
				czech.columnNote(), czech.rock("x"), czech.ascentCount(1), czech.ascentCount(3),
				czech.ascents(), czech.comments(), czech.footerPage(1));
		for (String font : FONTS) {
			BaseFont baseFont = loadBaseFont(font);
			for (int codePoint : labels.codePoints().toArray()) {
				assertTrue(baseFont.charExists(codePoint), font + " misses U+" + Integer.toHexString(codePoint)
						+ " (" + new String(Character.toChars(codePoint)) + ")");
			}
		}
	}

	private static BaseFont loadBaseFont(String resource) throws IOException {
		try (InputStream in = PdfTextTest.class.getResourceAsStream(resource)) {
			assertTrue(in != null, "missing font resource " + resource);
			return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
		}
	}
}

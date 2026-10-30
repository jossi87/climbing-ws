package com.buldreinfo.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.buldreinfo.beans.Setup;

/**
 * RFC 9116 {@code security.txt}, published at {@code /.well-known/security.txt} on every domain this service
 * answers: the machine-readable half of {@code SECURITY.md} in climbing-ws and climbing-web, which a reporter only
 * finds after opening the repository.
 *
 * <p>Both mandatory fields are the reason this is rendered instead of checked in as a file. {@code Contact} is the
 * address from {@code SECURITY.md}, so the two routes reach the same inbox, and {@code Expires} may be at most a
 * year out (RFC 9116 section 2.5.5) and must not be in the past — a committed file is invalid the day it is
 * written, and the scanners that read it (Cloudflare's Security Insights among them) treat a missing or expired
 * document as no document at all. Deriving the timestamp keeps the finding closed without anyone having to
 * remember to bump it.
 *
 * <p>{@code Canonical} names the domain that was asked, the way {@code /robots.txt}, {@code /sitemap.txt} and
 * {@code /.well-known/ard.json} do: one deployment answers brattelinjer.no, buldreinfo.com and their subdomains,
 * and RFC 9116 wants the URI of the copy that belongs to the host serving it.
 */
public final class SecurityTxt {
	/** Where reports go; kept identical to {@code SECURITY.md} so both routes reach the same inbox. */
	private static final String CONTACT = "mailto:security@jossi.org";

	/** Half a year: comfortably inside the one-year cap, and re-computed on every request instead of running out. */
	private static final long LIFETIME_DAYS = 180;

	/** RFC 3339/ISO 8601 at second resolution, always UTC — the shape RFC 9116 gives for {@code Expires}. */
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
			.withZone(ZoneOffset.UTC);

	private SecurityTxt() {
	}

	public static String of(Setup setup) {
		var lines = List.of(
				"Contact: " + CONTACT,
				"Expires: " + expires(),
				// Which languages the maintainer answers in, not which the sites are built in.
				"Preferred-Languages: nb, en",
				"Canonical: " + setup.url() + "/.well-known/security.txt");
		// CRLF as in other header-style documents, plus a trailing newline so the last field is a complete line.
		return String.join("\r\n", lines) + "\r\n";
	}

	private static String expires() {
		return TIMESTAMP.format(Instant.now().plus(LIFETIME_DAYS, ChronoUnit.DAYS));
	}
}

package com.buldreinfo.model;

import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.beans.Setup;

/**
 * ARD (Agentic Resource Discovery, v0.91) manifest: the document agents and registries read to find the API on
 * their own, published at {@code /.well-known/ard.json}.
 *
 * <p>Nothing about a particular site is baked into it. ARD wants the identifier's publisher to be a domain and the
 * {@code url} to be an absolute URI ({@code "format": "uri"} in {@code ard-entry.schema.json}), and one web image is
 * served from buldreinfo.com, brattelinjer.no and is.brattelinjer.no. Both come from the setup of the domain that was
 * asked ({@link Setup#domain()}, {@link Setup#url()}), the same way {@code /robots.txt}, {@code /sitemap.txt} and the
 * OpenAPI {@code servers} entry follow the request host.
 *
 * <p>The three sites therefore publish three different identifiers rather than one shared one: ARD binds the URN's
 * publisher to the authority that publishes the entry (spec 4.5.1), and the entries are genuinely different
 * artifacts — each one names the API of its own domain, so putting them on one identifier would have three documents
 * claim the same primary key.
 */
public record ArdManifest(List<Entry> entries) {
	/** Endpoint families under {@code /api}: what a registry searches on and what a client can expect. */
	private static final List<String> CAPABILITIES = List.of("areas", "sectors", "problems", "ascents", "profiles",
			"media", "sitemap");

	/** 2-5 questions the API answers, so a registry can index it by intent (spec D.2 asks for that range). */
	private static final List<String> REPRESENTATIVE_QUERIES = List.of(
			"Which areas are near Stavanger?",
			"What routes are on this sector and what grade are they?",
			"What is the access restriction or danger warning for this crag?",
			"What have people climbed there recently?");

	public static ArdManifest from(Setup setup) {
		var siteName = siteName(setup);
		var disciplines = disciplines(setup);
		return new ArdManifest(List.of(new Entry(
				// urn:air:<publisher>:<namespace>:<agent-name>, publisher being the domain that serves this document.
				"urn:air:" + setup.domain() + ":api:climbing-api",
				siteName + " API",
				"Public REST API behind " + siteName + ": areas, sectors, problems and routes, media, ascents and user "
						+ "profiles" + (disciplines.isEmpty() ? "" : ", covering " + String.join(" and ", disciplines))
						+ ". Reads are public; writes require a bearer token.",
				"application/vnd.oai.openapi+json;version=3.1",
				setup.url() + "/api/v3/api-docs",
				"1.0",
				CAPABILITIES,
				tags(disciplines),
				REPRESENTATIVE_QUERIES)));
	}

	/** The site's own name; the domain stands in for a region without one ({@code region.title} is nullable). */
	private static String siteName(Setup setup) {
		return setup.title() == null || setup.title().isBlank() ? setup.domain() : setup.title();
	}

	/** The disciplines this site covers, from the same flags that build its own front page. */
	private static List<String> disciplines(Setup setup) {
		var disciplines = new ArrayList<String>();
		if (setup.isBouldering()) {
			disciplines.add("bouldering");
		}
		if (setup.isClimbing()) {
			disciplines.add("route climbing");
		}
		if (setup.isIce()) {
			disciplines.add("ice climbing");
		}
		return disciplines;
	}

	/** Filter tokens, so each discipline becomes one word; the rest describe the transport. */
	private static List<String> tags(List<String> disciplines) {
		var tags = new ArrayList<String>();
		tags.add("climbing");
		disciplines.forEach(d -> tags.add(d.replace(' ', '-')));
		tags.addAll(List.of("norway", "rest", "openapi"));
		return tags;
	}

	/**
	 * One agentic resource. {@code url} points at the artifact document (the OpenAPI JSON); the terms around it are
	 * what a registry indexes and filters on.
	 */
	public record Entry(String identifier, String displayName, String description, String type, String url,
			String version, List<String> capabilities, List<String> tags, List<String> representativeQueries) {}
}

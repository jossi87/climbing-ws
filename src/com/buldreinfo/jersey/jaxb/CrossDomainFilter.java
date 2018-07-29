package com.buldreinfo.jersey.jaxb;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jersey.repackaged.com.google.common.collect.Lists;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CrossDomainFilter implements ContainerResponseFilter {
	private static Logger logger = LogManager.getLogger();
	private static List<String> LEGAL_ORIGINS = Lists.newArrayList(
			"https://buldreinfo.com",
			"https://buldring.bergen-klatreklubb.no",
			"https://buldring.fredrikstadklatreklubb.org",
			"https://sis.buldreinfo.com",
			"https://brattelinjer.no",
			"https://dev.jossi.org",
			"http://localhost:3000" // TODO REMOVE
			);

	@Override
	public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
		final String origin = creq.getHeaderString("origin");
		if (origin != null) {
			if (LEGAL_ORIGINS.contains(origin)) {
				cres.getHeaders().add("Access-Control-Allow-Origin", origin);
				cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
				cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
				cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
				cres.getHeaders().add("Access-Control-Max-Age", "1209600");
			}
			else {
				logger.fatal("Invalid origin: " + origin + ", creq.getHeaders()=" + creq.getHeaders());
			}
		}
	}
}
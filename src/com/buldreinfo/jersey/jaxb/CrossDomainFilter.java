package com.buldreinfo.jersey.jaxb;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.server.Server;
import com.buldreinfo.jersey.jaxb.server.Setup;
import com.google.common.collect.Sets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CrossDomainFilter implements ContainerResponseFilter {
	private static Logger logger = LogManager.getLogger();
	private static Set<String> LEGAL_ORIGINS = Sets.newHashSet();

	@Override
	public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
		String from = creq.getHeaderString("origin");
		if (from == null) {
			from = creq.getHeaderString("host");
		}
		if (from == null) {
			logger.warn("from is null, creq.getHeaders().keySet()=" + creq.getHeaders().keySet());
		}
		else {
			if (from.equals("localhost:3000")) {
				from = "http://localhost:3000";
			}
			else if (from.equals("localhost:8080")) {
				from = "http://localhost:8080";
			}
			if (!from.startsWith("http")) {
				from = "https://" + from;
			}
			
			if (LEGAL_ORIGINS.isEmpty()) {
				for (String domain : Server.getSetups()
						.stream()
						.map(Setup::getDomain)
						.collect(Collectors.toList())) {
					LEGAL_ORIGINS.add("https://" + domain);
				}
				LEGAL_ORIGINS.add("http://localhost:3000");
				LEGAL_ORIGINS.add("http://localhost:8080");
			}
			if (LEGAL_ORIGINS.contains(from)) {
				cres.getHeaders().add("Access-Control-Allow-Origin", from);
				cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
				cres.getHeaders().add("Access-Control-Expose-Headers", "Content-Disposition");
				cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
				cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
				cres.getHeaders().add("Access-Control-Max-Age", "1209600");
				cres.getHeaders().add("Vary", "Cookie");
			}
			else {
				logger.fatal("Invalid from: " + from + ", LEGAL_ORIGINS=" + LEGAL_ORIGINS);
			}
		}
	}
}
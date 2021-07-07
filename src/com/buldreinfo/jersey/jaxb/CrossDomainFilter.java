package com.buldreinfo.jersey.jaxb;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.google.common.collect.Sets;

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
			from = from.replace("https://", "").replace("http://localhost:3000","localhost:3000");
			if (LEGAL_ORIGINS.isEmpty()) {
				for (String domain : new MetaHelper().getSetups()
						.stream()
						.map(Setup::getDomain)
						.collect(Collectors.toList())) {
					LEGAL_ORIGINS.add(domain);
				}
				LEGAL_ORIGINS.add("localhost:3000");
			}
			if (LEGAL_ORIGINS.contains(from)) {
				cres.getHeaders().add("Access-Control-Allow-Origin", "https://" + from);
				cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
				cres.getHeaders().add("Access-Control-Expose-Headers", "Content-Disposition");
				cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
				cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
				cres.getHeaders().add("Access-Control-Max-Age", "1209600");
			}
			else {
				logger.fatal("Invalid from: " + from + ", LEGAL_ORIGINS=" + LEGAL_ORIGINS);
			}
		}
	}
}
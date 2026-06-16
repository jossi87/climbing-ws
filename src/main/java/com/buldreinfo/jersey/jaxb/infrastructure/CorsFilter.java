package com.buldreinfo.jersey.jaxb.infrastructure;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.google.common.collect.Sets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final Logger logger = LogManager.getLogger();
	private static final long CACHE_TTL_MS = 60_000L;

	private record CacheState(Set<String> origins, long lastRefresh) {}
	private static final AtomicReference<CacheState> cacheStateRef = new AtomicReference<>(new CacheState(Set.of(), 0L));
	
	private static Set<String> getLegalOrigins() {
		long now = System.currentTimeMillis();
		CacheState currentState = cacheStateRef.get();
		
		if (now - currentState.lastRefresh() < CACHE_TTL_MS && !currentState.origins().isEmpty()) {
			return currentState.origins();
		}
		
		synchronized (CorsFilter.class) {
			currentState = cacheStateRef.get();
			if (now - currentState.lastRefresh() < CACHE_TTL_MS && !currentState.origins().isEmpty()) {
				return currentState.origins();
			}

			Set<String> newOrigins = Sets.newHashSet();
			newOrigins.add("http://localhost:3001");
			try {
				DatabaseContext.getSetups().stream()
						.map(Setup::domain)
						.map(domain -> "https://" + domain)
						.forEach(newOrigins::add);
			} catch (Exception e) {
				logger.warn("Could not initialize legal origins from setups: {}", e.getMessage());
				if (!currentState.origins().isEmpty()) {
					return currentState.origins();
				}
			}
			
			Set<String> initialized = Set.copyOf(newOrigins);
			cacheStateRef.set(new CacheState(initialized, now));
			return initialized;
		}
	}

	@Override
	public void filter(ContainerRequestContext creq) throws IOException {
		if (creq.getMethod().equalsIgnoreCase("OPTIONS")) {
			String origin = creq.getHeaderString("Origin");
			if (origin != null && getLegalOrigins().contains(origin)) {
				Response preflightResponse = Response.ok()
						.header("Access-Control-Allow-Origin", origin)
						.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
						.header("Access-Control-Expose-Headers", "Content-Disposition")
						.header("Access-Control-Allow-Credentials", "true")
						.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
						.header("Access-Control-Max-Age", "1209600")
						.header("Vary", "Origin, Cookie")
						.build();
				creq.abortWith(preflightResponse);
			} else {
				creq.abortWith(Response.status(Response.Status.FORBIDDEN).build());
			}
		}
	}

	@Override
	public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
		if (creq.getMethod().equalsIgnoreCase("OPTIONS")) {
			return;
		}

		String origin = creq.getHeaderString("Origin");
		if (origin == null) {
			return;
		}

		if (getLegalOrigins().contains(origin)) {
			cres.getHeaders().add("Access-Control-Allow-Origin", origin);
			cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
			cres.getHeaders().add("Access-Control-Expose-Headers", "Content-Disposition");
			cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
			cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
			cres.getHeaders().add("Vary", "Origin, Cookie");
		} else {
			logger.warn("Unauthorized CORS origin request attempted: {}", origin);
		}
	}
}
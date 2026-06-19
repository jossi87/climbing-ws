package com.buldreinfo.jersey.jaxb.infrastructure;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	private static final String LOCAL_DEV_ORIGIN = "http://localhost:3001";
	private static final AtomicReference<Set<String>> lastKnownGoodOrigins = new AtomicReference<>(Set.of(LOCAL_DEV_ORIGIN));
	private static final Logger logger = LogManager.getLogger();
	private static final String ORIGIN = "Origin";
	private final Supplier<Set<String>> legalOriginsSupplier;

	@Inject
	public CorsFilter(TransactionManager txManager, RegionRepository regionRepo) {
		this.legalOriginsSupplier = Suppliers.memoizeWithExpiration(
				() -> {
					Set<String> newOrigins = Sets.newHashSet(LOCAL_DEV_ORIGIN);
					try {
						txManager.executeInTransaction(() -> regionRepo.getSetups()).stream()
								.map(Setup::domain)
								.map(domain -> "https://" + domain)
								.forEach(newOrigins::add);
						
						Set<String> immutableSet = Set.copyOf(newOrigins);
						lastKnownGoodOrigins.set(immutableSet);
						return immutableSet;
					} catch (Exception e) {
						logger.warn("Could not initialize legal origins from setups: {}. Using stale fallback.", e.getMessage());
						return lastKnownGoodOrigins.get();
					}
				},
				60, TimeUnit.SECONDS
		);
	}

	@Override
	public void filter(ContainerRequestContext creq) throws IOException {
		if (creq.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS)) {
			String origin = creq.getHeaderString(ORIGIN);
			if (origin != null && legalOriginsSupplier.get().contains(origin)) {
				Response preflightResponse = Response.ok()
						.header(ACCESS_CONTROL_ALLOW_ORIGIN, origin)
						.header(ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization")
						.header(ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition")
						.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
						.header(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD")
						.header(ACCESS_CONTROL_MAX_AGE, "1209600")
						.header(HttpHeaders.VARY, "Origin, Cookie")
						.build();
				creq.abortWith(preflightResponse);
			} else {
				creq.abortWith(Response.status(Response.Status.FORBIDDEN).build());
			}
		}
	}

	@Override
	public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {
		if (creq.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS)) {
			return;
		}

		String origin = creq.getHeaderString(ORIGIN);
		if (origin == null) {
			return;
		}

		if (legalOriginsSupplier.get().contains(origin)) {
			cres.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			cres.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization");
			cres.getHeaders().add(ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");
			cres.getHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
			cres.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD");
			cres.getHeaders().add(HttpHeaders.VARY, "Origin, Cookie");
		} else {
			logger.warn("Unauthorized CORS origin request attempted: {}", origin);
		}
	}
}
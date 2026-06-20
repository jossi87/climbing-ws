package com.buldreinfo.infrastructure;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class CorsFilter extends HttpFilter {
	private static final long serialVersionUID = 1L;
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

    public CorsFilter(ClimbingTransactionManager txManager, RegionRepository regionRepo) {
        this.legalOriginsSupplier = Suppliers.memoizeWithExpiration(
            () -> {
                Set<String> newOrigins = Sets.newHashSet(LOCAL_DEV_ORIGIN);
                try {
                    txManager.executeInTransaction(regionRepo::getSetups).stream()
                            .map(Setup::domain)
                            .map(domain -> "https://" + domain)
                            .forEach(newOrigins::add);
                    
                    Set<String> immutableSet = Set.copyOf(newOrigins);
                    lastKnownGoodOrigins.set(immutableSet);
                    return immutableSet;
                } catch (Exception e) {
                    logger.warn("Could not initialize legal origins: {}. Using stale fallback.", e.getMessage());
                    return lastKnownGoodOrigins.get();
                }
            },
            60, TimeUnit.SECONDS
        );
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        String origin = request.getHeader(ORIGIN);
        
        if (origin != null && legalOriginsSupplier.get().contains(origin)) {
            response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization");
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");
            response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            response.setHeader(ACCESS_CONTROL_MAX_AGE, "1209600");
            response.setHeader("Vary", "Origin, Cookie");
        } else if (origin != null) {
            logger.warn("Unauthorized CORS origin request attempted: {}", origin);
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            if (origin != null && legalOriginsSupplier.get().contains(origin)) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
        }

        chain.doFilter(request, response);
    }
}
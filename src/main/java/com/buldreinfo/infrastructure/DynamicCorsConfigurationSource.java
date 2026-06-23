package com.buldreinfo.infrastructure;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {

    private static final Logger logger = LogManager.getLogger();
    private static final String LOCAL_DEV_ORIGIN = "http://localhost:3001";
    
    private final ClimbingTransactionManager txManager;
    private final RegionRepository regionRepo;
    private final Supplier<Set<String>> legalOriginsSupplier;

    public DynamicCorsConfigurationSource(ClimbingTransactionManager txManager, RegionRepository regionRepo) {
        this.txManager = txManager;
        this.regionRepo = regionRepo;
        this.legalOriginsSupplier = Suppliers.memoizeWithExpiration(this::fetchOrigins, 60, TimeUnit.SECONDS);
    }

    private Set<String> fetchOrigins() {
        Set<String> origins = Sets.newHashSet(LOCAL_DEV_ORIGIN);
        try {
            txManager.executeInTransaction(regionRepo::getSetups).stream()
                     .map(Setup::domain)
                     .map(domain -> "https://" + domain)
                     .forEach(origins::add);
        } catch (Exception e) {
            logger.error("Failed to fetch CORS origins from database. Using stale cache.", e);
        }
        return origins;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && legalOriginsSupplier.get().contains(origin)) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(origin));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
            config.setAllowedHeaders(List.of("origin", "content-type", "accept", "authorization"));
            config.setExposedHeaders(List.of("Content-Disposition"));
            config.setAllowCredentials(true);
            config.setMaxAge(1209600L);
            return config;
        }
        return null;
    }
}
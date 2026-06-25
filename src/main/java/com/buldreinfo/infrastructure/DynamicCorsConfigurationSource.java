package com.buldreinfo.infrastructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {
    private static final Logger logger = LogManager.getLogger();
    private static final String LOCAL_DEV_ORIGIN = "http://localhost:3001";
    private final ClimbingTransactionManager txManager;
    private final RegionRepository regionRepo;
    private final DynamicCorsConfigurationSource self;

    public DynamicCorsConfigurationSource(ClimbingTransactionManager txManager, 
                                          RegionRepository regionRepo,
                                          @Lazy DynamicCorsConfigurationSource self) {
        this.txManager = txManager;
        this.regionRepo = regionRepo;
        this.self = self;
    }

    @Cacheable(value = CacheConstants.CORS_CACHE_NAME, key = "'all'")
    public Set<String> fetchOrigins() {
        Set<String> origins = new HashSet<>();
        origins.add(LOCAL_DEV_ORIGIN);
        try {
            txManager.executeInTransaction(regionRepo::getSetups).stream()
                     .map(Setup::domain)
                     .map(domain -> "https://" + domain)
                     .forEach(origins::add);
        } catch (Exception e) {
            logger.error("Failed to fetch CORS origins from database.", e);
        }
        return origins;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && self.fetchOrigins().contains(origin)) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of(origin));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
            config.setAllowedHeaders(List.of("origin", "content-type", "accept", "authorization"));
            config.setExposedHeaders(List.of("Content-Disposition"));
            config.setAllowCredentials(true);
            config.setMaxAge(1209600L);
            return config;
        }
        return null;
    }
}
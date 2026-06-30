package com.buldreinfo.infrastructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {
	private static final String LOCAL_DEV_ORIGIN = "http://localhost:3001";
	private static final String LOCAL_SWAGGER_ORIGIN = "http://localhost:8080";
	private static final Logger logger = LogManager.getLogger();

	private final RegionRepository regionRepo;

	public DynamicCorsConfigurationSource(RegionRepository regionRepo) {
		this.regionRepo = regionRepo;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		if (origin != null && getAllowedOrigins().contains(origin)) {
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

	private Set<String> getAllowedOrigins() {
		Set<String> origins = new HashSet<>();
		origins.add(LOCAL_DEV_ORIGIN);
		origins.add(LOCAL_SWAGGER_ORIGIN);
		try {
			regionRepo.getSetups().stream()
			.map(Setup::domain)
			.map(domain -> "https://" + domain)
			.forEach(origins::add);
		} catch (Exception e) {
			logger.error("Failed to fetch CORS origins from database.", e);
		}
		return origins;
	}
}

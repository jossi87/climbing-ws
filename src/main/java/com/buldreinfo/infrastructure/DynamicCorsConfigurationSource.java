package com.buldreinfo.infrastructure;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.buldreinfo.service.ServerUrlService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {
	private final ServerUrlService serverUrlService;

	public DynamicCorsConfigurationSource(ServerUrlService serverUrlService) {
		this.serverUrlService = serverUrlService;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		if (origin != null && serverUrlService.getAllowedOrigins().contains(origin)) {
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

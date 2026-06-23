package com.buldreinfo.security;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.infrastructure.DynamicCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final JwtFilter jwtFilter;
	private final DynamicCorsConfigurationSource corsSource;

	public SecurityConfig(JwtFilter jwtFilter, DynamicCorsConfigurationSource corsSource) {
		this.jwtFilter = jwtFilter;
		this.corsSource = corsSource;
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager(
				CacheConstants.AUTH_CACHE_NAME,
				CacheConstants.EXISTS_CACHE_NAME,
				CacheConstants.REGION_CACHE_NAME
				);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.cors(cors -> cors.configurationSource(corsSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.anyRequest().permitAll()
						)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
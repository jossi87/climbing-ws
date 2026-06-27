package com.buldreinfo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.buldreinfo.infrastructure.DynamicCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final DynamicCorsConfigurationSource corsSource;
	private final JwtFilter jwtFilter;

	public SecurityConfig(JwtFilter jwtFilter, DynamicCorsConfigurationSource corsSource) {
		this.jwtFilter = jwtFilter;
		this.corsSource = corsSource;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) {
		return http
				.cors(cors -> cors.configurationSource(corsSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}
}
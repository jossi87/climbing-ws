package com.buldreinfo.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
	private static final Set<String> LOGGED_HEADER_ALLOWLIST = Set.of("user-agent", "x-forwarded-for", "x-real-ip", "cf-connecting-ip", "accept-language", "origin", "referer");
	private static final Set<String> REDACTED_HEADER_NAMES = Set.of(OpenApiConstants.AUTH_HEADER.toLowerCase(Locale.ROOT), "cookie", "set-cookie", "x-api-key");
	private final ObjectMapper objectMapper;
	private final ClimbingTransactionManager txManager;
	private final RegionRepository regionRepo;
	private final TokenService tokenService;

	public JwtFilter(ObjectMapper objectMapper, ClimbingTransactionManager txManager, TokenService tokenService, RegionRepository regionRepo) {
		this.objectMapper = objectMapper;
		this.txManager = txManager;
		this.tokenService = tokenService;
		this.regionRepo = regionRepo;
	}

	private String extractToken(HttpServletRequest request) {
		String authHeader = request.getHeader(OpenApiConstants.AUTH_HEADER);
		if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith(OpenApiConstants.BEARER_PREFIX)) {
			return authHeader.substring(OpenApiConstants.BEARER_PREFIX.length());
		}
		return request.getParameter(OpenApiConstants.ACCESS_TOKEN_PARAM);
	}

	private Map<String, String> getHeaders(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		Set<String> seen = new HashSet<>();
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String lower = name.toLowerCase(Locale.ROOT);
			if (REDACTED_HEADER_NAMES.contains(lower)) {
				map.put(name, "[REDACTED]");
				seen.add(lower);
			}
			else if (LOGGED_HEADER_ALLOWLIST.contains(lower)) {
				map.put(name, request.getHeader(name));
				seen.add(lower);
			}
		}
		if (!seen.contains("authorization") && request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			map.put(HttpHeaders.AUTHORIZATION, "[REDACTED]");
		}
		return map;
	}

	private Setup getSetup(HttpServletRequest request) throws Exception {
		return txManager.executeInTransaction(() -> regionRepo.getSetups().stream()
				.filter(s -> s.domain().equalsIgnoreCase(request.getServerName()))
				.findFirst()
				.orElseThrow());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String accessToken = extractToken(request);
		if (accessToken != null && !accessToken.isBlank()) {
			try {
				txManager.executeInTransaction(() -> {
					Setup setup = getSetup(request);
					String headerJson = objectMapper.writeValueAsString(getHeaders(request));
					tokenService.processAuthentication(accessToken, setup, headerJson)
					.ifPresent(userId -> {
						UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
						SecurityContextHolder.getContext().setAuthentication(auth);
					});
					return null;
				});
			} catch (Exception e) {
				logger.error("JWT Authentication failed", e);
			}
		}
		filterChain.doFilter(request, response);
	}
}